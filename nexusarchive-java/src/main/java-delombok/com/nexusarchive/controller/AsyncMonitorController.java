// Input: Spring Web、Lombok
// Output: AsyncMonitorController 类（异步任务监控API）
// Pos: 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.config.AsyncTaskMonitor;
import com.nexusarchive.config.AsyncTaskMonitor.ThreadPoolStatus;
import com.nexusarchive.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "异步任务监控", description = """
    异步任务线程池状态监控接口。

    **功能说明:**
    - 查询所有线程池状态
    - 查询指定线程池状态
    - 线程池健康检查
    - 线程池概要统计

    **已注册线程池:**
    - taskExecutor: 通用任务线程池
    - ingestTaskExecutor: SIP 接收任务线程池
    - erpSyncExecutor: ERP 同步任务线程池
    - batchOperationExecutor: 批量操作线程池
    - reconciliationExecutor: 对账任务线程池

    **监控指标:**
    - activeCount: 活跃线程数
    - corePoolSize: 核心线程数
    - maximumPoolSize: 最大线程数
    - queueSize: 队列中等待任务数
    - completedTaskCount: 已完成任务数
    - isHealthy: 健康状态

    **使用场景:**
    - 运维监控仪表盘
    - 线程池调优参考
    - 性能问题排查

    **权限要求:**
    - SYSTEM_ADMIN: 系统管理员
    - AUDIT_ADMIN: 安全审计员
    """
)
@SecurityRequirement(name = "Bearer Authentication")
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
    @GetMapping("/thread-pools")
    @Operation(
        summary = "获取所有线程池状态",
        description = """
            返回所有已注册线程池的详细状态信息。

            **返回数据包括:**
            - 每个线程池的详细状态映射
            - 包括活跃线程数、队列大小、完成任务数等

            **使用场景:**
            - 运维监控仪表盘
            - 线程池状态概览
            """,
        operationId = "getAllThreadPoolStatus",
        tags = {"异步任务监控"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
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
    @GetMapping("/thread-pools/{executorName}")
    @Operation(
        summary = "获取指定线程池状态",
        description = """
            返回指定线程池的详细状态信息。

            **路径参数:**
            - executorName: 线程池名称

            **可用线程池:**
            - taskExecutor: 通用任务线程池
            - ingestTaskExecutor: SIP 接收任务线程池
            - erpSyncExecutor: ERP 同步任务线程池
            - batchOperationExecutor: 批量操作线程池
            - reconciliationExecutor: 对账任务线程池

            **使用场景:**
            - 单个线程池详细监控
            - 性能问题定位
            """,
        operationId = "getThreadPoolStatus",
        tags = {"异步任务监控"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "线程池不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN')")
    public ApiResponse<ThreadPoolStatus> getThreadPoolStatus(
            @Parameter(description = "线程池名称", required = true, example = "taskExecutor")
            @PathVariable String executorName) {
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
    @GetMapping("/health")
    @Operation(
        summary = "获取线程池健康状态",
        description = """
            检查所有线程池的健康状态。

            **健康判定标准:**
            - 活跃线程数 < 最大线程数的 80%
            - 队列未满
            - 线程池未关闭

            **返回数据包括:**
            - 各线程池的健康状态（true/false）

            **使用场景:**
            - 系统健康检查
            - 告警触发判定
            """,
        operationId = "getThreadPoolHealth",
        tags = {"异步任务监控"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "检查完成"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
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
    @GetMapping("/summary")
    @Operation(
        summary = "获取线程池概要",
        description = """
            返回线程池的概要统计信息。

            **返回数据包括:**
            - executorCount: 线程池总数
            - totalActiveThreads: 总活跃线程数
            - totalMaxThreads: 总最大线程数
            - totalCompletedTasks: 总完成任务数
            - totalQueuedTasks: 总排队任务数
            - overallUsagePercent: 整体使用率百分比

            **使用场景:**
            - 系统资源概览
            - 容量规划参考
            """,
        operationId = "getThreadPoolSummary",
        tags = {"异步任务监控"}
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未认证"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
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
