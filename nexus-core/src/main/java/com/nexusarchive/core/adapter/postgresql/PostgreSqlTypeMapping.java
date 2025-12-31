// Input: PostgreSQL 类型映射
// Output: PostgreSQL 数据类型映射表
// Pos: NexusCore DB Adapter PostgreSQL
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter.postgresql;

import com.nexusarchive.core.adapter.DataType;
import com.nexusarchive.core.adapter.DataTypeMapping;
import java.util.EnumMap;
import java.util.Map;

public final class PostgreSqlTypeMapping extends DataTypeMapping {
    private static final int DEFAULT_VARCHAR = 255;
    private static final int DEFAULT_PRECISION = 18;
    private static final int DEFAULT_SCALE = 2;

    public PostgreSqlTypeMapping() {
        super(buildBaseTypes(), DEFAULT_VARCHAR, DEFAULT_PRECISION, DEFAULT_SCALE);
    }

    private static Map<DataType, String> buildBaseTypes() {
        Map<DataType, String> baseTypes = new EnumMap<>(DataType.class);
        baseTypes.put(DataType.STRING, "VARCHAR");
        baseTypes.put(DataType.TEXT, "TEXT");
        baseTypes.put(DataType.INTEGER, "INTEGER");
        baseTypes.put(DataType.BIGINT, "BIGINT");
        baseTypes.put(DataType.DECIMAL, "NUMERIC");
        baseTypes.put(DataType.BOOLEAN, "BOOLEAN");
        baseTypes.put(DataType.DATE, "DATE");
        baseTypes.put(DataType.TIMESTAMP, "TIMESTAMP");
        baseTypes.put(DataType.JSON, "JSONB");
        baseTypes.put(DataType.BLOB, "BYTEA");
        return baseTypes;
    }
}
