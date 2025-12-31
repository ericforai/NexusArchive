// Input: Lombok、Spring Security、Spring Framework、等
// Output: SqlAuditRuleController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.SysSqlAuditRule;
import com.nexusarchive.mapper.SysSqlAuditRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/admin/sql-audit-rules")
@PreAuthorize("hasAuthority('manage_settings') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
@Tag(name = "SQL审计规则")
public class SqlAuditRuleController {
    private final SysSqlAuditRuleMapper ruleMapper;

    @GetMapping
    @Operation(summary = "获取 SQL 审计规则列表")
    public Result<List<SysSqlAuditRule>> list() {
        return Result.success(ruleMapper.selectList(null));
    }

    @GetMapping("/{ruleKey}")
    @Operation(summary = "获取单条 SQL 审计规则")
    public Result<SysSqlAuditRule> getByKey(@PathVariable String ruleKey) {
        if (!StringUtils.hasText(ruleKey)) {
            return Result.error("ruleKey 不能为空");
        }
        return Result.success(ruleMapper.selectById(ruleKey));
    }

    @PutMapping
    @Operation(summary = "批量更新 SQL 审计规则")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SQL_AUDIT_RULE", description = "批量更新 SQL 审计规则")
    public Result<Void> saveAll(@RequestBody Map<String, List<SysSqlAuditRule>> payload) {
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

    @PostMapping
    @Operation(summary = "新增/更新 SQL 审计规则")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SQL_AUDIT_RULE", description = "新增/更新 SQL 审计规则")
    public Result<Void> save(@RequestBody SysSqlAuditRule rule) {
        String error = validateRule(rule);
        if (error != null) {
            return Result.error(error);
        }
        upsertRule(rule);
        return Result.success();
    }

    @DeleteMapping("/{ruleKey}")
    @Operation(summary = "删除 SQL 审计规则")
    @ArchivalAudit(operationType = "DELETE", resourceType = "SQL_AUDIT_RULE", description = "删除 SQL 审计规则")
    public Result<Void> delete(@PathVariable String ruleKey) {
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
