// Input: 表结构定义
// Output: 表结构模型
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TableDefinition {
    private final String name;
    private final List<ColumnDefinition> columns;
    private final String comment;

    private TableDefinition(Builder builder) {
        this.name = requireName(builder.name);
        if (builder.columns.isEmpty()) {
            throw new IllegalArgumentException("columns must not be empty");
        }
        this.columns = Collections.unmodifiableList(new ArrayList<>(builder.columns));
        this.comment = builder.comment;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public List<ColumnDefinition> columns() {
        return columns;
    }

    public String comment() {
        return comment;
    }

    public List<String> primaryKeyColumns() {
        List<String> keys = new ArrayList<>();
        for (ColumnDefinition column : columns) {
            if (column.primaryKey()) {
                keys.add(column.name());
            }
        }
        return keys;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    public static final class Builder {
        private final String name;
        private final List<ColumnDefinition> columns = new ArrayList<>();
        private String comment;

        private Builder(String name) {
            this.name = name;
        }

        public Builder addColumn(ColumnDefinition column) {
            this.columns.add(Objects.requireNonNull(column, "column"));
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public TableDefinition build() {
            return new TableDefinition(this);
        }
    }
}
