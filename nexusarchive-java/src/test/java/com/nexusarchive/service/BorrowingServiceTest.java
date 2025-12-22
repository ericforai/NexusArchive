// Input: MyBatis-Plus、org.junit、org.mockito、Java 标准库、等
// Output: BorrowingServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.enums.BorrowingStatus;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Borrowing;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.BorrowingMapper;
import com.nexusarchive.service.impl.BorrowingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BorrowingService 单元测试
 * 
 * 测试覆盖:
 * - 借阅申请创建
 * - 借阅审批（通过/拒绝）
 * - 档案归还
 * - 借阅取消
 * - 状态机验证
 * 
 * @author Agent E - 质量保障工程师
 */
@ExtendWith(MockitoExtension.class)
class BorrowingServiceTest {

    @Mock
    private BorrowingMapper borrowingMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private BorrowingServiceImpl borrowingService;

    private Borrowing validBorrowing;
    private Archive testArchive;

    @BeforeEach
    void setUp() {
        // 创建测试档案
        testArchive = new Archive();
        testArchive.setId("arc-001");
        testArchive.setTitle("测试档案");
        testArchive.setArchiveCode("ARC-2023-001");

        // 创建有效的借阅申请
        validBorrowing = new Borrowing();
        validBorrowing.setArchiveId("arc-001");
        validBorrowing.setReason("工作需要");
    }

    // ========== 创建借阅申请测试 ==========

    @Nested
    @DisplayName("创建借阅申请")
    class CreateBorrowingTests {

        @Test
        @DisplayName("正常创建借阅申请 - 成功")
        void createBorrowing_ValidRequest_Success() {
            // Arrange
            when(archiveMapper.selectById("arc-001")).thenReturn(testArchive);
            when(borrowingMapper.insert(any(Borrowing.class))).thenReturn(1);

            // Act
            Borrowing result = borrowingService.createBorrowing(validBorrowing, "user-001", "张三");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user-001");
            assertThat(result.getUserName()).isEqualTo("张三");
            assertThat(result.getArchiveTitle()).isEqualTo("测试档案");
            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.PENDING.getCode());
            assertThat(result.getBorrowDate()).isNotNull();
            assertThat(result.getExpectedReturnDate()).isNotNull();

            verify(borrowingMapper).insert(any(Borrowing.class));
        }

        @Test
        @DisplayName("用户ID为空 - 抛出异常")
        void createBorrowing_NullUserId_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> borrowingService.createBorrowing(validBorrowing, null, "张三"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未获取到当前用户");
        }

