// Input: Lombok、Java 标准库
// Output: FieldMappingConfig 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 字段映射配置
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 * 说明: CSV/Excel 列名 -> 系统字段名的映射
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMappingConfig {
    /**
     * 字段映射：CSV/Excel 列名 -> 系统字段名
     * 例如: {"全宗号" -> "fonds_no", "档案标题" -> "title"}
     */
    private Map<String, String> fieldMappings = new HashMap<>();
    
    /**
     * 添加字段映射
     */
    public void addMapping(String sourceField, String targetField) {
        if (fieldMappings == null) {
            fieldMappings = new HashMap<>();
        }
        fieldMappings.put(sourceField, targetField);
    }
    
    /**
     * 获取映射后的字段名（如果没有映射则返回原字段名）
     */
    public String getMappedField(String sourceField) {
        if (fieldMappings == null || !fieldMappings.containsKey(sourceField)) {
            return sourceField;
        }
        return fieldMappings.get(sourceField);
    }
}



