// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: MonitoringServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.monitoring.IntegrationMonitoringDTO;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final SyncHistoryMapper syncHistoryMapper;
    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper fileContentMapper;

    @Override
    public IntegrationMonitoringDTO getIntegrationMetrics() {
        // 1. 统计同步历史
        long totalSync = syncHistoryMapper.selectCount(null);
        long successSync = syncHistoryMapper.selectCount(new LambdaQueryWrapper<SyncHistory>()
                .eq(SyncHistory::getStatus, "SUCCESS"));
        double successRate = totalSync == 0 ? 0 : (double) successSync / totalSync;

        // 2. 统计档案库状态
        long totalArchived = archiveMapper.selectCount(null);
        long totalFiles = fileContentMapper.selectCount(null);

        // 计算证据覆盖率 (简单逻辑：关联了文件的档案数/总档案数)
        // 实际需更复杂的 join 统计
        double coverage = totalArchived == 0 ? 0 : Math.min(1.0, (double) totalFiles / totalArchived);

        // 3. 构造趋势数据 (最近7天 Mock)
        // 3. 构造趋势数据 (真实数据聚合)
        List<IntegrationMonitoringDTO.DataPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        // 查询最近7天的记录 (内存聚合以兼容多数据库Function差异)
        List<SyncHistory> recentHistory = syncHistoryMapper.selectList(
                new LambdaQueryWrapper<SyncHistory>()
                        .ge(SyncHistory::getSyncStartTime, startDate.atStartOfDay()));

        // 按日期和状态分组
        java.util.Map<String, java.util.Map<String, Long>> stats = recentHistory.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getSyncStartTime().toLocalDate().toString(),
                        Collectors.groupingBy(
                                h -> "SUCCESS".equals(h.getStatus()) ? "SUCCESS" : "FAIL",
                                Collectors.counting())));

        // 补全最近7天的数据 (即使无数据也显示0)
        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            java.util.Map<String, Long> dayStats = stats.getOrDefault(dateStr, java.util.Collections.emptyMap());

            trend.add(IntegrationMonitoringDTO.DataPoint.builder()
                    .date(dateStr)
                    .value(dayStats.getOrDefault("SUCCESS", 0L))
                    .type("SUCCESS")
                    .build());

            trend.add(IntegrationMonitoringDTO.DataPoint.builder()
                    .date(dateStr)
                    .value(dayStats.getOrDefault("FAIL", 0L))
                    .type("FAIL")
                    .build());
        }

        return IntegrationMonitoringDTO.builder()
                .totalSyncCount(totalSync)
                .successSyncCount(successSync)
                .successRate(successRate)
                .totalArchivedItems(totalArchived)
                .totalAttachedFiles(totalFiles)
                .evidenceCoverage(coverage)
                .erpHealthStatus("HEALTHY")
                .dailySyncTrend(trend)
                .build();
    }

    @Override
    public void runHealthCheck() {
        // 巡检逻辑
    }
}
