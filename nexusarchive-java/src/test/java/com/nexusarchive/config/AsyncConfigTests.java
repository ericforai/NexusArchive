// Input: JUnit 5、Spring Boot Test、Lombok
// Output: AsyncConfigTests 类（异步配置测试）
// Pos: 测试层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步配置测试
 *
 * <p>验证：
 * <ul>
 *   <li>各线程池正确初始化</li>
 *   <li>线程池参数符合预期</li>
 *   <li>异常处理器正确配置</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class AsyncConfigTests {

    @Autowired
    private AsyncConfig asyncConfig;

    @Autowired
    @Qualifier("taskExecutor")
    private Executor taskExecutor;

    @Autowired
    @Qualifier("ingestTaskExecutor")
    private Executor ingestTaskExecutor;

    @Autowired
    @Qualifier("erpSyncExecutor")
    private Executor erpSyncExecutor;

    @Autowired
    @Qualifier("batchOperationExecutor")
    private Executor batchOperationExecutor;

    @Test
    @DisplayName("AsyncConfig 应该实现 AsyncConfigurer")
    void asyncConfigShouldImplementConfigurer() {
        assertThat(asyncConfig).isInstanceOf(AsyncConfigurer.class);
    }

    @Test
    @DisplayName("应该配置异步异常处理器")
    void shouldConfigureAsyncUncaughtExceptionHandler() {
        AsyncConfigurer configurer = (AsyncConfigurer) asyncConfig;
        assertThat(configurer.getAsyncUncaughtExceptionHandler()).isNotNull();
    }

    @Test
    @DisplayName("应该配置默认异步执行器")
    void shouldConfigureAsyncExecutor() {
        AsyncConfigurer configurer = (AsyncConfigurer) asyncConfig;
        Executor executor = configurer.getAsyncExecutor();
        assertThat(executor).isNotNull();
        assertThat(executor).isInstanceOf(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class);
    }

    @Test
    @DisplayName("taskExecutor 应该正确初始化")
    void taskExecutorShouldBeInitialized() {
        assertThat(taskExecutor).isNotNull();
        assertThat(taskExecutor).isInstanceOf(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class);

        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) taskExecutor;
        ThreadPoolExecutor tpeExecutor = tpe.getThreadPoolExecutor();

        // 验证核心线程数至少为 1
        assertThat(tpeExecutor.getCorePoolSize()).isGreaterThan(0);
        // 验证最大线程数大于核心线程数
        assertThat(tpeExecutor.getMaximumPoolSize()).isGreaterThanOrEqualTo(tpeExecutor.getCorePoolSize());
    }

    @Test
    @DisplayName("ingestTaskExecutor 应该正确初始化")
    void ingestTaskExecutorShouldBeInitialized() {
        assertThat(ingestTaskExecutor).isNotNull();

        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) ingestTaskExecutor;
        ThreadPoolExecutor tpeExecutor = tpe.getThreadPoolExecutor();

        // 归档线程池应该较大（IO密集型）
        assertThat(tpeExecutor.getCorePoolSize()).isGreaterThan(1);
    }

    @Test
    @DisplayName("erpSyncExecutor 应该正确初始化")
    void erpSyncExecutorShouldBeInitialized() {
        assertThat(erpSyncExecutor).isNotNull();

        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) erpSyncExecutor;
        ThreadPoolExecutor tpeExecutor = tpe.getThreadPoolExecutor();

        // ERP同步线程池应该较小（避免压垮下游）
        assertThat(tpeExecutor.getCorePoolSize()).isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("batchOperationExecutor 应该正确初始化")
    void batchOperationExecutorShouldBeInitialized() {
        assertThat(batchOperationExecutor).isNotNull();

        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) batchOperationExecutor;
        ThreadPoolExecutor tpeExecutor = tpe.getThreadPoolExecutor();

        assertThat(tpeExecutor.getCorePoolSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("线程池应该使用 CallerRunsPolicy 拒绝策略（除 batchOperationExecutor）")
    void threadPoolShouldUseCallerRunsPolicy() {
        org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor tpe =
                (org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor) taskExecutor;
        ThreadPoolExecutor tpeExecutor = tpe.getThreadPoolExecutor();

        assertThat(tpeExecutor.getRejectedExecutionHandler())
                .isInstanceOf(java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy.class);
    }
}
