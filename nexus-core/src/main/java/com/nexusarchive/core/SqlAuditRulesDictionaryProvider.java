// Input: DataSource + 字典表配置
// Output: SQL 审计规则（来自数据库字典）
// Pos: NexusCore SQL 审计规则加载
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "nexus.audit.sql-guard", name = "dictionary-enabled", havingValue = "true")
public class SqlAuditRulesDictionaryProvider implements SqlAuditRulesProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlAuditRulesDictionaryProvider.class);
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    private final DataSource dataSource;
    private final SqlAuditGuardProperties properties;
    private final String sqlTemplate;

    public SqlAuditRulesDictionaryProvider(DataSource dataSource, SqlAuditGuardProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.sqlTemplate = loadSqlTemplate();
    }

    @Override
    public Optional<SqlAuditRules> load() {
        String table = safeIdentifier(properties.getDictionaryTable(), "dictionaryTable");
        String keyColumn = safeIdentifier(properties.getDictionaryKeyColumn(), "dictionaryKeyColumn");
        String valueColumn = safeIdentifier(properties.getDictionaryValueColumn(), "dictionaryValueColumn");
        String protectedKey = properties.getProtectedMarkersKey().toLowerCase(Locale.ROOT);
        String requiredKey = properties.getRequiredColumnsKey().toLowerCase(Locale.ROOT);
        String sql = sqlTemplate
                .replace("${keyColumn}", keyColumn)
                .replace("${valueColumn}", valueColumn)
                .replace("${table}", table);
        Map<String, String> rules = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, properties.getProtectedMarkersKey());
            statement.setString(2, properties.getRequiredColumnsKey());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String key = resultSet.getString(1);
                    String value = resultSet.getString(2);
                    if (key != null) {
                        rules.put(key.toLowerCase(Locale.ROOT), value);
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.warn("SQL 审计字典加载失败，回退到配置文件", ex);
            return Optional.empty();
        }

        List<String> protectedMarkers = parseCsv(rules.get(protectedKey));
        List<String> requiredColumns = parseCsv(rules.get(requiredKey));
        if (protectedMarkers.isEmpty() && requiredColumns.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(SqlAuditRules.of(
                protectedMarkers.isEmpty() ? properties.getProtectedMarkers() : protectedMarkers,
                requiredColumns.isEmpty() ? properties.getRequiredColumns() : requiredColumns));
    }

    private String loadSqlTemplate() {
        String resource = "sql/sql-audit-rules.sql";
        try (InputStream input = SqlAuditRulesDictionaryProvider.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing SQL template: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load SQL template: " + resource, ex);
        }
    }

    private String safeIdentifier(String value, String name) {
        if (value == null || value.isBlank() || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + name);
        }
        return value;
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] tokens = value.split(",");
        List<String> results = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                results.add(trimmed);
            }
        }
        return results;
    }
}
