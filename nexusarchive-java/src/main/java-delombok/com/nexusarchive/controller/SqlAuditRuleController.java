// Input: Lombok、Spring Security、Spring Framework、Swagger/OpenAPI、Jakarta EE、本地模块
// Output: SqlAuditRuleController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.SysSqlAuditRule;
import com.nexusarchive.mapper.SysSqlAuditRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * SQL 审计规则控制器
 *
 * PRD 来源: 安全审计模块
 * 提供 SQL 审计规则的增删改查功能
 */
@Tag(name = "SQL审计规则", description = """
    SQL 审计规则管理接口。

    **功能说明:**
    - 查询 SQL 审计规则列表
    - 新增/更新单条规则
    - 批量更新规则
    - 删除规则

    **规则类型:**
    - SENSITIVE_TABLE: 敏感表访问规则
    - SENSITIVE_COLUMN: 敏感字段访问规则
    - DML_OPERATION: DML 操作规则
    - MASS_DELETE: 批量删除规则
    - SCHEMA_CHANGE: 表结构变更规则

    **规则格式:**
    - ruleKey: 规则键（唯一标识）
    - ruleValue: 规则值（SQL 模式或正则表达式）
    - ruleType: 规则类型
    - enabled: 是否启用
    - severity: 严重级别（LOW/MEDIUM/HIGH/CRITICAL）

    **匹配模式:**
    - 精确匹配: 表名、字段名完全相同
    - 模糊匹配: LIKE 模式匹配
    - 正则匹配: 正则表达式匹配

    **审计动作:**
    - RECORD: 仅记录
    - ALERT: 记录并告警
    - BLOCK: 拦截执行

    **业务规则:**
    - 规则键全局唯一
    - 更新操作记录审计日志
    - 删除前检查是否被引用

    **使用场景:**
    - 数据安全审计
    - 敏感操作监控
    - 合规性检查

    **权限要求:**
    - manage_settings 权限
    - SYSTEM_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/sql-audit-rules")
@PreAuthorize("hasAuthority('manage_settings') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class SqlAuditRuleController {
    private final SysSqlAuditRuleMapper ruleMapper;

    /**
     * 获取规则列表
     */
    @GetMapping
    @Operation(
        summary = "获取 SQL 审计规则列表",
        description = """
            查询所有 SQL 审计规则。

            **返回数据包括:**
            - ruleKey: 规则键
            - ruleValue: 规则值（匹配模式）
            - ruleType: 规则类型
            - enabled: 是否启用
            - severity: 严重级别
            - action: 审计动作
            - description: 规则说明
            - createdTime: 创建时间
            - lastModifiedTime: 最后修改时间

            **使用场景:**
            - 规则列表展示
            - 规则管理界面
            """,
        operationId = "listSqlAuditRules",
        tags = {"SQL审计规则"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<SysSqlAuditRule>> list() {
        return Result.success(ruleMapper.selectList(null));
    }

    /**
     * 获取单条规则
     */
    @GetMapping("/{ruleKey}")
    @Operation(
        summary = "获取单条 SQL 审计规则",
        description = """
            根据规则键查询单条 SQL 审计规则详情。

            **路径参数:**
            - ruleKey: 规则键

            **返回数据:**
            完整的规则对象，包括所有字段

            **使用场景:**
            - 规则详情查看
            - 规则编辑回显
            """,
        operationId = "getSqlAuditRule",
        tags = {"SQL审计规则"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "ruleKey 不能为空"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "规则不存在")
    })
    public Result<SysSqlAuditRule> getByKey(
            @Parameter(description = "规则键", example = "sensitive_table_user_info", required = true)
            @PathVariable String ruleKey) {
        if (!StringUtils.hasText(ruleKey)) {
            return Result.error("ruleKey 不能为空");
        }
        return Result.success(ruleMapper.selectById(ruleKey));
    }

    /**
     * 批量更新规则
     */
    @PutMapping
    @Operation(
        summary = "批量更新 SQL 审计规则",
        description = """
            批量更新多条 SQL 审计规则。

            **请求参数:**
            - rules: 规则列表
              - ruleKey: 规则键（必填）
              - ruleValue: 规则值（必填）
              - ruleType: 规则类型（必填）
              - enabled: 是否启用（可选）
              - severity: 严重级别（可选）

            **业务规则:**
            - 规则键不能为空
            - 规则值不能为空
            - 存在则更新，不存在则创建
            - 更新操作记录审计日志

            **使用场景:**
            - 批量导入规则
            - 批量修改规则
            """,
        operationId = "batchUpdateSqlAuditRules",
        tags = {"SQL审计规则"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或验证失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SQL_AUDIT_RULE", description = "批量更新 SQL 审计规则")
    public Result<Void> saveAll(
            @Parameter(description = "批量更新请求参数", required = true,
                    schema = @Schema(example = "{\"rules\": [{\"ruleKey\": \"sensitive_table_user\", \"ruleValue\": \"sys_user\"}]}"))
            @RequestBody Map<String, List<SysSqlAuditRule>> payload) {
        List<SysSqlAuditRule> rules = payload == null ? null : payload.get("rules");
        if (rules == null || rules.isEmpty()) {
            return Result.error("规则列表不能为空");
        }
        for (SysSqlAuditRule rule : rules) {
            String error = validateRule(rule);
            if (error != null) {
                return Result.error(error);
            }
            upsertRule(rule);
        }
        return Result.success();
    }

    /**
     * 新增/更新单条规则
     */
    @PostMapping
    @Operation(
        summary = "新增/更新 SQL 审计规则",
        description = """
            创建新规则或更新现有规则。

            **请求参数:**
            完整的规则对象，所有字段可选

            **业务规则:**
            - 规则键不能为空
            - 规则值不能为空
            - 使用 @Valid 自动校验
            - 创建时自动设置创建时间
            - 更新时自动更新修改时间

            **使用场景:**
            - 创建新规则
            - 更新现有规则
            """,
        operationId = "saveSqlAuditRule",
        tags = {"SQL审计规则"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或验证失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SQL_AUDIT_RULE", description = "新增/更新 SQL 审计规则")
    public Result<Void> save(
            @Parameter(description = "规则对象", required = true)
            @Valid @RequestBody SysSqlAuditRule rule) {
        String error = validateRule(rule);
        if (error != null) {
            return Result.error(error);
        }
        upsertRule(rule);
        return Result.success();
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{ruleKey}")
    @Operation(
        summary = "删除 SQL 审计规则",
        description = """
            删除指定的 SQL 审计规则。

            **路径参数:**
            - ruleKey: 规则键

            **业务规则:**
            - 删除前检查规则键
            - 删除操作不可逆
            - 删除操作记录审计日志

            **使用场景:**
            - 移除不再需要的规则
            - 清理无效规则
            """,
        operationId = "deleteSqlAuditRule",
        tags = {"SQL审计规则"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "ruleKey 不能为空"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "规则不存在")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "SQL_AUDIT_RULE", description = "删除 SQL 审计规则")
    public Result<Void> delete(
            @Parameter(description = "规则键", example = "sensitive_table_user_info", required = true)
            @PathVariable String ruleKey) {
        if (!StringUtils.hasText(ruleKey)) {
            return Result.error("ruleKey 不能为空");
        }
        ruleMapper.deleteById(ruleKey);
        return Result.success();
    }

    private String validateRule(SysSqlAuditRule rule) {
        if (rule == null) {
            return "规则不能为空";
        }
        if (!StringUtils.hasText(rule.getRuleKey())) {
            return "ruleKey 不能为空";
        }
        if (!StringUtils.hasText(rule.getRuleValue())) {
            return "ruleValue 不能为空";
        }
        return null;
    }

    private void upsertRule(SysSqlAuditRule rule) {
        LocalDateTime now = LocalDateTime.now();
        SysSqlAuditRule existing = ruleMapper.selectById(rule.getRuleKey());
        rule.setLastModifiedTime(now);
        if (existing == null) {
            if (rule.getCreatedTime() == null) {
                rule.setCreatedTime(now);
            }
            ruleMapper.insert(rule);
        } else {
            ruleMapper.updateById(rule);
        }
    }
}
