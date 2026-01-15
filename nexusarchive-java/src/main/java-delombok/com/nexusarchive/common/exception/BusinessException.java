// Input: Lombok、Java 标准库
// Output: BusinessException 类
// Pos: 异常定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    /**
     * 默认错误码：服务器内部错误
     */
    private static final int DEFAULT_ERROR_CODE = 500;

    public BusinessException(String message) {
        super(message);
        this.code = DEFAULT_ERROR_CODE;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = DEFAULT_ERROR_CODE;
    }

    /**
     * 使用 ErrorCode 枚举创建异常
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用 ErrorCode 枚举创建异常（支持参数化消息）
     * @param errorCode 错误码枚举
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessage(args));
        this.code = errorCode.getCode();
    }

    // Manual Getters
    public Integer getCode() { return code; }
}
