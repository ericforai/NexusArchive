// Input: Spring Framework、Lombok、Java 标准库、本地模块
// Output: AsyncCheckTaskManager 类
// Pos: 服务层 - 异步检测任务管理器

package com.nexusarchive.service.compliance;

import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步四性检测任务管理器
 * <p>
 * 负责管理异步检测任务的生命周期、状态跟踪和结果存储
 * </p>
 */
@Slf4j
@Component
public class AsyncCheckTaskManager {

    /**
     * 任务存储 (taskId -> TaskStatus)
     */
    private final Map<String, AsyncCheckTaskStatus> tasks = new ConcurrentHashMap<>();

    /**
     * 档案ID到任务ID的映射 (archiveId -> taskId)
     * 支持同一档案同时只有一个检测任务
     */
    private final Map<String, String> archiveTaskMap = new ConcurrentHashMap<>();

    /**
     * 任务结果缓存 (taskId -> FourNatureReport)
     * 完成后的结果保留时间由 @Scheduled 清理任务控制
     */
    private final Map<String, FourNatureReport> results = new ConcurrentHashMap<>();

    /**
     * 任务过期时间 (分钟)
     */
    private static final long TASK_EXPIRY_MINUTES = 30;

    /**
     * 创建新任务
     *
     * @param taskId   任务ID
     * @param archiveId 档案ID
     * @param archiveCode 档案编码
     * @return 任务状态
     */
    public AsyncCheckTaskStatus createTask(String taskId, String archiveId, String archiveCode) {
        // 取消该档案的旧任务（如果存在）
        String oldTaskId = archiveTaskMap.get(archiveId);
        if (oldTaskId != null) {
            AsyncCheckTaskStatus oldTask = tasks.get(oldTaskId);
            if (oldTask != null && (oldTask.getStatus() == AsyncCheckTaskStatus.TaskStatus.PENDING
                    || oldTask.getStatus() == AsyncCheckTaskStatus.TaskStatus.RUNNING)) {
                oldTask.setStatus(AsyncCheckTaskStatus.TaskStatus.CANCELLED);
                log.info("取消旧任务: taskId={}, archiveId={}", oldTaskId, archiveId);
            }
        }

        AsyncCheckTaskStatus taskStatus = AsyncCheckTaskStatus.builder()
                .taskId(taskId)
                .archiveId(archiveId)
                .archiveCode(archiveCode)
                .status(AsyncCheckTaskStatus.TaskStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .progress(0)
                .build();

        tasks.put(taskId, taskStatus);
        archiveTaskMap.put(archiveId, taskId);

        log.debug("创建异步检测任务: taskId={}, archiveId={}, archiveCode={}", taskId, archiveId, archiveCode);
        return taskStatus;
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态，不存在则返回 null
     */
    public AsyncCheckTaskStatus getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * 根据档案ID获取当前任务
     *
     * @param archiveId 档案ID
     * @return 任务状态，不存在则返回 null
     */
    public AsyncCheckTaskStatus getTaskByArchiveId(String archiveId) {
        String taskId = archiveTaskMap.get(archiveId);
        return taskId != null ? tasks.get(taskId) : null;
    }

    /**
     * 更新任务状态为运行中
     *
     * @param taskId 任务ID
     */
    public void markTaskRunning(String taskId) {
        AsyncCheckTaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(AsyncCheckTaskStatus.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            task.setProgress(10);
            task.setCurrentPhase("开始执行四性检测");
        }
    }

    /**
     * 更新任务阶段
     *
     * @param taskId  任务ID
     * @param phase   阶段名称
     * @param progress 进度百分比
     */
    public void updateTaskPhase(String taskId, String phase, int progress) {
        AsyncCheckTaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setCurrentPhase(phase);
            task.setProgress(progress);
        }
    }

    /**
     * 标记任务完成
     *
     * @param taskId 任务ID
     * @param result 检测结果
     */
    public void markTaskCompleted(String taskId, FourNatureReport result) {
        AsyncCheckTaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(AsyncCheckTaskStatus.TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            task.setProgress(100);
            task.setCurrentPhase("检测完成");
            task.setResult(result);
            results.put(taskId, result);
            log.info("任务完成: taskId={}, archiveId={}, status={}",
                    taskId, task.getArchiveId(), result.getStatus());
        }
    }

    /**
     * 标记任务失败
     *
     * @param taskId      任务ID
     * @param errorMessage 错误信息
     */
    public void markTaskFailed(String taskId, String errorMessage) {
        AsyncCheckTaskStatus task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(AsyncCheckTaskStatus.TaskStatus.FAILED);
            task.setCompletedAt(LocalDateTime.now());
            task.setErrorMessage(errorMessage);
            log.error("任务失败: taskId={}, archiveId={}, error={}",
                    taskId, task.getArchiveId(), errorMessage);
        }
    }

    /**
     * 获取检测结果
     *
     * @param taskId 任务ID
     * @return 检测结果，不存在则返回 null
     */
    public FourNatureReport getResult(String taskId) {
        return results.get(taskId);
    }

    /**
     * 清理过期任务 (定时执行)
     * 每小时执行一次，清理超过30分钟的任务
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredTasks() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(TASK_EXPIRY_MINUTES);

        int removedTasks = 0;
        int removedResults = 0;

        for (Map.Entry<String, AsyncCheckTaskStatus> entry : tasks.entrySet()) {
            AsyncCheckTaskStatus task = entry.getValue();
            LocalDateTime completedAt = task.getCompletedAt();

            // 清理已完成或失败且超过过期时间的任务
            if ((task.getStatus() == AsyncCheckTaskStatus.TaskStatus.COMPLETED
                    || task.getStatus() == AsyncCheckTaskStatus.TaskStatus.FAILED
                    || task.getStatus() == AsyncCheckTaskStatus.TaskStatus.CANCELLED)
                    && completedAt != null
                    && completedAt.isBefore(expiryThreshold)) {

                tasks.remove(entry.getKey());
                results.remove(entry.getKey());
                archiveTaskMap.remove(task.getArchiveId());
                removedTasks++;
                removedResults++;
            }
        }

        if (removedTasks > 0) {
            log.info("清理过期任务: {} 个任务, {} 个结果", removedTasks, removedResults);
        }
    }

    /**
     * 获取当前任务数量
     *
     * @return 任务数量
     */
    public int getTaskCount() {
        return tasks.size();
    }

    /**
     * 获取运行中的任务数量
     *
     * @return 运行中的任务数量
     */
    public int getRunningTaskCount() {
        return (int) tasks.values().stream()
                .filter(t -> t.getStatus() == AsyncCheckTaskStatus.TaskStatus.RUNNING)
                .count();
    }
}
