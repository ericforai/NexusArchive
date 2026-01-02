// Input: Lombok, Java Time
// Output: ErpMetadata DTO
// Pos: integration.erp.dto 包

package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * ERP 适配器元数据
 * <p>
 * 记录适配器的运行时可查询信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMetadata {

    /**
     * 适配器唯一标识
     */
    private String identifier;

    /**
     * 适配器显示名称
     */
    private String name;

    /**
     * 适配器描述
     */
    private String description;

    /**
     * 适配器版本
     */
    private String version;

    /**
     * ERP 系统类型
     */
    private String erpType;

    /**
     * 支持的业务场景
     */
    private Set<String> supportedScenarios;

    /**
     * 是否支持 Webhook
     */
    private boolean supportsWebhook;

    /**
     * 适配器优先级
     */
    private int priority;

    /**
     * 适配器实现类全限定名
     */
    private String implementationClass;

    /**
     * 注册时间
     */
    private LocalDateTime registeredAt;

    /**
     * 是否启用
     */
    private boolean enabled;
}
