// Input: Spring Framework、Lombok、Java 标准库
// Output: SyncTaskCleanupService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 同步任务清理服务
 * <p>
 * 定期清理已完成的过期同步任务记录，防止数据库膨胀。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SyncTaskCleanupService {

    private final AsyncErpSyncService asyncErpSyncService;

    /**
     * 每天凌晨 2 点清理过期任务
     * <p>
     * 保留最近 30 天的完成任务记录
     * </p>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTasks() {
        log.info("开始清理过期同步任务");
        try {
            int deleted = asyncErpSyncService.cleanupOldTasks(30);
            log.info("清理过期同步任务完成: 删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("清理过期同步任务失败", e);
        }
    }

    /**
     * 每小时检查一次运行中的任务
     * <p>
     * 检测是否有长时间运行的任务（超过 2 小时），
     * 可以添加告警或强制终止逻辑
     * </p>
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkRunningTasks() {
        log.debug("检查运行中的同步任务");
        try {
            var runningTasks = asyncErpSyncService.getRunningTasks();
            if (!runningTasks.isEmpty()) {
                log.info("当前运行中的同步任务数量: {}", runningTasks.size());
                // 可以添加告警逻辑，例如检测超过 2 小时仍在运行的任务
                long now = System.currentTimeMillis();
                for (var task : runningTasks) {
                    if (task.getStartTime() != null) {
                        long runningMinutes = (now - task.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()) / 60000;
                        if (runningMinutes > 120) {
                            log.warn("同步任务运行时间过长: taskId={}, 运行时间={} 分钟",
                                task.getTaskId(), runningMinutes);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("检查运行中的同步任务失败", e);
        }
    }
}
