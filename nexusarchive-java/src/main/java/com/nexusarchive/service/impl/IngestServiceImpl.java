// Input: cn.hutool、Lombok、Spring Framework、Java 标准库、等
// Output: IngestServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import cn.hutool.core.codec.Base64;
import com.nexusarchive.common.constant.ErrorCode;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.*;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.IngestService;
import com.nexusarchive.util.PathSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIP 接收服务实现 (Event-Driven Refactoring)
 * 
 * 核心职责 (Sync Phase):
 * 1. 业务规则校验 (Business Rules)
 * 2. 落地临时文件 (Save to Temp)
 * 3. 记录请求状态 (Init Status)
 * 4. 发布事件 (Publish Event)
 * 5. 立即返回 (Return Immediately)
 */
@Service
public class IngestServiceImpl implements IngestService, org.springframework.beans.factory.DisposableBean {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IngestServiceImpl.class);

    private final IngestRequestStatusMapper ingestRequestStatusMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private final com.nexusarchive.service.ArchivalPackageService archivalPackageService;
    private final com.nexusarchive.service.ArchiveService archiveService;
    private final com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper;
    private final com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory;
    private final com.nexusarchive.service.ArchiveSecurityService archiveSecurityService;
    private final PathSecurityUtils pathSecurityUtils;
    private final com.nexusarchive.service.helper.IngestHelper helper;

    // [FIXED P1-4] 异步归档专用线程池 - 需要在销毁时关闭
    private java.util.concurrent.ExecutorService archivalExecutor;

    @org.springframework.beans.factory.annotation.Value("${archive.temp.path:/tmp/nexusarchive}")
    private String tempRootPath;
    
    @org.springframework.beans.factory.annotation.Value("${archive.async.pool-size:4}")
    private int poolSize;

    public IngestServiceImpl(IngestRequestStatusMapper ingestRequestStatusMapper,
            ApplicationEventPublisher eventPublisher,
            com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper,
            com.nexusarchive.service.ArchivalPackageService archivalPackageService,
            com.nexusarchive.service.ArchiveService archiveService,
            com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper,
            com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory,
            com.nexusarchive.service.ArchiveSecurityService archiveSecurityService,
            PathSecurityUtils pathSecurityUtils,
            com.nexusarchive.service.helper.IngestHelper helper) {
        this.ingestRequestStatusMapper = ingestRequestStatusMapper;
        this.eventPublisher = eventPublisher;
        this.arcFileContentMapper = arcFileContentMapper;
        this.archivalPackageService = archivalPackageService;
        this.archiveService = archiveService;
        this.erpConfigMapper = erpConfigMapper;
        this.erpAdapterFactory = erpAdapterFactory;
        this.archiveSecurityService = archiveSecurityService;
        this.pathSecurityUtils = pathSecurityUtils;
        this.helper = helper;
    }

    /**
     * [ADDED P1-4] 初始化线程池
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        this.archivalExecutor = java.util.concurrent.Executors.newFixedThreadPool(poolSize);
        log.info("Archival executor initialized with pool size: {}", poolSize);
    }

    /**
     * [ADDED P1-4] 销毁时关闭线程池
     */
    @Override
    public void destroy() throws Exception {
        if (archivalExecutor != null) {
            archivalExecutor.shutdown();
            if (!archivalExecutor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) archivalExecutor.shutdownNow();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestResponse ingestSip(AccountingSipDto sipDto) {
        String requestId = sipDto.getRequestId();
        helper.validateBusinessRules(sipDto);
        // [S2229] 路径遍历防护：使用 PathSecurityUtils 验证路径
        String tempPath = pathSecurityUtils.validateTempPath(requestId).toString();

        try {
            Map<String, byte[]> fileStreams = new HashMap<>();
            helper.prepareTempFiles(sipDto, tempPath, fileStreams);

            ingestRequestStatusMapper.insert(IngestRequestStatus.builder().requestId(requestId)
                    .fondsNo(sipDto.getHeader().getFondsCode()).status("RECEIVED").message("已接收").build());

            eventPublisher.publishEvent(new VoucherReceivedEvent(this, sipDto, tempPath, fileStreams));

            return IngestResponse.builder().requestId(requestId).status("RECEIVED")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .message("请求已接收").build();
        } catch (Exception e) {
            log.error("SIP 接收失败", e);
            throw new BusinessException(500, "SIP 接收失败: " + e.getMessage());
        }
    }

    @Override
    public com.nexusarchive.dto.FileUploadResponse handleFileUpload(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException(400, "File is empty");
        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = java.util.UUID.randomUUID().toString();
            String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            // [S2229] 路径遍历防护：使用 PathSecurityUtils 验证路径
            java.nio.file.Path targetPath = pathSecurityUtils.validateTempPath("uploads/" + fileId + ext);
            java.nio.file.Files.createDirectories(targetPath.getParent());
            byte[] fileBytes = file.getBytes();
            String fileHash = bytesToHex(java.security.MessageDigest.getInstance("SHA-256").digest(fileBytes));
            java.nio.file.Files.write(targetPath, fileBytes);

            String tempArchivalCode = "TEMP-POOL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + fileId.substring(0, 8).toUpperCase();
            arcFileContentMapper.insert(com.nexusarchive.entity.ArcFileContent.builder().id(fileId).archivalCode(tempArchivalCode)
                    .fileName(originalFilename).fileType(ext.isEmpty() ? "UNKNOWN" : ext.substring(1).toUpperCase())
                    .fileSize(file.getSize()).fileHash(fileHash).hashAlgorithm("SHA-256").storagePath(targetPath.toString())
                    .createdTime(LocalDateTime.now()).preArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.PENDING_CHECK.getCode())
                    .sourceSystem("Web上传").build());

            return com.nexusarchive.dto.FileUploadResponse.builder().code("POOL-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + fileId.substring(0, 8).toUpperCase())
                    .fileName(originalFilename).fileSize(file.getSize()).fileType(ext.isEmpty() ? "未知" : ext.substring(1).toUpperCase())
                    .storagePath(targetPath.toString()).uploadTime(LocalDateTime.now()).source("Web上传").status("已识别")
                    .fileHash(fileHash).hashAlgorithm("SHA-256").build();
        } catch (Exception e) {
            throw new BusinessException(500, "File upload failed: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void archivePoolItems(java.util.List<String> poolItemIds, String userId) {
        log.info("异步归档任务发起: count={}", poolItemIds.size());
        for (String id : poolItemIds) {
            com.nexusarchive.entity.ArcFileContent f = arcFileContentMapper.selectById(id);
            if (f == null || !com.nexusarchive.entity.enums.PreArchiveStatus.READY_TO_ARCHIVE.getCode().equals(f.getPreArchiveStatus()))
                throw new BusinessException(400, "状态不合法: " + id);
            
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.nexusarchive.entity.ArcFileContent> w = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            w.eq(com.nexusarchive.entity.ArcFileContent::getId, id).eq(com.nexusarchive.entity.ArcFileContent::getPreArchiveStatus, com.nexusarchive.entity.enums.PreArchiveStatus.READY_TO_ARCHIVE.getCode())
             .set(com.nexusarchive.entity.ArcFileContent::getPreArchiveStatus, com.nexusarchive.entity.enums.PreArchiveStatus.COMPLETED.getCode());
            if (arcFileContentMapper.update(null, w) == 0) throw new BusinessException(409, "状态冲突: " + id);
        }
        archivalExecutor.submit(() -> performArchivingTask(poolItemIds, userId));
    }

    private void performArchivingTask(java.util.List<String> poolItemIds, String userId) {
        java.util.List<com.nexusarchive.entity.ArcFileContent> processed = new java.util.ArrayList<>();
        for (String id : poolItemIds) {
            try {
                com.nexusarchive.entity.ArcFileContent f = arcFileContentMapper.selectById(id);
                if (f == null || !com.nexusarchive.entity.enums.PreArchiveStatus.COMPLETED.getCode().equals(f.getPreArchiveStatus())) continue;

                String code = helper.generateArchivalCode(f);
                String targetName = "voucher_" + code + "." + f.getFileType().toLowerCase();
                // [S2229] 路径遍历防护：使用 PathSecurityUtils 验证目标路径和源路径
                java.nio.file.Path targetPath = pathSecurityUtils.validateTempPath("uploads/" + targetName);
                java.nio.file.Files.createDirectories(targetPath.getParent());
                // 源路径来自数据库记录，使用 validateTempPath 确保其在允许的目录内
                java.nio.file.Path sourcePath = pathSecurityUtils.validateTempPath(f.getStoragePath());
                java.nio.file.Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                archivalPackageService.archivePackage(helper.buildSimpleSip(code, id, targetName, f), tempRootPath + "/uploads");

                com.nexusarchive.entity.Archive a = new com.nexusarchive.entity.Archive();
                a.setArchiveCode(code); a.setTitle("会计凭证-" + code); a.setFondsNo(f.getFondsCode());
                a.setFiscalYear(f.getFiscalYear() != null ? f.getFiscalYear() : String.valueOf(java.time.LocalDate.now().getYear()));
                a.setFiscalPeriod(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MM")));
                a.setUniqueBizId(id);
                archiveService.createArchive(a, userId != null ? userId : "user_admin");
                
                f.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.COMPLETED.getCode());
                arcFileContentMapper.updateById(f);
                processed.add(f);

                com.nexusarchive.entity.ErpConfig ec = erpConfigMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nexusarchive.entity.ErpConfig>().eq(com.nexusarchive.entity.ErpConfig::getName, f.getSourceSystem()));
                helper.triggerErpFeedback(f, code, ec);
            } catch (Exception e) {
                log.error("归档失败: {}", id, e);
                com.nexusarchive.entity.Archive ex = archiveService.getByUniqueBizId(id);
                if (ex != null) archiveService.deleteArchive(ex.getId());
                com.nexusarchive.entity.ArcFileContent rec = arcFileContentMapper.selectById(id);
                if (rec != null) { rec.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.READY_TO_ARCHIVE.getCode()); arcFileContentMapper.updateById(rec); }
            }
        }
        if (!processed.isEmpty()) archiveSecurityService.createSecurityBatch("BAT-" + System.currentTimeMillis(), processed, userId != null ? userId : "user_admin");
    }
}
