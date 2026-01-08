// Input: Lombok、Java 标准库
// Output: PeriodNotClosedException 类
// Pos: 异常定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.exception;

import lombok.Getter;

/**
 * 期间未关账异常
 * 当用户配置为强制模式，且目标期间在 ERP 系统中未关账时抛出
 */
@Getter
public class PeriodNotClosedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 期间，格式 "yyyy-MM"
     */
    private final String period;

    /**
     * 账套代码
     */
    private final String accbookCode;

    public PeriodNotClosedException(String period, String accbookCode) {
        super(String.format("期间 %s (账套: %s) 未关账，请先在 ERP 系统完成关账", period, accbookCode));
        this.period = period;
        this.accbookCode = accbookCode;
    }

    public PeriodNotClosedException(String period, String accbookCode, String message) {
        super(message);
        this.period = period;
        this.accbookCode = accbookCode;
    }
}
