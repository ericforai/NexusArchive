// Input: Jackson、Lombok、Java 标准库
// Output: DirectionType 枚举
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 借贷方向枚举
 * Reference: DA/T 94-2022 会计分录方向
 */
@Getter
public enum DirectionType {
    
    /**
     * 借方
     */
    DEBIT("DEBIT", "借方"),
    
    /**
     * 贷方
     */
    CREDIT("CREDIT", "贷方");
    
    private final String code;
    private final String description;
    
    DirectionType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    @JsonCreator
    public static DirectionType fromCode(String code) {
        for (DirectionType type : DirectionType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid direction type: " + code + 
            ". Must be one of: DEBIT, CREDIT");
    }
}
