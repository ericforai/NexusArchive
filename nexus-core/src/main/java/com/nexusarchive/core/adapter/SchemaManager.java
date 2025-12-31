// Input: 表结构定义与适配器
// Output: DDL 生成结果
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class SchemaManager {
    private final DbAdapter adapter;

    public SchemaManager(DbAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public List<String> generateCreateTable(TableDefinition table) {
        return adapter.createTableSql(table);
    }

    public String joinStatements(List<String> statements) {
        Objects.requireNonNull(statements, "statements");
        StringJoiner joiner = new StringJoiner(";\n");
        for (String statement : statements) {
            joiner.add(statement);
        }
        return joiner.toString();
    }
}
