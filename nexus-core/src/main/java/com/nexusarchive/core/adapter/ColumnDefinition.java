// Input: 列定义参数
// Output: 列定义模型
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.Objects;

public final class ColumnDefinition {
    private final String name;
    private final DataType dataType;
    private final Integer length;
    private final Integer precision;
    private final Integer scale;
    private final boolean nullable;
    private final boolean primaryKey;
    private final String defaultValue;
    private final String comment;

    private ColumnDefinition(Builder builder) {
        this.name = requireName(builder.name);
        this.dataType = Objects.requireNonNull(builder.dataType, "dataType");
        this.length = builder.length;
        this.precision = builder.precision;
        this.scale = builder.scale;
        this.nullable = builder.nullable;
        this.primaryKey = builder.primaryKey;
        this.defaultValue = builder.defaultValue;
        this.comment = builder.comment;
    }

    public static Builder builder(String name, DataType dataType) {
        return new Builder(name, dataType);
    }

    public String name() {
        return name;
    }

    public DataType dataType() {
        return dataType;
    }

    public Integer length() {
        return length;
    }

    public Integer precision() {
        return precision;
    }

    public Integer scale() {
        return scale;
    }

    public boolean nullable() {
        return nullable;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String comment() {
        return comment;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    public static final class Builder {
        private final String name;
        private final DataType dataType;
        private Integer length;
        private Integer precision;
        private Integer scale;
        private boolean nullable = true;
        private boolean primaryKey;
        private String defaultValue;
        private String comment;

        private Builder(String name, DataType dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        public Builder length(Integer length) {
            this.length = length;
            return this;
        }

        public Builder precision(Integer precision) {
            this.precision = precision;
            return this;
        }

        public Builder scale(Integer scale) {
            this.scale = scale;
            return this;
        }

        public Builder nullable(boolean nullable) {
            this.nullable = nullable;
            return this;
        }

        public Builder primaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
            return this;
        }

        public Builder defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }

        public ColumnDefinition build() {
            return new ColumnDefinition(this);
        }
    }
}
