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
