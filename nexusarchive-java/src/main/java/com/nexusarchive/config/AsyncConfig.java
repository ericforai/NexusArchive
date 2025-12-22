// Input: Spring Framework、Java 标准库、Lombok
// Output: AsyncConfig 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ingestTaskExecutor")
    public Executor ingestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cores = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(cores * 2);
        executor.setMaxPoolSize(cores * 4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("IngestAsync-");
        executor.initialize();
        return executor;
    }

    /**
     * 对账服务专用线程池 (修复Critical bug #1)
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
                    return thread;
                });
    }
}
