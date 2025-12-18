package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步历史记录实体
 * 对应 sys_sync_history 表
 */
@Data
@TableName("sys_sync_history")
public class SyncHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的场景ID
    private Long scenarioId;

    // 同步开始时间
    private LocalDateTime syncStartTime;

    // 同步结束时间
    private LocalDateTime syncEndTime;

    // 同步状态: RUNNING, SUCCESS, FAIL
    private String status;

    // 总记录数
    private Integer totalCount;

    // 成功数
    private Integer successCount;

    // 失败数
    private Integer failCount;

    // 错误信息
    private String errorMessage;

    // 同步参数 (JSON)
    private String syncParams;

    // 操作人ID
    private Long operatorId;

    // 操作客户端IP
    private String clientIp;

    // 四性检测检测摘要 (JSON): { authenticity: 0.98, integrity: 1.0, usability: 1.0, security: 1.0 }
    private String fourNatureSummary;

    private LocalDateTime createdTime;
}
