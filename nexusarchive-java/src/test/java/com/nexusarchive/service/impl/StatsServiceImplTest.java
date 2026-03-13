// Input: org.junit、org.mockito、Spring Framework、Java 标准库、等
// Output: StatsServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StatsService 单元测试
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private IngestRequestStatusMapper ingestRequestStatusMapper;

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(statsService, "archiveRootPath", System.getProperty("java.io.tmpdir") + "/test-archives");
    }

    @Test
    void getDashboardStats_ShouldReturnCorrectStats() {
        // Arrange
        when(archiveMapper.selectCount(any())).thenReturn(1500L); // total
        when(arcFileContentMapper.sumFileSize()).thenReturn(1024L * 1024 * 500); // 500 MB
        when(ingestRequestStatusMapper.selectCount(any())).thenReturn(5L); // pending tasks
        
        // Mock data scope resolve
        when(dataScopeService.resolve()).thenReturn(DataScopeService.DataScopeContext.all());

        // Act
        DashboardStatsDto result = statsService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(1500L, result.getTotalArchives());
        assertEquals(5L, result.getPendingTasks());
        assertNotNull(result.getStorageUsed());
        assertTrue(result.getStorageUsed().contains("500.0 MB"));
    }

    @Test
    void getStorageStats_ShouldReturnCorrectStats() {
        // Arrange
        long usedBytes = 1024L * 1024 * 1024 * 50L; // 50 GB
        when(arcFileContentMapper.sumFileSize()).thenReturn(usedBytes);

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        assertTrue(result.getUsed().contains("50.0 GB"));
    }

    @Test
    void getArchivalTrend_ShouldReturn30DaysData() {
        // Arrange
        List<Map<String, Object>> mockDbResult = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 模拟数据库返回 2 天的数据
        Map<String, Object> row1 = new HashMap<>();
        row1.put("date", today.toString());
        row1.put("count", 10L);
        mockDbResult.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("date", today.minusDays(1).toString());
        row2.put("count", 20L);
        mockDbResult.add(row2);

        when(dataScopeService.resolve()).thenReturn(DataScopeService.DataScopeContext.all());
        when(archiveMapper.selectMaps(any())).thenReturn(mockDbResult);

        // Act
        List<ArchivalTrendDto> result = statsService.getArchivalTrend();

        // Assert
        assertNotNull(result);
        assertEquals(30, result.size());
        
        // Today's data (last element)
        assertEquals(10L, result.get(29).getCount());
        // Yesterday's data
        assertEquals(20L, result.get(28).getCount());
    }

    @Test
    void getTaskStatusStats_ShouldReturnCorrectStats() {
        // Arrange
        List<Map<String, Object>> mockDbResult = new ArrayList<>();

        Map<String, Object> completedRow = new HashMap<>();
        completedRow.put("status", "COMPLETED");
        completedRow.put("cnt", 100L);
        mockDbResult.add(completedRow);

        Map<String, Object> failedRow = new HashMap<>();
        failedRow.put("status", "FAILED");
        failedRow.put("cnt", 2L);
        mockDbResult.add(failedRow);

        when(jdbcTemplate.queryForList(anyString())).thenReturn(mockDbResult);

        // Act
        TaskStatusStatsDto result = statsService.getTaskStatusStats();

        // Assert
        assertNotNull(result);
        assertEquals(102L, result.getTotal());
        assertEquals(100L, result.getCompleted());
        assertEquals(2L, result.getFailed());
    }

    @Test
    void getStorageStats_WhenTotalSpaceIsUnknown_ShouldReturnUnknown() {
        // Arrange
        when(arcFileContentMapper.sumFileSize()).thenReturn(1024L);
        ReflectionTestUtils.setField(statsService, "archiveRootPath", "/invalid/nonexistent/path/that/does/not/exist");

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        assertEquals("未知", result.getTotal());
    }

    @Test
    void formatSize_ShouldFormatCorrectly() {
        // Arrange
        when(arcFileContentMapper.sumFileSize()).thenReturn(1024L);

        // Act
        StorageStatsDto result = statsService.getStorageStats();

        // Assert
        assertNotNull(result);
        assertEquals("1.0 KB", result.getUsed());
    }
}
