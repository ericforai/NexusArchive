// Input: Java 标准库
// Output: LinkType 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 关联类型枚举
 */
@Getter
@RequiredArgsConstructor
public enum LinkType {
    
    MUST_LINK("必关联", "缺失会阻止归档"),
    SHOULD_LINK("应关联", "缺失会标记风险"),
    MAY_LINK("可关联", "可选佐证材料");
    
    private final String name;
    private final String description;
}
