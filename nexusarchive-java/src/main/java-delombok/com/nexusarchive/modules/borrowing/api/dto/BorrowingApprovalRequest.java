// Input: Lombok
// Output: BorrowingApprovalRequest
// Pos: borrowing/api/dto
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.api.dto;

import lombok.Data;

@Data
public class BorrowingApprovalRequest {
    private boolean approved;
    private String comment;
}
