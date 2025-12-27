// Input: Lombok
// Output: MappingConfirmation DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 映射确认请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingConfirmation {
    
    private String type;  // ACCOUNT / DOC_TYPE
    private String code;
    private String role;
    private String displayName;
}
