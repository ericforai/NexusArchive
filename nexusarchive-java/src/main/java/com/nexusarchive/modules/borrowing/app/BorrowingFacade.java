// Input: MyBatis-Plus、Borrowing DTO
// Output: BorrowingFacade 接口
// Pos: borrowing/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingApprovalRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingCreateRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingDto;

public interface BorrowingFacade {
    BorrowingDto createBorrowing(BorrowingCreateRequest request, String userId, String userName);

    Page<BorrowingDto> getBorrowings(int page, int limit, String status, String userId);

    BorrowingDto approveBorrowing(String id, BorrowingApprovalRequest approvalRequest);

    void returnArchive(String id);

    void cancelBorrowing(String id);
}
