// Input: MyBatis-Plus、Spring Framework、Lombok、Spring Security、等
// Output: PoolController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.common.result.BatchOperationResult;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.MetadataUpdateDTO;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.PreArchiveSubmitService;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.PoolService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.annotation.ArchivalAudit;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.nexusarchive.common.constants.HttpConstants;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final com.nexusarchive.service.PreArchiveCheckService preArchiveCheckService;
    private final com.nexusarchive.service.PreArchiveSubmitService preArchiveSubmitService;
    private final com.nexusarchive.service.AuditLogService auditLogService;
    private final com.nexusarchive.service.AttachmentService attachmentService;
    private final com.nexusarchive.service.PoolService poolService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.nexusarchive.service.helper.PoolHelper helper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== 元数据补录 API =====

    @PostMapping("/candidates/search")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<PoolItemDto>> searchCandidates(@Valid @RequestBody com.nexusarchive.dto.search.CandidateSearchRequest request) {
        try { return Result.success(poolService.searchCandidates(request)); }
        catch (Exception e) { return Result.error("搜索失败: " + e.getMessage()); }
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<PoolItemDetailDto> getFileDetail(@PathVariable String id) {
        ArcFileContent file = poolService.getFileById(id);
        return file == null ? Result.error("文件不存在") : Result.success(helper.mapToDetail(file));
    }

    @GetMapping("/related/{id}")
    public Result<List<PoolItemDto>> getRelatedFiles(@PathVariable String id) {
        ArcFileContent main = poolService.getFileById(id);
        if (main == null) return Result.error("文件不存在");
        List<ArcFileContent> linked = attachmentService.getAttachmentsByArchive(id);
        if (main.getBusinessDocNo() != null && !main.getBusinessDocNo().isEmpty()) {
            poolService.getLegacyAttachments(main.getBusinessDocNo()).forEach(l -> {
                if (linked.stream().noneMatch(a -> a.getId().equals(l.getId()))) linked.add(l);
            });
        }
        return Result.success(linked.stream().map(poolService::convertToPoolItemDto).toList());
    }

    @PostMapping("/metadata/update")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "METADATA_UPDATE", resourceType = "ARC_FILE_CONTENT", description = "更新预归档元数据")
    public Result<String> updateMetadata(@RequestBody @Validated MetadataUpdateDTO dto, HttpServletRequest request) {
        ArcFileContent file = poolService.getFileById(dto.getId());
        if (file == null) return Result.error("文件不存在");
        String before = String.format("fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s", file.getFiscalYear(), file.getVoucherType(), file.getCreator(), file.getFondsCode());
        helper.updateFields(dto);
        String after = String.format("fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s", dto.getFiscalYear(), dto.getVoucherType(), dto.getCreator(), dto.getFondsCode());
        auditLogService.log((String) request.getAttribute("userId"), (String) request.getAttribute("username"), "METADATA_UPDATE", "ARC_FILE_CONTENT", dto.getId(), OperationResult.SUCCESS, "补录: " + dto.getModifyReason() + " | " + before + " -> " + after, request.getRemoteAddr());
        FourNatureReport r = preArchiveCheckService.checkSingleFile(dto.getId());
        return Result.success("更新成功，结果: " + r.getStatus());
    }

    @lombok.Data
    public static class PoolItemDetailDto {
        private String id; private String fileName; private String fileType; private Long fileSize;
        private String status; private LocalDateTime createdTime; private String fiscalYear;
        private String voucherType; private String creator; private String fondsCode; private String sourceSystem;
    }

    @GetMapping("/list")
    public Result<List<PoolItemDto>> listPoolItems(@RequestParam(required = false) String category) {
        return Result.success(poolService.listPoolItems(category));
    }

    @GetMapping("/list/status/{status}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<PoolItemDto>> listByStatus(@PathVariable String status, @RequestParam(required = false) String category) {
        return Result.success(poolService.listByStatus(status, category));
    }

    @GetMapping("/stats/status")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.Map<String, Long>> getStatusStats(@RequestParam(required = false) String category) {
        return Result.success(poolService.getStatusStats(category));
    }

    @GetMapping("/status/{id}/{status}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "STATUS_UPDATE", resourceType = "PRE_ARCHIVE", description = "更新预归档状态")
    public Result<String> updateStatus(@PathVariable String id, @PathVariable String status) {
        poolService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    @GetMapping("/check/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<FourNatureReport> checkSingleFile(@PathVariable String id) {
        return Result.success(preArchiveCheckService.checkSingleFile(id));
    }

    @PostMapping("/check/batch")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.List<FourNatureReport>> checkBatchFiles(@RequestBody java.util.List<String> fileIds) {
        return Result.success(preArchiveCheckService.checkMultipleFiles(fileIds));
    }

    @GetMapping("/check/all-pending")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.List<FourNatureReport>> checkAllPendingFiles() {
        java.util.List<String> ids = poolService.listPendingCheckFiles().stream().map(ArcFileContent::getId).toList();
        return Result.success(preArchiveCheckService.checkMultipleFiles(ids));
    }

    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE", resourceType = "PRE_ARCHIVE", description = "提交归档申请")
    public Result<ArchiveApproval> submitForArchival(@PathVariable String id, @Valid @RequestBody SubmitRequest request) {
        try { return Result.success(preArchiveSubmitService.submitForArchival(id, request.getApplicantId(), request.getApplicantName(), request.getReason())); }
        catch (Exception e) { return Result.error(500, e.getMessage()); }
    }

    @PostMapping("/submit/batch")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE_BATCH", resourceType = "PRE_ARCHIVE", description = "批量提交归档申请")
    public Result<BatchOperationResult<ArchiveApproval>> submitBatchForArchival(@Valid @RequestBody BatchSubmitRequest request) {
        try { return Result.success(preArchiveSubmitService.submitBatchForArchival(request.getFileIds(), request.getApplicantId(), request.getApplicantName(), request.getReason())); }
        catch (Exception e) { return Result.error(500, e.getMessage()); }
    }

    @PostMapping("/complete/{archiveId}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "COMPLETE_ARCHIVE", resourceType = "ARCHIVE", description = "完成归档")
    public Result<String> completeArchival(@PathVariable String archiveId) {
        preArchiveSubmitService.completeArchival(archiveId);
        return Result.success("归档完成");
    }

    @lombok.Data
    public static class SubmitRequest { private String applicantId; private String applicantName; private String reason; }

    @lombok.Data
    public static class BatchSubmitRequest { private java.util.List<String> fileIds; private String applicantId; private String applicantName; private String reason; }

    @GetMapping("/preview/{id}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Resource> previewFile(@PathVariable String id) {
        ArcFileContent fc = poolService.getFileById(id);
        String path = null, name = null, data = null;
        if (fc != null) { path = fc.getStoragePath(); name = fc.getFileName(); data = fc.getSourceData(); }
        else {
            try {
                var row = jdbcTemplate.queryForMap("SELECT storage_path, file_name FROM arc_original_voucher_file WHERE id = ? AND deleted = 0", id);
                path = (String) row.get("storage_path"); name = (String) row.get("file_name");
            } catch (Exception e) { return ResponseEntity.notFound().build(); }
        }
        try {
            Resource r = helper.loadPreview(id, path, name, data);
            if (!r.exists()) return ResponseEntity.notFound().build();
            String n = name.toLowerCase();
            String type = n.endsWith(".pdf") ? HttpConstants.APPLICATION_PDF : n.endsWith(".ofd") ? HttpConstants.APPLICATION_OFD : (n.endsWith(".jpg") || n.endsWith(".jpeg")) ? "image/jpeg" : n.endsWith(".png") ? "image/png" : n.endsWith(".xml") ? "text/xml" : "application/octet-stream";
            // 安全处理文件名：过滤危险字符并使用 RFC 5987 编码支持非 ASCII 字符
            String safeName = name.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_");
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
            String contentDisposition = String.format("inline; filename=\"%s\"; filename*=UTF-8''%s", safeName, encodedName);
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition).body(r);
        } catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/generate-demo")
    public Result<String> generateDemoData() {
        try { helper.generateDemo(); return Result.success("成功生成10条演示数据"); }
        catch (Exception e) { return Result.error("生成失败: " + e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "DELETE_POOL_ITEM", resourceType = "ARC_FILE_CONTENT", description = "删除预归档记录")
    public Result<String> deletePoolItem(@PathVariable String id, HttpServletRequest request) {
        ArcFileContent file = poolService.getFileById(id);
        if (file == null) return Result.error("文件不存在");

        String before = String.format("id=%s, fileName=%s, status=%s", file.getId(), file.getFileName(), file.getPreArchiveStatus());
        poolService.deletePoolItem(id);
        auditLogService.log((String) request.getAttribute("userId"), (String) request.getAttribute("username"), "DELETE_POOL_ITEM", "ARC_FILE_CONTENT", id, OperationResult.SUCCESS, "删除预归档记录 | " + before, request.getRemoteAddr());

        return Result.success("删除成功");
    }
}
