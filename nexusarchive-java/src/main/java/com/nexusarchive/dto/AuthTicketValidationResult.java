// Input: Java 标准库
// Output: AuthTicketValidationResult DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 授权票据验证结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTicketValidationResult {
    
    /**
     * 是否有效
     */
    private boolean valid;
    
    /**
     * 授权票据ID
     */
    private String ticketId;
    
    /**
     * 申请人ID
     */
    private String applicantId;
    
    /**
     * 源全宗号
     */
    private String sourceFonds;
    
    /**
     * 目标全宗号
     */
    private String targetFonds;
    
    /**
     * 有效期
     */
    private LocalDateTime expiresAt;
    
    /**
     * 如果无效，说明原因
     */
    private String reason;
    
    /**
     * 创建无效结果
     */
    public static AuthTicketValidationResult invalid(String reason) {
        return AuthTicketValidationResult.builder()
            .valid(false)
            .reason(reason)
            .build();
    }
    
    /**
     * 创建有效结果
     */
    public static AuthTicketValidationResult valid(String ticketId, String applicantId, 
                                                   String sourceFonds, String targetFonds, 
                                                   LocalDateTime expiresAt) {
        return AuthTicketValidationResult.builder()
            .valid(true)
            .ticketId(ticketId)
            .applicantId(applicantId)
            .sourceFonds(sourceFonds)
            .targetFonds(targetFonds)
            .expiresAt(expiresAt)
            .build();
    }
}



