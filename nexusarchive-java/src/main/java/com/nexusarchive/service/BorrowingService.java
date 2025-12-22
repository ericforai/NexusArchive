// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: BorrowingService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Borrowing;

public interface BorrowingService {
    Borrowing createBorrowing(Borrowing borrowing, String userId, String userName);

    Page<Borrowing> getBorrowings(int page, int limit, String status, String userId);

    Borrowing approveBorrowing(String id, boolean approved, String comment);

    void returnArchive(String id);

    void cancelBorrowing(String id);
}
