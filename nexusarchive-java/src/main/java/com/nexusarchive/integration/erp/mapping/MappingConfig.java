// Input: Lombok, Java 标准库
// Output: MappingConfig 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ERP 映射配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingConfig {
    /**
     * ERP 系统标识
     */
    private String sourceSystem;

    /**
     * 目标模型
     */
    private String targetModel;

    /**
     * 配置版本
     */
    private String version;

    /**
     * 顶层字段映射（header 的字段）
     */
    private Map<String, FieldMapping> headerMappings;

    /**
     * 分录映射
     */
    private ObjectMapping entries;

    /**
     * 附件映射
     */
    private ObjectMapping attachments;
}
