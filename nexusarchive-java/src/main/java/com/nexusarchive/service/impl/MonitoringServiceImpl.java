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
        List<IntegrationMonitoringDTO.DataPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            String dateStr = today.minusDays(i).toString();
            trend.add(IntegrationMonitoringDTO.DataPoint.builder()
                    .date(dateStr)
                    .value(100 + (long)(Math.random() * 50))
                    .type("SUCCESS")
                    .build());
            trend.add(IntegrationMonitoringDTO.DataPoint.builder()
                    .date(dateStr)
                    .value((long)(Math.random() * 10))
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
