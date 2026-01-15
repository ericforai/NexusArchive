// Input: Spring Web、AuthTicketService、AuthTicketApprovalService
// Output: AuthTicketController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketDetail;
import com.nexusarchive.service.AuthTicketApprovalService;
import com.nexusarchive.service.AuthTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * 授权票据控制器
 *
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
@Tag(name = "授权票据管理", description = """
    跨全宗访问授权票据的申请、审批、查询、撤销接口。

    **功能说明:**
    - 跨全宗访问授权申请
    - 两级审批流程（第一审批人、第二审批人复核）
    - 授权票据查询和管理
    - 授权票据撤销

    **授权范围 (AuthScope):**
    - fondsNo: 目标全宗号
    - allowedPeriods: 允许访问的会计期间列表
    - allowedCategories: 允许访问的档案分类
    - allowDownload: 是否允许下载
    - allowExport: 是否允许导出

    **票据状态:**
    - PENDING: 待审批
    - FIRST_APPROVED: 第一审批通过
    - APPROVED: 审批通过（已授权）
    - REJECTED: 已驳回
    - REVOKED: 已撤销
    - EXPIRED: 已过期

    **审批流程:**
    1. 申请人创建授权申请 → PENDING
    2. 第一审批人审批 → FIRST_APPROVED 或 REJECTED
    3. 第二审批人复核 → APPROVED 或 REJECTED
    4. 审批通过后票据生效

    **使用场景:**
    - 审计人员跨全宗查阅档案
    - 总部人员查看子公司档案
    - 临时跨全宗数据访问
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/auth-ticket")
@RequiredArgsConstructor
public class AuthTicketController {

    private final AuthTicketService authTicketService;
    private final AuthTicketApprovalService authTicketApprovalService;

    @PostMapping("/apply")
    @Operation(
        summary = "创建授权票据申请",
        description = """
            创建跨全宗访问授权票据申请。

            **请求参数:**
            - targetFonds: 目标全宗号（必填）
            - scope: 授权范围（必填，JSON 格式）
            - expiresAt: 过期时间（必填，格式：YYYY-MM-DDTHH:mm:ss）
            - reason: 申请原因（必填）

            **请求头:**
            - X-Current-Fonds-No: 当前全宗号（源全宗）
            - X-User-Id: 申请人 ID

            **业务规则:**
            - 申请人必须属于源全宗
            - 目标全宗必须存在
            - 过期时间不能早于当前时间
            - 最长授权期限 30 天

            **使用场景:**
            - 审计人员申请跨全宗访问权限
            """,
        operationId = "createAuthTicket",
        tags = {"授权票据管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "申请创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<Map<String, Object>> createAuthTicket(
            @Parameter(description = "目标全宗号", required = true, example = "F002")
            @RequestParam String targetFonds,
            @Parameter(description = "授权范围", required = true)
            @Valid @RequestBody AuthScope scope,
            @Parameter(description = "过期时间", required = true, example = "2025-12-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @Parameter(description = "申请原因", required = true, example = "年度审计需要")
            @RequestParam String reason,
            @Parameter(description = "源全宗号", hidden = true)
            @RequestHeader("X-Current-Fonds-No") String sourceFonds,
            @Parameter(description = "申请人ID", hidden = true)
            @RequestHeader("X-User-Id") String applicantId) {

        try {
            String ticketId = authTicketService.createAuthTicket(
                applicantId, sourceFonds, targetFonds, scope, expiresAt, reason);

            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", ticketId);
            data.put("status", "PENDING");
            data.put("createdAt", LocalDateTime.now());

            return Result.success(data);
        } catch (Exception e) {
            log.error("创建授权票据申请失败", e);
            return Result.error("创建授权票据申请失败: " + e.getMessage());
        }
    }

    @GetMapping("/{ticketId}")
    @Operation(
        summary = "查询授权票据详情",
        description = """
            查询指定授权票据的详细信息。

            **路径参数:**
            - ticketId: 票据 ID

            **返回数据包括:**
            - ticketId: 票据 ID
            - sourceFonds: 源全宗号
            - targetFonds: 目标全宗号
            - scope: 授权范围
            - status: 票据状态
            - expiresAt: 过期时间
            - applicantId: 申请人 ID
            - createdAt: 创建时间
            - approvals: 审批记录列表

            **使用场景:**
            - 查看票据审批状态
            - 确认授权范围
            """,
        operationId = "getAuthTicketDetail",
        tags = {"授权票据管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "票据不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<AuthTicketDetail> getAuthTicketDetail(
            @Parameter(description = "票据ID", required = true, example = "TKT-20250114-001")
            @PathVariable String ticketId) {
        try {
            AuthTicketDetail detail = authTicketService.getAuthTicketDetail(ticketId);
            return Result.success(detail);
        } catch (Exception e) {
            log.error("查询授权票据详情失败: ticketId={}", ticketId, e);
            return Result.error("查询授权票据详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/{ticketId}/revoke")
    @Operation(
        summary = "撤销授权票据",
        description = """
            撤销指定的授权票据。

            **路径参数:**
            - ticketId: 票据 ID

            **请求参数:**
            - reason: 撤销原因（必填）

            **请求头:**
            - X-User-Id: 操作人 ID

            **业务规则:**
            - 只有申请人或管理员可以撤销
            - 已过期的票据不能撤销
            - 撤销后票据状态变更为 REVOKED

            **使用场景:**
            - 授权完成提前撤销
            - 发现安全风险紧急撤销
            """,
        operationId = "revokeAuthTicket",
        tags = {"授权票据管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "撤销成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或票据状态不允许撤销"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "票据不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<Void> revokeAuthTicket(
            @Parameter(description = "票据ID", required = true, example = "TKT-20250114-001")
            @PathVariable String ticketId,
            @Parameter(description = "撤销原因", required = true, example = "审计工作已完成")
            @RequestParam String reason,
            @Parameter(description = "操作人ID", hidden = true)
            @RequestHeader("X-User-Id") String operatorId) {

        try {
            authTicketService.revokeAuthTicket(ticketId, operatorId, reason);
            return Result.success(null);
        } catch (Exception e) {
            log.error("撤销授权票据失败: ticketId={}", ticketId, e);
            return Result.error("撤销授权票据失败: " + e.getMessage());
        }
    }

    @PostMapping("/{ticketId}/first-approval")
    @Operation(
        summary = "第一审批人审批",
        description = """
            第一审批人对授权票据进行审批。

            **路径参数:**
            - ticketId: 票据 ID

            **请求参数:**
            - comment: 审批意见（必填）
            - approved: 是否批准（必填，true=批准，false=驳回）

            **请求头:**
            - X-User-Id: 审批人 ID
            - X-User-Name: 审批人姓名

            **业务规则:**
            - 只有第一审批人角色可以操作
            - 票据状态必须为 PENDING
            - 批准后状态变更为 FIRST_APPROVED
            - 驳回后状态变更为 REJECTED

            **使用场景:**
            - 部门负责人审批
            """,
        operationId = "firstApproval",
        tags = {"授权票据管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "审批成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或票据状态不允许审批"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "票据不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:approve', 'archive:manage')")
    public Result<Void> firstApproval(
            @Parameter(description = "票据ID", required = true, example = "TKT-20250114-001")
            @PathVariable String ticketId,
            @Parameter(description = "审批意见", required = true, example = "同意授权")
            @RequestParam String comment,
            @Parameter(description = "是否批准", required = true, example = "true")
            @RequestParam boolean approved,
            @Parameter(description = "审批人ID", hidden = true)
            @RequestHeader("X-User-Id") String approverId,
            @Parameter(description = "审批人姓名", hidden = true)
            @RequestHeader("X-User-Name") String approverName) {

        try {
            authTicketApprovalService.firstApproval(ticketId, approverId, approverName, comment, approved);
            return Result.success(null);
        } catch (Exception e) {
            log.error("第一审批失败: ticketId={}", ticketId, e);
            return Result.error("第一审批失败: " + e.getMessage());
        }
    }

    @PostMapping("/{ticketId}/second-approval")
    @Operation(
        summary = "第二审批人审批（复核）",
        description = """
            第二审批人对授权票据进行复核审批。

            **路径参数:**
            - ticketId: 票据 ID

            **请求参数:**
            - comment: 审批意见（必填）
            - approved: 是否批准（必填，true=批准，false=驳回）

            **请求头:**
            - X-User-Id: 审批人 ID
            - X-User-Name: 审批人姓名

            **业务规则:**
            - 只有第二审批人角色可以操作
            - 票据状态必须为 FIRST_APPROVED
            - 批准后状态变更为 APPROVED（票据生效）
            - 驳回后状态变更为 REJECTED

            **使用场景:**
            - 安全管理员复核
            - 最终授权确认
            """,
        operationId = "secondApproval",
        tags = {"授权票据管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "审批成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或票据状态不允许审批"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "票据不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:approve', 'archive:manage')")
    public Result<Void> secondApproval(
            @Parameter(description = "票据ID", required = true, example = "TKT-20250114-001")
            @PathVariable String ticketId,
            @Parameter(description = "审批意见", required = true, example = "复核通过")
            @RequestParam String comment,
            @Parameter(description = "是否批准", required = true, example = "true")
            @RequestParam boolean approved,
            @Parameter(description = "审批人ID", hidden = true)
            @RequestHeader("X-User-Id") String approverId,
            @Parameter(description = "审批人姓名", hidden = true)
            @RequestHeader("X-User-Name") String approverName) {

        try {
            authTicketApprovalService.secondApproval(ticketId, approverId, approverName, comment, approved);
            return Result.success(null);
        } catch (Exception e) {
            log.error("第二审批失败: ticketId={}", ticketId, e);
            return Result.error("第二审批失败: " + e.getMessage());
        }
    }
}
