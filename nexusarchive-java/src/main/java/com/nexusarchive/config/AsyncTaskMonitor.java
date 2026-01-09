// Input: Spring Framework、Java 标准库、Lombok
// Output: AsyncTaskMonitor 类（异步任务监控工具）
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务监控组件
 *
 * <p>提供线程池状态监控和统计信息：
 * <ul>
 *   <li>活跃线程数</li>
 *   <li>队列大小</li>
 *   <li>已完成任务数</li>
 *   <li>线程池使用率</li>
 * </ul>
 */
@Slf4j
@Component
public class AsyncTaskMonitor {

    private final Map<String, Executor> executors = new ConcurrentHashMap<>();
    private final Map<String, TaskStatistics> statistics = new ConcurrentHashMap<>();

    /**
     * 注册需要监控的线程池
     *
     * @param name     线程池名称
     * @param executor 线程池执行器
     */
    public void registerExecutor(String name, Executor executor) {
        executors.put(name, executor);
        statistics.putIfAbsent(name, new TaskStatistics());
        log.info("注册异步线程池监控: {}", name);
    }

    /**
     * 获取线程池状态信息
     *
     * @param executorName 线程池名称
     * @return 状态信息
     */
    public ThreadPoolStatus getThreadPoolStatus(String executorName) {
        Executor executor = executors.get(executorName);
        if (!(executor instanceof ThreadPoolTaskExecutor)) {
            return null;
        }

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;
        ThreadPoolExecutor threadPool = taskExecutor.getThreadPoolExecutor();
        TaskStatistics stats = statistics.get(executorName);

        ThreadPoolStatus status = new ThreadPoolStatus();
        status.setExecutorName(executorName);
        status.setCorePoolSize(threadPool.getCorePoolSize());
        status.setMaximumPoolSize(threadPool.getMaximumPoolSize());
        status.setActiveCount(threadPool.getActiveCount());
        status.setPoolSize(threadPool.getPoolSize());
        status.setQueueSize(threadPool.getQueue().size());
        status.setQueueRemainingCapacity(threadPool.getQueue().remainingCapacity());
        status.setCompletedTaskCount(threadPool.getCompletedTaskCount());
        status.setTaskCount(threadPool.getTaskCount());
        status.setRejectedTaskCount(stats != null ? stats.getRejectedCount() : 0);

        // 计算使用率
        double usage = (double) threadPool.getActiveCount() / threadPool.getMaximumPoolSize() * 100;
        status.setUsagePercentage(Math.round(usage * 100.0) / 100.0);

        return status;
    }

    /**
     * 获取所有线程池状态
     *
     * @return 状态信息列表
     */
    public Map<String, ThreadPoolStatus> getAllThreadPoolStatus() {
        Map<String, ThreadPoolStatus> result = new ConcurrentHashMap<>();
        executors.keySet().forEach(name -> {
            ThreadPoolStatus status = getThreadPoolStatus(name);
            if (status != null) {
                result.put(name, status);
            }
        });
        return result;
    }

    /**
     * 记录任务拒绝
     *
     * @param executorName 线程池名称
     */
    public void recordRejectedTask(String executorName) {
        TaskStatistics stats = statistics.get(executorName);
        if (stats != null) {
            stats.incrementRejectedCount();
        }
        log.warn("异步任务被拒绝 - 线程池: {}", executorName);
    }

    /**
     * 定时打印线程池状态（每5分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void logThreadPoolStatus() {
        if (log.isInfoEnabled()) {
            Map<String, ThreadPoolStatus> statuses = getAllThreadPoolStatus();
            statuses.forEach((name, status) -> {
                log.info("线程池 [{}] 状态 - 活跃: {}/{}, 队列: {}/{}, 使用率: {}%, 完成: {}, 拒绝: {}",
                        name,
                        status.getActiveCount(),
                        status.getMaximumPoolSize(),
                        status.getQueueSize(),
                        status.getQueueSize() + status.getQueueRemainingCapacity(),
                        status.getUsagePercentage(),
                        status.getCompletedTaskCount(),
                        status.getRejectedTaskCount());

                // 使用率告警阈值
                if (status.getUsagePercentage() > 80) {
                    log.warn("线程池 [{}] 使用率过高: {}%", name, status.getUsagePercentage());
                }
            });
        }
    }

    /**
     * 检查线程池健康状态
     *
     * @param executorName 线程池名称
     * @return 是否健康
     */
    public boolean isHealthy(String executorName) {
        ThreadPoolStatus status = getThreadPoolStatus(executorName);
        if (status == null) {
            return true; // 未注册视为健康
        }

        // 健康标准：使用率 < 90% 且队列未满
        boolean usageHealthy = status.getUsagePercentage() < 90;
        boolean queueHealthy = status.getQueueRemainingCapacity() > 0;

        return usageHealthy && queueHealthy;
    }

    /**
     * 线程池状态信息
     */
    @Data
    public static class ThreadPoolStatus {
        private String executorName;
        private int corePoolSize;
        private int maximumPoolSize;
        private int activeCount;
        private int poolSize;
        private int queueSize;
        private int queueRemainingCapacity;
        private long completedTaskCount;
        private long taskCount;
        private long rejectedTaskCount;
        private double usagePercentage;
    }

    /**
     * 任务统计信息
     */
    @Data
    private static class TaskStatistics {
        private volatile long rejectedCount = 0;

        public synchronized void incrementRejectedCount() {
            this.rejectedCount++;
        }

        public synchronized long getRejectedCount() {
            return rejectedCount;
        }
    }
}
