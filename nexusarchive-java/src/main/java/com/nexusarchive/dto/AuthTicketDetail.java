// Input: AuthTicket Entity, ApprovalChain DTO
// Output: AuthTicketDetail DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 授权票据详情 DTO
 */
@Data
public class AuthTicketDetail {
    
    private String id;
    private String applicantId;
    private String applicantName;
    private String sourceFonds;
    private String targetFonds;
    private AuthScope scope;
    private LocalDateTime expiresAt;
    private String status;
    private ApprovalChain approvalChain;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedTime;
}



