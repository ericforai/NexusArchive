package com.nexusarchive.domain.borrow;

// Input: 审批输入参数
// Output: 不可变的审批指令对象
// Pos: domain/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import java.util.Objects;

/**
 * 审批借阅申请指令 (Immutable Record)
 */
public record ApproveBorrowRequestCommand(
    String requestId,
    String approverId,
    String approverName,
    boolean approved,
    String comment
) {
    public ApproveBorrowRequestCommand {
        Objects.requireNonNull(requestId, "Request ID is required");
        Objects.requireNonNull(approverId, "Approver ID is required");
        Objects.requireNonNull(approverName, "Approver Name is required");
    }
}
