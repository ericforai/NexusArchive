// Input: 数据库厂商枚举
// Output: 适配器工厂
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import com.nexusarchive.core.adapter.dameng.DamengAdapter;
import com.nexusarchive.core.adapter.kingbase.KingbaseAdapter;
import com.nexusarchive.core.adapter.postgresql.PostgreSqlAdapter;

public final class DbAdapters {
    private DbAdapters() {
    }

    public static DbAdapter forVendor(DbVendor vendor) {
        if (vendor == null) {
            throw new IllegalArgumentException("vendor must not be null");
        }
        switch (vendor) {
            case POSTGRESQL:
                return new PostgreSqlAdapter();
            case DAMENG:
                return new DamengAdapter();
            case KINGBASE:
                return new KingbaseAdapter();
            default:
                throw new IllegalArgumentException("Unsupported vendor: " + vendor);
        }
    }
}
