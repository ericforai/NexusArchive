// Input: 达梦类型映射
// Output: 达梦数据类型映射表
// Pos: NexusCore DB Adapter Dameng
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter.dameng;

import com.nexusarchive.core.adapter.DataType;
import com.nexusarchive.core.adapter.DataTypeMapping;
import java.util.EnumMap;
import java.util.Map;

public final class DamengTypeMapping extends DataTypeMapping {
    private static final int DEFAULT_VARCHAR = 255;
    private static final int DEFAULT_PRECISION = 18;
    private static final int DEFAULT_SCALE = 2;

    public DamengTypeMapping() {
        super(buildBaseTypes(), DEFAULT_VARCHAR, DEFAULT_PRECISION, DEFAULT_SCALE);
    }

    private static Map<DataType, String> buildBaseTypes() {
        Map<DataType, String> baseTypes = new EnumMap<>(DataType.class);
        baseTypes.put(DataType.STRING, "VARCHAR");
        baseTypes.put(DataType.TEXT, "CLOB");
        baseTypes.put(DataType.INTEGER, "INTEGER");
        baseTypes.put(DataType.BIGINT, "BIGINT");
        baseTypes.put(DataType.DECIMAL, "DECIMAL");
        baseTypes.put(DataType.BOOLEAN, "SMALLINT");
        baseTypes.put(DataType.DATE, "DATE");
        baseTypes.put(DataType.TIMESTAMP, "TIMESTAMP");
        baseTypes.put(DataType.JSON, "CLOB");
        baseTypes.put(DataType.BLOB, "BLOB");
        return baseTypes;
    }
}
