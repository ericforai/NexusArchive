// Input: Java 标准库、Lombok
// Output: BorrowingCreateRequest
// Pos: borrowing/api/dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.api.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BorrowingCreateRequest {
    private String archiveId;
    private String reason;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
}
