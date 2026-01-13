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
@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理", description = "ERP集成业务场景的配置、同步和监控")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;
    private final AsyncErpSyncService asyncErpSyncService;

    @GetMapping("/list/{configId}")
    @Operation(summary = "获取指定ERP配置的场景列表", description = "根据ERP配置ID查询其下配置的所有业务场景")
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
    @Operation(summary = "更新场景配置", description = "更新ERP业务场景的配置信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO", description = "更新ERP场景配置")
    public Result<Void> update(@Valid @RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(
            summary = "手动触发同步（异步）",
            description = "手动触发指定场景的数据同步任务。任务在后台异步执行，接口立即返回任务ID。可以通过 /sync/status/{taskId} 接口查询任务执行状态。"
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
    @Operation(summary = "获取场景的同步任务列表", description = "查询指定场景的所有同步任务记录")
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
    @Operation(summary = "查询同步任务状态", description = "查询指定同步任务的执行状态和进度")
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
    @Operation(summary = "获取所有集成通道（聚合视图）")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<com.nexusarchive.dto.IntegrationChannelDTO>> listAllChannels() {
        return Result.success(erpScenarioService.listAllChannels());
    }

    // ============ 子接口管理 API ============

    @GetMapping("/{scenarioId}/interfaces")
    @Operation(summary = "获取场景的子接口列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpSubInterface>> listSubInterfaces(@PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.listSubInterfaces(scenarioId));
    }

    @PutMapping("/interface")
    @Operation(summary = "更新子接口配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "更新ERP子接口配置")
    public Result<Void> updateSubInterface(@Valid @RequestBody ErpSubInterface subInterface,
                                            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.updateSubInterface(subInterface, operatorId, clientIp);
        return Result.success();
    }

    @PutMapping("/interface/toggle/{id}")
    @Operation(summary = "切换子接口启用状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SUB_INTERFACE", description = "切换ERP子接口启用状态")
    public Result<Void> toggleSubInterface(@PathVariable Long id,
                                           jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.toggleSubInterface(id, operatorId, clientIp);
        return Result.success();
    }

    // ============ 同步历史 API ============

    @GetMapping("/{scenarioId}/history")
    @Operation(summary = "获取场景的同步历史 (最近10条)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER', 'AUDITOR')")
    public Result<List<SyncHistory>> getSyncHistory(@PathVariable Long scenarioId) {
        return Result.success(erpScenarioService.getSyncHistory(scenarioId));
    }

    // ============ 场景参数配置 API ============

    @PutMapping("/{id}/params")
    @Operation(summary = "更新场景参数配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO_PARAMS", description = "更新ERP场景参数配置")
    public Result<Void> updateScenarioParams(@PathVariable Long id,
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
