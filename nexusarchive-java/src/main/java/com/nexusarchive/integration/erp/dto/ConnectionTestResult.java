package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ERP 连接测试结果
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionTestResult {

    /**
     * 测试是否成功
     */
    private boolean success;

    /**
     * 测试消息
     */
    private String message;

    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;

    /**
     * 错误代码（如有）
     */
    private String errorCode;

    public static ConnectionTestResult success(String message, long responseTime) {
        return new ConnectionTestResult(true, message, responseTime, null);
    }

    public static ConnectionTestResult fail(String message, String errorCode) {
        return new ConnectionTestResult(false, message, 0, errorCode);
    }
}
