package com.nexusarchive.service.borrow.impl;

// Input: BorrowRequestService 接口, Borrow Commands
// Output: 业务逻辑实现, 数据库持久化
// Pos: service/borrow/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.entity.BorrowRequest.BorrowStatus;
import com.nexusarchive.mapper.BorrowRequestMapper;
import com.nexusarchive.service.borrow.BorrowRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 借阅申请服务实现
 * 
 * 核心理念：原子性操作，强类型校验，明确的状态机流转。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowRequestServiceImpl implements BorrowRequestService {

    private final BorrowRequestMapper borrowRequestMapper;
    private final ObjectMapper objectMapper;
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    @Override
    @Transactional
    public BorrowRequest submit(SubmitBorrowRequestCommand command) {
        BorrowRequest request = new BorrowRequest();
        request.setRequestNo(generateRequestNo());
        request.setApplicantId(command.applicantId());
        request.setApplicantName(command.applicantName());
        request.setDeptId(command.deptId());
        request.setDeptName(command.deptName());
        request.setPurpose(command.purpose());
        request.setBorrowType(command.borrowType());
        request.setExpectedStartDate(command.expectedStartDate());
        request.setExpectedEndDate(command.expectedEndDate());
        request.setStatus(BorrowStatus.PENDING);
        request.setArchiveIds(toJson(command.archiveIds()));
        request.setArchiveCount(command.archiveIds().size());
        
        request.setCreatedTime(LocalDateTime.now());
        request.setUpdatedTime(LocalDateTime.now());
        
        borrowRequestMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public void approve(ApproveBorrowRequestCommand command) {
        BorrowRequest request = getOrThrow(command.requestId());
        if (request.getStatus() != BorrowStatus.PENDING) {
            throw new BusinessException(ErrorCode.BORROW_INVALID_STATUS);
        }

        request.setStatus(command.approved() ? BorrowStatus.APPROVED : BorrowStatus.REJECTED);
        request.setApproverId(command.approverId());
        request.setApproverName(command.approverName());
        request.setApprovalTime(LocalDateTime.now());
        request.setApprovalComment(command.comment());

        borrowRequestMapper.updateById(request);
    }

    @Override
    @Transactional
    public void confirmOut(String requestId) {
        BorrowRequest request = getOrThrow(requestId);
        if (request.getStatus() != BorrowStatus.APPROVED) {
            throw new BusinessException(ErrorCode.BORROW_INVALID_STATUS);
        }

        request.setStatus(BorrowStatus.BORROWING);
        request.setActualStartDate(LocalDateTime.now().toLocalDate());
        borrowRequestMapper.updateById(request);
    }

    @Override
    @Transactional
    public void returnArchives(String requestId, String operatorId) {
        BorrowRequest request = getOrThrow(requestId);
        if (request.getStatus() != BorrowStatus.BORROWING && request.getStatus() != BorrowStatus.OVERDUE) {
            throw new BusinessException(ErrorCode.BORROW_INVALID_STATUS);
        }

        request.setStatus(BorrowStatus.RETURNED);
        request.setReturnTime(LocalDateTime.now());
        request.setActualEndDate(LocalDateTime.now().toLocalDate());
        request.setReturnOperatorId(operatorId);
        borrowRequestMapper.updateById(request);
    }

    private BorrowRequest getOrThrow(String id) {
        BorrowRequest request = borrowRequestMapper.selectById(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.BORROW_RECORD_NOT_FOUND);
        }
        return request;
    }

    private String generateRequestNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return String.format("BR-%s-%04d", date, seq);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize archive IDs", e);
        }
    }
}
