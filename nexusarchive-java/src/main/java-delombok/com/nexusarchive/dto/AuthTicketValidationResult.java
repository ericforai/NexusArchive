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
@NoArgsConstructor
public class AuthTicketValidationResult {

    public AuthTicketValidationResult(boolean valid, String ticketId, String applicantId, String sourceFonds, String targetFonds, LocalDateTime expiresAt, String reason) {
        this.valid = valid;
        this.ticketId = ticketId;
        this.applicantId = applicantId;
        this.sourceFonds = sourceFonds;
        this.targetFonds = targetFonds;
        this.expiresAt = expiresAt;
        this.reason = reason;
    }

    public static AuthTicketValidationResultBuilder builder() {
        return new AuthTicketValidationResultBuilder();
    }

    public static class AuthTicketValidationResultBuilder {
        private boolean valid;
        private String ticketId;
        private String applicantId;
        private String sourceFonds;
        private String targetFonds;
        private LocalDateTime expiresAt;
        private String reason;

        AuthTicketValidationResultBuilder() {}

        public AuthTicketValidationResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public AuthTicketValidationResultBuilder ticketId(String ticketId) {
            this.ticketId = ticketId;
            return this;
        }

        public AuthTicketValidationResultBuilder applicantId(String applicantId) {
            this.applicantId = applicantId;
            return this;
        }

        public AuthTicketValidationResultBuilder sourceFonds(String sourceFonds) {
            this.sourceFonds = sourceFonds;
            return this;
        }

        public AuthTicketValidationResultBuilder targetFonds(String targetFonds) {
            this.targetFonds = targetFonds;
            return this;
        }

        public AuthTicketValidationResultBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public AuthTicketValidationResultBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public AuthTicketValidationResult build() {
            return new AuthTicketValidationResult(valid, ticketId, applicantId, sourceFonds, targetFonds, expiresAt, reason);
        }
    }
    
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

    // Manual Getters
    public String getApplicantId() { return applicantId; }
    public String getSourceFonds() { return sourceFonds; }
    public String getTargetFonds() { return targetFonds; }
    public String getReason() { return reason; }
    public boolean isValid() { return valid; }
    
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





