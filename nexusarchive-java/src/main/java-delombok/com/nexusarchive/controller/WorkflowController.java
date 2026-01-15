// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: WorkflowController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.workflow.WorkflowTaskDto;
import com.nexusarchive.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 *
 * <p>提供工作流启动、任务审批等流程管理功能</p>
 */
@Tag(name = "工作流管理", description = """
    工作流引擎接口。

    **功能说明:**
    - 启动工作流实例
    - 查询待办任务
    - 任务批准/拒绝
    - 流程进度跟踪

    **工作流类型:**
    - ARCHIVE_APPROVAL: 归档审批流程
    - DESTRUCTION_APPROVAL: 销毁审批流程
    - BATCH_APPROVAL: 批次审批流程
    - BORROW_APPROVAL: 借阅审批流程

    **任务状态:**
    - PENDING: 待处理
    - IN_PROGRESS: 处理中
    - APPROVED: 已批准
    - REJECTED: 已拒绝
    - CANCELLED: 已取消

    **审批规则:**
    - 支持多级审批
    - 支持并行审批
    - 支持会签审批
    - 支持驳回重审

    **使用场景:**
    - 归档审批
    - 销毁审批
    - 借阅审批
    - 批次审批

    **权限要求:**
    - workflow:manage: 工作流管理权限
    - workflow:approve: 审批权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    /**
     * 启动工作流
     */
    @PostMapping("/start")
    @Operation(
        summary = "启动工作流",
        description = """
            启动指定类型的工作流实例。

            **请求参数:**
            - workflowCode: 工作流编码（必填）
              - ARCHIVE_APPROVAL: 归档审批
              - DESTRUCTION_APPROVAL: 销毁审批
              - BORROW_APPROVAL: 借阅审批
            - businessId: 业务 ID（必填）

            **返回数据:**
            - instanceId: 流程实例 ID
            - status: 流程状态
            - currentStep: 当前步骤

            **业务规则:**
            - 自动创建流程实例
            - 自动分配待办任务
            - 记录启动日志
            - 发送待办通知

            **使用场景:**
            - 提交归档申请
            - 提交销毁申请
            - 提交借阅申请
            """,
        operationId = "startWorkflow",
        tags = {"工作流管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "工作流启动成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或工作流不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Void> startWorkflow(
            @Parameter(description = "启动工作流请求参数", required = true,
                    schema = @Schema(example = "{\"workflowCode\": \"ARCHIVE_APPROVAL\", \"businessId\": \"BIZ-123\"}"))
            @RequestBody Map<String, String> payload) {
        String workflowCode = payload.get("workflowCode");
        String businessId = payload.get("businessId");
        // Mock initiator
        String initiator = "current-user";
        workflowService.startWorkflow(workflowCode, businessId, initiator);
        return Result.success();
    }

    /**
     * 查询待办任务
     */
    @GetMapping("/tasks")
    @Operation(
        summary = "查询当前用户的待办任务",
        description = """
            获取当前用户的所有待办任务。

            **返回数据 (WorkflowTaskDto):**
            - taskId: 任务 ID
            - taskName: 任务名称
            - workflowCode: 工作流编码
            - businessId: 业务 ID
            - status: 任务状态
            - createTime: 创建时间
            - assignee: 处理人
            - dueDate: 到期时间

            **查询规则:**
            - 只返回当前用户的待办
            - 按创建时间倒序
            - 分页返回结果

            **使用场景:**
            - 待办任务列表
            - 我的待办
            - 任务提醒
            """,
        operationId = "getWorkflowTasks",
        tags = {"工作流管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<List<WorkflowTaskDto>> getTasks() {
        // Mock current user
        String currentUser = "admin";
        return Result.success(workflowService.getTasks(currentUser));
    }

    /**
     * 批准任务
     */
    @PostMapping("/instances/{id}/approve")
    @Operation(
        summary = "批准审批任务",
        description = """
            批准指定的审批任务，流程进入下一环节。

            **路径参数:**
            - id: 任务 ID

            **请求参数（可选）:**
            - comment: 审批意见

            **业务规则:**
            - 任务状态变更为 APPROVED
            - 自动流转到下一环节
            - 记录审批日志
            - 发送状态通知

            **使用场景:**
            - 归档审批批准
            - 销毁审批批准
            - 借阅审批批准
            """,
        operationId = "approveTask",
        tags = {"工作流管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批准成功"),
        @ApiResponse(responseCode = "400", description = "任务状态不允许批准"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限或非任务处理人"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public Result<Void> approveTask(
            @Parameter(description = "任务ID", required = true, example = "TASK-123") @PathVariable String id,
            @Parameter(description = "审批意见（可选）",
                    schema = @Schema(example = "{\"comment\": \"资料齐全，同意归档\"}"))
            @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : "";
        workflowService.completeTask(id, comment, true);
        return Result.success();
    }

    /**
     * 拒绝任务
     */
    @PostMapping("/instances/{id}/reject")
    @Operation(
        summary = "拒绝审批任务",
        description = """
            拒绝指定的审批任务，流程终止或驳回。

            **路径参数:**
            - id: 任务 ID

            **请求参数（可选）:**
            - comment: 拒绝原因

            **业务规则:**
            - 任务状态变更为 REJECTED
            - 流程终止或驳回发起人
            - 记录拒绝日志
            - 发送拒绝通知

            **使用场景:**
            - 归档审批拒绝
            - 销毁审批拒绝
            - 借阅审批拒绝
            """,
        operationId = "rejectTask",
        tags = {"工作流管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "拒绝成功"),
        @ApiResponse(responseCode = "400", description = "任务状态不允许拒绝"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限或非任务处理人"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public Result<Void> rejectTask(
            @Parameter(description = "任务ID", required = true, example = "TASK-123") @PathVariable String id,
            @Parameter(description = "拒绝原因（可选）",
                    schema = @Schema(example = "{\"comment\": \"资料不全，请补充后重新提交\"}"))
            @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : "";
        workflowService.completeTask(id, comment, false);
        return Result.success();
    }
}
