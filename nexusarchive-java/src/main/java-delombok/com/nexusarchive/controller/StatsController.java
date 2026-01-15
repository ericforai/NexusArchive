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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析控制器
 *
 * PRD 来源: 仪表盘模块
 * 提供各类统计分析数据
 */
@Tag(name = "统计分析", description = """
    系统统计分析接口。

    **功能说明:**
    - 获取仪表盘统计数据
    - 获取存储空间统计
    - 获取归档趋势数据
    - 获取任务状态统计
    - 获取借阅统计

    **仪表盘指标:**
    - totalArchives: 档案总数
    - pendingApprovals: 待审批数量
    - todayIngest: 今日归档数
    - systemHealth: 系统健康度

    **存储统计:**
    - totalStorage: 总存储空间
    - usedStorage: 已用存储
    - availableStorage: 可用存储
    - storageUsage: 存储使用率

    **归档趋势:**
    - 按日期统计归档数量
    - 支持最近 30 天数据
    - 按全宗分组统计

    **任务状态:**
    - pending: 待处理
    - processing: 处理中
    - completed: 已完成
    - failed: 失败

    **借阅统计:**
    - pendingCount: 待审批
    - approvedCount: 已批准
    - borrowedCount: 借阅中
    - overdueCount: 已逾期
    - totalActiveCount: 活跃总数

    **使用场景:**
    - 仪表盘数据展示
    - 数据分析报表
    - 运营监控

    **权限要求:**
    - 认证用户可访问
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final BorrowingMapper borrowingMapper;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('archive:view', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取仪表盘统计数据",
        description = """
            获取系统仪表盘的核心指标数据。

            **返回数据包括:**
            - totalArchives: 档案总数
            - pendingApprovals: 待审批数量
            - todayIngest: 今日归档数
            - systemHealth: 系统健康度
            - recentActivities: 最近活动列表

            **业务规则:**
            - 今日归档按自然日统计
            - 系统健康度综合计算
            - 按当前用户全宗过滤

            **使用场景:**
            - 仪表盘首页
            - 系统概览
            """,
        operationId = "getDashboardStats",
        tags = {"统计分析"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<DashboardStatsDto> getDashboardStats() {
        return Result.success(statsService.getDashboardStats());
    }

    /**
     * 获取存储空间统计
     */
    @GetMapping("/storage")
    @PreAuthorize("hasAnyAuthority('archive:view', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取存储空间统计",
        description = """
            获取系统存储空间的使用情况。

            **返回数据包括:**
            - totalStorage: 总存储空间（字节）
            - usedStorage: 已用存储（字节）
            - availableStorage: 可用存储（字节）
            - storageUsage: 存储使用率（百分比）
            - fileCount: 文件总数

            **业务规则:**
            - 扫描配置的存储路径
            - 递归计算子目录
            - 实时统计

            **使用场景:**
            - 存储监控
            - 容量规划
            """,
        operationId = "getStorageStats",
        tags = {"统计分析"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<StorageStatsDto> getStorageStats() {
        return Result.success(statsService.getStorageStats());
    }

    /**
     * 获取归档趋势数据
     */
    @GetMapping("/archival-trend")
    @PreAuthorize("hasAnyAuthority('archive:view', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取归档趋势数据",
        description = """
            获取最近一段时间的归档趋势数据。

            **返回数据包括:**
            - date: 日期
            - count: 归档数量
            - volumeCount: 案卷数量
            - fondsCode: 全宗号

            **业务规则:**
            - 默认返回最近 30 天
            - 按日期升序排列
            - 按当前用户全宗过滤

            **使用场景:**
            - 趋势图表展示
            - 归档进度分析
            """,
        operationId = "getArchivalTrend",
        tags = {"统计分析"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<ArchivalTrendDto>> getArchivalTrend() {
        return Result.success(statsService.getArchivalTrend());
    }

    /**
     * 获取任务状态统计
     */
    @GetMapping("/ingest-status")
    @PreAuthorize("hasAnyAuthority('archive:view', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取任务状态统计",
        description = """
            获取预归档任务的状态分布统计。

            **返回数据包括:**
            - pending: 待处理数量
            - processing: 处理中数量
            - completed: 已完成数量
            - failed: 失败数量
            - totalCount: 总任务数

            **业务规则:**
            - 统计 arc_file_content 表
            - 按预归档状态分组
            - 按当前用户全宗过滤

            **使用场景:**
            - 任务监控
            - 进度查看
            """,
        operationId = "getTaskStatusStats",
        tags = {"统计分析"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<TaskStatusStatsDto> getTaskStatus() {
        return Result.success(statsService.getTaskStatusStats());
    }

    /**
     * 获取借阅统计
     */
    @GetMapping("/borrowing")
    @PreAuthorize("hasAnyAuthority('borrowing:view', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取借阅统计",
        description = """
            获取档案借阅的状态分布统计。

            **返回数据包括:**
            - pendingCount: 待审批数量
            - approvedCount: 已批准数量
            - borrowedCount: 借阅中数量
            - overdueCount: 已逾期数量
            - totalActiveCount: 活跃总数（待审批+已批准+借阅中）

            **状态说明:**
            - PENDING: 待审批
            - APPROVED: 已批准
            - BORROWED: 借阅中
            - OVERDUE: 已逾期

            **业务规则:**
            - 统计 biz_borrowing 表
            - 按状态分组统计
            - 逾期判断基于应还日期

            **使用场景:**
            - 借阅管理仪表盘
            - 逾期提醒
            """,
        operationId = "getBorrowingStats",
        tags = {"统计分析"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
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
     * 借阅统计 DTO
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
