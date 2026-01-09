// Input: Java 标准库、本地模块
// Output: StatsService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

    /**
     * 清除仪表盘缓存
     */
    void evictDashboardCache();

    /**
     * 清除趋势缓存
     */
    void evictTrendCache();
}
