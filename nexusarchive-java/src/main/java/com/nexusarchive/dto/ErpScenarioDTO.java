package com.nexusarchive.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ErpScenarioDTO {
    private Long id;
    private Long configId;
    private String scenarioKey; // 场景唯一标识
    private String name; // 场景显示名称
    private String description; // 描述
    private Boolean isActive; // 是否启用
    private String syncStrategy; // 同步策略: MANUAL, CRON, REALTIME
    private String cronExpression;// Cron表达式
    private LocalDateTime lastSyncTime;
    private String lastSyncStatus;
    private Integer lastSyncCount; // 本次同步条数 (需在Entity中增加此字段或从Log表关联)
}
