// Input: 适配器基础能力
// Output: 通用 DDL 生成逻辑
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BaseDbAdapter implements DbAdapter {
    private final String name;
    private final DataTypeMapping typeMapping;

    protected BaseDbAdapter(String name, DataTypeMapping typeMapping) {
        this.name = requireText(name, "name");
        this.typeMapping = Objects.requireNonNull(typeMapping, "typeMapping");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public DataTypeMapping typeMapping() {
        return typeMapping;
    }

    @Override
    public List<String> createTableSql(TableDefinition table) {
        Objects.requireNonNull(table, "table");
        List<String> statements = new ArrayList<>();
        statements.add(buildCreateTable(table));
        statements.addAll(buildCommentStatements(table));
        return statements;
    }

    private String buildCreateTable(TableDefinition table) {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(table.name()).append(" (");
        List<ColumnDefinition> columns = table.columns();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            ddl.append(renderColumn(column));
            if (i < columns.size() - 1) {
                ddl.append(", ");
            }
        }
        List<String> primaryKeys = table.primaryKeyColumns();
        if (!primaryKeys.isEmpty()) {
            ddl.append(", PRIMARY KEY (");
            ddl.append(String.join(", ", primaryKeys));
            ddl.append(")");
        }
        ddl.append(")");
        return ddl.toString();
    }

    private String renderColumn(ColumnDefinition column) {
        StringBuilder definition = new StringBuilder();
        definition.append(column.name()).append(" ");
        definition.append(typeMapping.resolve(column));
        boolean notNull = !column.nullable() || column.primaryKey();
        if (notNull) {
            definition.append(" NOT NULL");
        }
        if (column.defaultValue() != null) {
            definition.append(" DEFAULT ").append(column.defaultValue());
        }
        return definition.toString();
    }

    private List<String> buildCommentStatements(TableDefinition table) {
        List<String> comments = new ArrayList<>();
        if (hasText(table.comment())) {
            comments.add("COMMENT ON TABLE " + table.name() + " IS '" + escapeComment(table.comment()) + "'");
        }
        for (ColumnDefinition column : table.columns()) {
            if (hasText(column.comment())) {
                comments.add("COMMENT ON COLUMN " + table.name() + "." + column.name()
                        + " IS '" + escapeComment(column.comment()) + "'");
            }
        }
        return comments;
    }

    private String escapeComment(String comment) {
        return comment.replace("'", "''");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
