// Input: SQL 审计规则字典
// Output: 字典优先策略验证
// Pos: NexusCore tests
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Optional;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlAuditRulesResolverTests {
    @Test
    void shouldPreferDictionaryRulesWhenEnabled() throws Exception {
        DataSource dataSource = createDataSource();
        executeSql(dataSource, "sql/sql-audit-rule-setup.sql");
        executeSql(dataSource, "sql/sql-audit-rule-insert.sql");

        SqlAuditGuardProperties properties = new SqlAuditGuardProperties();
        properties.setDictionaryEnabled(true);
        properties.setProtectedMarkers(java.util.List.of("cfg_marker"));
        properties.setRequiredColumns(java.util.List.of("cfg_column"));

        SqlAuditRulesDictionaryProvider provider = new SqlAuditRulesDictionaryProvider(dataSource, properties);
        SqlAuditRulesResolver resolver = new SqlAuditRulesResolver(properties, Optional.of(provider));

        SqlAuditRules rules = resolver.resolve();
        assertTrue(rules.getProtectedMarkers().contains("arc_dict"));
        assertTrue(rules.getRequiredColumns().contains("fiscal_year"));
        assertFalse(rules.getProtectedMarkers().contains("cfg_marker"));
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:sql_audit_rules;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void executeSql(DataSource dataSource, String resource) throws Exception {
        String sql = loadSql(resource);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String part : sql.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private String loadSql(String resource) throws IOException {
        URL url = SqlAuditRulesResolverTests.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("Missing SQL file: " + resource);
        }
        try {
            Path path = Path.of(url.toURI());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("Invalid SQL URI: " + resource, ex);
        }
    }
}
