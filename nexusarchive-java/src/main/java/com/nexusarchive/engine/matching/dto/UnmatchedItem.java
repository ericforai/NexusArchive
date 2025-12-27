// Input: Lombok
// Output: UnmatchedItem & MappingConfirmation DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 未匹配项（需人工确认）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnmatchedItem {
    
    private String type;  // ACCOUNT / DOC_TYPE
    private String code;
    private String name;
    private String suggestedRole;  // 建议的角色（如果有）
    private double confidence;
}
