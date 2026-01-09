# 异步任务配置指南

## 概述

本项目使用 Spring `@Async` 注解支持异步任务执行。通过 `AsyncConfig` 配置类，提供了多个专用线程池，针对不同业务场景优化资源使用。

## 可用线程池

| 线程池名称 | 用途 | 核心线程数 | 最大线程数 | 队列容量 |
|-----------|------|-----------|-----------|---------|
| `taskExecutor` | 默认通用异步任务 | CPU核心数 | CPU核心数×2 | 100 |
| `ingestTaskExecutor` | 文件归档、四性检测等IO密集型 | CPU核心数×2 | CPU核心数×4 | 500 |
| `reconciliationExecutor` | 对账服务 | min(8, CPU核心数) | 固定 | - |
| `erpSyncExecutor` | ERP同步 | 2 | 5 | 500 |
| `batchOperationExecutor` | 批量操作（审批、导出） | 4 | 8 | 200 |

## 使用方式

### 1. 基础用法（使用默认线程池）

```java
@Service
public class MyService {

    @Async
    public void asyncMethod() {
        // 使用默认的 taskExecutor
    }
}
```

### 2. 指定线程池

```java
@Service
public class ArchiveService {

    @Async("ingestTaskExecutor")
    public void ingestArchive(Archive archive) {
        // 使用归档专用线程池
    }
}
```

### 3. 返回值的异步方法

```java
@Service
public class MyService {

    @Async("taskExecutor")
    public CompletableFuture<String> asyncMethodWithResult() {
        // 执行异步操作
        return CompletableFuture.completedFuture("result");
    }
}
```

### 4. 批量操作示例

```java
@Service
public class BatchService {

    @Async("batchOperationExecutor")
    public CompletableFuture<BatchResult> processBatch(List<Long> ids) {
        BatchResult result = new BatchResult();
        for (Long id : ids) {
            try {
                processOne(id);
                result.addSuccess(id);
            } catch (Exception e) {
                result.addFailure(id, e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(result);
    }
}
```

## 配置参数

在 `application.yml` 中可配置以下参数：

```yaml
# 异步任务线程池配置
async:
  core-pool-size: 8              # 核心线程数（默认：CPU核心数）
  max-pool-size: 16              # 最大线程数（默认：CPU核心数×2）
  queue-capacity: 100            # 队列容量
  thread-name-prefix: async-     # 线程名前缀
  keep-alive-seconds: 60         # 空闲线程存活时间（秒）
  monitor:
    enabled: true                # 监控开关
    log-interval: 300000         # 监控日志间隔（毫秒）
    alert-threshold: 80          # 使用率告警阈值（百分比）
```

## 异常处理

异步方法的异常由 `AsyncUncaughtExceptionHandler` 统一处理，会记录详细日志：

```java
// 日志示例
ERROR c.n.config.AsyncConfig - 异步任务执行异常 - 方法: processArchive, 参数: [Archive(id=123)], 异常: NullPointerException
```

## 监控组件

### AsyncTaskMonitor

提供线程池状态监控：

```java
@Autowired
private AsyncTaskMonitor monitor;

// 获取单个线程池状态
ThreadPoolStatus status = monitor.getThreadPoolStatus("taskExecutor");

// 获取所有线程池状态
Map<String, ThreadPoolStatus> allStatus = monitor.getAllThreadPoolStatus();

// 检查健康状态
boolean healthy = monitor.isHealthy("ingestTaskExecutor");
```

### ThreadPoolStatus 字段

| 字段 | 说明 |
|-----|------|
| `executorName` | 线程池名称 |
| `corePoolSize` | 核心线程数 |
| `maximumPoolSize` | 最大线程数 |
| `activeCount` | 活跃线程数 |
| `poolSize` | 当前线程池大小 |
| `queueSize` | 队列中任务数 |
| `queueRemainingCapacity` | 队列剩余容量 |
| `completedTaskCount` | 已完成任务数 |
| `taskCount` | 总任务数 |
| `rejectedTaskCount` | 被拒绝的任务数 |
| `usagePercentage` | 使用率百分比 |

## 拒绝策略

| 线程池 | 拒绝策略 | 说明 |
|-------|---------|------|
| `taskExecutor` | `CallerRunsPolicy` | 由调用线程执行 |
| `ingestTaskExecutor` | `CallerRunsPolicy` | 由调用线程执行 |
| `erpSyncExecutor` | `CallerRunsPolicy` | 由调用线程执行 |
| `batchOperationExecutor` | `AbortPolicy` | 抛出异常（避免过多积压） |

## 最佳实践

1. **选择合适的线程池**：根据任务类型选择专用线程池，避免使用默认线程池处理所有任务

2. **避免循环依赖**：异步方法不能在同一类内部调用（Spring AOP 代理限制）

3. **处理超时**：对于可能长时间运行的任务，考虑使用超时控制

4. **资源清理**：使用完毕后及时释放资源

5. **监控告警**：定期检查线程池使用率，避免队列积压

## 常见问题

### Q: 异步方法不生效？

A: 检查以下几点：
- `@EnableAsync` 已启用（`AsyncConfig` 中已配置）
- 方法是 public 的
- 没有在同一个类内部调用
- 方法没有被 final/static 修饰

### Q: 如何追踪异步任务执行状态？

A: 使用 `AsyncTaskInfo` DTO 记录任务状态：

```java
@Service
public class MyService {

    private final Map<String, AsyncTaskInfo> taskRegistry = new ConcurrentHashMap<>();

    @Async("taskExecutor")
    public void asyncMethod(String taskId) {
        AsyncTaskInfo info = AsyncTaskInfo.builder()
                .taskId(taskId)
                .status(AsyncTaskInfo.TaskStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        taskRegistry.put(taskId, info);

        try {
            // 执行任务
            info.setStatus(AsyncTaskInfo.TaskStatus.COMPLETED);
            info.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            info.setStatus(AsyncTaskInfo.TaskStatus.FAILED);
            info.setErrorMessage(e.getMessage());
        }
    }
}
```

### Q: 线程池满了怎么办？

A: 根据拒绝策略：
- `CallerRunsPolicy`：任务会在调用线程中执行，降低提交速度
- `AbortPolicy`：抛出异常，需要在调用方处理

建议：
1. 增加队列容量
2. 增加最大线程数
3. 优化任务执行时间
