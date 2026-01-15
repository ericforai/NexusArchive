// Input: Lombok
// Output: AutoMappingResult DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自动映射结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoMappingResult {
    
    private String kitId;
    private String kitName;
    
    // 科目映射
    private int accountsMapped;
    private int accountsPending;
    private List<MappedItem> accountMappings;
    
    // 单据类型映射
    private int docTypesMapped;
    private int docTypesPending;
    private List<MappedItem> docTypeMappings;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MappedItem {
        private String sourceCode;
        private String sourceName;
        private String targetRole;
        private String targetRoleName;
        private double confidence;
        private String matchRule;  // 匹配规则描述
    }
}
