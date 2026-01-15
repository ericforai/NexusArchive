// Input: Java 标准库
// Output: BusinessScene 枚举
// Pos: 匹配引擎/枚举
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务场景枚举
 * 
 * 基于借贷科目角色组合识别的业务场景类型
 */
@Getter
@RequiredArgsConstructor
public enum BusinessScene {
    
    PAYMENT("付款", "T01_PAYMENT"),
    RECEIPT("收款", "T02_RECEIPT"),
    EXPENSE("费用报销", "T03_EXPENSE"),
    PURCHASE_IN("采购入库", "T04_PURCHASE"),
    SALES_OUT("销售出库", "T05_SALES"),
    SALARY_ACCRUAL("工资计提", "T06_SALARY"),
    SALARY_PAYMENT("工资发放", "T06_SALARY"),
    TAX_ACCRUAL("税费计提", "T07_TAX"),
    TAX_PAYMENT("税费缴纳", "T07_TAX"),
    ASSET_PURCHASE("固定资产购置", "T08_ASSET"),
    DEPRECIATION("折旧计提", "T08_ASSET"),
    LOAN_IN("借款", "T00_MANUAL"),
    LOAN_OUT("还款", "T00_MANUAL"),
    TRANSFER("内部转账", "T00_MANUAL"),
    CLOSING("期末结转", "T00_MANUAL"),
    UNKNOWN("未识别", "T00_MANUAL");
    
    private final String name;
    private final String defaultTemplateId;
}
