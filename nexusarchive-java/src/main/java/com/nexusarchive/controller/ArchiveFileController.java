// Input: MyBatis-Plus、io.swagger、Lombok、Spring Framework、等
// Output: ArchiveFileController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.dto.VoucherDataDto;
import com.nexusarchive.dto.VoucherDataDto.AttachmentInfo;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nexusarchive.common.constants.HttpConstants;
import com.nexusarchive.util.HttpHeaderUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "档案文件内容")
public class ArchiveFileController {

    private final ArchiveFileContentService archiveFileContentService;
    private final ArchiveMapper archiveMapper;
    private final FileStorageService fileStorageService;
    private final DataScopeService dataScopeService;
    private final VoucherRelationMapper voucherRelationMapper;
    private final OriginalVoucherMapper originalVoucherMapper;

    @GetMapping("/{id}/content")
    @Operation(summary = "获取档案文件内容（支持 PDF/OFD 等）")
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> getFileContent(@PathVariable String id,
                                                   jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);

        // 1. Query file content record by item_id
        ArcFileContent content = archiveFileContentService.getFileContentByItemId(id, operatorId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND_FOR_ARCHIVE, id);
        }
        authorizeArchiveAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());
        
        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException(ErrorCode.PHYSICAL_FILE_NOT_FOUND, content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());
        
        String contentType = determineContentType(content.getFileType(), content.getFileName());
        
        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.inlineContentDisposition(content.getFileName()))
                .contentType(MediaType.parseMediaType(contentType));
        Long contentLength = resolveContentLength(resource, content);
        if (contentLength != null) {
            builder.contentLength(contentLength);
        }
        return builder.body(resource);
    }

    /**
     * 通过文件 ID 直接下载文件
     * 用于全景视图附件预览（不需要额外权限校验，已登录即可）
     */
    @GetMapping("/files/download/{fileId}")
    @Operation(summary = "通过文件ID下载文件")
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> downloadByFileId(@PathVariable String fileId,
                                                     jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);

        // 直接通过文件 ID 查询
        ArcFileContent content = archiveFileContentService.getFileContentById(fileId, operatorId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, fileId);
        }

        // 授权检查：支持 acc_archive 和 arc_original_voucher 两种来源
        authorizeFileAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());

        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException(ErrorCode.PHYSICAL_FILE_NOT_FOUND, content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());

        String contentType = determineContentType(content.getFileType(), content.getFileName());

        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.inlineContentDisposition(content.getFileName()))
                .contentType(MediaType.parseMediaType(contentType));
        Long contentLength = resolveContentLength(resource, content);
        if (contentLength != null) {
            builder.contentLength(contentLength);
        }
        return builder.body(resource);
    }

    /**
     * 获取档案关联的凭证分录数据（source_data）
     * 用于凭证预览标签页
     *
     * 支持两种数据关联方式：
     * 1. 通过 item_id 关联查询（acc_archive -> arc_file_content）
     * 2. 直接通过 id 查询（arc_file_content 自身记录）
     *
     * 同时返回关联的原始凭证附件（发票等）
     */
    @GetMapping("/{id}/voucher-data")
    @Operation(summary = "获取档案关联的凭证分录数据")
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<com.nexusarchive.common.result.Result<com.nexusarchive.dto.VoucherDataDto>> getVoucherData(
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        log.info("获取凭证分录数据: id={}", id);

        // 优先从 acc_archive.custom_metadata 获取分录数据（这是正确的来源）
        Archive archive = archiveMapper.selectById(id);
        if (archive != null && archive.getCustomMetadata() != null && !archive.getCustomMetadata().isEmpty()) {
            log.info("从 acc_archive.custom_metadata 获取分录数据: id={}", id);
            com.nexusarchive.dto.VoucherDataDto dto = new com.nexusarchive.dto.VoucherDataDto();
            dto.setFileId(id);
            // 将 custom_metadata 转换为 sourceData 格式（兼容前端解析器）
            dto.setSourceData(convertCustomMetadataToSourceData(archive.getCustomMetadata()));
            dto.setVoucherWord(extractVoucherWord(archive.getArchiveCode()));
            dto.setSummary(archive.getSummary());
            dto.setDocDate(archive.getDocDate() != null ? archive.getDocDate().toString() : null);
            dto.setCreator(archive.getCreatedBy());
            // 查询关联的原始凭证附件
            dto.setAttachments(getRelatedOriginalVoucherAttachments(id, operatorId));

            log.info("返回凭证分录数据（来自 custom_metadata）: archiveId={}, attachments={}", id, dto.getAttachments() != null ? dto.getAttachments().size() : 0);
            return ResponseEntity.ok(com.nexusarchive.common.result.Result.success(dto));
        }

        // 降级：尝试从 arc_file_content.source_data 获取
        ArcFileContent content = null;
        content = archiveFileContentService.getFileContentByItemId(id, operatorId);
        if (content == null) {
            content = archiveFileContentService.getFileContentById(id, operatorId);
        }

        if (content == null) {
            log.warn("未找到凭证分录数据: id={}", id);
            return ResponseEntity.ok(com.nexusarchive.common.result.Result.success(null));
        }

        com.nexusarchive.dto.VoucherDataDto dto = new com.nexusarchive.dto.VoucherDataDto();
        dto.setFileId(content.getId());
        dto.setSourceData(content.getSourceData());
        dto.setVoucherWord(content.getVoucherWord());
        dto.setSummary(content.getSummary());
        dto.setDocDate(content.getDocDate() != null ? content.getDocDate().toString() : null);
        dto.setCreator(content.getCreator());
        // 查询关联的原始凭证附件
        dto.setAttachments(getRelatedOriginalVoucherAttachments(id, operatorId));

        log.info("返回凭证分录数据（来自 source_data）: fileId={}, hasSourceData={}, attachments={}",
                 content.getId(), content.getSourceData() != null, dto.getAttachments() != null ? dto.getAttachments().size() : 0);
        return ResponseEntity.ok(com.nexusarchive.common.result.Result.success(dto));
    }

    /**
     * 获取关联的原始凭证附件（发票等）
     * 通过 arc_voucher_relation 表查询关联的原始凭证，然后获取其文件
     */
    private java.util.List<AttachmentInfo> getRelatedOriginalVoucherAttachments(String accountingVoucherId, String operatorId) {
        try {
            // 1. 查询关联的原始凭证
            java.util.List<VoucherRelation> relations = voucherRelationMapper.findByAccountingVoucherId(accountingVoucherId);
            if (relations == null || relations.isEmpty()) {
                log.debug("未找到关联的原始凭证: accountingVoucherId={}", accountingVoucherId);
                return java.util.Collections.emptyList();
            }

            // 2. 查询每个原始凭证的文件
            java.util.List<AttachmentInfo> attachments = new java.util.ArrayList<>();
            for (VoucherRelation relation : relations) {
                String originalVoucherId = relation.getOriginalVoucherId();
                log.debug("查询原始凭证文件: originalVoucherId={}", originalVoucherId);

                ArcFileContent fileContent = archiveFileContentService.getFileContentByItemId(originalVoucherId, operatorId);
                if (fileContent != null) {
                    AttachmentInfo info = new AttachmentInfo(
                        fileContent.getId(),
                        fileContent.getFileName(),
                        fileContent.getFileType(),
                        fileContent.getFileSize(),
                        originalVoucherId
                    );
                    attachments.add(info);
                    log.info("找到关联附件: fileName={}, fileSize={}", fileContent.getFileName(), fileContent.getFileSize());
                } else {
                    log.warn("未找到原始凭证文件: originalVoucherId={}", originalVoucherId);
                }
            }

            return attachments;
        } catch (Exception e) {
            log.warn("查询关联附件失败: accountingVoucherId={}, error={}", accountingVoucherId, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 将 custom_metadata 转换为 sourceData 格式
     * custom_metadata 格式: [{"id": "1", "debit_org": 201, "accsubject": {...}, "description": "..."}]
     * sourceData 格式: {"bodies": [{"description": "...", "debit_original": 201, "subjectName": "..."}]}
     */
    private String convertCustomMetadataToSourceData(String customMetadata) {
        try {
            // 解析 custom_metadata JSON
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(customMetadata);
            if (!root.isArray()) {
                return null;
            }

            // 构建符合 sourceData 格式的 JSON
            StringBuilder sb = new StringBuilder();
            sb.append("{\"bodies\":[");

            for (com.fasterxml.jackson.databind.JsonNode entry : root) {
                sb.append("{");

                // description
                String description = entry.has("description") ? entry.get("description").asText()
                    : entry.has("digest") ? entry.get("digest").asText()
                    : entry.has("summary") ? entry.get("summary").asText()
                    : "无摘要";
                sb.append("\"description\":\"").append(escapeJson(description)).append("\",");

                // 借方金额
                if (entry.has("debit_org")) {
                    sb.append("\"debit_original\":").append(entry.get("debit_org").asDouble()).append(",");
                }
                if (entry.has("credit_org")) {
                    sb.append("\"credit_original\":").append(entry.get("credit_org").asDouble()).append(",");
                }

                // 科目信息
                if (entry.has("accsubject") && entry.get("accsubject").isObject()) {
                    com.fasterxml.jackson.databind.JsonNode accsubject = entry.get("accsubject");
                    String subjectName = accsubject.has("name") ? accsubject.get("name").asText() : "未知科目";
                    sb.append("\"subjectName\":\"").append(escapeJson(subjectName)).append("\"");
                } else if (entry.has("subjectName")) {
                    sb.append("\"subjectName\":\"").append(escapeJson(entry.get("subjectName").asText())).append("\"");
                }

                sb.append("},");
            }

            // 移除最后一个逗号并闭合
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setLength(sb.length() - 1);
            }
            sb.append("]}");

            return sb.toString();
        } catch (Exception e) {
            log.warn("转换 custom_metadata 失败: {}", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractVoucherWord(String archiveCode) {
        if (archiveCode == null) return "记";
        // 从 archive_code 提取凭证字（如 "JZ-202311-0052" -> "记"）
        if (archiveCode.startsWith("JZ-")) return "记";
        if (archiveCode.startsWith("PZ-")) return "付";
        if (archiveCode.startsWith("ZZ-")) return "转";
        if (archiveCode.startsWith("CZ-")) return "资产";
        return "记";
    }

    private String determineContentType(String fileType, String fileName) {
        if (fileType != null) {
            switch (fileType.toLowerCase()) {
                case "ofd":
                    return HttpConstants.APPLICATION_OFD;
                case "pdf":
                    return HttpConstants.APPLICATION_PDF;
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "xml":
                    return "application/xml";
            }
        }
        
        if (fileName != null) {
            if (fileName.endsWith(".ofd")) return HttpConstants.APPLICATION_OFD;
            if (fileName.endsWith(".pdf")) return HttpConstants.APPLICATION_PDF;
        }
        
        return "application/octet-stream";
    }

    private Long resolveContentLength(Resource resource, ArcFileContent content) {
        try {
            long actualLength = resource.contentLength();
            return actualLength >= 0 ? actualLength : null;
        } catch (IOException e) {
            Long fallback = content.getFileSize();
            log.warn("无法读取物理文件长度，回退到数据库 fileSize: fileId={}, path={}, fileSize={}",
                    content.getId(), content.getStoragePath(), fallback, e);
            return fallback;
        }
    }

    private void authorizeArchiveAccess(String archivalCode) {
        authorizeFileAccess(archivalCode);
    }

    /**
     * 授权检查文件访问权限
     * 支持 acc_archive（正式档案）和 arc_original_voucher（原始凭证）两种来源
     */
    private void authorizeFileAccess(String archivalCode) {
        if (archivalCode == null || archivalCode.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_BOUND_TO_ARCHIVE);
        }

        // 1. 先尝试在正式档案表 (acc_archive) 中查找
        Archive archive = archiveMapper.selectById(archivalCode);
        if (archive == null) {
            // 尝试按 archive_code 查找
            archive = archiveMapper.selectOne(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getArchiveCode, archivalCode)
                            .last("LIMIT 1")
            );
        }

        if (archive != null) {
            // 找到正式档案，使用常规全宗权限检查
            DataScopeService.DataScopeContext scope = dataScopeService.resolve();
            if (!dataScopeService.canAccessArchive(archive, scope)) {
                throw new BusinessException(ErrorCode.ARCHIVE_ACCESS_DENIED);
            }
            return;
        }

        // 2. 如果在正式档案表中未找到，尝试在原始凭证表 (arc_original_voucher) 中查找
        // 必须验证原始凭证的全宗权限，防止权限绕过
        OriginalVoucher voucher = originalVoucherMapper.selectById(archivalCode);
        if (authorizeOriginalVoucher(voucher, archivalCode)) {
            return;
        }

        // 常见场景：archivalCode 传的是 voucher_no（例如 INV-202311-089），而非主键 ID
        voucher = originalVoucherMapper.selectOne(
                new LambdaQueryWrapper<OriginalVoucher>()
                        .eq(OriginalVoucher::getVoucherNo, archivalCode)
                        .last("LIMIT 1")
        );
        if (authorizeOriginalVoucher(voucher, archivalCode)) {
            return;
        }

        // 历史数据兜底：部分数据将 source_doc_id 记录为 INV-* 业务号
        voucher = originalVoucherMapper.selectOne(
                new LambdaQueryWrapper<OriginalVoucher>()
                        .eq(OriginalVoucher::getSourceDocId, archivalCode)
                        .last("LIMIT 1")
        );
        if (authorizeOriginalVoucher(voucher, archivalCode)) {
            return;
        }

        // 3. 既不是正式档案也不是原始凭证，拒绝访问
        log.warn("无法找到 archivalCode={} 对应的档案或原始凭证", archivalCode);
        throw new BusinessException(ErrorCode.ARCHIVE_NOT_FOUND, archivalCode);
    }

    private boolean authorizeOriginalVoucher(OriginalVoucher voucher, String archivalCode) {
        if (voucher == null) {
            return false;
        }
        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessOriginalVoucher(voucher, scope)) {
            log.warn("原始凭证访问被拒绝: archivalCode={}, allowedFonds={}", archivalCode, scope.allowedFonds());
            throw new BusinessException(ErrorCode.ARCHIVE_ACCESS_DENIED);
        }
        log.debug("原始凭证权限验证通过: archivalCode={}, voucherId={}, fondsCode={}",
                archivalCode, voucher.getId(), voucher.getFondsCode());
        return true;
    }

    private String resolveUserId(jakarta.servlet.http.HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.nexusarchive.security.CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
