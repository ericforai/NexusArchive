package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ERP 业务场景配置 (Layer 2)
 * 对应 sys_erp_scenario 表
 */
@Data
@TableName("sys_erp_scenario")
public class ErpScenario {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的 ERP 配置 ID (Layer 1)
    private Long configId;

    // 场景标识 (如: VOUCHER_SYNC, INVENTORY_SYNC)
    private String scenarioKey;

    // 场景名称 (如: 凭证同步)
    private String name;

    private String description;

    // 是否启用
    private Boolean isActive;

    // 同步策略: MANUAL, CRON, REALTIME
    private String syncStrategy;

    // Cron 表达式 (仅当 strategy=CRON 时有效)
    private String cronExpression;

    // 最后一次同步时间
    private LocalDateTime lastSyncTime;

    // 最后一次同步状态: SUCCESS, FAIL
    private String lastSyncStatus;

    // 最后一次同步消息/日志
    private String lastSyncMsg;

    private LocalDateTime createdTime;

    private LocalDateTime lastModifiedTime;
}
