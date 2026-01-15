// Input: Spring Web、EntityConfigService、Result
// Output: EntityConfigController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.EntityConfig;
import com.nexusarchive.service.EntityConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * 法人配置控制器
 *
 * 功能: 管理每个法人的独立配置（ERP接口、业务规则、合规策略等）
 */
@Tag(name = "法人配置管理", description = """
    法人级别配置管理接口。

    **功能说明:**
    - 管理每个法人的独立配置
    - 支持配置按类型分组
    - 提供配置的增删改查操作

    **配置类型 (configType):**
    - erp_interface: ERP 接口配置
    - business_rule: 业务规则配置
    - compliance_policy: 合规策略配置
    - retention_period: 保管期限配置
    - metadata_template: 元数据模板配置
    - approval_flow: 审批流程配置

    **数据结构:**
    - entityId: 法人 ID（必填）
    - configType: 配置类型（必填）
    - configKey: 配置键（必填）
    - configValue: 配置值（JSON 格式）
    - description: 配置描述

    **使用场景:**
    - 为不同法人配置不同的 ERP 接口
    - 设置法人级别的保管期限策略
    - 配置法人特定的元数据模板
    - 管理法人审批流程

    **权限要求:**
    - entity:view: 查看法人配置
    - entity:manage: 管理法人配置
    - SYS_ADMIN: 系统管理员（全部权限）
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/entity-config")
@RequiredArgsConstructor
public class EntityConfigController {

    private final EntityConfigService configService;

    @GetMapping("/entity/{entityId}")
    @Operation(
        summary = "查询指定法人的所有配置",
        description = """
            获取指定法人的所有配置项。

            **路径参数:**
            - entityId: 法人 ID

            **返回数据包括:**
            - 该法人的所有配置项列表
            - 按创建时间倒序排列

            **使用场景:**
            - 查看法人完整配置
            - 配置导出备份
            """,
        operationId = "getEntityConfigs",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityConfig>> getConfigsByEntityId(
            @Parameter(description = "法人ID", required = true, example = "ENT001")
            @PathVariable String entityId) {
        return Result.success(configService.getConfigsByEntityId(entityId));
    }

    @GetMapping("/entity/{entityId}/type/{configType}")
    @Operation(
        summary = "查询指定法人和配置类型的配置",
        description = """
            获取指定法人下特定类型的配置项。

            **路径参数:**
            - entityId: 法人 ID
            - configType: 配置类型

            **返回数据包括:**
            - 该法人下指定类型的所有配置项

            **使用场景:**
            - 查询特定类型配置
            - 按配置类型筛选
            """,
        operationId = "getEntityConfigsByType",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityConfig>> getConfigsByEntityIdAndType(
            @Parameter(description = "法人ID", required = true, example = "ENT001")
            @PathVariable String entityId,
            @Parameter(description = "配置类型", required = true, example = "erp_interface")
            @PathVariable String configType) {
        return Result.success(configService.getConfigsByEntityIdAndType(entityId, configType));
    }

    @GetMapping("/entity/{entityId}/grouped")
    @Operation(
        summary = "查询指定法人的配置（按类型分组）",
        description = """
            获取指定法人的所有配置，按配置类型分组返回。

            **路径参数:**
            - entityId: 法人 ID

            **返回数据包括:**
            - 以配置类型为 key 的 Map 结构
            - 每个类型包含对应的配置项列表

            **使用场景:**
            - 配置页面按类型展示
            - 配置分类管理
            """,
        operationId = "getEntityConfigsGrouped",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, List<EntityConfig>>> getConfigsGroupedByType(
            @Parameter(description = "法人ID", required = true, example = "ENT001")
            @PathVariable String entityId) {
        return Result.success(configService.getConfigsGroupedByType(entityId));
    }

    @PostMapping
    @Operation(
        summary = "保存或更新配置",
        description = """
            创建新配置或更新已存在的配置。

            **请求体:**
            - entityId: 法人 ID（必填）
            - configType: 配置类型（必填）
            - configKey: 配置键（必填）
            - configValue: 配置值（必填，JSON 格式）
            - description: 配置描述（可选）

            **业务规则:**
            - 相同 entityId + configType + configKey 视为同一配置
            - 配置已存在则更新，不存在则创建

            **使用场景:**
            - 创建新配置
            - 更新现有配置
            """,
        operationId = "saveEntityConfig",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> saveOrUpdateConfig(
            @Parameter(description = "配置对象", required = true)
            @Valid @RequestBody EntityConfig config) {
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
    @Operation(
        summary = "删除指定法人的所有配置",
        description = """
            删除指定法人的所有配置项。

            **路径参数:**
            - entityId: 法人 ID

            **业务规则:**
            - 此操作不可逆
            - 删除后该法人的所有配置将清空

            **使用场景:**
            - 法人注销时清理配置
            """,
        operationId = "deleteEntityConfigs",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> deleteConfigsByEntityId(
            @Parameter(description = "法人ID", required = true, example = "ENT001")
            @PathVariable String entityId) {
        configService.deleteConfigsByEntityId(entityId, null);
        return Result.success(null);
    }

    @DeleteMapping("/entity/{entityId}/type/{configType}")
    @Operation(
        summary = "删除指定法人和配置类型的配置",
        description = """
            删除指定法人下特定类型的所有配置项。

            **路径参数:**
            - entityId: 法人 ID
            - configType: 配置类型

            **业务规则:**
            - 此操作不可逆
            - 仅删除指定类型的配置

            **使用场景:**
            - 清理特定类型配置
            - 重置某类配置
            """,
        operationId = "deleteEntityConfigsByType",
        tags = {"法人配置管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> deleteConfigsByEntityIdAndType(
            @Parameter(description = "法人ID", required = true, example = "ENT001")
            @PathVariable String entityId,
            @Parameter(description = "配置类型", required = true, example = "erp_interface")
            @PathVariable String configType) {
        configService.deleteConfigsByEntityId(entityId, configType);
        return Result.success(null);
    }
}
