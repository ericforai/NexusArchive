package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ERP 回写结果 DTO
 * 
 * 用于记录归档编号异步写回 ERP 系统的执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * ERP 凭证/单据 ID
     */
    private String voucherId;
    
    /**
     * 生成的档号
     */
    private String archivalCode;
    
    /**
     * ERP 类型 (如 YONSUITE, KINGDEE)
     */
    private String erpType;
    
    /**
     * 回写时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 错误信息 (失败时)
     */
    private String errorMessage;
    
    /**
     * 是否为模拟执行 (非真实 API 调用)
     */
    private boolean mocked;
    
    /**
     * 创建成功结果
     */
    public static FeedbackResult success(String voucherId, String archivalCode, String erpType, boolean mocked) {
        return FeedbackResult.builder()
                .success(true)
                .voucherId(voucherId)
                .archivalCode(archivalCode)
                .erpType(erpType)
                .timestamp(LocalDateTime.now())
                .mocked(mocked)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static FeedbackResult failure(String voucherId, String archivalCode, String erpType, String errorMessage) {
        return FeedbackResult.builder()
                .success(false)
                .voucherId(voucherId)
                .archivalCode(archivalCode)
                .erpType(erpType)
                .timestamp(LocalDateTime.now())
                .errorMessage(errorMessage)
                .mocked(false)
                .build();
    }
}
