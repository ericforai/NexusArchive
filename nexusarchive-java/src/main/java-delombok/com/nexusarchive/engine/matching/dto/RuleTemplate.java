// Input: Lombok、Java 标准库
// Output: RuleTemplate DTO
// Pos: 匹配引擎/DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则模板 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTemplate {
    
    private String id;
    private String name;
    private String version;
    private String scene;
    private String config;  // JSON 格式的规则配置
}
