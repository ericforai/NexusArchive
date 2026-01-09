// Input: Lombok、Java 标准库、IngestRequestStatus Entity
// Output: IngestRequestStatusResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * SIP 接收请求状态响应 DTO
 * <p>
 * 用于 Controller 返回 SIP 接收请求状态信息，避免直接暴露 IngestRequestStatus Entity
 * </p>
 */
@Data
public class IngestRequestStatusResponse {

    /**
     * 请求ID
     */
    private String id;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 请求状态: PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 处理进度 (0-100)
     */
    private Integer progress;

    /**
     * 接收的档案数量
     */
    private Integer receivedCount;

    /**
     * 成功处理的档案数量
     */
    private Integer successCount;

    /**
     * 失败的档案数量
     */
    private Integer failureCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 处理完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
