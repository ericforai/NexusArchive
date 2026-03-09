// Input: Java 标准库、Lombok
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

    public static final String ISSUE_TYPE_OK = "OK";
    public static final String ISSUE_TYPE_MISSING_LOG = "MISSING_LOG";
    public static final String ISSUE_TYPE_BROKEN_CHAIN = "BROKEN_CHAIN";
    public static final String ISSUE_TYPE_HASH_MISMATCH = "HASH_MISMATCH";
    
    /**
     * 是否有效
     */
    private boolean valid;
    
    /**
     * 审计日志ID
     */
    private String logId;

    /**
     * 结果类型：OK / MISSING_LOG / BROKEN_CHAIN / HASH_MISMATCH
     */
    private String issueType;
    
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
            .issueType(ISSUE_TYPE_HASH_MISMATCH)
            .reason(reason)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }

    public static VerificationResult invalid(String logId, String issueType, String reason) {
        return VerificationResult.builder()
            .valid(false)
            .logId(logId)
            .issueType(issueType)
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
            .issueType(ISSUE_TYPE_HASH_MISMATCH)
            .reason(reason)
            .expectedHash(expectedHash)
            .actualHash(actualHash)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }

    public static VerificationResult invalid(String logId, String issueType, String reason,
                                            String expectedHash, String actualHash) {
        return VerificationResult.builder()
            .valid(false)
            .logId(logId)
            .issueType(issueType)
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
            .issueType(ISSUE_TYPE_OK)
            .expectedHash(hash)
            .actualHash(hash)
            .verifiedAt(java.time.LocalDateTime.now())
            .build();
    }
}




