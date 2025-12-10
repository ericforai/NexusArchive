package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.BatchOperationResult;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.MetadataUpdateDTO;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.PreArchiveSubmitService;
import com.nexusarchive.service.AuditLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 电子凭证池控制器
 */
@RestController
@RequestMapping("/pool")
@RequiredArgsConstructor
@Slf4j
public class PoolController {

    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private final PreArchiveCheckService preArchiveCheckService;
    private final PreArchiveSubmitService preArchiveSubmitService;
    private final AuditLogService auditLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] SOURCE_SYSTEMS = {
            "Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"
    };

    // ===== 元数据补录 API =====

    /**
     * 获取文件详情 (包含元数据)
     * 
     * @param id 文件ID
     * @return 文件详情
     */
    @GetMapping("/detail/{id}")
    public Result<PoolItemDetailDto> getFileDetail(@PathVariable String id) {
        log.info("获取文件详情: {}", id);

        ArcFileContent file = arcFileContentMapper.selectById(id);
        if (file == null) {
            return Result.error("文件不存在");
        }

        PoolItemDetailDto dto = new PoolItemDetailDto();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setFileType(file.getFileType());
        dto.setFileSize(file.getFileSize());
        dto.setStatus(file.getPreArchiveStatus());
        dto.setCreatedTime(file.getCreatedTime());
        dto.setFiscalYear(file.getFiscalYear());
        dto.setVoucherType(file.getVoucherType());
        dto.setCreator(file.getCreator());
        dto.setFondsCode(file.getFondsCode());
        dto.setSourceSystem(file.getSourceSystem());

        return Result.success(dto);
    }

    /**
     * 更新文件元数据 (用于待补录状态)
     * 合规要求：记录审计日志 (GB/T 39784-2021)
     * 
     * @param dto     元数据更新请求
     * @param request HTTP请求 (获取用户信息)
     * @return 更新结果
     */
    @PostMapping("/metadata/update")
    public Result<String> updateMetadata(
            @RequestBody @Validated MetadataUpdateDTO dto,
            HttpServletRequest request) {
        log.info("更新文件元数据: fileId={}, reason={}", dto.getId(), dto.getModifyReason());

        ArcFileContent file = arcFileContentMapper.selectById(dto.getId());
        if (file == null) {
            return Result.error("文件不存在");
        }

        // 1. 记录修改前的值 (用于审计)
        String beforeValue = String.format(
                "fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s",
                file.getFiscalYear(), file.getVoucherType(), file.getCreator(), file.getFondsCode());

        // 2. 执行更新
        file.setFiscalYear(dto.getFiscalYear());
        file.setVoucherType(dto.getVoucherType());
        file.setCreator(dto.getCreator());
        if (dto.getFondsCode() != null && !dto.getFondsCode().isEmpty()) {
            file.setFondsCode(dto.getFondsCode());
        }

        arcFileContentMapper.updateById(file);

        // 3. 记录审计日志 (合规要求)
        String afterValue = String.format(
                "fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s",
                dto.getFiscalYear(), dto.getVoucherType(), dto.getCreator(), dto.getFondsCode());

        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        String clientIp = request.getRemoteAddr();
        try {
            auditLogService.log(
                    userId != null ? userId : "anonymous",
                    username != null ? username : "未知用户",
                    "METADATA_UPDATE",
                    "ARC_FILE_CONTENT",
                    dto.getId(),
                    "SUCCESS",
                    "元数据补录: " + dto.getModifyReason() + " | 修改前:" + beforeValue + " | 修改后:" + afterValue,
                    clientIp);
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }

        // 4. 自动重新触发四性检测
        log.info("自动触发四性检测: {}", dto.getId());
        FourNatureReport report = preArchiveCheckService.checkSingleFile(dto.getId());

        return Result.success("元数据更新成功，检测结果: " + report.getStatus());
    }

    /**
     * 文件详情 DTO
     */
    @lombok.Data
    public static class PoolItemDetailDto {
        private String id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String status;
        private LocalDateTime createdTime;
        private String fiscalYear;
        private String voucherType;
        private String creator;
        private String fondsCode;
        private String sourceSystem;
    }

    /**
     * 查询电子凭证池列表
     * 
     * @return 凭证池列表
     */
    @GetMapping("/list")
    public Result<List<PoolItemDto>> listPoolItems() {
        log.info("查询电子凭证池列表");

        // 查询所有临时档号开头的记录
        // 查询所有预归档相关记录 (临时档号 或 已有预归档状态的正式档号)
        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.and(w -> w.likeRight("archival_code", "TEMP-POOL-")
                .or()
                .isNotNull("pre_archive_status"))
                .orderByDesc("created_time");

        List<ArcFileContent> fileContents = arcFileContentMapper.selectList(queryWrapper);

        // 转换为前端需要的格式
        List<PoolItemDto> poolItems = fileContents.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());

        log.info("查询到 {} 条电子凭证池记录", poolItems.size());
        return Result.success(poolItems);
    }

    /**
     * 按状态查询预归档文件
     * 
     * @param status 状态:
     *               PENDING_CHECK/CHECK_FAILED/PENDING_METADATA/PENDING_ARCHIVE/ARCHIVED
     * @return 文件列表
     */
    @GetMapping("/list/status/{status}")
    public Result<List<PoolItemDto>> listByStatus(@PathVariable String status) {
        log.info("按状态查询预归档文件: {}", status);

        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("pre_archive_status", status)
                .orderByDesc("created_time");

        List<ArcFileContent> fileContents = arcFileContentMapper.selectList(queryWrapper);

        List<PoolItemDto> poolItems = fileContents.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());

        log.info("状态 {} 共有 {} 条记录", status, poolItems.size());
        return Result.success(poolItems);
    }

    /**
     * 统计各状态数量
     * 
     * @return 各状态计数
     */
    @GetMapping("/stats/status")
    public Result<java.util.Map<String, Long>> getStatusStats() {
        log.info("统计预归档各状态数量");

        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        String[] statuses = { "PENDING_CHECK", "CHECK_FAILED", "PENDING_METADATA", "PENDING_ARCHIVE",
                "PENDING_APPROVAL", "ARCHIVED" };

        for (String status : statuses) {
            QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("pre_archive_status", status);
            Long count = arcFileContentMapper.selectCount(queryWrapper);
            stats.put(status, count);
        }

        // 统计无状态的记录（旧数据）
        QueryWrapper<ArcFileContent> nullStatusQuery = new QueryWrapper<>();
        nullStatusQuery.likeRight("archival_code", "TEMP-POOL-")
                .isNull("pre_archive_status");
        Long nullCount = arcFileContentMapper.selectCount(nullStatusQuery);
        stats.put("NO_STATUS", nullCount);

        return Result.success(stats);
    }

    /**
     * 更新预归档状态
     * 
     * @param id     文件ID
     * @param status 新状态
     * @return 结果
     */
    @GetMapping("/status/{id}/{status}")
    public Result<String> updateStatus(@PathVariable String id, @PathVariable String status) {
        log.info("更新文件状态: {} -> {}", id, status);

        ArcFileContent fileContent = arcFileContentMapper.selectById(id);
        if (fileContent == null) {
            return Result.error("文件不存在");
        }

        fileContent.setPreArchiveStatus(status);

        // 记录状态变更时间
        if ("ARCHIVED".equals(status)) {
            fileContent.setArchivedTime(LocalDateTime.now());
        }

        arcFileContentMapper.updateById(fileContent);

        log.info("文件 {} 状态已更新为 {}", id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 执行单个文件四性检测
     * 
     * @param id 文件ID
     * @return 检测报告
     */
    @GetMapping("/check/{id}")
    public Result<FourNatureReport> checkSingleFile(@PathVariable String id) {
        log.info("执行四性检测: {}", id);
        FourNatureReport report = preArchiveCheckService.checkSingleFile(id);
        return Result.success(report);
    }

    /**
     * 批量执行四性检测
     * 
     * @param fileIds 文件ID列表
     * @return 检测报告列表
     */
    @PostMapping("/check/batch")
    public Result<java.util.List<FourNatureReport>> checkBatchFiles(@RequestBody java.util.List<String> fileIds) {
        log.info("批量执行四性检测: {} 个文件", fileIds.size());
        java.util.List<FourNatureReport> reports = preArchiveCheckService.checkMultipleFiles(fileIds);
        return Result.success(reports);
    }

    /**
     * 检测所有待检测文件
     * 
     * @return 检测报告列表
     */
    @GetMapping("/check/all-pending")
    public Result<java.util.List<FourNatureReport>> checkAllPendingFiles() {
        log.info("检测所有待检测文件");
        QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("archival_code", "TEMP-POOL-")
                .and(w -> w.isNull("pre_archive_status")
                        .or().eq("pre_archive_status", "PENDING_CHECK"));

        java.util.List<ArcFileContent> pendingFiles = arcFileContentMapper.selectList(queryWrapper);
        java.util.List<String> fileIds = pendingFiles.stream()
                .map(ArcFileContent::getId)
                .collect(Collectors.toList());

        log.info("找到 {} 个待检测文件", fileIds.size());
        java.util.List<FourNatureReport> reports = preArchiveCheckService.checkMultipleFiles(fileIds);
        return Result.success(reports);
    }

    /**
     * 提交单个文件归档申请
     * 
     * @param id      文件ID
     * @param request 申请信息
     * @return 审批记录
     */
    @PostMapping("/submit/{id}")
    public Result<ArchiveApproval> submitForArchival(
            @PathVariable String id,
            @RequestBody SubmitRequest request) {
        log.info("提交归档申请: fileId={}", id);
        try {
            ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                    id, request.getApplicantId(), request.getApplicantName(), request.getReason());
            return Result.success(approval);
        } catch (Exception e) {
            log.error("提交归档申请失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 批量提交归档申请
     * 
     * @param request 批量申请信息
     * @return 审批记录列表
     */
    @PostMapping("/submit/batch")
    public Result<BatchOperationResult<ArchiveApproval>> submitBatchForArchival(
            @RequestBody BatchSubmitRequest request) {
        log.info("批量提交归档申请: {} 个文件", request.getFileIds().size());
        try {
            BatchOperationResult<ArchiveApproval> result = preArchiveSubmitService.submitBatchForArchival(
                    request.getFileIds(), request.getApplicantId(),
                    request.getApplicantName(), request.getReason());
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量提交归档申请失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 完成归档（审批通过后调用）
     * 
     * @param archiveId 档案ID
     * @return 结果
     */
    @PostMapping("/complete/{archiveId}")
    public Result<String> completeArchival(@PathVariable String archiveId) {
        log.info("完成归档: archiveId={}", archiveId);
        try {
            preArchiveSubmitService.completeArchival(archiveId);
            return Result.success("归档完成");
        } catch (Exception e) {
            log.error("完成归档失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 提交申请请求DTO
     */
    @lombok.Data
    public static class SubmitRequest {
        private String applicantId;
        private String applicantName;
        private String reason;
    }

    /**
     * 批量提交申请请求DTO
     */
    @lombok.Data
    public static class BatchSubmitRequest {
        private java.util.List<String> fileIds;
        private String applicantId;
        private String applicantName;
        private String reason;
    }

    /**
     * 预览文件
     * 
     * @param id 文件ID
     * @return 文件流
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable String id) {
        log.info("请求预览文件: {}", id);

        ArcFileContent fileContent = arcFileContentMapper.selectById(id);
        if (fileContent == null) {
            log.error("文件不存在: {}", id);
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(fileContent.getStoragePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                String fileName = fileContent.getFileName().toLowerCase();
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".ofd")) {
                    contentType = "application/ofd";
                } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".xml")) {
                    contentType = "text/xml";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileContent.getFileName() + "\"")
                        .body(resource);
            } else {
                log.error("文件无法读取: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("文件路径错误", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成演示数据
     * 
     * @return 结果
     */
    @GetMapping("/generate-demo")
    public Result<String> generateDemoData() {
        log.info("开始生成演示数据...");

        try {
            ClassPathResource templateResource = new ClassPathResource("templates/default_voucher.pdf");
            if (!templateResource.exists()) {
                return Result.error("模板文件不存在(classpath): templates/default_voucher.pdf");
            }
            Random random = new Random();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateStr = LocalDateTime.now().format(dateFormatter);

            // 1. 清理旧的演示数据
            QueryWrapper<ArcFileContent> queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("file_hash", "DEMO_HASH_");
            List<ArcFileContent> oldFiles = arcFileContentMapper.selectList(queryWrapper);

            // 删除元数据
            if (!oldFiles.isEmpty()) {
                List<String> oldFileIds = oldFiles.stream().map(ArcFileContent::getId).collect(Collectors.toList());
                arcFileMetadataIndexMapper.delete(new QueryWrapper<ArcFileMetadataIndex>().in("file_id", oldFileIds));
            }

            int deletedCount = arcFileContentMapper.delete(queryWrapper);
            log.info("已清理 {} 条旧演示数据", deletedCount);

            // 2. 生成新数据
            for (int i = 0; i < 10; i++) {
                String fileId = UUID.randomUUID().toString();
                String targetFileName = fileId + ".pdf";
                Path targetPath = Paths.get("/tmp/nexusarchive/uploads", targetFileName);

                // 确保目录存在
                Files.createDirectories(targetPath.getParent());

                // 复制模板文件 (从Classpath读取)
                try (java.io.InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 使用固定金额 (与模板 default_voucher.pdf 一致)
                // 凭证金额 = 借方合计 = 贷方合计 (不能双算)
                BigDecimal amount = new BigDecimal("43758.00");

                // 随机来源系统 (0-6)
                int sourceIndex = random.nextInt(SOURCE_SYSTEMS.length);

                // 创建记录
                ArcFileContent content = ArcFileContent.builder()
                        .id(fileId)
                        .archivalCode("TEMP-POOL-" + dateStr + "-" + fileId.substring(0, 8).toUpperCase())
                        .fileName("凭证_" + dateStr + "_" + (1000 + i) + ".pdf")
                        .fileType("PDF")
                        .fileSize(Files.size(targetPath))
                        .fileHash("DEMO_HASH_" + fileId.substring(0, 8) + "_" + sourceIndex) // 演示数据用伪哈希 + 来源索引
                        .hashAlgorithm("SHA-256")
                        .storagePath(targetPath.toString())
                        .createdTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .build();

                arcFileContentMapper.insert(content);

                // 创建元数据索引 (包含金额)
                ArcFileMetadataIndex metadata = ArcFileMetadataIndex.builder()
                        .fileId(fileId)
                        .totalAmount(amount)
                        .invoiceNumber("INV-" + dateStr + "-" + (1000 + i))
                        .issueDate(java.time.LocalDate.now())
                        .sellerName("演示供应商 " + (char) ('A' + random.nextInt(26)))
                        .parsedTime(LocalDateTime.now())
                        .parserType("DEMO_GENERATOR")
                        .build();
                arcFileMetadataIndexMapper.insert(metadata);
            }

            return Result.success("成功生成10条演示数据");
        } catch (Exception e) {
            log.error("生成演示数据失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 转换实体为DTO
     */
    private PoolItemDto convertToPoolItemDto(ArcFileContent fileContent) {
        // 生成显示用的流水号 (去掉 TEMP- 前缀)
        String displayCode = fileContent.getArchivalCode().replace("TEMP-", "");

        // 查询元数据获取金额
        String amountStr = "-";
        ArcFileMetadataIndex metadata = arcFileMetadataIndexMapper.selectOne(
                new QueryWrapper<ArcFileMetadataIndex>().eq("file_id", fileContent.getId()));

        if (metadata != null && metadata.getTotalAmount() != null) {
            amountStr = metadata.getTotalAmount().toString();
        }

        // 解析来源系统
        String source = "Web上传";
        String fileHash = fileContent.getFileHash();
        if (fileHash != null && fileHash.startsWith("DEMO_HASH_")) {
            try {
                String[] parts = fileHash.split("_");
                if (parts.length >= 4) { // DEMO, HASH, ID, INDEX
                    int index = Integer.parseInt(parts[3]);
                    if (index >= 0 && index < SOURCE_SYSTEMS.length) {
                        source = SOURCE_SYSTEMS[index];
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        return PoolItemDto.builder()
                .id(fileContent.getId())
                .businessDocNo(fileContent.getBusinessDocNo())
                .code(displayCode)
                .source(source)
                .type(fileContent.getFileType())
                .amount(amountStr)
                .date(fileContent.getCreatedTime().format(FORMATTER))
                .status(fileContent.getPreArchiveStatus() != null ? fileContent.getPreArchiveStatus() : "PENDING_CHECK")
                .build();
    }
}
