// Input: AuthTicket Entity, AuthScope DTO
// Output: AuthTicketService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketDetail;

import java.time.LocalDateTime;

/**
 * 授权票据服务
 * 
 * 功能：
 * 1. 创建授权票据申请
 * 2. 查询授权票据详情
 * 3. 撤销授权票据
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
public interface AuthTicketService {
    
    /**
     * 创建授权票据申请
     * 
     * @param applicantId 申请人ID
     * @param sourceFonds 源全宗号（申请人所属全宗）
     * @param targetFonds 目标全宗号
     * @param scope 访问范围
     * @param expiresAt 有效期（必须 >= 当前时间 + 1天，<= 当前时间 + 90天）
     * @param reason 申请原因
     * @return 授权票据ID
     */
    String createAuthTicket(String applicantId, String sourceFonds, 
                            String targetFonds, AuthScope scope, 
                            LocalDateTime expiresAt, String reason);
    
    /**
     * 查询授权票据详情
     * 
     * @param ticketId 授权票据ID
     * @return 授权票据详情
     */
    AuthTicketDetail getAuthTicketDetail(String ticketId);
    
    /**
     * 撤销授权票据（仅申请人或管理员可撤销）
     * 
     * @param ticketId 授权票据ID
     * @param operatorId 操作人ID
     * @param reason 撤销原因
     */
    void revokeAuthTicket(String ticketId, String operatorId, String reason);
}





