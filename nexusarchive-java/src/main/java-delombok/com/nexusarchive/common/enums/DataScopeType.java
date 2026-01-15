// Input: Java 标准库
// Output: DataScopeType 枚举
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.enums;

public enum DataScopeType {

    SELF(0),
    DEPARTMENT(1),
    DEPARTMENT_AND_CHILD(2),
    ALL(3);

    private final int priority;

    DataScopeType(int priority) {
        this.priority = priority;
    }

    public static DataScopeType from(String value) {
        if (value == null) {
            return SELF;
        }
        switch (value.trim().toLowerCase()) {
            case "all":
                return ALL;
            case "department_and_child":
            case "dept_and_child":
                return DEPARTMENT_AND_CHILD;
            case "department":
            case "dept":
                return DEPARTMENT;
            default:
                return SELF;
        }
    }

    public static DataScopeType max(DataScopeType a, DataScopeType b) {
        if (a == null) {
            return b != null ? b : SELF;
        }
        if (b == null) {
            return a;
        }
        return a.priority >= b.priority ? a : b;
    }

    public boolean isAll() {
        return this == ALL;
    }

    public boolean isSelf() {
        return this == SELF;
    }
}
