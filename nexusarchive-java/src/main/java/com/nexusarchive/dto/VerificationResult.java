// Input: Java 标准库
// Output: VerificationResult DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 单条审计日志验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {
    
    /**
     * 是否有效
     */
    private boolean valid;
    
    /**
     * 审计日志ID
     */
    private String logId;
    
    /**
     * 期望的哈希值
     */
    private String expectedHash;
    
    /**
     * 实际的哈希值
     */
    private String actualHash;
    
    /**
     * 如果无效，说明原因
     */
    private String reason;
    
    /**
     * 验证时间
     */
    private LocalDateTime verifiedAt;
    
    /**
     * 创建无效结果
     */
    public static VerificationResult invalid(String logId, String reason) {
        return VerificationResult.builder()
            .valid(false)
            .logId(logId)
            .reason(reason)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }
    
    /**
     * 创建无效结果（带哈希值）
     */
    public static VerificationResult invalid(String logId, String reason, 
                                            String expectedHash, String actualHash) {
        return VerificationResult.builder()
            .valid(false)
            .logId(logId)
            .reason(reason)
            .expectedHash(expectedHash)
            .actualHash(actualHash)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }
    
    /**
     * 创建有效结果
     */
    public static VerificationResult valid(String logId, String hash) {
        return VerificationResult.builder()
            .valid(true)
            .logId(logId)
            .expectedHash(hash)
            .actualHash(hash)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }
}





