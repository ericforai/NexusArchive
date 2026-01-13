package com.nexusarchive.domain.borrow;

// Input: 用户输入与业务参数
// Output: 不可变的借阅指令对象
// Pos: domain/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.nexusarchive.entity.BorrowRequest.BorrowType;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 提交借阅申请指令 (Immutable Record)
 */
public record SubmitBorrowRequestCommand(
    String applicantId,
    String applicantName,
    String deptId,
    String deptName,
    String purpose,
    BorrowType borrowType,
    List<String> archiveIds,
    LocalDate expectedStartDate,
    LocalDate expectedEndDate
) {
    public SubmitBorrowRequestCommand {
        Objects.requireNonNull(applicantId, "Applicant ID is required");
        Objects.requireNonNull(applicantName, "Applicant Name is required");
        Objects.requireNonNull(archiveIds, "At least one archive must be selected");
        if (archiveIds.isEmpty()) throw new IllegalArgumentException("Archive list cannot be empty");
        
        Objects.requireNonNull(expectedStartDate, "Start date is required");
        Objects.requireNonNull(expectedEndDate, "End date is required");
        if (expectedEndDate.isBefore(expectedStartDate)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
    }
}
