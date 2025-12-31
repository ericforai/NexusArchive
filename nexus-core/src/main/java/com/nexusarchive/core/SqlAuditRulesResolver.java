// Input: 配置文件 + 可选数据库字典
// Output: SQL 审计规则解析结果
// Pos: NexusCore SQL 审计规则装配
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SqlAuditRulesResolver {
    private final SqlAuditGuardProperties properties;
    private final Optional<SqlAuditRulesProvider> dictionaryProvider;

    public SqlAuditRulesResolver(SqlAuditGuardProperties properties,
                                 Optional<SqlAuditRulesProvider> dictionaryProvider) {
        this.properties = properties;
        this.dictionaryProvider = dictionaryProvider;
    }

    public SqlAuditRules resolve() {
        if (properties.isDictionaryEnabled()) {
            Optional<SqlAuditRules> fromDictionary = dictionaryProvider.flatMap(SqlAuditRulesProvider::load);
            if (fromDictionary.isPresent()) {
                return fromDictionary.get();
            }
        }
        List<String> protectedMarkers = properties.getProtectedMarkers();
        List<String> requiredColumns = properties.getRequiredColumns();
        if ((protectedMarkers == null || protectedMarkers.isEmpty())
                && (requiredColumns == null || requiredColumns.isEmpty())) {
            return SqlAuditRules.defaults();
        }
        return SqlAuditRules.of(protectedMarkers, requiredColumns);
    }
}
