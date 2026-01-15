// Input: MyBatis-Plus、Lombok、Spring Framework、Spring Security、等
// Output: ArchiveApprovalController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.approval.BatchApprovalRequest;
import com.nexusarchive.dto.approval.BatchApprovalResponse;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.service.ArchiveApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 档案审批控制器
 *
 * PRD 来源: 档案归档审批模块
 * 提供档案归档审批的创建、批准、拒绝功能
 *
 * <p>支持批量审批操作</p>
 */
@Tag(name = "档案审批", description = """
    档案归档审批接口。

    **功能说明:**
    - 获取审批列表
    - 获取审批详情
    - 创建审批申请
    - 批准归档
    - 拒绝归档
    - 批量批准归档
    - 批量拒绝归档

    **审批状态:**
    - PENDING: 待审批
    - APPROVED: 已批准
    - REJECTED: 已拒绝

    **审批流程:**
    1. 用户创建档案并提交审批
    2. 审批人批准/拒绝
    3. 批准后档案正式归档
    4. 拒绝后档案返回草稿状态

    **批量审批:**
    - 单次最多 100 条
    - 原子操作（全部成功或全部失败）
    - 返回详细结果

    **使用场景:**
    - 归档审批流程
    - 批量审批处理
    - 审批历史查询

    **权限要求:**
    - archive:read: 查看权限
    - archive:manage: 管理权限
    - nav:all: 全部导航权限
    - SYSTEM_ADMIN: 系统管理员
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/archive-approval")
@RequiredArgsConstructor
public class ArchiveApprovalController {

    private final ArchiveApprovalService approvalService;

    /**
     * 获取审批列表
     */
    @GetMapping("/list")
    @Operation(
        summary = "获取审批列表",
        description = """
            分页查询审批申请列表。

            **查询参数:**
            - page: 页码（从 1 开始）
            - limit: 每页条数
            - status: 状态过滤（可选）

            **返回数据包括:**
            - records: 审批记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **使用场景:**
            - 审批列表展示
            - 待办事项查询
            """,
        operationId = "getArchiveApprovalList",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<ArchiveApproval>> getApprovalList(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "状态过滤", example = "PENDING")
            @RequestParam(required = false) String status) {
        Page<ArchiveApproval> result = approvalService.getApprovalList(page, limit, status);
        return Result.success(result);
    }

    /**
     * 获取审批详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取审批详情",
        description = """
            查询指定审批申请的详细信息。

            **路径参数:**
            - id: 审批ID

            **返回数据包括:**
            - id: 审批ID
            - archiveId: 档案ID
            - applicantId: 申请人ID
            - applicantName: 申请人姓名
            - status: 审批状态
            - comment: 审批意见
            - createdAt: 创建时间

            **使用场景:**
            - 审批详情查看
            - 审批历史追溯
            """,
        operationId = "getArchiveApprovalDetail",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "审批记录不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<ArchiveApproval> getApprovalById(
            @Parameter(description = "审批ID", required = true, example = "approval-001")
            @PathVariable String id) {
        ArchiveApproval approval = approvalService.getApprovalById(id);
        if (approval == null) {
            return Result.error(404, "Approval record not found");
        }
        return Result.success(approval);
    }

    /**
     * 创建审批申请
     */
    @PostMapping("/create")
    @Operation(
        summary = "创建审批申请",
        description = """
            创建新的档案归档审批申请。

            **请求体:**
            - archiveId: 档案ID
            - applicantId: 申请人ID（可选，自动从认证上下文获取）
            - comment: 申请说明

            **返回数据:**
            - 创建的审批申请详情

            **业务规则:**
            - 申请人ID自动从认证上下文获取
            - 初始状态为 PENDING

            **使用场景:**
            - 提交归档审批
            """,
        operationId = "createArchiveApproval",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_APPROVAL", resourceType = "ARCHIVE_APPROVAL", description = "创建归档审批申请")
    public Result<ArchiveApproval> createApproval(
            @Valid @RequestBody ArchiveApproval approval,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        if (user != null && approval.getApplicantId() == null) {
            approval.setApplicantId(user.getId());
            approval.setApplicantName(user.getFullName());
        }
        try {
            ArchiveApproval created = approvalService.createApproval(approval);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 批准归档
     */
    @PostMapping("/approve")
    @Operation(
        summary = "批准归档",
        description = """
            批准档案归档申请，档案状态变为已归档。

            **请求体:**
            - id: 审批ID
            - approverId: 审批人ID（可选，自动获取）
            - approverName: 审批人姓名（可选，自动获取）
            - comment: 审批意见

            **业务规则:**
            - 仅 PENDING 状态可批准
            - 审批人信息自动从认证上下文获取

            **使用场景:**
            - 单条审批批准
            """,
        operationId = "approveArchive",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批准成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "审批记录不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批准归档申请")
    public Result<Void> approveArchive(
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        String approverName = user != null ? user.getFullName() : "system";
        approvalService.approveArchive(
            request.getId(),
            approverId,
            approverName,
            request.getComment()
        );
        return Result.success(null);
    }

    /**
     * 拒绝归档
     */
    @PostMapping("/reject")
    @Operation(
        summary = "拒绝归档",
        description = """
            拒绝档案归档申请，档案返回草稿状态。

            **请求体:**
            - id: 审批ID
            - approverId: 审批人ID（可选，自动获取）
            - approverName: 审批人姓名（可选，自动获取）
            - comment: 拒绝理由

            **业务规则:**
            - 仅 PENDING 状态可拒绝
            - 拒绝理由建议填写

            **使用场景:**
            - 单条审批拒绝
            """,
        operationId = "rejectArchive",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "拒绝成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "审批记录不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "REJECT_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "拒绝归档申请")
    public Result<Void> rejectArchive(
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        String approverName = user != null ? user.getFullName() : "system";
        approvalService.rejectArchive(
            request.getId(),
            approverId,
            approverName,
            request.getComment()
        );
        return Result.success(null);
    }

    /**
     * 批量批准归档
     */
    @PostMapping("/batch-approve")
    @Operation(
        summary = "批量批准归档",
        description = """
            批量批准多个档案归档申请。

            **请求体:**
            - ids: 审批ID列表
            - approverId: 审批人ID（可选，自动获取）
            - approverName: 审批人姓名（可选，自动获取）
            - comment: 审批意见

            **返回数据包括:**
            - totalCount: 总数
            - successCount: 成功数
            - failedCount: 失败数
            - errors: 失败详情

            **业务规则:**
            - 单次最多 100 条
            - 原子操作（全部成功或全部失败）

            **使用场景:**
            - 批量审批处理
            """,
        operationId = "batchApproveArchives",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量操作完成"),
        @ApiResponse(responseCode = "400", description = "参数错误或超过限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_APPROVE_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批量批准归档申请")
    public Result<BatchApprovalResponse> batchApprove(
            @Valid @RequestBody BatchApprovalRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        // 从认证上下文获取审批人信息（如果请求中未提供）
        if (user != null) {
            if (request.getApproverId() == null) {
                request.setApproverId(user.getId());
            }
            if (request.getApproverName() == null) {
                request.setApproverName(user.getFullName());
            }
        }

        // 设置默认值（系统调用）
        if (request.getApproverId() == null) {
            request.setApproverId("system");
        }
        if (request.getApproverName() == null) {
            request.setApproverName("system");
        }

        BatchApprovalResponse response = approvalService.batchApprove(request);
        return Result.success(response);
    }

    /**
     * 批量拒绝归档
     */
    @PostMapping("/batch-reject")
    @Operation(
        summary = "批量拒绝归档",
        description = """
            批量拒绝多个档案归档申请。

            **请求体:**
            - ids: 审批ID列表
            - approverId: 审批人ID（可选，自动获取）
            - approverName: 审批人姓名（可选，自动获取）
            - comment: 拒绝理由（必填）

            **返回数据包括:**
            - totalCount: 总数
            - successCount: 成功数
            - failedCount: 失败数
            - errors: 失败详情

            **业务规则:**
            - 拒绝理由必填
            - 单次最多 100 条
            - 原子操作

            **使用场景:**
            - 批量审批处理
            """,
        operationId = "batchRejectArchives",
        tags = {"档案审批"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量操作完成"),
        @ApiResponse(responseCode = "400", description = "参数错误、拒绝理由为空或超过限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "BATCH_REJECT_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批量拒绝归档申请")
    public Result<BatchApprovalResponse> batchReject(
            @Valid @RequestBody BatchApprovalRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        // 验证拒绝理由必填
        if (request.getComment() == null || request.getComment().isBlank()) {
            return Result.error(com.nexusarchive.common.exception.ErrorCode.BAD_REQUEST.getCode(), "拒绝理由不能为空");
        }

        // 从认证上下文获取审批人信息（如果请求中未提供）
        if (user != null) {
            if (request.getApproverId() == null) {
                request.setApproverId(user.getId());
            }
            if (request.getApproverName() == null) {
                request.setApproverName(user.getFullName());
            }
        }

        // 设置默认值（系统调用）
        if (request.getApproverId() == null) {
            request.setApproverId("system");
        }
        if (request.getApproverName() == null) {
            request.setApproverName("system");
        }

        BatchApprovalResponse response = approvalService.batchReject(request);
        return Result.success(response);
    }

    /**
     * 审批请求DTO
     */
    @lombok.Data
    public static class ApprovalRequest {
        private String id;
        private String approverId;
        private String approverName;
        private String comment;
    }
}
