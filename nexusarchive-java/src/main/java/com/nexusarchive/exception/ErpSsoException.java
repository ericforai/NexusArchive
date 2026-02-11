// Input: Java 标准库
// Output: ErpSsoException 异常
// Pos: 业务异常层

package com.nexusarchive.exception;

import lombok.Getter;

@Getter
public class ErpSsoException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public ErpSsoException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