        @Test
        @DisplayName("用户ID为空字符串 - 抛出异常")
        void createBorrowing_BlankUserId_ThrowsException() {
            // Act & Assert
            assertThatThrownBy(() -> borrowingService.createBorrowing(validBorrowing, "  ", "张三"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未获取到当前用户");
        }

        @Test
        @DisplayName("档案ID为空 - 抛出异常")
        void createBorrowing_NullArchiveId_ThrowsException() {
            // Arrange
            validBorrowing.setArchiveId(null);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.createBorrowing(validBorrowing, "user-001", "张三"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("借阅档案不能为空");
        }

        @Test
        @DisplayName("档案不存在 - 抛出异常")
        void createBorrowing_NonExistentArchive_ThrowsException() {
            // Arrange
            when(archiveMapper.selectById("arc-001")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.createBorrowing(validBorrowing, "user-001", "张三"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("档案不存在");
        }

        @Test
        @DisplayName("自定义借阅日期和归还日期")
        void createBorrowing_CustomDates_Preserved() {
            // Arrange
            LocalDate borrowDate = LocalDate.of(2023, 12, 1);
            LocalDate returnDate = LocalDate.of(2024, 1, 1);
            validBorrowing.setBorrowDate(borrowDate);
            validBorrowing.setExpectedReturnDate(returnDate);
            
            when(archiveMapper.selectById("arc-001")).thenReturn(testArchive);
            when(borrowingMapper.insert(any(Borrowing.class))).thenReturn(1);

            // Act
            Borrowing result = borrowingService.createBorrowing(validBorrowing, "user-001", "张三");

            // Assert
            assertThat(result.getBorrowDate()).isEqualTo(borrowDate);
            assertThat(result.getExpectedReturnDate()).isEqualTo(returnDate);
        }
    }

    // ========== 借阅审批测试 ==========

    @Nested
    @DisplayName("借阅审批")
    class ApproveBorrowingTests {

        @Test
        @DisplayName("审批通过 - 成功")
        void approveBorrowing_Approve_Success() {
            // Arrange
            Borrowing pendingBorrowing = createBorrowingWithStatus(BorrowingStatus.PENDING);
            when(borrowingMapper.selectById("bor-001")).thenReturn(pendingBorrowing);
            when(borrowingMapper.updateById(any(Borrowing.class))).thenReturn(1);

            // Act
            Borrowing result = borrowingService.approveBorrowing("bor-001", true, "同意借阅");

            // Assert
            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.APPROVED.getCode());
            assertThat(result.getApprovalComment()).isEqualTo("同意借阅");
            assertThat(result.getBorrowDate()).isNotNull();

            verify(borrowingMapper).updateById(any(Borrowing.class));
        }

        @Test
        @DisplayName("审批拒绝 - 成功")
        void approveBorrowing_Reject_Success() {
            // Arrange
            Borrowing pendingBorrowing = createBorrowingWithStatus(BorrowingStatus.PENDING);
            when(borrowingMapper.selectById("bor-001")).thenReturn(pendingBorrowing);
            when(borrowingMapper.updateById(any(Borrowing.class))).thenReturn(1);

            // Act
            Borrowing result = borrowingService.approveBorrowing("bor-001", false, "资料不外借");

            // Assert
            assertThat(result.getStatus()).isEqualTo(BorrowingStatus.REJECTED.getCode());
            assertThat(result.getApprovalComment()).isEqualTo("资料不外借");
        }

        @Test
        @DisplayName("借阅记录不存在 - 抛出异常")
        void approveBorrowing_NotFound_ThrowsException() {
            // Arrange
            when(borrowingMapper.selectById("non-existent")).thenReturn(null);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.approveBorrowing("non-existent", true, ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("借阅记录不存在");
        }

        @Test
        @DisplayName("非待审批状态无法审批 - 抛出异常")
        void approveBorrowing_WrongStatus_ThrowsException() {
            // Arrange
            Borrowing approvedBorrowing = createBorrowingWithStatus(BorrowingStatus.APPROVED);
            when(borrowingMapper.selectById("bor-001")).thenReturn(approvedBorrowing);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.approveBorrowing("bor-001", true, ""))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("当前状态不允许执行此操作");
        }
    }

    // ========== 归还档案测试 ==========

    @Nested
    @DisplayName("归还档案")
    class ReturnArchiveTests {

        @Test
        @DisplayName("归还成功")
        void returnArchive_Success() {
            // Arrange
            Borrowing approvedBorrowing = createBorrowingWithStatus(BorrowingStatus.APPROVED);
            when(borrowingMapper.selectById("bor-001")).thenReturn(approvedBorrowing);
            when(borrowingMapper.updateById(any(Borrowing.class))).thenReturn(1);

            // Act
            borrowingService.returnArchive("bor-001");

            // Assert
            ArgumentCaptor<Borrowing> captor = ArgumentCaptor.forClass(Borrowing.class);
            verify(borrowingMapper).updateById(captor.capture());
            
            Borrowing updated = captor.getValue();
            assertThat(updated.getStatus()).isEqualTo(BorrowingStatus.RETURNED.getCode());
            assertThat(updated.getActualReturnDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("非已批准状态无法归还 - 抛出异常")
        void returnArchive_WrongStatus_ThrowsException() {
            // Arrange
            Borrowing pendingBorrowing = createBorrowingWithStatus(BorrowingStatus.PENDING);
            when(borrowingMapper.selectById("bor-001")).thenReturn(pendingBorrowing);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.returnArchive("bor-001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("当前状态不允许执行此操作");
        }

        @Test
        @DisplayName("已归还状态无法再次归还 - 抛出异常")
        void returnArchive_AlreadyReturned_ThrowsException() {
            // Arrange
            Borrowing returnedBorrowing = createBorrowingWithStatus(BorrowingStatus.RETURNED);
            when(borrowingMapper.selectById("bor-001")).thenReturn(returnedBorrowing);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.returnArchive("bor-001"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== 取消借阅测试 ==========

    @Nested
    @DisplayName("取消借阅")
    class CancelBorrowingTests {

        @Test
        @DisplayName("取消成功")
        void cancelBorrowing_Success() {
            // Arrange
            Borrowing pendingBorrowing = createBorrowingWithStatus(BorrowingStatus.PENDING);
            when(borrowingMapper.selectById("bor-001")).thenReturn(pendingBorrowing);
            when(borrowingMapper.updateById(any(Borrowing.class))).thenReturn(1);

            // Act
            borrowingService.cancelBorrowing("bor-001");

            // Assert
            ArgumentCaptor<Borrowing> captor = ArgumentCaptor.forClass(Borrowing.class);
            verify(borrowingMapper).updateById(captor.capture());
            
            assertThat(captor.getValue().getStatus()).isEqualTo(BorrowingStatus.CANCELLED.getCode());
        }

        @Test
        @DisplayName("非待审批状态无法取消 - 抛出异常")
        void cancelBorrowing_WrongStatus_ThrowsException() {
            // Arrange
            Borrowing approvedBorrowing = createBorrowingWithStatus(BorrowingStatus.APPROVED);
            when(borrowingMapper.selectById("bor-001")).thenReturn(approvedBorrowing);

            // Act & Assert
            assertThatThrownBy(() -> borrowingService.cancelBorrowing("bor-001"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("当前状态不允许执行此操作");
        }
    }

    // ========== 查询借阅列表测试 ==========

    @Nested
    @DisplayName("查询借阅列表")
    class GetBorrowingsTests {

        @Test
        @DisplayName("带状态筛选的分页查询")
        void getBorrowings_WithStatus_Success() {
            // Arrange
            Page<Borrowing> mockPage = new Page<>(1, 10);
            mockPage.setTotal(1);
            
            when(dataScopeService.resolve()).thenReturn(mock(DataScopeService.DataScopeContext.class));
            doNothing().when(dataScopeService).applyBorrowingScope(any(QueryWrapper.class), any());
            when(borrowingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            // Act
            Page<Borrowing> result = borrowingService.getBorrowings(1, 10, "PENDING", "user-001");

            // Assert
            assertThat(result).isNotNull();
            verify(borrowingMapper).selectPage(any(Page.class), any(QueryWrapper.class));
        }

        @Test
        @DisplayName("多状态筛选")
        void getBorrowings_MultipleStatuses_Success() {
            // Arrange
            Page<Borrowing> mockPage = new Page<>(1, 10);
            when(dataScopeService.resolve()).thenReturn(mock(DataScopeService.DataScopeContext.class));
            doNothing().when(dataScopeService).applyBorrowingScope(any(QueryWrapper.class), any());
            when(borrowingMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

            // Act
            Page<Borrowing> result = borrowingService.getBorrowings(1, 10, "PENDING,APPROVED", null);

            // Assert
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("无效状态 - 抛出异常")
        void getBorrowings_InvalidStatus_ThrowsException() {
            // Act & Assert
            // 异常在 parseStatuses 阶段就会抛出，不需要 mock dataScopeService
            assertThatThrownBy(() -> borrowingService.getBorrowings(1, 10, "INVALID_STATUS", null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ========== 辅助方法 ==========

    private Borrowing createBorrowingWithStatus(BorrowingStatus status) {
        Borrowing borrowing = new Borrowing();
        borrowing.setId("bor-001");
        borrowing.setUserId("user-001");
        borrowing.setUserName("张三");
        borrowing.setArchiveId("arc-001");
        borrowing.setArchiveTitle("测试档案");
        borrowing.setStatus(status.getCode());
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setExpectedReturnDate(LocalDate.now().plusDays(30));
        return borrowing;
    }
}
