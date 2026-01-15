// Input: Spring Web, Jakarta Validation, Lombok, Swagger/OpenAPI
// Output: CollectionBatchController
// Pos: Controller Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.Result;
import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.dto.batch.BatchArchiveRequest;
import com.nexusarchive.dto.batch.BatchArchiveResponse;
import com.nexusarchive.service.CollectionBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * PRD 来源: 批量归档模块
 * 提供批量上传的 REST API
 */
@Tag(name = "批量归档", description = """
    批量归档管理接口。

    **功能说明:**
    - 创建上传批次
    - 上传文件到批次
    - 完成批次上传
    - 取消批次
    - 查询批次详情和文件列表
    - 执行四性检测
    - 批量批准/拒绝归档

    **批次状态:**
    - CREATED: 已创建
    - UPLOADING: 上传中
    - COMPLETED: 上传完成
    - CHECKING: 检测中
    - CHECKED: 检测完成
    - APPROVED: 已批准
    - REJECTED: 已拒绝
    - CANCELLED: 已取消

    **四性检测:**
    - 真实性: 数字签名验证
    - 完整性: SM3 哈希校验
    - 可用性: 格式验证
    - 安全性: 病毒扫描

    **支持的文件格式:**
    - PDF: 便携式文档格式
    - OFD: 版式文档
    - XML: 结构化数据
    - JPG/PNG/TIFF: 图像格式

    **限制:**
    - 单文件最大 100MB
    - 单批次最多 1000 个文件

    **使用场景:**
    - 批量归档上传
    - 归档审批处理
    - 四性检测

    **权限要求:**
    - archive:manage 管理权限
    - archive:view 查看权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/collection/batch")
@RequiredArgsConstructor
@Slf4j
public class CollectionBatchController {

    private final CollectionBatchService collectionBatchService;

    /**
     * 创建上传批次
     */
    @PostMapping("/create")
    @Operation(
        summary = "创建上传批次",
        description = """
            创建新的批量上传批次。

            **请求参数:**
            - fondsNo: 全宗号
            - batchName: 批次名称（可选）
            - description: 批次描述（可选）

            **返回数据包括:**
            - batchId: 批次 ID
            - batchNo: 批次编号（自动生成）
            - status: 批次状态
            - createTime: 创建时间

            **业务规则:**
            - 批次编号自动生成（格式: YYYYMMDD + 序号）
            - 同一全宗下批次编号唯一
            - 创建操作记录审计日志

            **使用场景:**
            - 开始批量上传
            - 创建归档批次
            """,
        operationId = "createUploadBatch",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批次创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "创建批量上传批次")
    public Result<BatchUploadResponse> createBatch(
            @Parameter(description = "创建批次请求", required = true)
            @Valid @RequestBody BatchUploadRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        BatchUploadResponse response = collectionBatchService.createBatch(request, userId);

        log.info("批次创建成功: batchId={}, batchNo={}", response.getBatchId(), response.getBatchNo());
        return Result.success("批次创建成功", response);
    }

    /**
     * 上传单个文件
     */
    @PostMapping("/{batchId}/upload")
    @Operation(
        summary = "上传文件到批次",
        description = """
            上传单个文件到指定批次。

            **路径参数:**
            - batchId: 批次 ID

            **请求参数:**
            - file: 上传的文件（MultipartFile）

            **支持格式:**
            - PDF, OFD, XML, JPG, PNG, TIFF

            **限制:**
            - 单文件最大 100MB

            **返回数据包括:**
            - fileId: 文件 ID
            - fileName: 文件名
            - fileSize: 文件大小
            - status: 上传状态

            **使用场景:**
            - 批量添加文件
            - 逐个上传归档
            """,
        operationId = "uploadToBatch",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件为空、格式不支持或超过大小限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.FileUploadResult> uploadFile(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId,
            @Parameter(description = "上传的文件", required = true,
                    content = @Content(mediaType = "multipart/form-data"))
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
     */
    @PostMapping("/{batchId}/complete")
    @Operation(
        summary = "完成批次上传",
        description = """
            完成批次上传，准备进行四性检测。

            **路径参数:**
            - batchId: 批次 ID

            **返回数据包括:**
            - batchId: 批次 ID
            - fileCount: 文件数量
            - totalSize: 总大小
            - status: 批次状态

            **业务规则:**
            - 状态变更为 COMPLETED
            - 触发四性检测准备
            - 记录审计日志

            **使用场景:**
            - 完成文件上传
            - 开始检测流程
            """,
        operationId = "completeUploadBatch",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批次已完成"),
        @ApiResponse(responseCode = "400", description = "状态不允许完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "COMPLETE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "完成批量上传")
    public Result<CollectionBatchService.BatchCompleteResult> completeBatch(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCompleteResult result =
            collectionBatchService.completeBatch(batchId, userId);

        return Result.success("批次已完成", result);
    }

    /**
     * 取消批次
     */
    @PostMapping("/{batchId}/cancel")
    @Operation(
        summary = "取消批次",
        description = """
            取消指定的上传批次。

            **路径参数:**
            - batchId: 批次 ID

            **业务规则:**
            - 已批准的批次不能取消
            - 取消后状态变更为 CANCELLED
            - 已上传的文件保留但不再处理

            **使用场景:**
            - 取消错误批次
            - 放弃上传
            """,
        operationId = "cancelUploadBatch",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批次已取消"),
        @ApiResponse(responseCode = "400", description = "状态不允许取消"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CANCEL_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "取消批量上传批次")
    public Result<String> cancelBatch(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        collectionBatchService.cancelBatch(batchId, userId);

        return Result.success("批次已取消");
    }

    /**
     * 获取批次详情
     */
    @GetMapping("/{batchId}")
    @Operation(
        summary = "获取批次详情",
        description = """
            获取指定批次的详细信息。

            **路径参数:**
            - batchId: 批次 ID

            **返回数据包括:**
            - batchId: 批次 ID
            - batchNo: 批次编号
            - fondsNo: 全宗号
            - batchName: 批次名称
            - status: 批次状态
            - fileCount: 文件数量
            - totalSize: 总大小
            - createTime: 创建时间

            **使用场景:**
            - 批次详情查看
            - 状态查询
            """,
        operationId = "getBatchDetail",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<CollectionBatchService.BatchDetailResponse> getBatchDetail(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId) {

        CollectionBatchService.BatchDetailResponse detail =
            collectionBatchService.getBatchDetail(batchId);

        return Result.success(detail);
    }

    /**
     * 获取批次文件列表
     */
    @GetMapping("/{batchId}/files")
    @Operation(
        summary = "获取批次文件列表",
        description = """
            获取指定批次的所有文件。

            **路径参数:**
            - batchId: 批次 ID

            **返回数据包括:**
            - fileId: 文件 ID
            - fileName: 文件名
            - fileSize: 文件大小
            - uploadTime: 上传时间
            - status: 文件状态

            **使用场景:**
            - 文件列表展示
            - 上传进度查看
            """,
        operationId = "getBatchFiles",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchFileResponse>> getBatchFiles(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId) {

        List<CollectionBatchService.BatchFileResponse> files =
            collectionBatchService.getBatchFiles(batchId);

        return Result.success(files);
    }

    /**
     * 执行四性检测
     */
    @PostMapping("/{batchId}/check")
    @Operation(
        summary = "执行四性检测",
        description = """
            对批次中的文件执行四性检测。

            **路径参数:**
            - batchId: 批次 ID

            **四性检测包括:**
            1. **真实性**: 数字签名验证
            2. **完整性**: SM3 哈希校验
            3. **可用性**: 格式验证
            4. **安全性**: 病毒扫描

            **返回数据包括:**
            - batchId: 批次 ID
            - totalCount: 总文件数
            - passedCount: 通过数量
            - failedCount: 失败数量
            - details: 检测详情

            **使用场景:**
            - 归档前检测
            - 合规性验证
            """,
        operationId = "runFourNatureCheck",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检测完成"),
        @ApiResponse(responseCode = "400", description = "状态不允许检测"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "FOUR_NATURE_CHECK", resourceType = "COLLECTION_BATCH",
                  description = "执行批次四性检测")
    public Result<CollectionBatchService.BatchCheckResult> runFourNatureCheck(
            @Parameter(description = "批次 ID", required = true)
            @PathVariable Long batchId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        CollectionBatchService.BatchCheckResult result =
            collectionBatchService.runFourNatureCheck(batchId, userId);

        return Result.success(result);
    }

    /**
     * 获取用户的批次列表
     */
    @GetMapping("/list")
    @Operation(
        summary = "获取批次列表",
        description = """
            分页查询当前用户的批次列表。

            **查询参数:**
            - limit: 每页数量（默认 20）
            - offset: 偏移量（默认 0）

            **返回数据包括:**
            - 批次列表
            - 分页信息

            **使用场景:**
            - 批次列表展示
            - 历史记录查询
            """,
        operationId = "listUploadBatches",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<CollectionBatchService.BatchDetailResponse>> listBatches(
            @Parameter(description = "每页数量", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "偏移量", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        // TODO: 实现列表查询
        return Result.success(List.of());
    }

    /**
     * 批量批准批次归档
     */
    @PostMapping("/batch-approve")
    @Operation(
        summary = "批量批准批次归档",
        description = """
            批量批准多个批次的归档申请。

            **请求参数:**
            - batchIds: 批次 ID 列表
            - operatorId: 操作人 ID

            **返回数据包括:**
            - success: 成功数量
            - failed: 失败数量
            - results: 详细结果

            **业务规则:**
            - 只能批准检测通过的批次
            - 单次最多 100 个批次
            - 批准后进入正式归档流程
            - 记录审计日志

            **使用场景:**
            - 批量审批
            - 归档确认
            """,
        operationId = "batchApproveBatches",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量批准完成"),
        @ApiResponse(responseCode = "400", description = "参数错误或超出限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_APPROVE_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "批量批准批次归档")
    public Result<BatchArchiveResponse> batchApprove(
            @Parameter(description = "批量批准请求", required = true)
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
     */
    @PostMapping("/batch-reject")
    @Operation(
        summary = "批量拒绝批次归档",
        description = """
            批量拒绝多个批次的归档申请。

            **请求参数:**
            - batchIds: 批次 ID 列表
            - operatorId: 操作人 ID
            - reason: 拒绝原因

            **返回数据包括:**
            - success: 成功数量
            - failed: 失败数量
            - results: 详细结果

            **业务规则:**
            - 拒绝后批次不可恢复
            - 单次最多 100 个批次
            - 记录拒绝原因

            **使用场景:**
            - 批量拒绝
            - 归档驳回
            """,
        operationId = "batchRejectBatches",
        tags = {"批量归档"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量拒绝完成"),
        @ApiResponse(responseCode = "400", description = "参数错误或超出限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_REJECT_BATCH", resourceType = "COLLECTION_BATCH",
                  description = "批量拒绝批次归档")
    public Result<BatchArchiveResponse> batchReject(
            @Parameter(description = "批量拒绝请求", required = true)
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
