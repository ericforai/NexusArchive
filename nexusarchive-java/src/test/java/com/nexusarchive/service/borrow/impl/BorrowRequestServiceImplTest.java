package com.nexusarchive.service.borrow.impl;

// Input: JUnit 5, Mockito
// Output: 测试结果
// Pos: test/service/borrow/impl
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.entity.BorrowRequest.BorrowStatus;
import com.nexusarchive.entity.BorrowRequest.BorrowType;
import com.nexusarchive.mapper.BorrowRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BorrowRequestServiceImplTest {

    @Mock
    private BorrowRequestMapper borrowRequestMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private BorrowRequestServiceImpl borrowRequestService;

    private SubmitBorrowRequestCommand submitCommand;

    @BeforeEach
    void setUp() {
        submitCommand = new SubmitBorrowRequestCommand(
            "user-1", "Test User", "dept-1", "Test Dept",
            "Research", BorrowType.READING, List.of("arc-1", "arc-2"),
            LocalDate.now(), LocalDate.now().plusDays(7)
        );
    }

    @Test
    @DisplayName("提交申请应成功持久化")
    void testSubmitSuccess() {
        BorrowRequest result = borrowRequestService.submit(submitCommand);

        assertNotNull(result);
        assertEquals("Test User", result.getApplicantName());
        assertEquals(BorrowStatus.PENDING, result.getStatus());
        assertEquals(2, result.getArchiveCount());
        verify(borrowRequestMapper, times(1)).insert(any(BorrowRequest.class));
    }

    @Test
    @DisplayName("审批通过应更新状态为 APPROVED")
    void testApproveSuccess() {
        BorrowRequest request = new BorrowRequest();
        request.setId("req-1");
        request.setStatus(BorrowStatus.PENDING);

        when(borrowRequestMapper.selectById("req-1")).thenReturn(request);

        ApproveBorrowRequestCommand approveCommand = new ApproveBorrowRequestCommand(
            "req-1", "admin-1", "Admin", true, "OK"
        );

        borrowRequestService.approve(approveCommand);

        assertEquals(BorrowStatus.APPROVED, request.getStatus());
        assertEquals("Admin", request.getApproverName());
        verify(borrowRequestMapper, times(1)).updateById(request);
    }

    @Test
    @DisplayName("审批已处理的申请应抛出异常")
    void testApproveInvalidStatus() {
        BorrowRequest request = new BorrowRequest();
        request.setId("req-1");
        request.setStatus(BorrowStatus.BORROWING);

        when(borrowRequestMapper.selectById("req-1")).thenReturn(request);

        ApproveBorrowRequestCommand approveCommand = new ApproveBorrowRequestCommand(
            "req-1", "admin-1", "Admin", true, "OK"
        );

        assertThrows(BusinessException.class, () -> borrowRequestService.approve(approveCommand));
    }
}
