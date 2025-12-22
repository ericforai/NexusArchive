// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: StatsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;
import com.nexusarchive.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<DashboardStatsDto> getDashboardStats() {
        return Result.success(statsService.getDashboardStats());
    }

    @GetMapping("/storage")
    public Result<StorageStatsDto> getStorageStats() {
        return Result.success(statsService.getStorageStats());
    }

    @GetMapping("/archival-trend")
    public Result<List<ArchivalTrendDto>> getArchivalTrend() {
        return Result.success(statsService.getArchivalTrend());
    }

    @GetMapping("/ingest-status")
    public Result<TaskStatusStatsDto> getTaskStatus() {
        return Result.success(statsService.getTaskStatusStats());
    }
}
