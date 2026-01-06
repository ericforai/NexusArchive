// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: StatsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.stats.ArchivalTrendDto;
import com.nexusarchive.dto.stats.DashboardStatsDto;
import com.nexusarchive.dto.stats.StorageStatsDto;
import com.nexusarchive.dto.stats.TaskStatusStatsDto;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.modules.borrowing.infra.mapper.BorrowingMapper;
import com.nexusarchive.service.StatsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final BorrowingMapper borrowingMapper;

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

    @GetMapping("/borrowing")
    public Result<BorrowingStats> getBorrowingStats() {
        long pending = borrowingMapper.selectCount(
                new LambdaQueryWrapper<Borrowing>().eq(Borrowing::getStatus, "PENDING")
        );
        long approved = borrowingMapper.selectCount(
                new LambdaQueryWrapper<Borrowing>().eq(Borrowing::getStatus, "APPROVED")
        );
        long borrowed = borrowingMapper.selectCount(
                new LambdaQueryWrapper<Borrowing>().eq(Borrowing::getStatus, "BORROWED")
        );
        long overdue = borrowingMapper.selectCount(
                new LambdaQueryWrapper<Borrowing>().eq(Borrowing::getStatus, "OVERDUE")
        );

        BorrowingStats stats = new BorrowingStats();
        stats.setPendingCount((int) pending);
        stats.setApprovedCount((int) approved);
        stats.setBorrowedCount((int) borrowed);
        stats.setOverdueCount((int) overdue);
        stats.setTotalActiveCount((int) (pending + approved + borrowed));

        return Result.success(stats);
    }

    /**
     * 借阅统计
     */
    @Data
    public static class BorrowingStats {
        private int pendingCount;
        private int approvedCount;
        private int borrowedCount;
        private int overdueCount;
        private int totalActiveCount;
    }
}
