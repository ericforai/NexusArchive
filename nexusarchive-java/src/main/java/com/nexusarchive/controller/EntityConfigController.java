// Input: Spring Web、EntityConfigService、Result
// Output: EntityConfigController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.EntityConfig;
import com.nexusarchive.service.EntityConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 法人配置控制器
 * 
 * 功能: 管理每个法人的独立配置（ERP接口、业务规则、合规策略等）
 */
@Slf4j
@RestController
@RequestMapping("/api/entity-config")
@RequiredArgsConstructor
@Tag(name = "法人配置管理", description = "法人级别配置管理接口")
public class EntityConfigController {
    
    private final EntityConfigService configService;
    
    @GetMapping("/entity/{entityId}")
    @Operation(summary = "查询指定法人的所有配置")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityConfig>> getConfigsByEntityId(@PathVariable String entityId) {
        return Result.success(configService.getConfigsByEntityId(entityId));
    }
    
    @GetMapping("/entity/{entityId}/type/{configType}")
    @Operation(summary = "查询指定法人和配置类型的配置")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityConfig>> getConfigsByEntityIdAndType(
            @PathVariable String entityId,
            @PathVariable String configType) {
        return Result.success(configService.getConfigsByEntityIdAndType(entityId, configType));
    }
    
    @GetMapping("/entity/{entityId}/grouped")
    @Operation(summary = "查询指定法人的配置（按类型分组）")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, List<EntityConfig>>> getConfigsGroupedByType(@PathVariable String entityId) {
        return Result.success(configService.getConfigsGroupedByType(entityId));
    }
    
    @PostMapping
    @Operation(summary = "保存或更新配置")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> saveOrUpdateConfig(@RequestBody EntityConfig config) {
        if (config.getEntityId() == null || config.getConfigType() == null || config.getConfigKey() == null) {
            return Result.error("法人ID、配置类型和配置键不能为空");
        }
        String configId = configService.saveOrUpdateConfig(
            config.getEntityId(),
            config.getConfigType(),
            config.getConfigKey(),
            config.getConfigValue(),
            config.getDescription()
        );
        return Result.success(Map.of("configId", configId));
    }
    
    @DeleteMapping("/entity/{entityId}")
    @Operation(summary = "删除指定法人的所有配置")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> deleteConfigsByEntityId(@PathVariable String entityId) {
        configService.deleteConfigsByEntityId(entityId, null);
        return Result.success(null);
    }
    
    @DeleteMapping("/entity/{entityId}/type/{configType}")
    @Operation(summary = "删除指定法人和配置类型的配置")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> deleteConfigsByEntityIdAndType(
            @PathVariable String entityId,
            @PathVariable String configType) {
        configService.deleteConfigsByEntityId(entityId, configType);
        return Result.success(null);
    }
}


