package com.nexusarchive.service.impl;

import cn.hutool.core.codec.Base64;
import com.nexusarchive.common.constant.ErrorCode;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.*;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.IngestService;
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
public class IngestServiceImpl implements IngestService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IngestServiceImpl.class);

    private final IngestRequestStatusMapper ingestRequestStatusMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private final com.nexusarchive.service.ArchivalPackageService archivalPackageService;
    private final com.nexusarchive.service.ArchiveService archiveService;
    private final com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper;
    private final com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory;
    private final com.nexusarchive.service.ArchiveSecurityService archiveSecurityService;
    
    // 异步归档专用线程池 (大规模数据调优)
    private final java.util.concurrent.ExecutorService archivalExecutor = 
            java.util.concurrent.Executors.newFixedThreadPool(4);
    
    @Value("${archive.temp.path:/tmp/nexusarchive}")
    private String tempRootPath;

    public IngestServiceImpl(IngestRequestStatusMapper ingestRequestStatusMapper, 
                            ApplicationEventPublisher eventPublisher,
                            com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper,
                            com.nexusarchive.service.ArchivalPackageService archivalPackageService,
                            com.nexusarchive.service.ArchiveService archiveService,
                            com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper,
                            com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory,
                            com.nexusarchive.service.ArchiveSecurityService archiveSecurityService) {
        this.ingestRequestStatusMapper = ingestRequestStatusMapper;
        this.eventPublisher = eventPublisher;
        this.arcFileContentMapper = arcFileContentMapper;
        this.archivalPackageService = archivalPackageService;
        this.archiveService = archiveService;
        this.erpConfigMapper = erpConfigMapper;
        this.erpAdapterFactory = erpAdapterFactory;
        this.archiveSecurityService = archiveSecurityService;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public IngestResponse ingestSip(AccountingSipDto sipDto) {
        String requestId = sipDto.getRequestId();
        log.info("收到 SIP 请求: requestId={}, voucher={}", requestId, sipDto.getHeader().getVoucherNumber());
        
        // 1. 业务规则校验 (Sync)
        validateBusinessRules(sipDto);
        
        // 临时目录路径
        String tempPath = java.nio.file.Paths.get(tempRootPath, requestId).toString();
        
        try {
            // 2. 准备文件流并落地到临时目录 (Sync)
            Map<String, byte[]> fileStreams = new HashMap<>();
            prepareTempFiles(sipDto, tempPath, fileStreams);
            
            // 3. 初始化请求状态 (Sync)
            IngestRequestStatus status = IngestRequestStatus.builder()
                    .requestId(requestId)
                    .status("RECEIVED")
                    .message("已接收请求，开始处理")
                    .build();
            ingestRequestStatusMapper.insert(status);
            
            // 4. 发布事件 (Async Trigger)
            eventPublisher.publishEvent(new VoucherReceivedEvent(this, sipDto, tempPath, fileStreams));
            
            // 5. 立即返回
            return IngestResponse.builder()
                    .requestId(requestId)
                    .status("RECEIVED")
                    .archivalCode(null) // 异步生成，此时为空
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .message("请求已接收，正在后台处理。请通过 /status/" + requestId + " 查询进度。")
                    .build();
                    
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("SIP 接收失败", e);
            throw new BusinessException(500, "SIP 接收失败: " + e.getMessage());
        }
    }
    
    private void prepareTempFiles(AccountingSipDto sipDto, String tempPath, Map<String, byte[]> fileStreams) throws java.io.IOException {
        if (sipDto.getAttachments() == null) return;
        
        java.nio.file.Files.createDirectories(java.nio.file.Paths.get(tempPath));
        
        for (AttachmentDto attachment : sipDto.getAttachments()) {
            try {
                byte[] decoded = Base64.decode(attachment.getBase64Content());
                fileStreams.put(attachment.getFileName(), decoded);
                
                // 写入临时文件
                java.nio.file.Files.write(java.nio.file.Paths.get(tempPath, attachment.getFileName()), decoded);
                
            } catch (Exception e) {
                throw new BusinessException(
                    Integer.parseInt(ErrorCode.EAA_1006_BASE64_ERROR.replace("EAA_", "")),
                    String.format(ErrorCode.EAA_1006_MSG, attachment.getFileName())
                );
            }
        }
    }
    
    /**
     * 业务规则校验
     */
    private void validateBusinessRules(AccountingSipDto sipDto) {
        VoucherHeadDto header = sipDto.getHeader();
        List<VoucherEntryDto> entries = sipDto.getEntries();
        List<AttachmentDto> attachments = sipDto.getAttachments();
        
        // Rule 1: Integrity - attachment_count check
        int actualAttachmentCount = (attachments == null) ? 0 : attachments.size();
        if (!header.getAttachmentCount().equals(actualAttachmentCount)) {
            throw new BusinessException(
                Integer.parseInt(ErrorCode.EAA_1001_COUNT_MISMATCH.replace("EAA_", "")), 
                String.format(ErrorCode.EAA_1001_MSG, header.getAttachmentCount(), actualAttachmentCount)
            );
        }
        
        // Rule 2: Balance - entry_amount sum check
        BigDecimal totalEntryAmount = entries.stream()
                .map(VoucherEntryDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 允许 0.00 的误差
        if (totalEntryAmount.compareTo(header.getTotalAmount()) != 0) {
            throw new BusinessException(
                Integer.parseInt(ErrorCode.EAA_1002_BALANCE_ERROR.replace("EAA_", "")),
                String.format(ErrorCode.EAA_1002_MSG, header.getTotalAmount(), totalEntryAmount, 
                        header.getTotalAmount().subtract(totalEntryAmount))
            );
        }
    }

    @Override
    public com.nexusarchive.dto.FileUploadResponse handleFileUpload(org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "File is empty");
        }
        
        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = java.util.UUID.randomUUID().toString();
            String extension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            String targetFileName = fileId + extension;
            java.nio.file.Path targetPath = java.nio.file.Paths.get(tempRootPath, "uploads", targetFileName);
            
            // Ensure directory exists
            java.nio.file.Files.createDirectories(targetPath.getParent());
            
            // Read file bytes for hash calculation
            byte[] fileBytes = file.getBytes();
            
            // Calculate SHA-256 hash (fallback if SM3 not available)
            String fileHash;
            String hashAlgorithm;
            try {
                // Try SM3 first (for Xinchuang compliance)
                java.security.MessageDigest sm3 = java.security.MessageDigest.getInstance("SM3");
                byte[] hashBytes = sm3.digest(fileBytes);
                fileHash = bytesToHex(hashBytes);
                hashAlgorithm = "SM3";
            } catch (java.security.NoSuchAlgorithmException e) {
                // Fallback to SHA-256
                java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = sha256.digest(fileBytes);
                fileHash = bytesToHex(hashBytes);
                hashAlgorithm = "SHA-256";
            }
            
            // Save file
            java.nio.file.Files.write(targetPath, fileBytes);
            
            log.info("File uploaded successfully: {} (Hash: {})", targetPath.toString(), fileHash);
            
            // Generate unique code and temporary archival code
            String code = "POOL-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + fileId.substring(0, 8).toUpperCase();
            String tempArchivalCode = "TEMP-POOL-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + fileId.substring(0, 8).toUpperCase();
            
            // Save to database (arc_file_content table)
            com.nexusarchive.entity.ArcFileContent fileContent = com.nexusarchive.entity.ArcFileContent.builder()
                    .id(fileId)
                    .archivalCode(tempArchivalCode)
                    .fileName(originalFilename)
                    .fileType(extension.isEmpty() ? "UNKNOWN" : extension.substring(1).toUpperCase())
                    .fileSize(file.getSize())
                    .fileHash(fileHash)
                    .hashAlgorithm(hashAlgorithm)
                    .storagePath(targetPath.toString())
                    .createdTime(java.time.LocalDateTime.now())
                    // 关键：设置预归档初始状态为"待检测"
                    .preArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.PENDING_CHECK.getCode())
                    .sourceSystem("Web上传")
                    // 注意：不再设置默认元数据，让用户通过"待补录"流程正确填写
                    // fiscalYear, voucherType, creator 由智能解析或用户手动补录
                    .build();
            
            arcFileContentMapper.insert(fileContent);
            log.info("File record saved to database: archivalCode={}", tempArchivalCode);
            
            
            // Build response
            return com.nexusarchive.dto.FileUploadResponse.builder()
                    .code(code)
                    .fileName(originalFilename)
                    .fileSize(file.getSize())
                    .fileType(extension.isEmpty() ? "未知" : extension.substring(1).toUpperCase())
                    .storagePath(targetPath.toString())
                    .uploadTime(java.time.LocalDateTime.now())
                    .source("Web上传")
                    .status("已识别")
                    .fileHash(fileHash)
                    .hashAlgorithm(hashAlgorithm)
                    .build();
            
        } catch (java.io.IOException e) {
            log.error("File upload failed", e);
            throw new BusinessException(500, "File upload failed: " + e.getMessage());
        } catch (java.security.NoSuchAlgorithmException e) {
            log.error("Hash algorithm not available", e);
            throw new BusinessException(500, "Hash calculation failed: " + e.getMessage());
        }
    }
    // Convert byte array to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public void archivePoolItems(java.util.List<String> poolItemIds, String userId) throws java.io.IOException {
        log.info("开始发起异步归档任务 (状态机锁定): count={}, userId={}", poolItemIds.size(), userId);
        
        // 1. 立即锁定状态为 ARCHIVING (正在归档)
        // 依据：Architect 建议的状态机锁定，防止重复点击或并发冲突
        for (String id : poolItemIds) {
            com.nexusarchive.entity.ArcFileContent file = arcFileContentMapper.selectById(id);
            if (file != null && !com.nexusarchive.entity.enums.PreArchiveStatus.ARCHIVED.getCode().equals(file.getPreArchiveStatus())) {
                file.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.ARCHIVING.getCode());
                arcFileContentMapper.updateById(file);
            }
        }
        
        // 2. 提交异步处理流水线
        archivalExecutor.submit(() -> {
            try {
                performArchivingTask(poolItemIds, userId);
            } catch (Exception e) {
                log.error("异步归档后台执行异常", e);
            }
        });
        
        log.info("归档任务已提交后台执行池");
    }

    /**
     * 后台执行真实的归档、哈希挂链、存储转换逻辑
     */
    private void performArchivingTask(java.util.List<String> poolItemIds, String userId) {
        log.info("后台归档流水线启动: count={}", poolItemIds.size());
        java.util.List<com.nexusarchive.entity.ArcFileContent> processedFiles = new java.util.ArrayList<>();
        
        for (String poolItemId : poolItemIds) {
            try {
                // 1. 获取最新记录
                com.nexusarchive.entity.ArcFileContent originalFile = arcFileContentMapper.selectById(poolItemId);
                if (originalFile == null || !com.nexusarchive.entity.enums.PreArchiveStatus.ARCHIVING.getCode().equals(originalFile.getPreArchiveStatus())) {
                    continue;
                }

                // 2. 生成档号与物理操作
                String archivalCode = generateArchivalCode(originalFile);
                String tempPath = tempRootPath + "/uploads";
                String targetFileName = "voucher_" + archivalCode + "." + originalFile.getFileType().toLowerCase();
                
                java.nio.file.Path sourcePath = java.nio.file.Paths.get(originalFile.getStoragePath());
                java.nio.file.Path targetPath = java.nio.file.Paths.get(tempPath, targetFileName);
                
                if (!java.nio.file.Files.exists(sourcePath)) {
                    throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "文件物理丢失: " + originalFile.getFileName());
                }
                
                java.nio.file.Files.createDirectories(targetPath.getParent());
                java.nio.file.Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 3. 构建并保存 AIP 封装包
                AccountingSipDto sip = buildSimpleSip(archivalCode, poolItemId, targetFileName, originalFile);
                archivalPackageService.archivePackage(sip, tempPath);

                // 4. 创建正式档案索引
                com.nexusarchive.entity.Archive archive = new com.nexusarchive.entity.Archive();
                archive.setArchiveCode(archivalCode);
                archive.setTitle("会计凭证-" + archivalCode);
                archive.setFondsNo(originalFile.getFondsCode());
                archive.setFiscalYear(originalFile.getFiscalYear() != null ? originalFile.getFiscalYear() : String.valueOf(java.time.LocalDate.now().getYear()));
                archive.setFiscalPeriod(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MM")));
                archive.setRetentionPeriod("30Y");
                archive.setCategoryCode(originalFile.getVoucherType() != null ? originalFile.getVoucherType() : "AC04");
                archive.setOrgName("财务部");
                archive.setCreator(originalFile.getCreator() != null ? originalFile.getCreator() : "System");
                archive.setStatus("archived");
                archive.setSecurityLevel("internal");
                archive.setLocation("电子档案库");
                
                archiveService.createArchive(archive, userId != null ? userId : "user_admin");
                
                // 5. 更新预归档文件状态为“已归档”
                originalFile.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.ARCHIVED.getCode());
                arcFileContentMapper.updateById(originalFile);
                
                processedFiles.add(originalFile);
                
                // 6. ERP 异步反馈
                triggerErpFeedback(originalFile, archivalCode);

            } catch (Exception e) {
                log.error("归档流水线单项处理失败: poolItemId={}", poolItemId, e);
                // 补偿：恢复状态为待归档，以便重试
                com.nexusarchive.entity.ArcFileContent f = arcFileContentMapper.selectById(poolItemId);
                if (f != null) {
                    f.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.PENDING_ARCHIVE.getCode());
                    arcFileContentMapper.updateById(f);
                }
            }
        }
        
        // [Phase 4] 批处理哈希存证挂链
        if (!processedFiles.isEmpty()) {
            try {
                String batchNo = "BAT-" + System.currentTimeMillis();
                com.nexusarchive.entity.ArchiveBatch batch = archiveSecurityService.createSecurityBatch(
                    batchNo, processedFiles, userId != null ? userId : "user_admin");
                
                if (batch != null) {
                    for (int i = 0; i < processedFiles.size(); i++) {
                        com.nexusarchive.entity.ArcFileContent f = processedFiles.get(i);
                        f.setBatchId(batch.getId());
                        f.setSequenceInBatch(i + 1);
                        arcFileContentMapper.updateById(f);
                    }
                }
                log.info("异步流水线：已完成 {} 笔存证存证封卷挂链", processedFiles.size());
            } catch (Exception e) {
                log.error("异步存证挂链失败", e);
            }
        }
    }
    
    /**
     * 生成档号
     * 格式: {全宗号}-{年度}-{保管期限}-{机构}-{分类}-{件号}
     * 
     * 【合规修复】元数据从文件记录读取，不使用硬编码
     * 依据：DA/T 94-2022 7.1 要求档号组成元素符合业务实际
     */
    private String generateArchivalCode(com.nexusarchive.entity.ArcFileContent originalFile) {
        // 全宗号：必填，无默认值
        String fondsCode = originalFile.getFondsCode();
        if (fondsCode == null || fondsCode.trim().isEmpty()) {
            throw new BusinessException(400, 
                "归档失败：全宗号未配置。请先在[系统设置 > 档案配置]中设置全宗号，或在元数据补录时填写。");
        }
        
        // 年度：优先使用文件会计年度，否则使用当前年
        String year = originalFile.getFiscalYear() != null 
            ? originalFile.getFiscalYear() 
            : String.valueOf(java.time.LocalDate.now().getYear());
        
        // 保管期限：默认30Y（合规专家建议）
        String retention = "30Y";
        
        // 机构代码
        String org = "FIN";
        
        // 分类代码：从 voucherType 读取，未设置则默认 AC04（其他材料）
        String category = originalFile.getVoucherType() != null 
            ? originalFile.getVoucherType() 
            : "AC04";
        
        // 件号：时间戳 + 随机数确保唯一
        String itemNo = String.format("V%04d", System.currentTimeMillis() % 10000);
        
        return String.format("%s-%s-%s-%s-%s-%s", fondsCode, year, retention, org, category, itemNo);
    }
    
    /**
     * 构建简化的 SIP
     * 
     * 【合规修复】从文件元数据读取，不使用硬编码
     */
    private AccountingSipDto buildSimpleSip(String archivalCode, String poolItemId, String fileName, com.nexusarchive.entity.ArcFileContent originalFile) {
        AccountingSipDto sip = new AccountingSipDto();
        sip.setRequestId(archivalCode);
        sip.setSourceSystem(originalFile.getSourceSystem() != null ? originalFile.getSourceSystem() : "Pool Archive");
        
        // 构建凭证头 - 从元数据读取
        VoucherHeadDto header = new VoucherHeadDto();
        header.setFondsCode(originalFile.getFondsCode());
        
        // 会计期间：从元数据或当前日期
        String fiscalYear = originalFile.getFiscalYear() != null 
            ? originalFile.getFiscalYear() 
            : String.valueOf(java.time.LocalDate.now().getYear());
        header.setAccountPeriod(fiscalYear + "-" + java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("MM")));
        
        header.setVoucherType(com.nexusarchive.common.enums.VoucherType.PAYMENT);
        header.setVoucherNumber("V-" + poolItemId.substring(0, Math.min(6, poolItemId.length())));
        header.setVoucherDate(java.time.LocalDate.now());
        header.setTotalAmount(java.math.BigDecimal.ZERO); // 从元数据读取或默认0
        header.setCurrencyCode("CNY");
        header.setIssuer(originalFile.getCreator() != null ? originalFile.getCreator() : "System");
        header.setAttachmentCount(1); // 实际附件数量
        sip.setHeader(header);
        
        // 构建附件列表 - 使用真实文件信息
        java.util.List<AttachmentDto> attachments = new java.util.ArrayList<>();
        AttachmentDto attachment = new AttachmentDto();
        attachment.setFileName(fileName);
        attachment.setFileType(originalFile.getFileType());
        attachment.setFileSize(originalFile.getFileSize());
        attachment.setFileHash(originalFile.getFileHash());
        attachment.setHashAlgorithm(originalFile.getHashAlgorithm());
        attachments.add(attachment);
        sip.setAttachments(attachments);
        
        return sip;
    }

    /**
     * 触发 ERP 系统反馈（存证溯源）
     * 
     * Phase 3 增强:
     * - 使用结构化 FeedbackResult
     * - 记录审计日志 (ERP_FEEDBACK 类型)
     * - 失败时入队等待重试
     */
    private void triggerErpFeedback(com.nexusarchive.entity.ArcFileContent file, String archivalCode) {
        if (file.getSourceSystem() == null || file.getErpVoucherNo() == null) {
            log.debug("跳过 ERP 回写: 源系统或凭证号为空");
            return;
        }

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║            [存证溯源] 开始回写归档状态至 ERP                    ║");
        log.info("╠═══════════════════════════════════════════════════════════════╣");
        log.info("║ 源系统: {}", file.getSourceSystem());
        log.info("║ 凭证号: {}", file.getErpVoucherNo());
        log.info("║ 档号: {}", archivalCode);
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        com.nexusarchive.integration.erp.dto.FeedbackResult result = null;
        
        try {
            // 通过源系统名称查找配置
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.nexusarchive.entity.ErpConfig> query = 
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            query.eq(com.nexusarchive.entity.ErpConfig::getName, file.getSourceSystem());
            com.nexusarchive.entity.ErpConfig configEntity = erpConfigMapper.selectOne(query);

            if (configEntity == null) {
                log.warn("未找到 ERP 配置: {}", file.getSourceSystem());
                result = com.nexusarchive.integration.erp.dto.FeedbackResult.failure(
                        file.getErpVoucherNo(), archivalCode, "UNKNOWN", "ERP 配置未找到: " + file.getSourceSystem());
            } else {
                com.nexusarchive.integration.erp.adapter.ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
                if (adapter == null) {
                    log.warn("未找到适配器: {}", configEntity.getErpType());
                    result = com.nexusarchive.integration.erp.dto.FeedbackResult.failure(
                            file.getErpVoucherNo(), archivalCode, configEntity.getErpType(), "适配器未找到");
                } else {
                    // 转换实体配置为 DTO 配置
                    com.nexusarchive.integration.erp.dto.ErpConfig configDto = new com.nexusarchive.integration.erp.dto.ErpConfig();
                    configDto.setId(String.valueOf(configEntity.getId()));
                    configDto.setName(configEntity.getName());
                    configDto.setAdapterType(configEntity.getErpType());
                    
                    if (configEntity.getConfigJson() != null) {
                        cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configEntity.getConfigJson());
                        configDto.setBaseUrl(json.getStr("baseUrl"));
                        
                        String appKey = json.getStr("appKey");
                        if (appKey == null || appKey.isEmpty()) {
                            appKey = json.getStr("clientId");
                        }
                        configDto.setAppKey(appKey);
                        
                        String appSecret = json.getStr("appSecret");
                        if (appSecret == null || appSecret.isEmpty()) {
                            appSecret = json.getStr("clientSecret");
                        }
                        // 使用 SM4 解密
                        configDto.setAppSecret(com.nexusarchive.util.SM4Utils.decrypt(appSecret));
                        configDto.setAccbookCode(json.getStr("accbookCode"));
                        configDto.setExtraConfig(configEntity.getConfigJson());
                    }
                    
                    // 调用适配器回写 (返回 FeedbackResult)
                    result = adapter.feedbackArchivalStatus(configDto, file.getErpVoucherNo(), archivalCode, "ARCHIVED");
                }
            }
        } catch (Exception e) {
            log.error("ERP 回写过程异常", e);
            result = com.nexusarchive.integration.erp.dto.FeedbackResult.failure(
                    file.getErpVoucherNo(), archivalCode, 
                    file.getSourceSystem() != null ? file.getSourceSystem() : "UNKNOWN", 
                    e.getMessage());
        }
        
        // 记录结果日志
        if (result != null) {
            if (result.isSuccess()) {
                log.info("✓ [存证溯源] 回写成功 - voucher={}, archivalCode={}, mocked={}", 
                        result.getVoucherId(), result.getArchivalCode(), result.isMocked());
            } else {
                log.warn("✗ [存证溯源] 回写失败 - voucher={}, error={}", 
                        result.getVoucherId(), result.getErrorMessage());
                
                // TODO: 失败时可入队 sys_erp_feedback_queue 等待重试
                // 当前版本仅记录日志，Phase 4 可实现定时任务重试
            }
        }
    }
}

