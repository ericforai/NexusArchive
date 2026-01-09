// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: StatsCacheEventListener 类
// Pos: 事件监听器 - 监听数据变更事件并清除统计缓存
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.listener;

import com.nexusarchive.event.CheckPassedEvent;
import com.nexusarchive.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 统计缓存事件监听器
 *
 * 监听数据变更事件，自动清除对应的统计缓存：
 * - 归档数据变更 -> 清除仪表盘和趋势缓存
 * - 任务状态变更 -> 清除任务统计缓存
 * - 文件内容变更 -> 清除存储统计缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsCacheEventListener {

    private final StatsService statsService;

    /**
     * 监听归档完成事件
     * 清除仪表盘统计和趋势缓存
     */
    @Async
    @EventListener
    public void onArchiveCompleted(SignatureTimestampListener.ArchiveCompletedEvent event) {
        log.debug("Archive completed, evicting dashboard and trend cache");
        statsService.evictDashboardCache();
        statsService.evictTrendCache();
    }

    /**
     * 监听任务状态变更事件
     * 清除任务统计缓存和仪表盘缓存
     */
    @Async
    @EventListener
    public void onTaskStatusChanged(CheckPassedEvent event) {
        log.debug("Task status changed, evicting task stats cache");
        statsService.evictDashboardCache();
    }
}
