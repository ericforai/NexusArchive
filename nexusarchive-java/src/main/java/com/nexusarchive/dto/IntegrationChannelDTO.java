package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 集成通道聚合展示 DTO
 * 用于"资料收集/在线接收"页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationChannelDTO {

    /**
     * 场景 ID (来自 sys_erp_scenario)
     */
    private Long id;

    /**
     * 场景 Key (如 VOUCHER_SYNC)
     */
    private String name;

    /**
     * 场景名称 (如 凭证同步)
     */
    private String displayName;

    /**
     * 系统名称 (如 用友YonSuite)
     */
    private String configName;

    /**
     * 适配器类型 (如 yonsuite)
     */
    private String erpType;

    /**
     * 同步频率友好文本 (如 每日 1:00, 实时, 手动)
     */
    private String frequency;

    /**
     * 最后同步时间
     */
    private String lastSync;

    /**
     * 本次接收数量
     */
    private Integer receivedCount;

    /**
     * 状态: normal, error, syncing
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 同步 API 端点 (如 /integration/yonsuite/vouchers/sync)
     */
    private String apiEndpoint;

    /**
     * 账簿编码 (用于同步参数)
     */
    private String accbookCode;

    /**
     * 最后一次同步的详细日志消息
     */
    private String lastSyncMsg;
}
