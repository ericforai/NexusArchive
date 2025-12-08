package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsDto {
    private long totalArchives;
    private String storageUsed;
    private long pendingTasks;
    private long todayIngest;
    private List<ArchivalTrendDto> recentTrend;
}
