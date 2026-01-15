// Input: Java 标准库
// Output: MappingException 异常类
// Pos: 集成模块 - ERP 映射异常

package com.nexusarchive.integration.erp.mapping;

/**
 * ERP 映射异常
 * 当 ERP 数据映射失败时抛出
 */
public class MappingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
