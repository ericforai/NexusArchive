// Input: Jackson、Lombok、Java 标准库
// Output: SubjectExtractor 类
// Pos: 对账服务 - 科目分录提取层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.reconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 科目分录提取器
 * <p>
 * 从档案的 customMetadata 中提取科目分录数据
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubjectExtractor {

    private final ObjectMapper objectMapper;

    /**
     * 从档案元数据中提取科目聚合数据
     *
     * @param customMetadata 自定义元数据 JSON
     * @param subjectCode    目标科目代码
     * @return 科目聚合结果
     */
    public ArchiveAggregator.SubjectAggregation extract(String customMetadata, String subjectCode) {
        ArchiveAggregator.SubjectAggregation aggregation = new ArchiveAggregator.SubjectAggregation();

        if (!hasText(customMetadata)) {
            aggregation.metadataMissing = true;
            return aggregation;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(customMetadata);
        } catch (Exception e) {
            aggregation.parseError = true;
            return aggregation;
        }

        List<JsonNode> entries = extractEntries(root);
        if (entries.isEmpty()) {
            aggregation.metadataMissing = true;
            return aggregation;
        }

        String normalizedSubject = normalizeSubjectCode(subjectCode);
        for (JsonNode entry : entries) {
            if (entry == null) {
                continue;
            }
            String entrySubject = normalizeSubjectCode(extractSubjectCode(entry));
            if (entrySubject == null || !entrySubject.equalsIgnoreCase(normalizedSubject)) {
                continue;
            }
            aggregation.matched = true;
            aggregation.debit = aggregation.debit.add(readBigDecimal(entry, "debit_org", "debitOrg", "debit",
                    "debit_original", "debitOriginal"));
            aggregation.credit = aggregation.credit.add(readBigDecimal(entry, "credit_org", "creditOrg", "credit",
                    "credit_original", "creditOriginal"));
        }

        return aggregation;
    }

    /**
     * 提取所有分录节点
     */
    private List<JsonNode> extractEntries(JsonNode root) {
        List<JsonNode> entries = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(entries::add);
        } else if (root.isObject()) {
            JsonNode body = root.get("body");
            if (body != null && body.isArray()) {
                body.forEach(entries::add);
            }
            JsonNode entriesNode = root.get("entries");
            if (entriesNode != null && entriesNode.isArray()) {
                entriesNode.forEach(entries::add);
            }
        }
        return entries;
    }

    /**
     * 提取科目代码
     */
    private String extractSubjectCode(JsonNode entry) {
        JsonNode accSubject = entry.get("accsubject");
        if (accSubject == null || accSubject.isNull()) {
            accSubject = entry.get("accSubject");
        }
        if (accSubject != null && accSubject.has("code")) {
            return accSubject.path("code").asText(null);
        }
        String direct = textValue(entry, "subjectCode", "accSubjectCode", "accountCode", "accountcode",
                "account_code");
        if (hasText(direct)) {
            return direct;
        }
        JsonNode accountNode = entry.get("account");
        if (accountNode != null && accountNode.has("code")) {
            return accountNode.path("code").asText(null);
        }
        return null;
    }

    /**
     * 从 JSON 节点读取 BigDecimal 值
     */
    private BigDecimal readBigDecimal(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            JsonNode valueNode = node.get(field);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            String text = valueNode.asText();
            if (!hasText(text)) {
                continue;
            }
            try {
                BigDecimal value = new BigDecimal(text);
                return value;
            } catch (NumberFormatException e) {
                log.warn("金额解析失败: field={}, value={}, error={}",
                    field, text, e.getMessage());
            }
        }
        log.warn("所有金额字段均解析失败,返回 ZERO: fields={}", Arrays.toString(fieldNames));
        return BigDecimal.ZERO;
    }

    /**
     * 获取文本值
     */
    private String textValue(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String normalizeSubjectCode(String subjectCode) {
        return subjectCode == null ? null : subjectCode.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
