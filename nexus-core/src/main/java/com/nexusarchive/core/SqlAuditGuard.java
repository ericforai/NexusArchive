// Input: SQL 字符串与审计规则
// Output: SQL 合规校验（缺失关键字段直接阻断）
// Pos: NexusCore SQL 审计守卫
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SqlAuditGuard {
    private static final Pattern DDL_PATTERN = Pattern.compile(
            "^\\s*(create|alter|drop|comment)\\b",
            Pattern.CASE_INSENSITIVE);

    private final SqlAuditRules rules;
    private final List<Pattern> requiredColumnPatterns;

    public SqlAuditGuard(SqlAuditRules rules) {
        this.rules = rules == null ? SqlAuditRules.defaults() : rules;
        this.requiredColumnPatterns = buildPatterns(this.rules.getRequiredColumns());
    }

    public static SqlAuditGuard defaultGuard() {
        return new SqlAuditGuard(SqlAuditRules.defaults());
    }

    public void check(String sql) {
        if (sql == null || sql.isBlank()) {
            return;
        }
        String normalized = sql.trim().toLowerCase(Locale.ROOT);
        if (DDL_PATTERN.matcher(normalized).find()) {
            return;
        }
        if (!rules.isProtectedSql(normalized)) {
            return;
        }
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < requiredColumnPatterns.size(); i++) {
            Pattern pattern = requiredColumnPatterns.get(i);
            if (!pattern.matcher(normalized).find()) {
                missing.add(rules.getRequiredColumns().get(i));
            }
        }
        if (!missing.isEmpty()) {
            throw new FondsIsolationException("SQL missing required columns: " + String.join(", ", missing));
        }
    }

    private List<Pattern> buildPatterns(List<String> columns) {
        List<Pattern> patterns = new ArrayList<>();
        for (String column : columns) {
            patterns.add(Pattern.compile("\\b" + Pattern.quote(column) + "\\b", Pattern.CASE_INSENSITIVE));
        }
        return Collections.unmodifiableList(patterns);
    }
}
