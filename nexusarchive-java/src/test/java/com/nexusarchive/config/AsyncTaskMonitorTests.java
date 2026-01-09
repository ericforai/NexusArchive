// Input: JUnit 5、Spring Boot Test、Lombok、Mockito
// Output: AsyncTaskMonitorTests 类（异步监控测试）
// Pos: 测试层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步任务监控测试
 *
 * <p>验证：
 * <ul>
 *   <li>线程池注册</li>
 *   <li>状态查询</li>
 *   <li>健康检查</li>
 *   <li>拒绝任务记录</li>
 * </ul>
 */
class AsyncTaskMonitorTests {

    private AsyncTaskMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new AsyncTaskMonitor();
    }

    @Test
    @DisplayName("应该能够注册线程池")
    void shouldRegisterExecutor() {
        ThreadPoolTaskExecutor executor = createTestExecutor("test", 2, 4);

        monitor.registerExecutor("testExecutor", executor);

        Map<String, AsyncTaskMonitor.ThreadPoolStatus> status = monitor.getAllThreadPoolStatus();
        assertThat(status).containsKey("testExecutor");
    }

    @Test
    @DisplayName("应该能够获取线程池状态")
    void shouldGetThreadPoolStatus() {
        ThreadPoolTaskExecutor executor = createTestExecutor("test", 4, 8);
        executor.initialize();

        monitor.registerExecutor("testExecutor", executor);

        AsyncTaskMonitor.ThreadPoolStatus status = monitor.getThreadPoolStatus("testExecutor");

        assertThat(status).isNotNull();
        assertThat(status.getExecutorName()).isEqualTo("testExecutor");
        assertThat(status.getCorePoolSize()).isEqualTo(4);
        assertThat(status.getMaximumPoolSize()).isEqualTo(8);
    }

    @Test
    @DisplayName("应该能够检查线程池健康状态")
    void shouldCheckThreadPoolHealth() {
        ThreadPoolTaskExecutor executor = createTestExecutor("test", 4, 8);
        executor.initialize();

        monitor.registerExecutor("healthyExecutor", executor);

        boolean healthy = monitor.isHealthy("healthyExecutor");
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("未注册的线程池应该返回健康")
    void unregisteredExecutorShouldReturnHealthy() {
        boolean healthy = monitor.isHealthy("nonExistent");
        assertThat(healthy).isTrue();
    }

    @Test
    @DisplayName("应该能够记录拒绝的任务")
    void shouldRecordRejectedTasks() {
        ThreadPoolTaskExecutor executor = createTestExecutor("test", 1, 1);
        executor.initialize();

        monitor.registerExecutor("testExecutor", executor);
        monitor.recordRejectedTask("testExecutor");

        AsyncTaskMonitor.ThreadPoolStatus status = monitor.getThreadPoolStatus("testExecutor");
        assertThat(status.getRejectedTaskCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("空线程池应该返回空状态")
    void emptyMonitorShouldReturnEmptyStatus() {
        Map<String, AsyncTaskMonitor.ThreadPoolStatus> status = monitor.getAllThreadPoolStatus();
        assertThat(status).isEmpty();
    }

    private ThreadPoolTaskExecutor createTestExecutor(String namePrefix, int coreSize, int maxSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix(namePrefix + "-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
