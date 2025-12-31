// Input: SQL 审计规则配置
// Output: 规则快照（保护表标记 + 必填列）
// Pos: NexusCore SQL 审计规则
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SqlAuditRules {
    private final List<String> protectedMarkers;
    private final List<String> requiredColumns;

    private SqlAuditRules(List<String> protectedMarkers, List<String> requiredColumns) {
        this.protectedMarkers = normalizeList(protectedMarkers);
        this.requiredColumns = normalizeList(requiredColumns);
    }

    public static SqlAuditRules defaults() {
        return new SqlAuditRules(
                List.of("acc_archive", "arc_", "bas_fonds", "sys_fonds"),
                List.of("fonds_no", "fiscal_year"));
    }

    public static SqlAuditRules of(List<String> protectedMarkers, List<String> requiredColumns) {
        return new SqlAuditRules(protectedMarkers, requiredColumns);
    }

    public List<String> getProtectedMarkers() {
        return protectedMarkers;
    }

    public List<String> getRequiredColumns() {
        return requiredColumns;
    }

    public boolean isProtectedSql(String normalizedSql) {
        for (String marker : protectedMarkers) {
            if (normalizedSql.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public boolean requiresColumn(String columnName) {
        return requiredColumns.contains(columnName.toLowerCase(Locale.ROOT));
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                normalized.add(value.trim().toLowerCase(Locale.ROOT));
            }
        }
        return Collections.unmodifiableList(normalized);
    }
}
