// Input: AuthTicketMapper, Scheduled
// Output: AuthTicketExpirationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

/**
 * 授权票据过期处理服务
 * 
 * 功能：
 * 1. 定时扫描过期票据
 * 2. 自动将过期票据状态更新为 EXPIRED
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
public interface AuthTicketExpirationService {
    
    /**
     * 扫描并标记过期票据
     * 
     * @return 本次标记的过期票据数量
     */
    int scanAndMarkExpired();
}





