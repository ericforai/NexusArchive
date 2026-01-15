// Input: MyBatis-Plus、io.swagger、Lombok、Spring Framework、等
// Output: ErpScenarioController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.SyncTaskDTO;
import com.nexusarchive.dto.SyncTaskStatus;
import com.nexusarchive.dto.request.ScenarioParamsUpdateRequest;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.ErpSubInterface;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.service.ErpScenarioService;
import com.nexusarchive.service.erp.AsyncErpSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * ERP 业务场景管理控制器
 * <p>
 * 管理 ERP 系统集成业务场景的配置和同步。
 * 支持手动触发同步、查询同步状态、同步历史等功能。
 * </p>
 */
@Tag(name = "ERP业务场景管理", description = """
    ERP 集成业务场景的配置、同步和监控接口。

    **功能说明:**
    - 管理业务场景配置（凭证、退款单、分录等）
    - 手动触发异步同步任务
    - 查询同步任务状态和进度
    - 子接口配置管理
    - 同步历史记录查询
    - 场景参数配置

    **业务场景类型:**
    - voucher: 凭证同步
    - refund: 退款单同步
    - entry: 分录同步
    - org: 组织架构同步
    - inventory: 库存同步

    **同步策略:**
    - MANUAL: 手动触发
    - SCHEDULED: 定时执行
    - REALTIME: 实时推送

    **异步任务:**
    - 同步任务在后台异步执行
    - 接口立即返回任务 ID
    - 通过任务 ID 查询执行状态

    **使用场景:**
    - 在线采集手动同步
    - 场景配置管理
    - 同步任务监控
    - 历史记录追溯

    **权限要求:**
    - SYSTEM_ADMIN: 系统管理员（全部权限）
    - super_admin: 超级管理员（全部权限）
    - ARCHIVE_MANAGER: 档案管理员（查看+手动同步）
    - AUDITOR: 审计员（仅查看）
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;
    private final AsyncErpSyncService asyncErpSyncService;

    @GetMapping("/list/{configId}")
    @Operation(
        summary = "获取指定ERP配置的场景列表",
        description = """
            根据ERP配置ID查询其下配置的所有业务场景。

            **路径参数:**
            - configId: ERP配置ID

            **返回数据包括:**
            - id: 场景ID
            - scenarioKey: 场景标识
            - scenarioName: 场景名称
            - syncStrategy: 同步策略
            - isEnabled: 是否启用
            - lastSyncTime: 最后同步时间

            **使用场景:**
            - 场景列表展示
            - 配置页面加载
            """,
        operationId = "listErpScenarios",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpScenario>> listByConfig(
            @Parameter(description = "ERP配置ID", required = true, example = "1") @PathVariable Long configId) {
        return Result.success(erpScenarioService.listScenariosByConfigId(configId));
    }

    @PutMapping
    @Operation(
        summary = "更新场景配置",
        description = """
            更新ERP业务场景的配置信息。

            **请求体:**
            - id: 场景ID（必填）
            - scenarioName: 场景名称
            - syncStrategy: 同步策略
            - isEnabled: 是否启用
            - paramsJson: 参数配置（JSON）

            **业务规则:**
            - 场景标识（scenarioKey）不可修改
            - 更新操作会被审计记录

            **使用场景:**
            - 修改场景配置
            - 启用/禁用场景
            """,
        operationId = "updateErpScenario",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO", description = "更新ERP场景配置")
    public Result<Void> update(
            @Parameter(description = "场景配置", required = true)
            @Valid @RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(
        summary = "手动触发同步（异步）",
        description = """
            手动触发指定场景的数据同步任务。任务在后台异步执行，接口立即返回任务ID。可以通过 /sync/status/{taskId} 接口查询任务执行状态。

            **路径参数:**
            - id: 场景ID

            **请求体（可选）:**
            - periodStart: 开始日期（YYYY-MM-DD）
            - periodEnd: 结束日期（YYYY-MM-DD）
            - startDate: 开始日期（兼容参数名）
            - endDate: 结束日期（兼容参数名）

            **返回数据包括:**
            - taskId: 任务ID（用于查询状态）
            - scenarioId: 场景ID
            - status: 任务状态（PROCESSING/COMPLETED/FAILED）
            - createdAt: 创建时间

            **业务规则:**
            - 同步任务异步执行
            - 支持按日期范围过滤数据
            - 任务执行完成会记录同步历史

            **使用场景:**
            - 在线采集手动同步
            - 按日期范围获取数据
            """,
        operationId = "triggerScenarioSync",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "同步任务已提交", content = @Content(schema = @Schema(implementation = SyncTaskDTO.class))),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "场景不存在")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "CAPTURE", resourceType = "ERP_SYNC", description = "手动触发ERP同步场景")
    public Result<SyncTaskDTO> triggerSync(
            @Parameter(description = "场景ID", required = true) @PathVariable Long id,
            @Parameter(description = "同步参数（可选）", required = false) @RequestBody(required = false) java.util.Map<String, Object> params,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);

        // 提取可选的日期参数（支持多种参数名以兼容不同前端）
        String tempStartDate = null;
        String tempEndDate = null;
        if (params != null) {
            // 优先使用 periodStart/periodEnd（前端"在线采集"页面使用）
            tempStartDate = (String) params.get("periodStart");
            tempEndDate = (String) params.get("periodEnd");
            // 如果没有，尝试使用 startDate/endDate
            if (tempStartDate == null) tempStartDate = (String) params.get("startDate");
            if (tempEndDate == null) tempEndDate = (String) params.get("endDate");
        }

        // Submit async task with operator info
        // 获取当前全宗上下文，传递给异步线程（因为异步线程不会继承 ThreadLocal）
        String currentFonds = com.nexusarchive.security.FondsContext.getCurrentFondsNo();

        SyncTaskDTO task = asyncErpSyncService.submitSyncTask(id, operatorId, clientIp);
        asyncErpSyncService.syncScenarioAsync(task.getTaskId(), id, operatorId, clientIp, tempStartDate, tempEndDate, currentFonds);

        return Result.success(task);
    }

    @GetMapping("/{id}/sync/tasks")
    @Operation(
        summary = "获取场景的同步任务列表",
        description = """
            查询指定场景的所有同步任务记录。

            **路径参数:**
            - id: 场景ID

            **返回数据包括:**
            - taskId: 任务ID
            - status: 任务状态
            - startTime: 开始时间
            - endTime: 结束时间
            - resultCount: 结果数量
            - errorMessage: 错误消息

            **使用场景:**
            - 查看同步历史
            - 任务列表展示
            """,
        operationId = "listScenarioSyncTasks",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<com.nexusarchive.entity.SyncTask>> listSyncTasks(
            @Parameter(description = "场景ID", required = true) @PathVariable Long id) {
        return Result.success(asyncErpSyncService.getTasksByScenario(id));
    }

    @GetMapping("/{id}/sync/status/{taskId}")
    @Operation(
        summary = "查询同步任务状态",
        description = """
            查询指定同步任务的执行状态和进度。

            **路径参数:**
            - id: 场景ID
            - taskId: 任务ID

            **返回数据包括:**
            - taskId: 任务ID
            - status: 任务状态（PENDING/PROCESSING/COMPLETED/FAILED）
            - progress: 进度百分比
            - total: 总记录数
            - processed: 已处理数
            - failed: 失败数
            - message: 状态消息

            **使用场景:**
            - 轮询任务状态
            - 进度展示
            """,
        operationId = "getSyncTaskStatus",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<SyncTaskStatus> getSyncStatus(
            @Parameter(description = "场景ID", required = true) @PathVariable Long id,
            @Parameter(description = "任务ID", required = true) @PathVariable String taskId) {
        SyncTaskStatus status = asyncErpSyncService.getTaskStatus(taskId);
        if (status == null) {
            return Result.error("任务不存在: " + taskId);
        }
        return Result.success(status);
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
        return null; // 系统或未知用户
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "UNKNOWN";
    }

    @GetMapping("/channels")
    @Operation(
        summary = "获取所有集成通道（聚合视图）",
        description = """
            获取所有 ERP 集成通道的聚合视图。

            **返回数据包括:**
            - configId: ERP配置ID
            - configName: 配置名称
            - erpType: ERP类型
            - scenarios: 场景列表
            - activeCount: 启用场景数
            - totalCount: 总场景数

            **使用场景:**
            - 集成通道概览
            - 监控面板展示
            """,
        operationId = "listIntegrationChannels",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<com.nexusarchive.dto.IntegrationChannelDTO>> listAllChannels() {
        return Result.success(erpScenarioService.listAllChannels());
    }

    // ============ 子接口管理 API ============

    @GetMapping("/{scenarioId}/interfaces")
    @Operation(
        summary = "获取场景的子接口列表",
        description = """
            获取指定场景的所有子接口配置。

            **路径参数:**
            - scenarioId: 场景ID

            **返回数据包括:**
            - id: 子接口ID
            - interfaceKey: 接口标识
            - interfaceName: 接口名称
            - isEnabled: 是否启用
            - lastSyncTime: 最后同步时间

            **使用场景:**
            - 子接口管理
            - 接口启用/禁用
            """,
        operationId = "listSubInterfaces",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpSubInterface>> listSubInterfaces(
            @Parameter(description = "场景ID", required = true) @PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.listSubInterfaces(scenarioId));
    }

    @PutMapping("/interface")
    @Operation(
        summary = "更新子接口配置",
        description = """
            更新 ERP 子接口的配置信息。

            **请求体:**
            - id: 子接口ID（必填）
            - interfaceName: 接口名称
            - paramsJson: 接口参数
            - isEnabled: 是否启用

            **使用场景:**
            - 修改子接口配置
            - 启用/禁用子接口
            """,
        operationId = "updateSubInterface",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "更新ERP子接口配置")
    public Result<Void> updateSubInterface(
            @Parameter(description = "子接口配置", required = true)
            @Valid @RequestBody ErpSubInterface subInterface,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.updateSubInterface(subInterface, operatorId, clientIp);
        return Result.success();
    }

    @PutMapping("/interface/toggle/{id}")
    @Operation(
        summary = "切换子接口启用状态",
        description = """
            切换指定子接口的启用/禁用状态。

            **路径参数:**
            - id: 子接口ID

            **业务规则:**
            - 切换操作会记录审计日志

            **使用场景:**
            - 快速启用/禁用子接口
            """,
        operationId = "toggleSubInterface",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "切换成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "子接口不存在")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "切换ERP子接口启用状态")
    public Result<Void> toggleSubInterface(
            @Parameter(description = "子接口ID", required = true) @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.toggleSubInterface(id, operatorId, clientIp);
        return Result.success();
    }

    // ============ 同步历史 API ============

    @GetMapping("/{scenarioId}/history")
    @Operation(
        summary = "获取场景的同步历史 (最近10条)",
        description = """
            获取指定场景的同步历史记录（最近10条）。

            **路径参数:**
            - scenarioId: 场景ID

            **返回数据包括:**
            - id: 历史记录ID
            - syncTime: 同步时间
            - operatorId: 操作人
            - resultCount: 结果数量
            - status: 状态
            - errorMessage: 错误消息

            **使用场景:**
            - 查看同步历史
            - 问题追溯
            """,
        operationId = "getSyncHistory",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<SyncHistory>> getSyncHistory(
            @Parameter(description = "场景ID", required = true) @PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.getSyncHistory(scenarioId));
    }

    // ============ 场景参数配置 API ============

    @PutMapping("/{id}/params")
    @Operation(
        summary = "更新场景参数配置",
        description = """
            更新 ERP 场景的参数配置。

            **路径参数:**
            - id: 场景ID

            **请求体:**
            - scenarioKey: 场景标识
            - syncStrategy: 同步策略（MANUAL/SCHEDULED/REALTIME）
            - periodDays: 增量同步天数
            - mapping: 字段映射配置
            - filter: 数据过滤条件

            **业务规则:**
            - 更新操作会被审计记录

            **使用场景:**
            - 配置同步策略
            - 设置字段映射
            """,
        operationId = "updateScenarioParams",
        tags = {"ERP业务场景管理"}
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO_PARAMS", description = "更新ERP场景参数配置")
    public Result<Void> updateScenarioParams(
            @Parameter(description = "场景ID", required = true) @PathVariable Long id,
            @Parameter(description = "场景参数配置", required = true)
            @Valid @RequestBody ScenarioParamsUpdateRequest request) {
        // Convert validated DTO to Map
        Map<String, Object> params = convertToParamsMap(request);
        erpScenarioService.updateScenarioParams(id, params);
        return Result.success();
    }

    private Map<String, Object> convertToParamsMap(ScenarioParamsUpdateRequest request) {
        Map<String, Object> params = new HashMap<>();
        params.put("scenarioKey", request.getScenarioKey());
        params.put("syncStrategy", request.getSyncStrategy());
        if (request.getPeriodDays() != null) {
            params.put("periodDays", request.getPeriodDays());
        }
        if (request.getMapping() != null) {
            params.put("mapping", request.getMapping());
        }
        if (request.getFilter() != null) {
            params.put("filter", request.getFilter());
        }
        return params;
    }
}
