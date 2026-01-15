// Input: Lombok、Java 标准库
// Output: ErpException 类
// Pos: 异常定义 - ERP 集成通用异常
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.exception;

import lombok.Getter;

/**
 * ERP 集成通用异常
 * 当调用 ERP 系统接口失败时抛出
 *
 * <p>适用场景：</p>
 * <ul>
 *   <li>认证失败</li>
 *   <li>API 调用失败</li>
 *   <li>数据格式错误</li>
 *   <li>业务逻辑错误</li>
 * </ul>
 */
@Getter
public class ErpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误代码 (可选)
     */
    private final String errorCode;

    /**
     * ERP 系统类型 (如 sap, yonsuite, kingdee)
     */
    private final String erpType;

    public ErpException(String message) {
        super(message);
        this.errorCode = null;
        this.erpType = null;
    }

    public ErpException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.erpType = null;
    }

    public ErpException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.erpType = null;
    }

    public ErpException(String message, String errorCode, String erpType) {
        super(message);
        this.errorCode = errorCode;
        this.erpType = erpType;
    }

    public ErpException(String message, String errorCode, String erpType, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.erpType = erpType;
    }
}
