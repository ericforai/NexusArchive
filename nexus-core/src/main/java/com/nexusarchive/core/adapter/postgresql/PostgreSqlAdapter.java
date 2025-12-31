// Input: PostgreSQL 适配器配置
// Output: PostgreSQL DDL 生成适配器
// Pos: NexusCore DB Adapter PostgreSQL
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter.postgresql;

import com.nexusarchive.core.adapter.BaseDbAdapter;

public final class PostgreSqlAdapter extends BaseDbAdapter {
    public PostgreSqlAdapter() {
        super("PostgreSQL", new PostgreSqlTypeMapping());
    }
}
