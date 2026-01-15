// Input: AuditLogVerificationService, SysAuditLogMapper
// Output: AuditLogSamplingService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.SamplingCriteria;
import com.nexusarchive.dto.SamplingResult;

import java.time.LocalDate;

/**
 * 审计日志抽检服务
 * 
 * 功能：
 * 1. 随机抽检指定数量的审计日志
 * 2. 按条件抽检（按用户、操作类型、时间范围等）
 * 3. 返回抽检结果和验真报告
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
public interface AuditLogSamplingService {
    
    /**
     * 随机抽检审计日志
     * 
     * @param sampleSize 抽检数量
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 抽检结果
     */
    SamplingResult randomSample(int sampleSize, LocalDate startDate, LocalDate endDate);
    
    /**
     * 按条件抽检审计日志
     * 
     * @param criteria 抽检条件
     * @param sampleSize 抽检数量
     * @return 抽检结果
     */
    SamplingResult sampleByCriteria(SamplingCriteria criteria, int sampleSize);
}

