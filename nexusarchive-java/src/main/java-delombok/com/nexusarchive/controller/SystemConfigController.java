// Input: Lombok、Spring Security、Spring Framework、Swagger/OpenAPI、Java 标准库、本地模块
// Output: SystemConfigController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.SystemSetting;
import com.nexusarchive.service.SystemSettingService;
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

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 *
 * PRD 来源: 系统管理模块
 * 提供系统配置的查询和修改功能
 */
@Tag(name = "系统配置", description = """
    系统配置管理接口。

    **功能说明:**
    - 查询系统配置项
    - 批量更新系统配置
    - 自动初始化默认配置

    **配置类型:**
    - STORAGE: 存储路径配置
    - SECURITY: 安全策略配置
    - RETENTION: 保管期限配置
    - NOTIFICATION: 通知配置
    - INTEGRATION: 集成配置

    **配置项示例:**
    - archive.storage.path: 归档存储路径
    - archive.max.fileSize: 最大文件大小（MB）
    - archive.retention.default: 默认保管期限（年）
    - security.password.policy: 密码策略
    - notification.enabled: 通知开关

    **业务规则:**
    - 配置项键值全局唯一
    - 更新操作记录审计日志
    - 首次查询自动初始化默认值
    - 敏感配置需要额外权限

    **使用场景:**
    - 系统设置页面
    - 配置中心管理
    - 运行时参数调整

    **权限要求:**
    - manage_settings 权限
    - SYSTEM_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/settings")
@PreAuthorize("hasAuthority('manage_settings') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemSettingService systemSettingService;

    /**
     * 获取系统配置列表
     */
    @GetMapping
    @Operation(
        summary = "获取系统配置列表",
        description = """
            查询所有系统配置项。

            **返回数据包括:**
            - key: 配置键（如 archive.storage.path）
            - value: 配置值
            - category: 配置分类
            - description: 配置说明
            - dataType: 数据类型（STRING/NUMBER/BOOLEAN/JSON）
            - isSensitive: 是否敏感配置

            **业务规则:**
            - 首次查询自动初始化默认值
            - 敏感配置值可能被脱敏
            - 配置按分类分组返回

            **使用场景:**
            - 系统设置页面初始化
            - 配置查看
            - 配置导出
            """,
        operationId = "getSystemSettings",
        tags = {"系统配置"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<SystemSetting>> getSettings() {
        systemSettingService.initDefaultsIfEmpty();
        return Result.success(systemSettingService.listAll());
    }

    /**
     * 更新系统配置
     */
    @PutMapping
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SETTING", description = "更新系统配置")
    @Operation(
        summary = "批量更新系统配置",
        description = """
            批量更新系统配置项。

            **请求参数:**
            - settings: 配置项列表
              - key: 配置键（必填）
              - value: 配置值（必填）
              - category: 配置分类（可选）

            **业务规则:**
            - 只更新提供的配置项
            - 不存在的配置项自动创建
            - 更新操作记录审计日志
            - 敏感配置需要特殊权限

            **配置验证:**
            - 数据类型验证
            - 值范围验证
            - 格式验证（如路径、URL）

            **使用场景:**
            - 保存系统设置
            - 批量配置更新
            - 配置导入
            """,
        operationId = "updateSystemSettings",
        tags = {"系统配置"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或验证失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Void> updateSettings(
            @Parameter(description = "更新配置请求参数", required = true,
                    schema = @Schema(example = "{\"settings\": [{\"key\": \"archive.max.fileSize\", \"value\": \"500\"}]}"))
            @RequestBody Map<String, List<SystemSetting>> payload) {
        List<SystemSetting> items = payload.get("settings");
        systemSettingService.saveAll(items);
        return Result.success();
    }
}
