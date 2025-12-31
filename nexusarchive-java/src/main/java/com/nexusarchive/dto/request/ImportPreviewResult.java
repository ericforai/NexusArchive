// Input: Lombok、Java 标准库
// Output: ImportPreviewResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 导入预览结果
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportPreviewResult {
    /**
     * 总行数
     */
    private int totalRows;
    
    /**
     * 有效行数
     */
    private int validRows;
    
    /**
     * 无效行数
     */
    private int invalidRows;
    
    /**
     * 预览数据（前100行）
     */
    private List<ImportRowPreview> previewData;
    
    /**
     * 错误列表
     */
    private List<ImportError> errors;
    
    /**
     * 统计信息
     */
    private PreviewStatistics statistics;
    
    /**
     * 预览统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviewStatistics {
        /**
         * 全宗数量
         */
        private int fondsCount;
        
        /**
         * 实体数量
         */
        private int entityCount;
        
        /**
         * 将创建的全宗号列表
         */
        private List<String> willCreateFonds;
        
        /**
         * 将创建的实体名称列表
         */
        private List<String> willCreateEntities;
    }
    
    /**
     * 导入行预览数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowPreview {
        /**
         * 行号
         */
        private int rowNumber;
        
        /**
         * 解析后的数据（字段名 -> 值）
         */
        private Map<String, Object> data;
        
        /**
         * 验证错误列表
         */
        private List<ImportError> validationErrors;
    }
}

