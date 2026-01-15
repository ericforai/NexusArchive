// Input: Spring Framework、Java 标准库、Lombok、Spring Scheduling
// Output: AsyncConfig 类（异步任务配置）
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 *
 * <p>提供多线程池支持，不同业务场景使用独立线程池：
 * <ul>
 *   <li>taskExecutor - 默认通用异步任务线程池</li>
 *   <li>ingestTaskExecutor - 归档任务专用线程池</li>
 *   <li>reconciliationExecutor - 对账服务专用线程池</li>
 *   <li>erpSyncExecutor - ERP同步服务专用线程池</li>
 * </ul>
 *
 * <p>配置参数（可在 application.yml 中覆盖）：
 * <ul>
 *   <li>async.core-pool-size - 核心线程数（默认：CPU核心数）</li>
 *   <li>async.max-pool-size - 最大线程数（默认：CPU核心数*2）</li>
 *   <li>async.queue-capacity - 队列容量（默认：100）</li>
 *   <li>async.thread-name-prefix - 线程名前缀（默认：async-）</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${async.core-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int corePoolSize;

    @Value("${async.max-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors() * 2}}")
    private int maxPoolSize;

    @Value("${async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.thread-name-prefix:async-}")
    private String threadNamePrefix;

    @Value("${async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * 默认异步任务执行器
     * <p>用于未指定 executor 的 @Async 方法
     */
    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    /**
     * 默认异步异常处理器
     * <p>捕获未处理的异步任务异常并记录日志
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常 - 方法: {}, 参数: {}, 异常: {}",
                    method.getName(),
                    params,
                    throwable.getMessage(),
                    throwable);

            // 可在此处添加告警通知逻辑
            // asyncExceptionHandler.notify(throwable, method, params);
        };
    }

    /**
     * 通用异步任务线程池
     * <p>适用于一般的异步任务场景
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("初始化通用异步线程池 - coreSize: {}, maxSize: {}, queueCapacity: {}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 配置线程装饰器，用于监控
        executor.setTaskDecorator(runnable -> {
            String originalName = Thread.currentThread().getName();
            return () -> {
                String taskName = originalName + "-async-" + System.currentTimeMillis();
                Thread.currentThread().setName(taskName);
                try {
                    runnable.run();
                } finally {
                    Thread.currentThread().setName(originalName);
                }
            };
        });

        executor.initialize();
        return executor;
    }

    /**
     * 归档任务专用线程池
     * <p>处理文件归档、四性检测等IO密集型任务
     */
    @Bean(name = "ingestTaskExecutor")
    public Executor ingestTaskExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // IO密集型任务：核心线程数 = CPU核心数 * 2
        executor.setCorePoolSize(cores * 2);
        executor.setMaxPoolSize(cores * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("IngestAsync-");
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("初始化归档线程池 - coreSize: {}, maxSize: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * 对账服务专用线程池
     * <p>修复Critical bug #1：固定大小线程池，避免动态扩展导致的资源竞争
     */
    @Bean(name = "reconciliationExecutor", destroyMethod = "shutdown")
    public ExecutorService reconciliationExecutor() {
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        log.info("初始化对账线程池,线程数: {}", threadCount);

        return Executors.newFixedThreadPool(
                threadCount,
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("recon-worker-" + thread.getId());
                    thread.setDaemon(false);
                    thread.setUncaughtExceptionHandler((t, e) -> {
                        log.error("对账线程 [{}] 发生未捕获异常: {}", t.getName(), e.getMessage(), e);
                    });
                    return thread;
                });
    }

    /**
     * ERP 同步服务专用线程池
     * <p>处理外部系统同步，控制并发避免压垮下游服务
     */
    @Bean(name = "erpSyncExecutor")
    public Executor erpSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("erp-sync-");
        executor.setKeepAliveSeconds(300);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("初始化ERP同步线程池");

        return executor;
    }

    /**
     * 批量操作专用线程池
     * <p>处理批量审批、批量导出等耗时操作
     */
    @Bean(name = "batchOperationExecutor")
    public Executor batchOperationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("batch-op-");
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        log.info("初始化批量操作线程池");

        return executor;
    }

    /**
     * 四性检测服务专用线程池
     * <p>
     * 用于并行执行真实性、完整性、可用性、安全性检测
     * 线程池大小根据CPU核心数动态配置，支持高并发检测
     * </p>
     */
    @Bean(name = "fourNatureCheckExecutor")
    public Executor fourNatureCheckExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数 = CPU核心数，确保四性检测可以并行执行
        executor.setCorePoolSize(cores);
        // 最大线程数 = CPU核心数 * 2，应对峰值负载
        executor.setMaxPoolSize(cores * 2);
        // 队列容量允许一定数量的任务排队
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("four-nature-check-");
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        // 拒绝策略：调用者线程执行，确保任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        log.info("初始化四性检测线程池 - coreSize: {}, maxSize: {}", cores, cores * 2);

        return executor;
    }
}
