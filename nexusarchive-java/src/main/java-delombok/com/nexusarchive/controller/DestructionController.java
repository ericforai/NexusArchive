// Input: MyBatis-Plus、Lombok、Spring Framework、Spring Security、DtoMapper、DestructionResponse
// Output: DestructionController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.DestructionResponse;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.service.DestructionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.annotation.ArchivalAudit;

import jakarta.validation.Valid;

/**
 * 档案销毁控制器
 *
 * PRD 来源: 档案销毁管理模块
 * 提供档案销毁申请、审批、执行功能
 *
 * <p>符合《会计档案管理办法》销毁流程要求</p>
 */
@Tag(name = "档案销毁", description = """
    档案销毁管理接口。

    **功能说明:**
    - 创建销毁申请
    - 获取销毁申请列表
    - 批准销毁申请
    - 执行销毁操作
    - 获取销毁统计信息

    **销毁状态:**
    - PENDING: 待审批
    - APPROVED: 已批准
    - REJECTED: 已拒绝
    - EXECUTING: 执行中
    - COMPLETED: 已完成
    - FAILED: 执行失败

    **销毁流程:**
    1. 用户创建销毁申请
    2. 审批人批准/拒绝
    3. 批准后进入待执行状态
    4. 执行人执行销毁操作
    5. 记录销毁结果和审计日志

    **销毁方式:**
    - 逻辑删除: 标记为已销毁，物理文件保留
    - 物理销毁: 彻底删除物理文件（需要二次确认）

    **使用场景:**
    - 到期档案销毁
    - 批量销毁处理
    - 销毁申请审批

    **权限要求:**
    - archive:destruction: 销毁管理权限
    - archive:read: 查看权限
    - archive:manage: 管理权限
    - nav:all: 全部导航权限
    - SYSTEM_ADMIN: 系统管理员

    **业务规则:**
    - 销毁操作会被审计记录
    - 已批准的销毁申请不可撤销
    - 执行销毁前需再次确认
    - 销毁完成后生成销毁证明
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/destruction")
@RequiredArgsConstructor
public class DestructionController {

    private final DestructionService destructionService;
    private final DtoMapper dtoMapper;

    /**
     * 创建销毁申请
     */
    @PostMapping
    @Operation(
        summary = "创建销毁申请",
        description = """
            创建新的档案销毁申请。

            **请求体:**
            - batchId: 批次ID（如果基于批次创建）
            - archiveIds: 档案ID列表
            - reason: 销毁原因（必填）
            - applicantId: 申请人ID（可选，自动获取）
            - applicantName: 申请人姓名（可选，自动获取）

            **返回数据:**
            - 创建的销毁申请详情

            **业务规则:**
            - 申请人ID自动从认证上下文获取
            - 初始状态为 PENDING
            - 销毁原因必填

            **使用场景:**
            - 创建到期档案销毁申请
            - 创建批次销毁申请
            """,
        operationId = "createDestruction",
        tags = {"档案销毁"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_DESTRUCTION", resourceType = "DESTRUCTION", description = "创建销毁申请")
    public Result<DestructionResponse> createDestruction(
            @Parameter(description = "销毁申请信息", required = true)
            @Valid @RequestBody Destruction destruction,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (destruction.getApplicantId() == null && user != null) {
            destruction.setApplicantId(user.getId());
            destruction.setApplicantName(user.getFullName());
        }
        Destruction created = destructionService.createDestruction(destruction);
        return Result.success(dtoMapper.toDestructionResponse(created));
    }

    /**
     * 获取销毁申请列表
     */
    @GetMapping
    @Operation(
        summary = "获取销毁申请列表",
        description = """
            分页查询销毁申请列表。

            **查询参数:**
            - page: 页码（从 1 开始）
            - limit: 每页条数
            - status: 状态过滤（可选）

            **返回数据包括:**
            - records: 销毁申请记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **状态过滤:**
            - PENDING: 待审批
            - APPROVED: 已批准
            - EXECUTING: 执行中
            - COMPLETED: 已完成
            - REJECTED: 已拒绝
            - FAILED: 执行失败

            **使用场景:**
            - 销毁申请列表展示
            - 待办事项查询
            - 销毁历史追溯
            """,
        operationId = "getDestructions",
        tags = {"档案销毁"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<DestructionResponse>> getDestructions(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "状态过滤", example = "PENDING")
            @RequestParam(required = false) String status) {
        Page<Destruction> entityPage = destructionService.getDestructions(page, limit, status);
        return Result.success(dtoMapper.toDestructionResponsePage(entityPage));
    }

    /**
     * 批准销毁申请
     */
    @PostMapping("/{id}/approve")
    @Operation(
        summary = "批准销毁申请",
        description = """
            批准档案销毁申请，申请状态变为已批准。

            **路径参数:**
            - id: 销毁申请ID

            **请求体:**
            - comment: 审批意见（可选）

            **业务规则:**
            - 仅 PENDING 状态可批准
            - 批准后状态变为 APPROVED
            - 审批意见建议填写
            - 批准操作会被审计记录

            **使用场景:**
            - 单条销毁申请批准
            """,
        operationId = "approveDestruction",
        tags = {"档案销毁"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批准成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "销毁申请不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE_DESTRUCTION", resourceType = "DESTRUCTION", description = "审批销毁")
    public Result<Void> approveDestruction(
            @Parameter(description = "销毁申请ID", required = true, example = "des-001")
            @PathVariable String id,
            @Parameter(description = "审批意见")
            @RequestBody(required = false) String comment,
            @AuthenticationPrincipal CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        destructionService.approveDestruction(id, approverId, comment);
        return Result.success();
    }

    /**
     * 执行销毁操作
     */
    @PostMapping("/{id}/execute")
    @Operation(
        summary = "执行销毁操作",
        description = """
            执行已批准的档案销毁操作。

            **路径参数:**
            - id: 销毁申请ID

            **业务规则:**
            - 仅 APPROVED 状态可执行
            - 执行后状态变为 EXECUTING 或 COMPLETED
            - 执行操作会被审计记录
            - 销毁完成后生成销毁证明

            **销毁方式:**
            - 逻辑删除: 标记为已销毁
            - 物理销毁: 彻底删除文件（需二次确认）

            **使用场景:**
            - 执行已批准的销毁申请
            """,
        operationId = "executeDestruction",
        tags = {"档案销毁"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "执行成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许执行"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "销毁申请不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "EXECUTE_DESTRUCTION", resourceType = "DESTRUCTION", description = "执行销毁")
    public Result<Void> executeDestruction(
            @Parameter(description = "销毁申请ID", required = true, example = "des-001")
            @PathVariable String id) {
        destructionService.executeDestruction(id);
        return Result.success();
    }

    /**
     * 获取销毁统计信息
     */
    @GetMapping("/stats")
    @Operation(
        summary = "获取销毁统计信息",
        description = """
            获取档案销毁相关统计数据。

            **返回数据包括:**
            - pendingAppraisal: 待鉴定档案数量
            - aiSuggested: AI 建议销毁数量
            - activeBatches: 进行中的销毁批次数量
            - safeDestructionCount: 已安全销毁数量

            **统计指标:**
            - 待鉴定档案: 保管期限已到的档案
            - AI 建议销毁: 根据规则建议销毁的档案
            - 进行中批次: 状态为 PENDING 的销毁申请
            - 已安全销毁: 状态为 COMPLETED 的销毁记录

            **使用场景:**
            - 销毁工作台统计数据
            - 销毁进度监控
            - 到期档案提醒
            """,
        operationId = "getDestructionStats",
        tags = {"档案销毁"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        // 1. 待鉴定档案 (模拟查询: 真实应查询 acc_archive where retention_period expired)
        stats.put("pendingAppraisal", 0);

        // 2. AI 建议销毁
        stats.put("aiSuggested", 0);

        // 3. 进行中批次
        stats.put("activeBatches", destructionService.getDestructions(1, 100, "PENDING").getTotal());

        // 4. 已安全销毁 (模拟)
        stats.put("safeDestructionCount", 0);

        return Result.success(stats);
    }
}
