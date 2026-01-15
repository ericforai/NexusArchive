// Input: Lombok, Java 标准库
// Output: ObjectMapping 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 对象映射配置（用于 entries、attachments 等数组）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectMapping {
    /**
     * 源数组字段名
     */
    private String source;

    /**
     * 数组元素的字段映射
     */
    private Map<String, FieldMapping> item;
}
