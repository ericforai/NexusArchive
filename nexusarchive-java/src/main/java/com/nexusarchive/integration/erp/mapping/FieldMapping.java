// Input: Lombok, Java 标准库
// Output: FieldMapping 类
// Pos: 集成模块 - ERP 映射配置

package com.nexusarchive.integration.erp.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 字段映射配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)  // 支持链式调用
public class FieldMapping {
    /**
     * 源字段名（简单映射时使用）
     */
    private String field;

    /**
     * Groovy 脚本（复杂转换时使用）
     */
    private String script;

    /**
     * 类型转换
     */
    private String type;

    /**
     * 格式化模式
     */
    private String format;

    /**
     * 是否为复杂脚本（多行）
     */
    public boolean isScript() {
        return script != null && !script.isBlank();
    }
}
