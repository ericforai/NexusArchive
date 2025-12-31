// Input: MyBatis-Plus InnerInterceptor
// Output: Fonds isolation + archive_year 注入（spacing safe）
// Pos: NexusCore isolation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

public class FondsIsolationInterceptor implements InnerInterceptor {
    // [P1-FIX] 扩展 SQL 类型检测，增加 MERGE, WITH CTE 语句支持
    private static final Pattern SQL_TYPE_PATTERN = Pattern.compile(
            "^\\s*(select|update|delete|insert|merge|with)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WHERE_PATTERN = Pattern.compile("\\bwhere\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FONDS_PATTERN = Pattern.compile("\\bfonds_no\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARCHIVE_YEAR_PATTERN = Pattern.compile("\\bfiscal_year\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("\\border\\s+by\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_BY_PATTERN = Pattern.compile("\\bgroup\\s+by\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIMIT_PATTERN = Pattern.compile("\\blimit\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OFFSET_PATTERN = Pattern.compile("\\boffset\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FETCH_PATTERN = Pattern.compile("\\bfetch\\b", Pattern.CASE_INSENSITIVE);

    private final SqlAuditRules rules;

    public FondsIsolationInterceptor() {
        this(SqlAuditRules.defaults());
    }

    public FondsIsolationInterceptor(SqlAuditRules rules) {
        this.rules = Objects.requireNonNull(rules, "rules");
    }

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        String originalSql = statementHandler.getBoundSql().getSql();
        String updatedSql = applyIsolation(originalSql);
        if (!originalSql.equals(updatedSql)) {
            MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
            metaObject.setValue("delegate.boundSql.sql", updatedSql);
        }
    }

    String applyIsolation(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        String normalized = sql.toLowerCase(Locale.ROOT);
        if (!rules.isProtectedSql(normalized)) {
            return sql;
        }
        boolean hasFondsNo = FONDS_PATTERN.matcher(normalized).find();
        boolean hasArchiveYear = ARCHIVE_YEAR_PATTERN.matcher(normalized).find();
        boolean requiresArchiveYear = rules.requiresColumn("fiscal_year");
        SqlType sqlType = detectSqlType(normalized);

        if (sqlType == SqlType.SELECT) {
            return injectScopeClause(sql, normalized, hasFondsNo, hasArchiveYear, requiresArchiveYear);
        }

        if (!hasFondsNo || (requiresArchiveYear && !hasArchiveYear)) {
            throw new FondsIsolationException("DML without explicit fonds_no/fiscal_year is blocked");
        }
        return sql;
    }

    private SqlType detectSqlType(String normalizedSql) {
        Matcher matcher = SQL_TYPE_PATTERN.matcher(normalizedSql);
        if (!matcher.find()) {
            throw new FondsIsolationException("Unsupported SQL type: " + normalizedSql.substring(0, Math.min(50, normalizedSql.length())));
        }
        String token = matcher.group(1).toLowerCase(Locale.ROOT);
        // [P1-FIX] WITH CTE 语句按 SELECT 处理
        if ("with".equals(token)) {
            return SqlType.SELECT;
        }
        return SqlType.fromToken(token);
    }

    // [P0-FIX] SQL 注入防护：二次校验正则
    private static final Pattern SAFE_FONDS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private String injectScopeClause(
            String sql,
            String normalized,
            boolean hasFondsNo,
            boolean hasArchiveYear,
            boolean requiresArchiveYear) {
        if (!normalized.contains(" from ")) {
            throw new FondsIsolationException("Missing FROM clause in query");
        }
        List<String> clauses = new ArrayList<>();
        if (!hasFondsNo) {
            String fondsNo = FondsContext.requireFondsNo();
            // [P0-FIX] 二次校验：即使绕过 requireFondsNo 也阻断注入
            if (!SAFE_FONDS_PATTERN.matcher(fondsNo).matches()) {
                throw new FondsIsolationException("Invalid fonds_no format for SQL injection prevention");
            }
            clauses.add("fonds_no = '" + fondsNo + "'");
        }
        if (requiresArchiveYear && !hasArchiveYear) {
            int archiveYear = ArchiveYearContext.requireArchiveYear();
            // archiveYear 是 int 类型，无需额外校验
            clauses.add("fiscal_year = " + archiveYear);
        }
        if (clauses.isEmpty()) {
            return sql;
        }
        String clause = String.join(" AND ", clauses);
        int insertPos = findClauseInsertPosition(normalized);
        String prefix = sql.substring(0, insertPos).trim();
        String suffix = sql.substring(insertPos).trim();
        String tail = suffix.isEmpty() ? "" : " " + suffix;
        if (WHERE_PATTERN.matcher(normalized).find()) {
            return prefix + " AND " + clause + tail;
        }
        return prefix + " WHERE " + clause + tail;
    }

    private int findClauseInsertPosition(String normalized) {
        int position = normalized.length();
        position = minIndex(position, matchIndex(normalized, ORDER_BY_PATTERN));
        position = minIndex(position, matchIndex(normalized, GROUP_BY_PATTERN));
        position = minIndex(position, matchIndex(normalized, LIMIT_PATTERN));
        position = minIndex(position, matchIndex(normalized, OFFSET_PATTERN));
        position = minIndex(position, matchIndex(normalized, FETCH_PATTERN));
        return position;
    }

    private int matchIndex(String normalized, Pattern pattern) {
        Matcher matcher = pattern.matcher(normalized);
        return matcher.find() ? matcher.start() : -1;
    }

    private int minIndex(int current, int candidate) {
        if (candidate >= 0 && candidate < current) {
            return candidate;
        }
        return current;
    }

    enum SqlType {
        SELECT,
        UPDATE,
        DELETE,
        INSERT,
        MERGE; // [P1-FIX] 支持 MERGE 语句

        static SqlType fromToken(String token) {
            return SqlType.valueOf(token.toUpperCase(Locale.ROOT));
        }
    }
}
