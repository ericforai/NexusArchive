// Input: Java 标准库、Lombok、Spring Context
// Output: AsyncTaskInfo 类（异步任务信息DTO）
// Pos: 配置层 - 监控DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异步任务信息DTO
 *
 * <p>用于异步任务执行状态追踪和展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTaskInfo {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务类型（归档、同步、对账等）
     */
    private String taskType;

    /**
     * 执行器名称
     */
    private String executorName;

    /**
     * 任务状态（PENDING、RUNNING、COMPLETED、FAILED）
     */
    private TaskStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 任务结果摘要
     */
    private String resultSummary;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 等待执行
         */
        PENDING,
        /**
         * 执行中
         */
        RUNNING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 执行失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED
    }
}
