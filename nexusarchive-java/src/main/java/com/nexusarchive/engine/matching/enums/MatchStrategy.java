// Input: Java 标准库
// Output: MatchStrategy 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 匹配策略枚举
 * 
 * 定义字段匹配时使用的策略类型
 */
@Getter
@RequiredArgsConstructor
public enum MatchStrategy {
    
    EXACT("精确匹配", "字符串完全相等"),
    CONTAINS("包含匹配", "一方包含另一方"),
    SIMILARITY("相似度匹配", "Jaccard 相似度 >= 阈值"),
    NUMERIC_TOLERANCE("数值容差", "数值差额在容差范围内");
    
    private final String name;
    private final String description;
}
