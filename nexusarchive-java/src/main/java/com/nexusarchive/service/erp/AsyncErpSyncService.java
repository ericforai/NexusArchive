// Input: Spring Framework、MyBatis-Plus、Lombok、Java 标准库
// Output: AsyncErpSyncService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.SyncTaskDTO;
import com.nexusarchive.dto.SyncTaskStatus;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.SyncTask;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.SyncTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERP 异步同步服务
 * <p>
 * 将耗时的 ERP 数据同步操作异步化，避免阻塞 API 响应。
 * 使用数据库持久化任务状态，解决服务器重启状态丢失问题。
 * </p>
 *
 * <h3>任务状态流转</h3>
 * <pre>
 * SUBMITTED -> RUNNING -> SUCCESS / FAIL
 * </pre>
 *
 * <h3>使用示例</h3>
 * <pre>
 * // 1. 提交同步任务
 * SyncTaskDTO task = asyncErpSyncService.submitSyncTask(scenarioId, operatorId, clientIp);
 *
 * // 2. 异步执行同步
 * asyncErpSyncService.syncScenarioAsync(task.getTaskId(), scenarioId, operatorId, clientIp);
 *
 * // 3. 查询任务状态
 * SyncTaskStatus status = asyncErpSyncService.getTaskStatus(task.getTaskId());
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncErpSyncService {

    private final ErpSyncService erpSyncService;
    private final ErpScenarioMapper erpScenarioMapper;
    private final SyncTaskMapper syncTaskMapper;

    /**
     * 内存缓存，用于快速查询最近的任务状态
     * <p>
     * 注意：此缓存仅作为性能优化，真实状态以数据库为准。
     * 服务器重启后缓存清空，但数据库状态依然可用。
     * </p>
     */
    private final ConcurrentHashMap<String, SyncTaskStatus> taskCache = new ConcurrentHashMap<>();

    private static final int CACHE_MAX_SIZE = 1000;

    /**
     * 异步执行场景同步
     *
     * @param taskId     任务 ID
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp   客户端 IP
     */
    @Async("erpSyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncScenarioAsync(String taskId, Long scenarioId, String operatorId, String clientIp) {
        log.info("开始异步同步任务: taskId={}, scenarioId={}, operator={}", taskId, scenarioId, operatorId);

        try {
            // 更新状态为 RUNNING
            updateTaskStatus(taskId, "RUNNING", null, 0.0);

            // 执行同步
            erpSyncService.syncScenario(scenarioId, operatorId, clientIp);

            // 获取场景最新状态
            ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
            String finalStatus = "SUCCESS".equals(scenario.getLastSyncStatus()) ? "SUCCESS" : "FAIL";

            // 更新最终状态
            double finalProgress = "SUCCESS".equals(finalStatus) ? 1.0 : 0.0;
            updateTaskStatus(taskId, finalStatus,
                "FAIL".equals(finalStatus) ? scenario.getLastSyncMsg() : null,
                finalProgress);

            // 如果成功，同步统计数据
            if ("SUCCESS".equals(finalStatus)) {
                SyncTask task = syncTaskMapper.selectOne(
                    new LambdaQueryWrapper<SyncTask>().eq(SyncTask::getTaskId, taskId));
                if (task != null) {
                    // 这里可以从 ErpScenario 或其他来源获取统计数据
                    // 暂时保持默认值
                }
            }

        } catch (Exception e) {
            log.error("异步同步任务失败: taskId={}", taskId, e);
            updateTaskStatus(taskId, "FAIL", e.getMessage(), 0.0);
        }
    }

    /**
     * 提交同步任务
     *
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp   客户端 IP
     * @return 任务 DTO
     */
    @Transactional
    public SyncTaskDTO submitSyncTask(Long scenarioId, String operatorId, String clientIp) {
        String taskId = "sync-" + scenarioId + "-" + System.currentTimeMillis();

        // 持久化到数据库
        SyncTask task = SyncTask.builder()
            .taskId(taskId)
            .scenarioId(scenarioId)
            .status("SUBMITTED")
            .progress(0.0)
            .operatorId(operatorId)
            .clientIp(clientIp)
            .startTime(LocalDateTime.now())
            .createdTime(LocalDateTime.now())
            .updatedTime(LocalDateTime.now())
            .build();

        syncTaskMapper.insert(task);

        // 更新缓存
        SyncTaskStatus status = toStatus(task);
        updateCache(status);

        log.info("提交同步任务: taskId={}, scenarioId={}, operator={}", taskId, scenarioId, operatorId);

        return SyncTaskDTO.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .message("同步任务已提交")
            .build();
    }

    /**
     * 获取任务状态
     * <p>
     * 优先从缓存读取，缓存未命中则从数据库读取。
     * </p>
     *
     * @param taskId 任务 ID
     * @return 任务状态，不存在返回 null
     */
    public SyncTaskStatus getTaskStatus(String taskId) {
        // 先从缓存读取
        SyncTaskStatus cached = taskCache.get(taskId);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，从数据库读取
        SyncTask task = syncTaskMapper.selectOne(
            new LambdaQueryWrapper<SyncTask>().eq(SyncTask::getTaskId, taskId));

        if (task == null) {
            return null;
        }

        SyncTaskStatus status = toStatus(task);
        updateCache(status);
        return status;
    }

    /**
     * 获取场景的所有任务
     *
     * @param scenarioId 场景 ID
     * @return 任务列表，按创建时间倒序
     */
    public List<SyncTask> getTasksByScenario(Long scenarioId) {
        return syncTaskMapper.selectList(
            new LambdaQueryWrapper<SyncTask>()
                .eq(SyncTask::getScenarioId, scenarioId)
                .orderByDesc(SyncTask::getCreatedTime)
                .last("LIMIT 50"));
    }

    /**
     * 获取正在运行的任务列表
     *
     * @return 运行中的任务列表
     */
    public List<SyncTask> getRunningTasks() {
        return syncTaskMapper.selectList(
            new LambdaQueryWrapper<SyncTask>()
                .in(SyncTask::getStatus, List.of("SUBMITTED", "RUNNING"))
                .orderByAsc(SyncTask::getCreatedTime));
    }

    /**
     * 清理过期的完成任务（保留最近 N 天）
     *
     * @param daysToKeep 保留天数
     * @return 清理的记录数
     */
    @Transactional
    public int cleanupOldTasks(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = syncTaskMapper.delete(
            new LambdaQueryWrapper<SyncTask>()
                .in(SyncTask::getStatus, List.of("SUCCESS", "FAIL"))
                .lt(SyncTask::getEndTime, cutoff));

        // 同时清理缓存
        taskCache.entrySet().removeIf(entry -> {
            SyncTaskStatus status = entry.getValue();
            if (status.getEndTime() != null && status.getEndTime().isBefore(cutoff)) {
                return true;
            }
            return false;
        });

        log.info("清理过期同步任务: 删除 {} 条记录 (保留 {} 天)", deleted, daysToKeep);
        return deleted;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String errorMessage, double progress) {
        // 更新数据库
        SyncTask update = SyncTask.builder()
            .status(status)
            .errorMessage(errorMessage)
            .progress(progress)
            .updatedTime(LocalDateTime.now())
            .build();

        if ("SUCCESS".equals(status) || "FAIL".equals(status)) {
            update.setEndTime(LocalDateTime.now());
        }

        syncTaskMapper.update(update,
            new LambdaQueryWrapper<SyncTask>().eq(SyncTask::getTaskId, taskId));

        // 更新缓存
        SyncTask current = syncTaskMapper.selectOne(
            new LambdaQueryWrapper<SyncTask>().eq(SyncTask::getTaskId, taskId));
        if (current != null) {
            SyncTaskStatus taskStatus = toStatus(current);
            updateCache(taskStatus);
        }
    }

    /**
     * 更新缓存（LRU 策略）
     */
    private void updateCache(SyncTaskStatus status) {
        // 简单的缓存大小控制
        if (taskCache.size() >= CACHE_MAX_SIZE) {
            // 移除最早的一个非运行任务
            taskCache.entrySet().removeIf(entry ->
                !"RUNNING".equals(entry.getValue().getStatus()) &&
                    !"SUBMITTED".equals(entry.getValue().getStatus()));
        }
        taskCache.put(status.getTaskId(), status);
    }

    /**
     * 转换为 DTO
     */
    private SyncTaskStatus toStatus(SyncTask task) {
        return SyncTaskStatus.builder()
            .taskId(task.getTaskId())
            .status(task.getStatus())
            .totalCount(task.getTotalCount())
            .successCount(task.getSuccessCount())
            .failCount(task.getFailCount())
            .errorMessage(task.getErrorMessage())
            .startTime(task.getStartTime())
            .endTime(task.getEndTime())
            .progress(task.getProgress())
            .build();
    }
}
