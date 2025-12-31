// Input: 数据库类型名称
// Output: 标准化数据库厂商枚举
// Pos: NexusCore DB Adapter
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.adapter;

import java.util.Locale;

public enum DbVendor {
    POSTGRESQL,
    DAMENG,
    KINGBASE;

    public static DbVendor fromName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("postgres")) {
            return POSTGRESQL;
        }
        if (normalized.contains("dameng") || normalized.contains("dm")) {
            return DAMENG;
        }
        if (normalized.contains("kingbase") || normalized.contains("kbase")) {
            return KINGBASE;
        }
        throw new IllegalArgumentException("Unknown vendor: " + name);
    }
}
