// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SyncTask 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异步同步任务实体
 * <p>
 * 用于持久化 ERP 同步任务的执行状态，解决内存存储重启丢失的问题。
 * 任务状态流转：SUBMITTED -> RUNNING -> SUCCESS/FAIL
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sync_task")
public class SyncTask {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 任务唯一标识
     */
    private String taskId;

    /**
     * 关联的场景 ID
     */
    private Long scenarioId;

    /**
     * 任务状态: SUBMITTED, RUNNING, SUCCESS, FAIL
     */
    private String status;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 进度 (0.0 - 1.0)
     */
    private Double progress;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 操作人 ID
     */
    private String operatorId;

    /**
     * 操作客户端 IP
     */
    private String clientIp;

    /**
     * 同步参数 (JSON)
     */
    private String syncParams;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
