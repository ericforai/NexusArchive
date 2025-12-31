// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BorrowingApplicationService 类
// Pos: borrowing/app
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.app;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
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
public class BorrowingApplicationService implements BorrowingFacade {

    private final BorrowingMapper borrowingMapper;
    private final ArchiveReadService archiveService;
    private final DataScopeService dataScopeService;
    private final BorrowingScopePolicy borrowingScopePolicy;

    @Override
    @Transactional
    public BorrowingDto createBorrowing(BorrowingCreateRequest request, String userId, String userName) {
        if (request == null) {
            throw new BusinessException("借阅请求不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new BusinessException("未获取到当前用户，请重新登录后重试");
        }
        if (request.getArchiveId() == null || request.getArchiveId().isBlank()) {
            throw new BusinessException("借阅档案不能为空");
        }
        com.nexusarchive.entity.Archive archive = archiveService.getArchiveById(request.getArchiveId());
        if (archive == null) {
            throw new BusinessException("档案不存在，无法发起借阅");
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
        borrowingScopePolicy.apply(queryWrapper, scope);
        queryWrapper.orderByDesc("created_time");

        Page<Borrowing> result = borrowingMapper.selectPage(pageParam, queryWrapper);
        return mapPage(result);
    }

    @Override
    @Transactional
    public BorrowingDto approveBorrowing(String id, BorrowingApprovalRequest approvalRequest) {
        if (approvalRequest == null) {
            throw new BusinessException("审批参数不能为空");
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
        dto.setLastModifiedTime(borrowing.getLastModifiedTime());
        return dto;
    }

    private Page<BorrowingDto> mapPage(Page<Borrowing> page) {
        Page<BorrowingDto> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toDto).collect(Collectors.toList()));
        return dtoPage;
    }
}
