// Input: RuntimeException, Java 标准库
// Output: MappingScriptException 类
// Pos: 集成模块 - ERP 映射异常

package com.nexusarchive.integration.erp.mapping;

public class MappingScriptException extends RuntimeException {

    public MappingScriptException(String message) {
        super(message);
    }

    public MappingScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
