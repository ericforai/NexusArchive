// Input: AuditLogSamplingService, SysAuditLogMapper, AuditLogVerificationService
// Output: AuditLogSamplingServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.SamplingCriteria;
import com.nexusarchive.dto.SamplingResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.AuditLogSamplingService;
import com.nexusarchive.service.AuditLogVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 审计日志抽检服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogSamplingServiceImpl implements AuditLogSamplingService {
    
    private final SysAuditLogMapper auditLogMapper;
    private final AuditLogVerificationService verificationService;
    private final Random random = new Random();
    
    @Override
    public com.nexusarchive.dto.SamplingResult randomSample(int sampleSize, LocalDate startDate, LocalDate endDate) {
        // 1. 查询指定日期范围内的所有日志
        List<SysAuditLog> allLogs = auditLogMapper.findByDateRange(startDate, endDate);
        
        if (allLogs.isEmpty()) {
            return SamplingResult.builder()
                .totalLogs(0)
                .sampledLogs(0)
                .verificationResult(ChainVerificationResult.builder()
                    .chainIntact(true)
                    .totalLogs(0)
                    .validLogs(0)
                    .invalidLogs(0)
                    .build())
                .sampledLogIds(new ArrayList<>())
                .build();
        }
        
        // 2. 随机抽取指定数量的日志
        List<SysAuditLog> sampledLogs = randomSampleFromList(allLogs, sampleSize);
        List<String> sampledLogIds = sampledLogs.stream()
            .map(SysAuditLog::getId)
            .collect(Collectors.toList());
        
        // 3. 对抽检的日志进行验真
        ChainVerificationResult verificationResult = verificationService.verifyChainByLogIds(sampledLogIds);
        
        return SamplingResult.builder()
            .totalLogs(allLogs.size())
            .sampledLogs(sampledLogs.size())
            .verificationResult(verificationResult)
            .sampledLogIds(sampledLogIds)
            .build();
    }
    
    @Override
    public SamplingResult sampleByCriteria(SamplingCriteria criteria, int sampleSize) {
        // 1. 构建查询条件
        LambdaQueryWrapper<SysAuditLog> queryWrapper = new LambdaQueryWrapper<>();
        
        if (criteria.getUserId() != null && !criteria.getUserId().isEmpty()) {
            queryWrapper.eq(SysAuditLog::getUserId, criteria.getUserId());
        }
        if (criteria.getAction() != null && !criteria.getAction().isEmpty()) {
            queryWrapper.eq(SysAuditLog::getAction, criteria.getAction());
        }
        if (criteria.getResourceType() != null && !criteria.getResourceType().isEmpty()) {
            queryWrapper.eq(SysAuditLog::getResourceType, criteria.getResourceType());
        }
        // TODO: SysAuditLog 可能没有 fondsNo 字段，需要检查实体类
        // if (criteria.getFondsNo() != null && !criteria.getFondsNo().isEmpty()) {
        //     queryWrapper.eq(SysAuditLog::getFondsNo, criteria.getFondsNo());
        // }
        if (criteria.getStartDate() != null && criteria.getEndDate() != null) {
            queryWrapper.between(SysAuditLog::getCreatedTime, 
                criteria.getStartDate().atStartOfDay(), 
                criteria.getEndDate().atTime(23, 59, 59));
        }
        
        queryWrapper.orderByDesc(SysAuditLog::getCreatedTime);
        
        // 2. 查询符合条件的日志
        List<SysAuditLog> allLogs = auditLogMapper.selectList(queryWrapper);
        
        if (allLogs.isEmpty()) {
            return SamplingResult.builder()
                .totalLogs(0)
                .sampledLogs(0)
                .verificationResult(ChainVerificationResult.builder()
                    .chainIntact(true)
                    .totalLogs(0)
                    .validLogs(0)
                    .invalidLogs(0)
                    .build())
                .sampledLogIds(new ArrayList<>())
                .build();
        }
        
        // 3. 随机抽取指定数量的日志
        List<SysAuditLog> sampledLogs = randomSampleFromList(allLogs, sampleSize);
        List<String> sampledLogIds = sampledLogs.stream()
            .map(SysAuditLog::getId)
            .collect(Collectors.toList());
        
        // 4. 对抽检的日志进行验真
        ChainVerificationResult verificationResult = verificationService.verifyChainByLogIds(sampledLogIds);
        
        return SamplingResult.builder()
            .totalLogs(allLogs.size())
            .sampledLogs(sampledLogs.size())
            .verificationResult(verificationResult)
            .sampledLogIds(sampledLogIds)
            .build();
    }
    
    /**
     * 从列表中随机抽取指定数量的元素
     */
    private <T> List<T> randomSampleFromList(List<T> list, int sampleSize) {
        if (list.size() <= sampleSize) {
            return new ArrayList<>(list);
        }
        
        List<T> shuffled = new ArrayList<>(list);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, sampleSize);
    }
}

