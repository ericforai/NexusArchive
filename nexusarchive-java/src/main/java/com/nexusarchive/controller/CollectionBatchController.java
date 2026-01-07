// Input: Spring Web, Jakarta Validation, Lombok
// Output: CollectionBatchController
// Pos: Controller Layer

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.dto.batch.BatchArchiveRequest;
import com.nexusarchive.dto.batch.BatchArchiveResponse;
import com.nexusarchive.service.CollectionBatchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料收集批次控制器
 *
 * 提供批量上传的REST API:
 * - 创建批次
 * - 上传文件
 * - 完成批次
 * - 查询状态
 * - 四性检测
 * - 批量审批/拒绝
 */
@RestController
@RequestMapping("/collection/batch")
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchController {

    private final CollectionBatchService collectionBatchService;

    /**
     * 创建上传批次
     *
     * POST /api/collection/batch/create
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "创建批量上传批次")
    public Result<BatchUploadResponse> createBatch(
            @Valid @RequestBody BatchUploadRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        BatchUploadResponse response = collectionBatchService.createBatch(request, userId);

        log.info("批次创建成功: batchId={}, batchNo={}", response.getBatchId(), response.getBatchNo());
        return Result.success("批次创建成功", response);
    }

    /**
     * 上传单个文件
     *
     * POST /api/collection/batch/{batchId}/upload
     */
    @PostMapping("/{batchId}/upload")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.FileUploadResult> uploadFile(
            @PathVariable Long batchId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 文件类型验证
        String filename = file.getOriginalFilename();
        if (!isAllowedFileType(filename)) {
            return Result.error("不支持的文件类型，仅支持: PDF, OFD, XML, JPG, PNG, TIFF");
        }

        // 文件大小验证 (最大100MB)
        long maxSize = 100 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            return Result.error("文件大小超过限制 (最大100MB)");
        }

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.FileUploadResult result =
            collectionBatchService.uploadFile(batchId, file, userId);

        log.info("文件上传结果: filename={}, status={}", filename, result.status());
        return Result.success(result);
    }

    /**
     * 完成批次上传
     *
     * POST /api/collection/batch/{batchId}/complete
     */
    @PostMapping("/{batchId}/complete")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "COMPLETE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "完成批量上传")
    public Result<CollectionBatchService.BatchCompleteResult> completeBatch(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCompleteResult result =
            collectionBatchService.completeBatch(batchId, userId);

        return Result.success("批次已完成", result);
    }

    /**
     * 取消批次
     *
     * POST /api/collection/batch/{batchId}/cancel
     */
    @PostMapping("/{batchId}/cancel")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CANCEL_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "取消批量上传批次")
    public Result<String> cancelBatch(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        collectionBatchService.cancelBatch(batchId, userId);

        return Result.success("批次已取消");
    }

    /**
     * 获取批次详情
     *
     * GET /api/collection/batch/{batchId}
     */
    @GetMapping("/{batchId}")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.BatchDetailResponse> getBatchDetail(
            @PathVariable Long batchId) {

        CollectionBatchService.BatchDetailResponse detail =
            collectionBatchService.getBatchDetail(batchId);

        return Result.success(detail);
    }

    /**
     * 获取批次文件列表
     *
     * GET /api/collection/batch/{batchId}/files
     */
    @GetMapping("/{batchId}/files")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchFileResponse>> getBatchFiles(
            @PathVariable Long batchId) {

        List<CollectionBatchService.BatchFileResponse> files =
            collectionBatchService.getBatchFiles(batchId);

        return Result.success(files);
    }

    /**
     * 执行四性检测
     *
     * POST /api/collection/batch/{batchId}/check
     */
    @PostMapping("/{batchId}/check")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "FOUR_NATURE_CHECK", resourceType = "COLLECTION_BATCH",
                  description = "执行批次四性检测")
    public Result<CollectionBatchService.BatchCheckResult> runFourNatureCheck(
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCheckResult result =
            collectionBatchService.runFourNatureCheck(batchId, userId);

        return Result.success(result);
    }

    /**
     * 获取用户的批次列表
     *
     * GET /api/collection/batch/list
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchDetailResponse>> listBatches(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        // TODO: 实现列表查询
        return Result.success(List.of());
    }

    /**
     * 批量批准批次归档
     *
     * POST /api/collection/batch/batch-approve
     */
    @PostMapping("/batch-approve")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_APPROVE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "批量批准批次归档")
    public Result<BatchArchiveResponse> batchApprove(
            @Valid @RequestBody BatchArchiveRequest request,
            HttpServletRequest httpRequest) {

        // 从认证上下文获取操作人信息（如果请求中未提供）
        Long userId = getUserIdFromRequest(httpRequest);
        if (request.getOperatorId() == null) {
            request.setOperatorId(String.valueOf(userId));
        }

        BatchArchiveResponse response = collectionBatchService.batchApprove(request);

        log.info("批量批准批次完成: succeeded={}, failed={}",
                 response.getSuccess(), response.getFailed());
        return Result.success(response);
    }

    /**
     * 批量拒绝批次归档
     *
     * POST /api/collection/batch/batch-reject
     */
    @PostMapping("/batch-reject")
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_REJECT_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "批量拒绝批次归档")
    public Result<BatchArchiveResponse> batchReject(
            @Valid @RequestBody BatchArchiveRequest request,
            HttpServletRequest httpRequest) {

        // 从认证上下文获取操作人信息（如果请求中未提供）
        Long userId = getUserIdFromRequest(httpRequest);
        if (request.getOperatorId() == null) {
            request.setOperatorId(String.valueOf(userId));
        }

        BatchArchiveResponse response = collectionBatchService.batchReject(request);

        log.info("批量拒绝批次完成: succeeded={}, failed={}",
                 response.getSuccess(), response.getFailed());
        return Result.success(response);
    }

    // ===== Private Helper Methods =====

    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            return 1L; // 默认用户 (开发环境)
        }
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return Long.parseLong(userId.toString());
    }

    private boolean isAllowedFileType(String filename) {
        if (filename == null) return false;
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return List.of("pdf", "ofd", "xml", "jpg", "jpeg", "png", "tif", "tiff")
            .contains(ext);
    }
}
