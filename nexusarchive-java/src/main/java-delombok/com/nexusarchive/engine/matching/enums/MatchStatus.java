// Input: Java 标准库
// Output: MatchStatus 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 匹配状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum MatchStatus {
    
    PROCESSING("处理中"),
    MATCHED("已匹配"),
    PENDING("待补证"),
    NEED_CONFIRM("需确认"),
    CONFIRMED("已确认"),
    ERROR("错误");
    
    private final String name;
}
