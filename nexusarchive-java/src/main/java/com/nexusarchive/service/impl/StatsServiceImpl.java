// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: StatsServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.nexusarchive.security.FondsContext;
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
import java.util.ArrayList;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    @Override
    @Cacheable(value = "stats", key = "'dashboard:' + #root.target.getClass().getSimpleName() + ':' + T(com.nexusarchive.security.FondsContext).getCurrentFondsNo()")
    public DashboardStatsDto getDashboardStats() {
        DataScopeContext scope = dataScopeService.resolve();
        String currentFondsNo = FondsContext.getCurrentFondsNo();

        LambdaQueryWrapper<Archive> totalWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyArchiveScope(totalWrapper, scope);
        long totalArchives = archiveMapper.selectCount(totalWrapper);

        Long usedBytes = arcFileContentMapper.sumFileSize();
        long safeUsedBytes = usedBytes != null ? usedBytes : 0L;
        String storageUsed = formatSize(safeUsedBytes);

        // 修复: 添加全宗过滤到待处理任务统计
        LambdaQueryWrapper<IngestRequestStatus> pendingWrapper = new LambdaQueryWrapper<>();
        if (currentFondsNo != null && !currentFondsNo.isEmpty()) {
            pendingWrapper.eq(IngestRequestStatus::getFondsNo, currentFondsNo);
        }
        pendingWrapper.ne(IngestRequestStatus::getStatus, "COMPLETED")
                .ne(IngestRequestStatus::getStatus, "FAILED");
        long pendingTasks = ingestRequestStatusMapper.selectCount(pendingWrapper);

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Archive> todayWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyArchiveScope(todayWrapper, scope);
        todayWrapper.ge(Archive::getCreatedTime, startOfDay);
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
    @Cacheable(value = "stats", key = "'trend:' + T(java.time.LocalDate).now() + ':' + T(com.nexusarchive.security.FondsContext).getCurrentFondsNo()")
    public List<ArchivalTrendDto> getArchivalTrend() {
        // 近 30 天归档趋势，直接使用参数化 SQL，避免 LambdaQueryWrapper + apply/last 生成不稳定 SQL。
        Map<LocalDate, Long> dailyCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dailyCounts.put(date, 0L);
        }

        DataScopeContext scope = dataScopeService.resolve();
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT
                  date_trunc('day', created_time)::date AS date,
                  COUNT(*) AS count
                FROM acc_archive
                WHERE deleted = 0
                  AND created_time >= ?
                """);

        params.add(LocalDate.now().minusDays(29));
        applyArchiveScope(sql, params, scope);

        sql.append("""
                
                GROUP BY date_trunc('day', created_time)
                ORDER BY date
                """);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
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

    private void applyArchiveScope(StringBuilder sql, List<Object> params, DataScopeContext scope) {
        if (scope == null || scope.isAll()) {
            return;
        }

        String currentFondsNo = FondsContext.getCurrentFondsNo();
        if (currentFondsNo != null && !currentFondsNo.isBlank()) {
            sql.append(" AND fonds_no = ?");
            params.add(currentFondsNo);
            return;
        }

        if (!scope.allowedFonds().isEmpty()) {
            sql.append(" AND fonds_no IN (");
            for (int i = 0; i < scope.allowedFonds().size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")");
            params.addAll(scope.allowedFonds());
            return;
        }

        if (scope.isSelf()) {
            if (scope.userId() != null) {
                sql.append(" AND created_by = ?");
                params.add(scope.userId());
            } else {
                sql.append(" AND 1 = 0");
            }
            return;
        }

        sql.append(" AND 1 = 0");
    }

    @Override
    public TaskStatusStatsDto getTaskStatusStats() {
        // Use JdbcTemplate to completely avoid QueryWrapper issues in both ArchUnit and unit tests
        String sql = "SELECT status, COUNT(*) as cnt FROM sys_ingest_request_status GROUP BY status";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

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
