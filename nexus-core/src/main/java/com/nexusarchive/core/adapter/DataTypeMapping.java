// Input: 数据类型映射参数
// Output: 可扩展数据类型映射结果
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class DataTypeMapping {
    private final Map<DataType, String> baseTypes;
    private final int defaultVarcharLength;
    private final int defaultPrecision;
    private final int defaultScale;

    public DataTypeMapping(
            Map<DataType, String> baseTypes,
            int defaultVarcharLength,
            int defaultPrecision,
            int defaultScale) {
        this.baseTypes = Collections.unmodifiableMap(
                new EnumMap<>(Objects.requireNonNull(baseTypes, "baseTypes")));
        this.defaultVarcharLength = defaultVarcharLength;
        this.defaultPrecision = defaultPrecision;
        this.defaultScale = defaultScale;
    }

    public String resolve(ColumnDefinition column) {
        Objects.requireNonNull(column, "column");
        DataType dataType = column.dataType();
        String baseType = baseTypes.get(dataType);
        if (baseType == null) {
            throw new IllegalArgumentException("Missing mapping for " + dataType);
        }
        switch (dataType) {
            case STRING:
                return baseType + "(" + resolveLength(column.length(), defaultVarcharLength) + ")";
            case DECIMAL:
                int precision = resolveLength(column.precision(), defaultPrecision);
                int scale = resolveLength(column.scale(), defaultScale);
                return baseType + "(" + precision + "," + scale + ")";
            default:
                return baseType;
        }
    }

    private int resolveLength(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value <= 0) {
            throw new IllegalArgumentException("Invalid length: " + value);
        }
        return value;
    }
}
