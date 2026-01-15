// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BorrowingApplicationService 类
// Pos: borrowing/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingApprovalRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingCreateRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingDto;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.modules.borrowing.domain.BorrowingStatus;
import com.nexusarchive.modules.borrowing.infra.mapper.BorrowingMapper;
import com.nexusarchive.service.ArchiveReadService;
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
@Deprecated(since = "2026-01-13", forRemoval = true)
public class BorrowingApplicationService implements BorrowingFacade {

    private final BorrowingMapper borrowingMapper;
    private final ArchiveReadService archiveService;
    private final DataScopeService dataScopeService;
    private final BorrowingScopePolicy borrowingScopePolicy;

    @Override
    @Transactional
    public BorrowingDto createBorrowing(BorrowingCreateRequest request, String userId, String userName) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BORROW_REQUEST_CANNOT_BE_EMPTY);
        }
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.BORROW_USER_NOT_FOUND);
        }
        if (request.getArchiveId() == null || request.getArchiveId().isBlank()) {
            throw new BusinessException(ErrorCode.BORROW_ARCHIVE_CANNOT_BE_EMPTY);
        }
        com.nexusarchive.entity.Archive archive = archiveService.getArchiveById(request.getArchiveId());
        if (archive == null) {
            throw new BusinessException(ErrorCode.BORROW_ARCHIVE_NOT_FOUND);
        }

        Borrowing borrowing = new Borrowing();
        borrowing.setArchiveId(request.getArchiveId());
        borrowing.setReason(request.getReason());
        borrowing.setBorrowDate(request.getBorrowDate());
        borrowing.setExpectedReturnDate(request.getExpectedReturnDate());

        borrowing.setUserId(userId);
        borrowing.setUserName(userName);
        borrowing.setArchiveTitle(archive.getTitle());
        // 填充新增字段 - 2025-12-31 schema update
        borrowing.setFondsNo(archive.getFondsNo());
        if (archive.getFiscalYear() != null) {
            try {
                borrowing.setArchiveYear(Integer.parseInt(archive.getFiscalYear()));
            } catch (NumberFormatException e) {
                // Ignore invalid year format
            }
        }
        borrowing.setStatus(BorrowingStatus.PENDING.getCode());
        if (borrowing.getBorrowDate() == null) {
            borrowing.setBorrowDate(LocalDate.now());
        }
        if (borrowing.getExpectedReturnDate() == null) {
            borrowing.setExpectedReturnDate(borrowing.getBorrowDate().plusDays(30));
        }
        borrowingMapper.insert(borrowing);
        return toDto(borrowing);
    }

    @Override
    public Page<BorrowingDto> getBorrowings(int page, int limit, String status, String userId) {
        Page<Borrowing> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<>();
        List<String> statuses = parseStatuses(status);
        if (!statuses.isEmpty()) {
            if (statuses.size() == 1) {
                queryWrapper.eq(Borrowing::getStatus, statuses.get(0));
            } else {
                queryWrapper.in(Borrowing::getStatus, statuses);
            }
        }
        if (userId != null && !userId.isEmpty()) {
            queryWrapper.eq(Borrowing::getUserId, userId);
        }
        DataScopeContext scope = dataScopeService.resolve();
        borrowingScopePolicy.apply(queryWrapper, scope);
        queryWrapper.orderByDesc(Borrowing::getCreatedTime);

        Page<Borrowing> result = borrowingMapper.selectPage(pageParam, queryWrapper);
        return mapPage(result);
    }

    @Override
    @Transactional
    public BorrowingDto approveBorrowing(String id, BorrowingApprovalRequest approvalRequest) {
        if (approvalRequest == null) {
            throw new BusinessException(ErrorCode.BORROW_APPROVAL_PARAMS_CANNOT_BE_EMPTY);
        }
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.PENDING);

        borrowing.setStatus(approvalRequest.isApproved() ? BorrowingStatus.APPROVED.getCode() : BorrowingStatus.REJECTED.getCode());
        borrowing.setApprovalComment(approvalRequest.getComment());
        if (approvalRequest.isApproved() && borrowing.getBorrowDate() == null) {
            borrowing.setBorrowDate(LocalDate.now());
        }
        borrowingMapper.updateById(borrowing);
        return toDto(borrowing);
    }
    
    /**
     * 确认借出（从 APPROVED 状态转换为 BORROWED）
     */
    @Transactional
    public BorrowingDto confirmBorrowed(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.APPROVED);
        borrowing.setStatus(BorrowingStatus.BORROWED.getCode());
        if (borrowing.getBorrowDate() == null) {
            borrowing.setBorrowDate(LocalDate.now());
        }
        borrowingMapper.updateById(borrowing);
        return toDto(borrowing);
    }

    @Override
    @Transactional
    public void returnArchive(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        // 允许从 BORROWED 或 OVERDUE 状态归还
        assertStatus(borrowing, BorrowingStatus.BORROWED, BorrowingStatus.OVERDUE);
        borrowing.setStatus(BorrowingStatus.RETURNED.getCode());
        borrowing.setActualReturnDate(LocalDate.now());
        borrowingMapper.updateById(borrowing);
    }
    
    /**
     * 标记逾期
     */
    @Transactional
    public void markOverdue(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.BORROWED);
        borrowing.setStatus(BorrowingStatus.OVERDUE.getCode());
        borrowingMapper.updateById(borrowing);
    }
    
    /**
     * 标记丢失
     */
    @Transactional
    public void markLost(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        assertStatus(borrowing, BorrowingStatus.BORROWED, BorrowingStatus.OVERDUE);
        borrowing.setStatus(BorrowingStatus.LOST.getCode());
        borrowingMapper.updateById(borrowing);
    }

    @Override
    public boolean checkAccess(String userId, String archiveId, String action) {
        // 简单实现：检查用户是否有针对该档案的有效借阅记录
        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Borrowing::getUserId, userId)
                .eq(Borrowing::getArchiveId, archiveId)
                .in(Borrowing::getStatus,
                    BorrowingStatus.APPROVED.getCode(),
                    BorrowingStatus.BORROWED.getCode(),
                    BorrowingStatus.OVERDUE.getCode());
        return borrowingMapper.selectCount(queryWrapper) > 0;
    }

    @Override
    @Transactional
    public void cancelBorrowing(String id) {
        Borrowing borrowing = getExistingBorrowing(id);
        // 允许从 PENDING 或 APPROVED 状态取消
        assertStatus(borrowing, BorrowingStatus.PENDING, BorrowingStatus.APPROVED);
        borrowing.setStatus(BorrowingStatus.CANCELLED.getCode());
        borrowingMapper.updateById(borrowing);
    }

    private Borrowing getExistingBorrowing(String id) {
        Borrowing borrowing = borrowingMapper.selectById(id);
        if (borrowing == null) {
            throw new BusinessException(ErrorCode.BORROW_RECORD_NOT_FOUND);
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
            throw new BusinessException(ErrorCode.BORROW_INVALID_STATUS);
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

    private BorrowingDto toDto(Borrowing borrowing) {
        BorrowingDto dto = new BorrowingDto();
        dto.setId(borrowing.getId());
        dto.setUserId(borrowing.getUserId());
        dto.setUserName(borrowing.getUserName());
        dto.setArchiveId(borrowing.getArchiveId());
        dto.setArchiveTitle(borrowing.getArchiveTitle());
        dto.setReason(borrowing.getReason());
        dto.setBorrowDate(borrowing.getBorrowDate());
        dto.setExpectedReturnDate(borrowing.getExpectedReturnDate());
        dto.setActualReturnDate(borrowing.getActualReturnDate());
        dto.setStatus(borrowing.getStatus());
        dto.setApprovalComment(borrowing.getApprovalComment());
        dto.setCreatedTime(borrowing.getCreatedTime());
        // TODO: 临时禁用，待 updated_at 列添加后恢复
        // dto.setLastModifiedTime(borrowing.getLastModifiedTime());
        return dto;
    }

    private Page<BorrowingDto> mapPage(Page<Borrowing> page) {
        Page<BorrowingDto> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toDto).collect(Collectors.toList()));
        return dtoPage;
    }
}
