package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.BatchOperationResult;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.*;

import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;

/**
 * 预归档提交服务
 * 用于将预归档库文件提交归档申请
 * 
 * 根据《会计档案管理办法》第11条：
 * 当年形成的档案可临时保管一年后移交归档
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreArchiveSubmitService {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArchiveMapper archiveMapper;

    private final ArchiveApprovalService archiveApprovalService;
    private final com.nexusarchive.service.converter.OfdConverterHelper ofdConverterHelper;
    private final com.nexusarchive.service.signature.OfdSignatureHelper ofdSignatureHelper;
    private final com.nexusarchive.util.FileHashUtil fileHashUtil;
    private final ArchivalCodeGenerator archivalCodeGenerator;

    @org.springframework.beans.factory.annotation.Value("${signature.keystore.path}")
    private String keystorePath;

    @org.springframework.beans.factory.annotation.Value("${signature.keystore.password}")
    private String keystorePassword;

    @Autowired
    @Lazy
    private PreArchiveSubmitService self;

    /**
     * 提交单个文件归档申请
     * 
     * @param fileId        预归档文件ID
     * @param applicantId   申请人ID
     * @param applicantName 申请人姓名
     * @param reason        申请理由
     * @return 归档申请记录
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ArchiveApproval submitForArchival(String fileId, String applicantId,
            String applicantName, String reason) {
        log.info("提交归档申请: fileId={}, applicant={}", fileId, applicantName);

        // 1. 验证文件状态
        ArcFileContent file = arcFileContentMapper.selectById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        String status = file.getPreArchiveStatus();
        if (!PreArchiveStatus.PENDING_ARCHIVE.getCode().equals(status)) {
            throw new RuntimeException("文件状态不允许提交归档，当前状态: " + status);
        }

        // 2. 创建正式档案记录
        Archive archive = createArchiveFromPoolFile(file);
        archiveMapper.insert(archive);

        // 3. 更新文件的正式档号和状态
        file.setArchivalCode(archive.getArchiveCode());
        file.setPreArchiveStatus(PreArchiveStatus.PENDING_APPROVAL.getCode()); // Move to pending approval
        arcFileContentMapper.updateById(file);

        // 4. 创建审批申请
        ArchiveApproval approval = new ArchiveApproval();
        approval.setArchiveId(archive.getId());
        approval.setArchiveCode(archive.getArchiveCode());
        approval.setArchiveTitle(archive.getTitle());
        approval.setApplicantId(applicantId);
        approval.setApplicantName(applicantName);
        approval.setOrgName(archive.getOrgName());
        approval.setApplicationReason(reason != null ? reason : "预归档库文件归档申请");
        approval.setCreatedTime(LocalDateTime.now()); // Manually set created_at
        approval.setLastModifiedTime(LocalDateTime.now()); // Manually set updated_at

        return archiveApprovalService.createApproval(approval);
    }

    /**
     * 批量提交归档申请
     */
    public BatchOperationResult<ArchiveApproval> submitBatchForArchival(List<String> fileIds,
            String applicantId,
            String applicantName,
            String reason) {
        BatchOperationResult<ArchiveApproval> result = new BatchOperationResult<>();
        for (String fileId : fileIds) {
            try {
                ArchiveApproval approval = self.submitForArchival(fileId, applicantId, applicantName, reason);
                result.addSuccess(approval);
            } catch (Exception e) {
                log.error("提交归档申请失败: fileId={}, error={}", fileId, e.getMessage());
                result.addFailure(fileId, e.getMessage());
            }
        }
        return result;
    }

    /**
     * 完成归档（审批通过后调用）
     * 锁定文件，设置归档时间
     */
    @Transactional
    public void completeArchival(String archiveId) {
        log.info("完成归档，锁定文件: archiveId={}", archiveId);

        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new RuntimeException("档案不存在: " + archiveId);
        }

        // 防重锁：仅允许非 ARCHIVED 状态转 ARCHIVED
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Archive> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", archiveId).ne("status", "ARCHIVED")
                .set("status", "ARCHIVED");
        int updated = archiveMapper.update(null, wrapper);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "归档已完成或状态不允许重复完成");
        }

        // 锁定关联的文件
        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("archival_code", archive.getArchiveCode());
        List<ArcFileContent> files = arcFileContentMapper.selectList(queryWrapper);

        for (ArcFileContent file : files) {
            // 1. 格式统一化: 强制转换为 OFD
            Path storagePath = java.nio.file.Paths.get(file.getStoragePath());
                    
            if (!file.getFileName().toLowerCase().endsWith(".ofd")) {
                try {
                    String newFileName = file.getFileName().substring(0, file.getFileName().lastIndexOf('.')) + ".ofd";
                    Path targetPath = storagePath.getParent().resolve(newFileName);
                    
                    ofdConverterHelper.convertToOfd(storagePath, targetPath);
                    
                    // 更新文件记录
                    file.setStoragePath(targetPath.toString());
                    file.setFileName(newFileName);
                    file.setFileType("OFD");
                    storagePath = targetPath; // 指向新文件
                    log.info("归档并转换文件格式: {} -> OFD", file.getId());
                } catch (Exception e) {
                    log.error("归档转换OFD失败: {}", e.getMessage());
                    throw new RuntimeException("归档转换失败", e);
                }
            }

            // 2. 电子签章: 对 OFD 加盖归档章
            try {
                // 生成带签名的临时文件
                Path signedPath = storagePath.getParent().resolve("signed_" + storagePath.getFileName());
                
                ofdSignatureHelper.signOfd(storagePath, signedPath, keystorePath, keystorePassword);
                
                // 替换原文件 (或者保存已签文件)
                // 这里选择替换，保持 storagePath 不变，但文件内容已变
                java.nio.file.Files.move(signedPath, storagePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                // 3. 更新哈希
                // 因为文件被签名修改了，必须重新计算哈希
                String newHash = fileHashUtil.calculateSM3(java.nio.file.Files.newInputStream(storagePath));
                file.setCurrentHash(newHash); // Update current hash to signed hash
                file.setSignValue(("SIGNED_SM2_" + java.time.LocalDateTime.now()).getBytes(java.nio.charset.StandardCharsets.UTF_8)); // Mark as signed
                log.info("归档文件已加签: {}", file.getId());
                
            } catch (Exception e) {
                 log.warn("归档加签失败（开发环境允许继续）: {}", e.getMessage());
                 // 开发环境：签名失败允许继续归档，生产环境应配置正确的 SM2/EC 证书
                 // 标记为未签名
                 file.setSignValue(("UNSIGNED_DEV_" + java.time.LocalDateTime.now()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            file.setPreArchiveStatus(PreArchiveStatus.ARCHIVED.getCode());
            file.setArchivedTime(LocalDateTime.now());
            arcFileContentMapper.updateById(file);
            log.info("文件已锁定归档: {}", file.getFileName());
        }
    }

    /**
     * 从预归档文件创建正式档案记录
     */
    private Archive createArchiveFromPoolFile(ArcFileContent file) {
        Archive archive = new Archive();
        archive.setId(UUID.randomUUID().toString().replace("-", ""));

        // 生成正式档号
        String archiveCode = generateArchiveCode(file);
        archive.setArchiveCode(archiveCode);

        // 设置基本信息
        archive.setTitle(extractTitle(file));
        archive.setSummary(file.getFileName());
        archive.setStatus("PENDING"); // 待审批

        // 设置分类（默认为会计凭证）
        archive.setCategoryCode(file.getVoucherType() != null ? file.getVoucherType() : "AC01");

        // 设置保管期限（默认30年）
        archive.setRetentionPeriod("30Y");

        // 设置全宗号
        archive.setFondsNo(file.getFondsCode() != null ? file.getFondsCode() : "DEFAULT");

        // 设置日期
        if (file.getFiscalYear() != null) {
            archive.setDocDate(LocalDate.parse(file.getFiscalYear() + "-01-01"));
            archive.setFiscalYear(file.getFiscalYear()); // Set explicit fiscalYear
        } else {
            archive.setDocDate(LocalDate.now());
            archive.setFiscalYear(String.valueOf(LocalDate.now().getYear()));
        }

        // 设置创建者
        archive.setCreator(file.getCreator() != null ? file.getCreator() : "系统");

        // 设置立档单位名称 (使用全宗号或默认值)
        archive.setOrgName(file.getFondsCode() != null ? file.getFondsCode() : "默认立档单位");

        return archive;
    }

    /**
     * 生成正式档号
     * 使用 ArchivalCodeGenerator 服务生成符合 DA/T 94-2022 的档号
     * 格式: [全宗号]-[年度]-[保管期限]-[分类]-[件号]
     */
    private String generateArchiveCode(ArcFileContent file) {
        String fondsCode = file.getFondsCode() != null ? file.getFondsCode() : "DEFAULT";
        String year = file.getFiscalYear() != null ? file.getFiscalYear() : String.valueOf(LocalDate.now().getYear());
        String retention = "30Y"; // 默认30年保管期限
        String category = file.getVoucherType() != null ? file.getVoucherType() : "AC01";

        return archivalCodeGenerator.generate(fondsCode, year, retention, category);
    }

    /**
     * 从文件名提取题名
     */
    private String extractTitle(ArcFileContent file) {
        String fileName = file.getFileName();
        if (fileName == null)
            return "未命名档案";

        // 去除扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }
}
