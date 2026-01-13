// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: StatsServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import com.nexusarchive.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final IngestRequestStatusMapper ingestRequestStatusMapper;
    private final DataScopeService dataScopeService;

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    @Override
    @Cacheable(value = "stats", key = "'dashboard:' + #root.target.getClass().getSimpleName()")
    public DashboardStatsDto getDashboardStats() {
        DataScopeContext scope = dataScopeService.resolve();
        QueryWrapper<Archive> totalWrapper = new QueryWrapper<>();
        dataScopeService.applyArchiveScope(totalWrapper, scope);
        long totalArchives = archiveMapper.selectCount(totalWrapper);

        Long usedBytes = arcFileContentMapper.sumFileSize();
        long safeUsedBytes = usedBytes != null ? usedBytes : 0L;
        String storageUsed = formatSize(safeUsedBytes);

        long pendingTasks = ingestRequestStatusMapper.selectCount(new QueryWrapper<IngestRequestStatus>()
                .ne("status", "COMPLETED")
                .ne("status", "FAILED"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        QueryWrapper<Archive> todayWrapper = new QueryWrapper<>();
        dataScopeService.applyArchiveScope(todayWrapper, scope);
        todayWrapper.ge("created_time", startOfDay);
        long todayIngest = archiveMapper.selectCount(todayWrapper);

        return DashboardStatsDto.builder()
                .totalArchives(totalArchives)
                .storageUsed(storageUsed)
                .pendingTasks(pendingTasks)
                .todayIngest(todayIngest)
                .recentTrend(getArchivalTrend())
                .build();
    }

    @Override
    public StorageStatsDto getStorageStats() {
        Long usedBytes = arcFileContentMapper.sumFileSize();
        long safeUsedBytes = usedBytes != null ? usedBytes : 0L;
        long totalBytes = resolveTotalSpace();

        return StorageStatsDto.builder()
                .total(totalBytes > 0 ? formatSize(totalBytes) : "未知")
                .used(formatSize(safeUsedBytes))
                .usagePercent(totalBytes > 0 ? (double) safeUsedBytes / totalBytes * 100 : 0)
                .build();
    }

    @Override
    @Cacheable(value = "stats", key = "'trend:' + T(java.time.LocalDate).now()")
    public List<ArchivalTrendDto> getArchivalTrend() {
        // 近 30 天归档趋势，使用 SQL 聚合
        Map<LocalDate, Long> dailyCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dailyCounts.put(date, 0L);
        }

        DataScopeContext scope = dataScopeService.resolve();
        QueryWrapper<Archive> trendWrapper = new QueryWrapper<>();
        dataScopeService.applyArchiveScope(trendWrapper, scope);
        trendWrapper.select("to_char(date(created_time), 'YYYY-MM-DD') AS date", "COUNT(*) AS count");
        trendWrapper.ge("created_time", LocalDate.now().minusDays(29));
        trendWrapper.groupBy("date(created_time)");
        trendWrapper.orderByAsc("date(created_time)");
        List<Map<String, Object>> rows = archiveMapper.selectMaps(trendWrapper);
        for (Map<String, Object> row : rows) {
            String dateStr = String.valueOf(row.get("date"));
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
            long cnt = Long.parseLong(String.valueOf(row.get("count")));
            if (dailyCounts.containsKey(date)) {
                dailyCounts.put(date, cnt);
            }
        }

        return dailyCounts.entrySet().stream()
                .map(entry -> ArchivalTrendDto.builder()
                        .date(entry.getKey().format(DateTimeFormatter.ISO_DATE))
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public TaskStatusStatsDto getTaskStatusStats() {
        List<Map<String, Object>> rows = ingestRequestStatusMapper.selectMaps(new QueryWrapper<IngestRequestStatus>()
                .select("status as status", "count(*) as cnt")
                .groupBy("status"));

        Map<String, Long> byStatus = rows.stream()
                .collect(Collectors.toMap(
                        r -> (String) r.get("status"),
                        r -> ((Number) r.get("cnt")).longValue()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long completed = byStatus.getOrDefault("COMPLETED", 0L);
        long failed = byStatus.getOrDefault("FAILED", 0L);
        long running = byStatus.getOrDefault("PROCESSING", 0L) + byStatus.getOrDefault("CHECKING", 0L);
        long pending = total - completed - failed - running;

        return TaskStatusStatsDto.builder()
                .total(total)
                .byStatus(byStatus)
                .completed(completed)
                .failed(failed)
                .running(running)
                .pending(Math.max(pending, 0))
                .build();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private long resolveTotalSpace() {
        try {
            File root = new File(archiveRootPath);
            if (!root.exists()) {
                root.mkdirs();
            }
            return root.getTotalSpace();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 清除仪表盘缓存
     * 用于数据变更后刷新统计信息
     */
    @CacheEvict(value = "stats", allEntries = true)
    public void evictDashboardCache() {
        log.debug("Dashboard cache evicted");
    }

    /**
     * 清除趋势缓存
     * 用于数据变更后刷新趋势统计
     */
    @CacheEvict(value = "stats", allEntries = true)
    public void evictTrendCache() {
        log.debug("Trend cache evicted");
    }

    /**
     * 清除所有统计缓存
     * 用于数据变更后刷新所有统计信息
     */
    @CacheEvict(value = "stats", allEntries = true)
    public void evictStatsCache() {
        log.debug("All stats cache evicted");
    }

    /**
     * 清除存储统计缓存
     * 用于存储变更后刷新存储统计信息
     */
    @CacheEvict(value = "stats", allEntries = true)
    public void evictStorageCache() {
        log.debug("Storage cache evicted");
    }
}
