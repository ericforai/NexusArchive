// Input: Java 标准库、Lombok
// Output: BorrowingDto
// Pos: borrowing/api/dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BorrowingDto {
    private String id;
    private String userId;
    private String userName;
    private String archiveId;
    private String archiveTitle;
    private String reason;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private String status;
    private String approvalComment;
    private LocalDateTime createdTime;
    private LocalDateTime lastModifiedTime;
}
