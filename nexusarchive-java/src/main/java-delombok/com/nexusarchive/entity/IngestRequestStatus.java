// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: IngestRequestStatus 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * SIP 接收请求状态追踪实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_ingest_request_status")
public class IngestRequestStatus {

    @TableId(type = IdType.INPUT)
    private String requestId;

    /**
     * 状态: RECEIVED, CHECKING, CHECK_PASSED, PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 详细消息或错误信息
     */
    private String message;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    // Manual Getters
    public String getRequestId() { return requestId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }
}
