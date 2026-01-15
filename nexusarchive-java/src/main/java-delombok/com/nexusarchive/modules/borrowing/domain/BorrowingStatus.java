// Input: Java 标准库
// Output: BorrowingStatus 枚举
// Pos: borrowing/domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.domain;

import java.util.Arrays;

/**
 * 借阅流程状态机
 * 
 * 状态转换规则:
 * - PENDING -> APPROVED/REJECTED/CANCELLED
 * - APPROVED -> BORROWED/CANCELLED
 * - BORROWED -> RETURNED/OVERDUE/LOST/CANCELLED
 * - OVERDUE -> RETURNED/LOST
 * - REJECTED, RETURNED, LOST, CANCELLED (终态)
 */
public enum BorrowingStatus {
    PENDING,        // 待审批
    APPROVED,       // 已批准（待借出）
    REJECTED,       // 已拒绝
    BORROWED,       // 已借出
    RETURNED,       // 已归还
    OVERDUE,        // 逾期
    LOST,           // 丢失
    CANCELLED;      // 已取消

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

    /**
     * 返回表示"在借中"的状态代码列表
     * 用于判断档案是否可以销毁
     */
    public static java.util.List<String> borrowedCodes() {
        return java.util.List.of(BORROWED.name(), OVERDUE.name());
    }
    
    /**
     * 判断是否为终态（不能再次转换的状态）
     */
    public boolean isTerminal() {
        return this == REJECTED || this == RETURNED || this == LOST || this == CANCELLED;
    }
}
