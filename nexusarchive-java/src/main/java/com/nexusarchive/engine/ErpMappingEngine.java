package com.nexusarchive.engine;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.entity.Archive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ERP 数据映射引擎
 * 
 * 负责将 ERP 原始 JSON 数据根据映射配置转换为档案实体
 * 支持：
 * 1. 直接字段映射 (erpField)
 * 2. 常量定义 ("CONST:AC01")
 * 3. 模板字符串 ("报销单 - ${billNo}")
 * 4. 简单转换 (substring, replace)
 */
@Component
public class ErpMappingEngine {

    private static final Logger log = LoggerFactory.getLogger(ErpMappingEngine.class);

    // 安全白名单：允许通过映射修改的 Archive 字段 (High #6 Fix)
    private static final java.util.Set<String> ALLOWED_ARCHIVE_FIELDS = java.util.Set.of(
            "fondsCode", "fiscalYear", "fiscalPeriod", "voucherType", "voucherNumber",
            "voucherDate", "amount", "summary", "attachmentCount", "creator",
            "auditor", "poster", "sourceSystem", "externalId", "remark", "docDate");

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 将解析后的 ERP JSON 数据转换为 Archive 实体
     * 
     * @param sourceData    原始 ERP 数据
     * @param mappingConfig 映射规则配置 (JSON 格式)
     * @return 转换后的 Archive 实体
     */
    public Archive mapToArchive(JSONObject sourceData, JSONObject mappingConfig) {
        Archive archive = new Archive();

        mappingConfig.forEach((key, value) -> {
            String targetField = key;
            String rule = value.toString();

            try {
                String resultValue = resolveRule(sourceData, rule);
                setFieldValue(archive, targetField, resultValue);
            } catch (Exception e) {
                log.error("映射字段 [{}] 失败, 规则: {}, 错误: {}", targetField, rule, e.getMessage());
            }
        });

        return archive;
    }

    /**
     * 解析映射规则
     */
    private String resolveRule(JSONObject sourceData, String rule) {
        if (StrUtil.isEmpty(rule)) {
            return rule;
        }

        // 1. 处理常量
        if (rule.startsWith("CONST:")) {
            return rule.substring(6);
        }

        // 2. 处理模板字符串和变量
        if (rule.contains("${")) {
            return resolveTemplate(sourceData, rule);
        }

        // 3. 处理直接字段映射 (尝试从 JSON 中取值)
        Object val = sourceData.getByPath(rule);
        return val != null ? val.toString() : "";
    }

    /**
     * 处理模板字符串如 "凭证-${billNo}"
     */
    private String resolveTemplate(JSONObject sourceData, String template) {
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String expr = matcher.group(1);
            String replacement = resolveExpression(sourceData, expr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 解析简单表达式如 "date:substring(0,4)" 或 "billNo"
     */
    private String resolveExpression(JSONObject sourceData, String expr) {
        if (expr.contains(":")) {
            String[] parts = expr.split(":", 2);
            String field = parts[0];
            String action = parts[1];

            Object val = sourceData.getByPath(field);
            String strVal = val != null ? val.toString() : "";

            if (action.startsWith("substring(")) {
                String params = action.substring(10, action.length() - 1);
                String[] range = params.split(",");
                int start = Integer.parseInt(range[0].trim());
                int end = range.length > 1 ? Integer.parseInt(range[1].trim()) : strVal.length();
                return StrUtil.sub(strVal, start, end);
            }
            return strVal;
        }

        Object val = sourceData.getByPath(expr);
        return val != null ? val.toString() : "";
    }

    /**
     * 将解析后的 ERP JSON 数据转换为 ArcFileContent 实体 (预归档)
     * 
     * @param sourceData    原始 ERP 数据
     * @param mappingConfig 映射规则配置
     * @return 转换后的 ArcFileContent 实体
     */
    public com.nexusarchive.entity.ArcFileContent mapToArcFileContent(JSONObject sourceData, JSONObject mappingConfig) {
        com.nexusarchive.entity.ArcFileContent content = new com.nexusarchive.entity.ArcFileContent();
        content.setId(cn.hutool.core.util.IdUtil.fastSimpleUUID());
        content.setCreatedTime(java.time.LocalDateTime.now());
        content.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.PENDING_ARCHIVE.getCode());

        mappingConfig.forEach((key, value) -> {
            String targetField = key;
            String rule = value.toString();

            try {
                String resultValue = resolveRule(sourceData, rule);
                setArcFieldValue(content, targetField, resultValue);
            } catch (Exception e) {
                log.error("映射预归档字段 [{}] 失败: {}", targetField, e.getMessage());
            }
        });

        return content;
    }

    /**
     * 设置 Archive 字段值
     */
    private void setFieldValue(Archive archive, String fieldName, String value) {
        if (StrUtil.isEmpty(value))
            return;

        try {
            // 优先处理特殊逻辑字段
            if ("amount".equals(fieldName)) {
                archive.setAmount(new BigDecimal(value));
            } else if ("docDate".equals(fieldName)) {
                archive.setDocDate(LocalDate.parse(value));
            } else {
                // 通用反射设置 (遵循 camelCase)
                // 安全校验：必须在白名单内 (High #6 Fix)
                if (!ALLOWED_ARCHIVE_FIELDS.contains(fieldName)) {
                    throw new SecurityException("非法字段映射: " + fieldName + " 不在允许名单中");
                }
                cn.hutool.core.util.ReflectUtil.setFieldValue(archive, fieldName, value);
            }
        } catch (SecurityException e) {
            throw e; // 抛出安全异常
        } catch (Exception e) {
            log.warn("设置 Archive 字段 [{}] 出错: {}", fieldName, e.getMessage());
        }
    }

    /**
     * 设置 ArcFileContent 字段值
     */
    private void setArcFieldValue(com.nexusarchive.entity.ArcFileContent content, String fieldName, String value) {
        if (StrUtil.isEmpty(value))
            return;

        try {
            if ("fileSize".equals(fieldName)) {
                content.setFileSize(Long.parseLong(value));
            } else {
                cn.hutool.core.util.ReflectUtil.setFieldValue(content, fieldName, value);
            }
        } catch (Exception e) {
            log.warn("设置 ArcFileContent 字段 [{}] 出错: {}", fieldName, e.getMessage());
        }
    }
}
