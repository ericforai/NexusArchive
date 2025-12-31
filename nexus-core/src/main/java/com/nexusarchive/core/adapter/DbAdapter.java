// Input: 适配器定义
// Output: 数据库适配能力
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.List;

public interface DbAdapter {
    String name();

    DataTypeMapping typeMapping();

    List<String> createTableSql(TableDefinition table);
}
