// Input: Spring Boot 配置属性
// Output: SQL 审计守卫配置
// Pos: NexusCore 配置绑定
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nexus.audit.sql-guard")
public class SqlAuditGuardProperties {
    private boolean enabled = true;
    private boolean dictionaryEnabled = false;
    private String dictionaryTable = "sys_sql_audit_rule";
    private String dictionaryKeyColumn = "rule_key";
    private String dictionaryValueColumn = "rule_value";
    private String protectedMarkersKey = "protected_markers";
    private String requiredColumnsKey = "required_columns";
    private List<String> protectedMarkers = List.of("acc_archive", "arc_", "bas_fonds", "sys_fonds");
    private List<String> requiredColumns = List.of("fonds_no", "fiscal_year");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDictionaryEnabled() {
        return dictionaryEnabled;
    }

    public void setDictionaryEnabled(boolean dictionaryEnabled) {
        this.dictionaryEnabled = dictionaryEnabled;
    }

    public String getDictionaryTable() {
        return dictionaryTable;
    }

    public void setDictionaryTable(String dictionaryTable) {
        this.dictionaryTable = dictionaryTable;
    }

    public String getDictionaryKeyColumn() {
        return dictionaryKeyColumn;
    }

    public void setDictionaryKeyColumn(String dictionaryKeyColumn) {
        this.dictionaryKeyColumn = dictionaryKeyColumn;
    }

    public String getDictionaryValueColumn() {
        return dictionaryValueColumn;
    }

    public void setDictionaryValueColumn(String dictionaryValueColumn) {
        this.dictionaryValueColumn = dictionaryValueColumn;
    }

    public String getProtectedMarkersKey() {
        return protectedMarkersKey;
    }

    public void setProtectedMarkersKey(String protectedMarkersKey) {
        this.protectedMarkersKey = protectedMarkersKey;
    }

    public String getRequiredColumnsKey() {
        return requiredColumnsKey;
    }

    public void setRequiredColumnsKey(String requiredColumnsKey) {
        this.requiredColumnsKey = requiredColumnsKey;
    }

    public List<String> getProtectedMarkers() {
        return protectedMarkers;
    }

    public void setProtectedMarkers(List<String> protectedMarkers) {
        this.protectedMarkers = protectedMarkers;
    }

    public List<String> getRequiredColumns() {
        return requiredColumns;
    }

    public void setRequiredColumns(List<String> requiredColumns) {
        this.requiredColumns = requiredColumns;
    }
}
