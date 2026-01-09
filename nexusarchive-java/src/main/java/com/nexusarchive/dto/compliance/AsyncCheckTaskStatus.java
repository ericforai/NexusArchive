// Input: Lombok、Java 标准库
// Output: AsyncCheckTaskStatus 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.compliance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异步四性检测任务状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncCheckTaskStatus {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 档案ID
     */
    private String archiveId;

    /**
     * 档案编码
     */
    private String archiveCode;

    /**
     * 任务状态
     */
    private TaskStatus status;

    /**
     * 任务创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 任务开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 任务完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 当前进度百分比 (0-100)
     */
    private int progress;

    /**
     * 当前执行阶段
     */
    private String currentPhase;

    /**
     * 错误信息 (失败时)
     */
    private String errorMessage;

    /**
     * 各阶段检测状态
     */
    private Map<String, PhaseStatus> phaseStatuses;

    /**
     * 检测结果 (完成后可用)
     */
    private Object result;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        /**
         * 排队中
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
         * 失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED
    }

    /**
     * 阶段状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhaseStatus {
        /**
         * 阶段名称
         */
        private String phaseName;

        /**
         * 状态
         */
        private TaskStatus status;

        /**
         * 开始时间
         */
        private LocalDateTime startTime;

        /**
         * 完成时间
         */
        private LocalDateTime endTime;
    }
}
