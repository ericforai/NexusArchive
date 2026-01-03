// Input: AuthTicket Entity, AuthScope DTO
// Output: AuthTicketValidationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketValidationResult;

/**
 * 授权票据验证服务
 * 
 * 功能：
 * 1. 验证授权票据是否有效
 * 2. 检查访问范围是否在授权范围内
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
public interface AuthTicketValidationService {
    
    /**
     * 验证授权票据是否有效
     * 
     * @param ticketId 授权票据ID
     * @param targetFonds 目标全宗号
     * @param accessScope 本次访问范围
     * @return 验证结果
     * @throws AuthTicketException 票据无效时抛出异常
     */
    AuthTicketValidationResult validateTicket(String ticketId, String targetFonds, 
                                              AuthScope accessScope);
    
    /**
     * 检查访问范围是否在授权范围内
     * 
     * @param ticketScope 票据授权范围
     * @param accessScope 本次访问范围
     * @return 是否在授权范围内
     */
    boolean isAccessScopeAllowed(AuthScope ticketScope, AuthScope accessScope);
}


