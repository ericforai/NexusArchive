// Input: Spring Web、Lombok
// Output: AsyncMonitorController 类（异步任务监控API）
// Pos: 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.config.AsyncTaskMonitor;
import com.nexusarchive.config.AsyncTaskMonitor.ThreadPoolStatus;
import com.nexusarchive.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 异步任务监控控制器
 *
 * <p>提供线程池状态查询接口，用于运维监控
 */
@Tag(name = "异步任务监控", description = "异步任务线程池状态监控接口")
@RestController
@RequestMapping("/admin/async")
@RequiredArgsConstructor
public class AsyncMonitorController {

    private final AsyncTaskMonitor asyncTaskMonitor;

    /**
     * 获取所有线程池状态
     *
     * @return 线程池状态列表
     */
    @Operation(summary = "获取所有线程池状态", description = "返回所有已注册线程池的详细状态信息")
    @GetMapping("/thread-pools")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN')")
    public ApiResponse<Map<String, ThreadPoolStatus>> getAllThreadPoolStatus() {
        Map<String, ThreadPoolStatus> status = asyncTaskMonitor.getAllThreadPoolStatus();
        return ApiResponse.success(status);
    }

    /**
     * 获取指定线程池状态
     *
     * @param executorName 线程池名称（taskExecutor, ingestTaskExecutor, erpSyncExecutor 等）
     * @return 线程池状态
     */
    @Operation(summary = "获取指定线程池状态", description = "返回指定线程池的详细状态信息")
    @GetMapping("/thread-pools/{executorName}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN')")
    public ApiResponse<ThreadPoolStatus> getThreadPoolStatus(@PathVariable String executorName) {
        ThreadPoolStatus status = asyncTaskMonitor.getThreadPoolStatus(executorName);
        if (status == null) {
            return ApiResponse.error("404", "线程池不存在: " + executorName);
        }
        return ApiResponse.success(status);
    }

    /**
     * 获取线程池健康状态
     *
     * @return 健康状态
     */
    @Operation(summary = "获取线程池健康状态", description = "检查所有线程池的健康状态")
    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN')")
    public ApiResponse<Map<String, Boolean>> getThreadPoolHealth() {
        Map<String, Boolean> health = new HashMap<>();

        // 检查各线程池健康状态
        health.put("taskExecutor", asyncTaskMonitor.isHealthy("taskExecutor"));
        health.put("ingestTaskExecutor", asyncTaskMonitor.isHealthy("ingestTaskExecutor"));
        health.put("erpSyncExecutor", asyncTaskMonitor.isHealthy("erpSyncExecutor"));
        health.put("batchOperationExecutor", asyncTaskMonitor.isHealthy("batchOperationExecutor"));
        health.put("reconciliationExecutor", asyncTaskMonitor.isHealthy("reconciliationExecutor"));

        return ApiResponse.success(health);
    }

    /**
     * 获取线程池概要信息
     *
     * @return 概要信息
     */
    @Operation(summary = "获取线程池概要", description = "返回线程池的概要统计信息")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN')")
    public ApiResponse<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        Map<String, ThreadPoolStatus> statuses = asyncTaskMonitor.getAllThreadPoolStatus();

        int totalActiveThreads = 0;
        int totalMaxThreads = 0;
        long totalCompletedTasks = 0;
        long totalQueuedTasks = 0;

        for (ThreadPoolStatus status : statuses.values()) {
            totalActiveThreads += status.getActiveCount();
            totalMaxThreads += status.getMaximumPoolSize();
            totalCompletedTasks += status.getCompletedTaskCount();
            totalQueuedTasks += status.getQueueSize();
        }

        summary.put("executorCount", statuses.size());
        summary.put("totalActiveThreads", totalActiveThreads);
        summary.put("totalMaxThreads", totalMaxThreads);
        summary.put("totalCompletedTasks", totalCompletedTasks);
        summary.put("totalQueuedTasks", totalQueuedTasks);
        summary.put("overallUsagePercent", totalMaxThreads > 0
                ? Math.round((double) totalActiveThreads / totalMaxThreads * 10000) / 100.0
                : 0);

        return ApiResponse.success(summary);
    }
}
