// Input: AuditLogService, AuditLogVerificationService
// Output: AuditEvidencePackageService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import java.time.LocalDate;

/**
 * 审计证据包导出服务
 * 
 * 功能：
 * 1. 导出指定日期范围内的审计日志
 * 2. 生成可验证的证据包（ZIP 格式）
 * 3. 包含验真报告（JSON 格式）
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
public interface AuditEvidencePackageService {
    
    /**
     * 导出审计证据包
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param fondsNo 全宗号（可选）
     * @param includeVerificationReport 是否包含验真报告
     * @return 证据包文件（ZIP格式）
     */
    byte[] exportEvidencePackage(LocalDate startDate, LocalDate endDate, 
                                String fondsNo, boolean includeVerificationReport);
    
    /**
     * 导出审计证据包（异步，返回任务ID）
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param fondsNo 全宗号（可选）
     * @param includeVerificationReport 是否包含验真报告
     * @return 任务ID
     */
    String exportEvidencePackageAsync(LocalDate startDate, LocalDate endDate, 
                                     String fondsNo, boolean includeVerificationReport);
}

