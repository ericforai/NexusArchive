// Input: AuthTicket Entity, ApprovalChain DTO
// Output: AuthTicketApprovalService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.ApprovalChain;

/**
 * 授权票据审批服务
 * 
 * 功能：
 * 1. 第一审批人审批
 * 2. 第二审批人审批（复核）
 * 3. 获取审批链
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
public interface AuthTicketApprovalService {
    
    /**
     * 第一审批人审批
     * 
     * @param ticketId 授权票据ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否批准
     */
    void firstApproval(String ticketId, String approverId, String approverName, 
                      String comment, boolean approved);
    
    /**
     * 第二审批人审批（复核）
     * 
     * @param ticketId 授权票据ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否批准
     */
    void secondApproval(String ticketId, String approverId, String approverName, 
                       String comment, boolean approved);
    
    /**
     * 获取审批链
     * 
     * @param ticketId 授权票据ID
     * @return 审批链
     */
    ApprovalChain getApprovalChain(String ticketId);
}

