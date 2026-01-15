// Input: Java 标准库
// Output: AccountRole 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 科目角色枚举
 * 
 * 用于将客户的各种科目编码抽象为系统统一的科目角色，
 * 实现业务场景识别与科目编码的解耦。
 */
@Getter
@RequiredArgsConstructor
public enum AccountRole {
    
    CASH("现金类", "1001"),
    BANK("银行类", "1002"),
    RECEIVABLE("应收类", "1122,1123"),
    PAYABLE("应付类", "2202,2203"),
    EXPENSE("费用类", "6601-6603"),
    REVENUE("收入类", "6001,6051"),
    TAX("税费类", "2221"),
    ASSET("资产类", "1601,1602"),
    SALARY("薪酬类", "2211"),
    INVENTORY("存货类", "1401-1406"),
    LIABILITY("负债类", "2501,2502"),
    PROFIT_LOSS("损益类", "4103,4104");
    
    private final String name;
    private final String typicalCodes;

    // Manual Constructor (Lombok substitute)
    AccountRole(String name, String typicalCodes) {
        this.name = name;
        this.typicalCodes = typicalCodes;
    }

    // Manual Getters
    public String getName() { return name; }
    public String getTypicalCodes() { return typicalCodes; }
}
