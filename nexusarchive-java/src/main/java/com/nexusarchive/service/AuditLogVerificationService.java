// Input: AuditLogService, SysAuditLog
// Output: AuditLogVerificationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.VerificationResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 审计日志验真服务
 * 
 * 功能：
 * 1. 验证单条审计日志的哈希值
 * 2. 验证审计日志哈希链的完整性
 * 3. 验证指定范围内的审计日志链
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
public interface AuditLogVerificationService {
    
    /**
     * 验证单条审计日志的哈希值
     * 
     * @param logId 审计日志ID
     * @return 验证结果
     */
    VerificationResult verifySingleLog(String logId);
    
    /**
     * 验证审计日志哈希链的完整性
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param fondsNo 全宗号（可选）
     * @return 验证结果（包含所有异常日志）
     */
    ChainVerificationResult verifyChain(LocalDate startDate, LocalDate endDate, String fondsNo);
    
    /**
     * 验证指定范围内的审计日志链
     * 
     * @param logIds 审计日志ID列表
     * @return 验证结果
     */
    ChainVerificationResult verifyChainByLogIds(List<String> logIds);
}


