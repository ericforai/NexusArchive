package com.nexusarchive.service;

import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;

import java.util.List;

public interface StatsService {
    DashboardStatsDto getDashboardStats();
    StorageStatsDto getStorageStats();
    List<ArchivalTrendDto> getArchivalTrend();
    TaskStatusStatsDto getTaskStatusStats();
}
