// Input: org.junit、org.mockito、Spring Framework、Java 标准库、等
// Output: StatsServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.StatsMapper;
import com.nexusarchive.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StatsService 单元测试
 *
 * 测试覆盖：
 * 1. 仪表盘统计数据获取
 * 2. 存储统计数据获取
 * 3. 归档趋势数据获取
 * 4. 任务状态统计获取
 * 5. 缓存清除方法
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private StatsMapper statsMapper;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private StatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        // 设置默认的归档根路径
        ReflectionTestUtils.setField(statsService, "archiveRootPath", System.getProperty("java.io.tmpdir") + "/test-archives");
    }

    @Test
    void getDashboardStats_ShouldReturnCorrectStats() {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("total_archives", 1500L);
        mockStats.put("today_ingest", 25L);
        mockStats.put("pending_tasks", 5L);

        List<ArchivalTrendDto> mockTrend = createMockTrendData();

        when(statsMapper.getDashboardStats()).thenReturn(mockStats);
        when(statsMapper.getArchivalTrend(any(LocalDate.class))).thenReturn(new ArrayList<>());
        when(statsMapper.getStorageUsedBytes()).thenReturn(1024L * 1024 * 500); // 500 MB

        // Act
        DashboardStatsDto result = statsService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(1500L, result.getTotalArchives());
        assertEquals(25L, result.getTodayIngest());
        assertEquals(5L, result.getPendingTasks());
        assertNotNull(result.getStorageUsed());
        assertTrue(result.getStorageUsed().contains("MB"));

        // Verify interactions
        verify(statsMapper, times(1)).getDashboardStats();
        verify(statsMapper, times(1)).getArchivalTrend(any(LocalDate.class));
    }

    @Test
    void getStorageStats_ShouldReturnCorrectStats() {
        // Arrange
        long usedBytes = 1024L * 1024 * 1024 * 50; // 50 GB

        when(statsMapper.getStorageUsedBytes()).thenReturn(usedBytes);

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getUsed());
        assertTrue(result.getUsed().contains("GB") || result.getUsed().contains("GiB"));
        assertTrue(result.getUsagePercent() >= 0);

        verify(statsMapper, times(1)).getStorageUsedBytes();
    }

    @Test
    void getArchivalTrend_ShouldReturn30DaysData() {
        // Arrange
        List<Map<String, Object>> mockDbResult = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 模拟数据库返回最近 5 天的数据
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("date", today.minusDays(i));
            row.put("count", 10L + i);
            mockDbResult.add(row);
        }

        when(statsMapper.getArchivalTrend(any(LocalDate.class))).thenReturn(mockDbResult);

        // Act
        List<ArchivalTrendDto> result = statsService.getArchivalTrend();

        // Assert
        assertNotNull(result);
        assertEquals(30, result.size()); // 应该返回 30 天数据

        // 验证前 5 天有数据
        for (int i = 0; i < 5; i++) {
            ArchivalTrendDto dayData = result.get(29 - i);
            assertEquals(10L + i, dayData.getCount());
        }

        // 验证剩余天数为 0
        for (int i = 0; i < 25; i++) {
            assertEquals(0L, result.get(i).getCount());
        }

        verify(statsMapper, times(1)).getArchivalTrend(any(LocalDate.class));
    }

    @Test
    void getTaskStatusStats_ShouldReturnCorrectStats() {
        // Arrange
        List<Map<String, Object>> mockDbResult = new ArrayList<>();

        Map<String, Object> completedRow = new HashMap<>();
        completedRow.put("status", "COMPLETED");
        completedRow.put("cnt", 100L);
        mockDbResult.add(completedRow);

        Map<String, Object> processingRow = new HashMap<>();
        processingRow.put("status", "PROCESSING");
        processingRow.put("cnt", 10L);
        mockDbResult.add(processingRow);

        Map<String, Object> failedRow = new HashMap<>();
        failedRow.put("status", "FAILED");
        failedRow.put("cnt", 2L);
        mockDbResult.add(failedRow);

        when(statsMapper.getTaskStatusStats()).thenReturn(mockDbResult);

        // Act
        TaskStatusStatsDto result = statsService.getTaskStatusStats();

        // Assert
        assertNotNull(result);
        assertEquals(112L, result.getTotal()); // 100 + 10 + 2
        assertEquals(100L, result.getCompleted());
        assertEquals(2L, result.getFailed());
        assertEquals(10L, result.getRunning());
        assertEquals(0L, result.getPending());

        // 验证 byStatus 包含所有状态
        assertNotNull(result.getByStatus());
        assertEquals(3, result.getByStatus().size());

        verify(statsMapper, times(1)).getTaskStatusStats();
    }

    @Test
    void evictStatsCache_ShouldInvokeCacheEvict() {
        // Act & Assert - 这个测试主要是验证方法可以被调用
        // 实际的缓存清除由 Spring AOP 处理，这里只验证方法不会抛出异常
        assertDoesNotThrow(() -> statsService.evictStatsCache());
    }

    @Test
    void evictDashboardCache_ShouldInvokeCacheEvict() {
        assertDoesNotThrow(() -> statsService.evictDashboardCache());
    }

    @Test
    void evictTrendCache_ShouldInvokeCacheEvict() {
        assertDoesNotThrow(() -> statsService.evictTrendCache());
    }

    @Test
    void evictStorageCache_ShouldInvokeCacheEvict() {
        assertDoesNotThrow(() -> statsService.evictStorageCache());
    }

    @Test
    void getStorageStats_WhenTotalSpaceIsUnknown_ShouldReturnUnknown() {
        // Arrange
        when(statsMapper.getStorageUsedBytes()).thenReturn(1024L);
        ReflectionTestUtils.setField(statsService, "archiveRootPath", "/invalid/nonexistent/path");

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        assertEquals("未知", result.getTotal());
        assertEquals(0.0, result.getUsagePercent());
    }

    @Test
    void formatSize_ShouldFormatCorrectly() {
        // Arrange
        when(statsMapper.getStorageUsedBytes()).thenReturn(1024L);

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        // 1024 bytes = 1.0 KB
        assertTrue(result.getUsed().contains("KB"));
    }

    // Helper methods

    private List<ArchivalTrendDto> createMockTrendData() {
        List<ArchivalTrendDto> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 29; i >= 0; i--) {
            ArchivalTrendDto dto = ArchivalTrendDto.builder()
                    .date(today.minusDays(i).toString())
                    .count((long) (Math.random() * 50))
                    .build();
            trend.add(dto);
        }

        return trend;
    }
}
