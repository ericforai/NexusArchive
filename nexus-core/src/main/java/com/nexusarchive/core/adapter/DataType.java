// Input: 业务字段类型枚举
// Output: 适配层通用数据类型
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

public enum DataType {
    STRING,
    TEXT,
    INTEGER,
    BIGINT,
    DECIMAL,
    BOOLEAN,
    DATE,
    TIMESTAMP,
    JSON,
    BLOB
}
