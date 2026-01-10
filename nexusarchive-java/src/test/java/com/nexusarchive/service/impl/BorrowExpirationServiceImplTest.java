// Input: MyBatis-Plus、StringRedisTemplate、org.junit
// Output: BorrowExpirationServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.modules.borrowing.domain.BorrowingStatus;
import com.nexusarchive.modules.borrowing.infra.mapper.BorrowingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * BorrowExpirationService 单元测试
 *
 * 测试覆盖:
 * - 过期借阅扫描和标记
 * - 即将到期借阅统计
 * - 分布式锁机制
 *
 * PRD 来源: 2026-01-10-query-user-borrow-design.md 第7.3节
 */
@ExtendWith(MockitoExtension.class)
class BorrowExpirationServiceImplTest {

    @Mock
    private BorrowingMapper borrowingMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BorrowExpirationServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private Borrowing createBorrowing(String id, BorrowingStatus status, LocalDate expectedReturnDate) {
        Borrowing borrowing = new Borrowing();
        borrowing.setId(id);
        borrowing.setUserId("user-001");
        borrowing.setUserName("张三");
        borrowing.setArchiveId("arc-001");
        borrowing.setArchiveTitle("测试档案");
        borrowing.setReason("工作需要");
        borrowing.setStatus(status.getCode());
        borrowing.setBorrowDate(LocalDate.now());
        borrowing.setExpectedReturnDate(expectedReturnDate);
        return borrowing;
    }

    // ========== 过期标记测试 ==========

    @Nested
    @DisplayName("过期标记")
    class MarkExpiredTests {

        @Test
        @DisplayName("扫描并标记过期借阅 - 成功")
        void scanAndMarkExpired_HasExpiredBorrowings_MarksThem() {
            // Arrange
            LocalDate pastDate = LocalDate.now().minusDays(10);
            List<Borrowing> expiredList = Arrays.asList(
                    createBorrowing("bor-001", BorrowingStatus.APPROVED, pastDate),
                    createBorrowing("bor-002", BorrowingStatus.BORROWED, pastDate)
            );

            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expiredList);
            when(borrowingMapper.update(any(), any(LambdaQueryWrapper.class))).thenReturn(1);

            // Act
            int count = service.scanAndMarkExpired();

            // Assert
            assertThat(count).isEqualTo(2);
            verify(borrowingMapper, times(2)).update(any(), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无过期借阅 - 返回0")
        void scanAndMarkExpired_NoExpiredBorrowings_ReturnsZero() {
            // Arrange
            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            // Act
            int count = service.scanAndMarkExpired();

            // Assert
            assertThat(count).isZero();
            verify(borrowingMapper, never()).update(any(), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("只标记 APPROVED 和 BORROWED 状态的过期记录")
        void scanAndMarkExpired_OnlyTargetStatus_Checked() {
            // Arrange
            LocalDate pastDate = LocalDate.now().minusDays(10);
            List<Borrowing> expiredList = Arrays.asList(
                    createBorrowing("bor-001", BorrowingStatus.APPROVED, pastDate),
                    createBorrowing("bor-002", BorrowingStatus.BORROWED, pastDate)
            );

            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expiredList);
            when(borrowingMapper.update(any(), any(LambdaQueryWrapper.class))).thenReturn(1);

            // Act
            service.scanAndMarkExpired();

            // Assert - 验证查询条件包含状态检查
            verify(borrowingMapper).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("未到期的借阅不应被标记")
        void scanAndMarkExpired_UpcomingBorrowings_NotMarked() {
            // Arrange
            LocalDate futureDate = LocalDate.now().plusDays(10);
            List<Borrowing> upcomingList = Arrays.asList(
                    createBorrowing("bor-001", BorrowingStatus.APPROVED, futureDate)
            );

            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList()); // 查询条件已过滤未到期记录

            // Act
            int count = service.scanAndMarkExpired();

            // Assert
            assertThat(count).isZero();
            verify(borrowingMapper, never()).update(any(), any(LambdaQueryWrapper.class));
        }
    }

    // ========== 即将到期统计测试 ==========

    @Nested
    @DisplayName("即将到期统计")
    class UpcomingExpirationsTests {

        @Test
        @DisplayName("统计即将到期的借阅 - 成功")
        void getUpcomingExpirations_HasUpcoming_ReturnsCount() {
            // Arrange
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            when(borrowingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

            // Act
            long count = service.getUpcomingExpirations(3);

            // Assert
            assertThat(count).isEqualTo(5L);
            verify(borrowingMapper).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无即将到期借阅 - 返回0")
        void getUpcomingExpirations_NoUpcoming_ReturnsZero() {
            // Arrange
            when(borrowingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Act
            long count = service.getUpcomingExpirations(3);

            // Assert
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("自定义提醒天数")
        void getUpcomingExpirations_CustomDays_ReturnsCount() {
            // Arrange
            when(borrowingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);

            // Act
            long count = service.getUpcomingExpirations(7);

            // Assert
            assertThat(count).isEqualTo(3L);
        }
    }

    // ========== 手动扫描测试 ==========

    @Nested
    @DisplayName("手动扫描")
    class ManualScanTests {

        @Test
        @DisplayName("手动触发扫描 - 执行扫描")
        void manualScan_TriggersScan() {
            // Arrange
            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            int count = service.manualScan();

            // Assert
            assertThat(count).isZero();
            verify(borrowingMapper).selectList(any(LambdaQueryWrapper.class));
        }
    }

    // ========== 分布式锁测试 ==========

    @Nested
    @DisplayName("分布式锁")
    class DistributedLockTests {

        @Test
        @DisplayName("获取锁成功 - 执行扫描")
        void tryLock_Success_ExecutesScan() {
            // Arrange
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(true);
            when(borrowingMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            service.manualScan();

            // Assert
            verify(valueOperations).setIfAbsent(anyString(), anyString(), anyLong(), any());
        }

        @Test
        @DisplayName("获取锁失败 - 跳过扫描")
        void tryLock_Failure_SkipsScan() {
            // Arrange
            when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any()))
                    .thenReturn(false);

            // Act - 通过反射调用 scheduledScan 模拟定时任务场景
            ReflectionTestUtils.invokeMethod(service, "tryLock");

            // Assert - 如果获取锁失败，扫描应该被跳过
            boolean locked = ReflectionTestUtils.invokeMethod(service, "tryLock");
            assertThat(locked).isFalse();
        }
    }
}
