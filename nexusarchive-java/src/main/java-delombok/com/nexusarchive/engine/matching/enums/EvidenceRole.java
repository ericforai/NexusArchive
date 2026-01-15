// Input: Java 标准库
// Output: EvidenceRole 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 证据角色枚举
 * 
 * 用于将客户的各种单据类型抽象为系统统一的证据角色，
 * 实现规则模板与客户命名的解耦。
 */
@Getter
@RequiredArgsConstructor
public enum EvidenceRole {
    
    AUTHORIZATION("授权凭据", "业务审批/付款指令"),
    SETTLEMENT("结算凭据", "资金流转证明"),
    TAX_EVIDENCE("税务凭据", "增值税证明"),
    CONTRACTUAL_BASIS("合同依据", "法律关系证明"),
    EXECUTION_PROOF("执行证明", "业务执行证明"),
    ACCOUNTING_TRIGGER("记账触发", "记账业务来源");
    
    private final String name;
    private final String description;
}
