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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;
    private final AsyncErpSyncService asyncErpSyncService;

    @GetMapping("/list/{configId}")
    @Operation(summary = "获取指定ERP配置的场景列表")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<List<ErpScenario>> listByConfig(@PathVariable Long configId) {
        return Result.success(erpScenarioService.listScenariosByConfigId(configId));
    }

    @PutMapping
    @Operation(summary = "更新场景配置")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "UPDATE", resourceType = "ERP_SCENARIO", description = "更新ERP场景配置")
    public Result<Void> update(@Valid @RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步（异步）")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin')")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "CAPTURE", resourceType = "ERP_SYNC", description = "手动触发ERP同步场景")
    public Result<SyncTaskDTO> triggerSync(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);

        // Submit async task
        SyncTaskDTO task = asyncErpSyncService.submitSyncTask(id);
        asyncErpSyncService.syncScenarioAsync(task.getTaskId(), id, operatorId, clientIp);

        return Result.success(task);
    }

    @GetMapping("/{id}/sync/status/{taskId}")
    @Operation(summary = "查询同步任务状态")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'super_admin', 'ARCHIVE_MANAGER')")
    public Result<SyncTaskStatus> getSyncStatus(@PathVariable Long id, @PathVariable String taskId) {
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
