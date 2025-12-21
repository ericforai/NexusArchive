package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.ErpSubInterface;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.mapper.ErpSubInterfaceMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.service.ErpScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/erp/scenario")
@RequiredArgsConstructor
@Tag(name = "ERP业务场景管理")
public class ErpScenarioController {

    private final ErpScenarioService erpScenarioService;
    private final ErpSubInterfaceMapper subInterfaceMapper;
    private final SyncHistoryMapper syncHistoryMapper;

    @GetMapping("/list/{configId}")
    @Operation(summary = "获取指定ERP配置的场景列表")
    public Result<List<ErpScenario>> listByConfig(@PathVariable Long configId) {
        return Result.success(erpScenarioService.listScenariosByConfigId(configId));
    }

    @PutMapping
    @Operation(summary = "更新场景配置")
    public Result<Void> update(@RequestBody ErpScenario scenario) {
        erpScenarioService.updateScenario(scenario);
        return Result.success();
    }

    @PostMapping("/{id}/sync")
    @Operation(summary = "手动触发同步")
    @com.nexusarchive.annotation.ArchivalAudit(operationType = "CAPTURE", resourceType = "ERP_SYNC", description = "手动触发ERP同步场景")
    public Result<Void> triggerSync(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        String clientIp = getClientIp(request);
        erpScenarioService.syncScenario(id, operatorId, clientIp);
        return Result.success();
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
    public Result<List<com.nexusarchive.dto.IntegrationChannelDTO>> listAllChannels() {
        return Result.success(erpScenarioService.listAllChannels());
    }

    // ============ 子接口管理 API ============

    @GetMapping("/{scenarioId}/interfaces")
    @Operation(summary = "获取场景的子接口列表")
    public Result<List<ErpSubInterface>> listSubInterfaces(@PathVariable Long scenarioId) {
        List<ErpSubInterface> interfaces = subInterfaceMapper.selectList(
            new LambdaQueryWrapper<ErpSubInterface>()
                .eq(ErpSubInterface::getScenarioId, scenarioId)
                .orderByAsc(ErpSubInterface::getSortOrder)
        );
        return Result.success(interfaces);
    }

    @PutMapping("/interface")
    @Operation(summary = "更新子接口配置")
    public Result<Void> updateSubInterface(@RequestBody ErpSubInterface subInterface) {
        subInterface.setLastModifiedTime(java.time.LocalDateTime.now());
        subInterfaceMapper.updateById(subInterface);
        return Result.success();
    }

    @PutMapping("/interface/toggle/{id}")
    @Operation(summary = "切换子接口启用状态")
    public Result<Void> toggleSubInterface(@PathVariable Long id) {
        ErpSubInterface sub = subInterfaceMapper.selectById(id);
        if (sub != null) {
            sub.setIsActive(!sub.getIsActive());
            sub.setLastModifiedTime(java.time.LocalDateTime.now());
            subInterfaceMapper.updateById(sub);
        }
        return Result.success();
    }

    // ============ 同步历史 API ============

    @GetMapping("/{scenarioId}/history")
    @Operation(summary = "获取场景的同步历史 (最近10条)")
    public Result<List<SyncHistory>> getSyncHistory(@PathVariable Long scenarioId) {
        List<SyncHistory> history = syncHistoryMapper.selectList(
            new LambdaQueryWrapper<SyncHistory>()
                .eq(SyncHistory::getScenarioId, scenarioId)
                .orderByDesc(SyncHistory::getSyncStartTime)
                .last("LIMIT 10")
        );
        return Result.success(history);
    }

    // ============ 场景参数配置 API ============

    @PutMapping("/{id}/params")
    @Operation(summary = "更新场景参数配置")
    public Result<Void> updateScenarioParams(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        erpScenarioService.updateScenarioParams(id, params);
        return Result.success();
    }
}
