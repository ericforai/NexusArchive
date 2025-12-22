// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BorrowingServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.enums.BorrowingStatus;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Borrowing;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.BorrowingMapper;
import com.nexusarchive.service.BorrowingService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowingServiceImpl implements BorrowingService {

    private final BorrowingMapper borrowingMapper;
    private final ArchiveMapper archiveMapper;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional
    public Borrowing createBorrowing(Borrowing borrowing, String userId, String userName) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException("未获取到当前用户，请重新登录后重试");
        }
        if (borrowing.getArchiveId() == null || borrowing.getArchiveId().isBlank()) {
            throw new BusinessException("借阅档案不能为空");
        }
        Archive archive = archiveMapper.selectById(borrowing.getArchiveId());
        if (archive == null) {
            throw new BusinessException("档案不存在，无法发起借阅");
        }
        borrowing.setUserId(userId);
        borrowing.setUserName(userName);
        borrowing.setArchiveTitle(archive.getTitle());
        borrowing.setStatus(BorrowingStatus.PENDING.getCode()); // Default status
        if (borrowing.getBorrowDate() == null) {
            borrowing.setBorrowDate(LocalDate.now());
        }
        if (borrowing.getExpectedReturnDate() == null) {
            borrowing.setExpectedReturnDate(borrowing.getBorrowDate().plusDays(30)); // Default 30 days
        }
        borrowingMapper.insert(borrowing);
        return borrowing;
    }

    @Override
    public Page<Borrowing> getBorrowings(int page, int limit, String status, String userId) {
        Page<Borrowing> pageParam = new Page<>(page, limit);
        QueryWrapper<Borrowing> queryWrapper = new QueryWrapper<>();
        List<String> statuses = parseStatuses(status);
        if (!statuses.isEmpty()) {
            if (statuses.size() == 1) {
                queryWrapper.eq("status", statuses.get(0));
            } else {
                queryWrapper.in("status", statuses);
            }
        }
        if (userId != null && !userId.isEmpty()) {
            queryWrapper.eq("user_id", userId);
        }
        DataScopeContext scope = dataScopeService.resolve();
        dataScopeService.applyBorrowingScope(queryWrapper, scope);
        queryWrapper.orderByDesc("created_at");
        return borrowingMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    @Transactional
    public Borrowing approveBorrowing(String id, boolean approved, String comment) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.PENDING);

        borrowing.setStatus(approved ? BorrowingStatus.APPROVED.getCode() : BorrowingStatus.REJECTED.getCode());
        borrowing.setApprovalComment(comment);
        if (approved && borrowing.getBorrowDate() == null) {
            borrowing.setBorrowDate(LocalDate.now());
        }
        borrowingMapper.updateById(borrowing);
        return borrowing;
    }

    @Override
    @Transactional
    public void returnArchive(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.APPROVED);
        borrowing.setStatus(BorrowingStatus.RETURNED.getCode());
        borrowing.setActualReturnDate(LocalDate.now());
        borrowingMapper.updateById(borrowing);
    }

    @Override
    @Transactional
    public void cancelBorrowing(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.PENDING);
        borrowing.setStatus(BorrowingStatus.CANCELLED.getCode());
        borrowingMapper.updateById(borrowing);
    }

    private Borrowing getExistingBorrowing(String id) {
        Borrowing borrowing = borrowingMapper.selectById(id);
        if (borrowing == null) {
            throw new BusinessException("借阅记录不存在或已被删除");
        }
        return borrowing;
    }

    private void assertStatus(Borrowing borrowing, BorrowingStatus... allowed) {
        BorrowingStatus current;
        try {
            current = BorrowingStatus.fromCode(borrowing.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
        boolean match = Arrays.stream(allowed).anyMatch(status -> status == current);
        if (!match) {
            throw new BusinessException("当前状态不允许执行此操作");
        }
    }

    private List<String> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return BorrowingStatus.fromCode(s).getCode();
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException(e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }
}
