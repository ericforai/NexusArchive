// Input: Java 标准库
// Output: BorrowingStatus 枚举
// Pos: borrowing/domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.domain;

import java.util.Arrays;

/**
 * 借阅流程状态机
 */
public enum BorrowingStatus {
    PENDING,
    APPROVED,
    REJECTED,
    RETURNED,
    CANCELLED;

    public String getCode() {
        return name();
    }

    public static BorrowingStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("借阅状态不能为空");
        }
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知借阅状态: " + code));
    }
}
