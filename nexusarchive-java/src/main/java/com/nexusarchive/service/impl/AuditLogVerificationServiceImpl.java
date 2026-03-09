// Input: AuditLogVerificationService, AuditLogService, SysAuditLogMapper
// Output: AuditLogVerificationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.VerificationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.AuditLogVerificationService;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审计日志验真服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogVerificationServiceImpl implements AuditLogVerificationService {
    
    private final AuditLogService auditLogService;
    private final SysAuditLogMapper auditLogMapper;
    private final SM3Utils sm3Utils;
    
    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    @Override
    public VerificationResult verifySingleLog(String logId) {
        // 1. 查询审计日志
        SysAuditLog log = auditLogMapper.selectById(logId);
        if (log == null) {
            return VerificationResult.invalid(logId, "审计日志不存在: " + logId);
        }
        
        // 2. 重新计算当前日志的哈希值
        String recalculatedHash = calculateLogHash(log);
        
        // 3. 比较计算出的哈希值与存储的哈希值
        if (log.getLogHash() == null || !log.getLogHash().equals(recalculatedHash)) {
            return VerificationResult.invalid(logId, "哈希值不匹配", 
                log.getLogHash(), recalculatedHash);
        }
        
        // 4. 验证与前一条日志的关联（如果存在）
        if (log.getPrevLogHash() != null) {
            String prevLogHash = getPreviousLogHash(log);
            if (prevLogHash != null && !log.getPrevLogHash().equals(prevLogHash)) {
                return VerificationResult.invalid(logId, "与前一条日志的哈希关联不匹配");
            }
        }
        
        return VerificationResult.valid(logId, log.getLogHash());
    }
    
    @Override
    public ChainVerificationResult verifyChain(LocalDate startDate, LocalDate endDate, String fondsNo) {
        // 复用 AuditLogService.verifyLogChain() 方法
        var result = auditLogService.verifyLogChain(startDate, endDate);
        
        // 如果指定了全宗号，需要额外过滤（AuditLogService 当前不支持全宗过滤）
        // TODO: 扩展 AuditLogService.verifyLogChain() 支持全宗过滤
        
        // 转换结果格式
        List<VerificationResult> invalidResults = new ArrayList<>();
        int invalidCount = 0;
        if (result.getMessage() != null && !result.getMessage().isEmpty() && !result.isValid()) {
            for (String violation : splitViolations(result.getMessage())) {
                invalidResults.add(VerificationResult.invalid("", violation));
            }
            invalidCount = invalidResults.size();
        }
        
        return ChainVerificationResult.builder()
            .chainIntact(result.isValid())
            .totalLogs(result.getTotalLogs())
            .validLogs(result.isValid() ? result.getVerifiedLogs() : Math.max(0, result.getTotalLogs() - invalidCount))
            .invalidLogs(result.isValid() ? 0 : invalidCount)
            .invalidResults(invalidResults)
            .verifiedAt(LocalDateTime.now())
            .build();
    }
    
    @Override
    public ChainVerificationResult verifyChainByLogIds(List<String> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            return ChainVerificationResult.builder()
                .chainIntact(true)
                .totalLogs(0)
                .validLogs(0)
                .invalidLogs(0)
                .verifiedAt(LocalDateTime.now())
                .build();
        }
        
        // 1. 查询所有日志
        List<SysAuditLog> logs = new ArrayList<>(auditLogMapper.selectBatchIds(logIds));
        if (logs.size() != logIds.size()) {
            log.warn("部分审计日志不存在: 请求数量={}, 实际数量={}", logIds.size(), logs.size());
        }
        
        // 2. 按时间排序
        logs.sort((a, b) -> {
            if (a.getCreatedTime() == null || b.getCreatedTime() == null) {
                return 0;
            }
            return a.getCreatedTime().compareTo(b.getCreatedTime());
        });
        
        // 3. 验证每条日志
        Map<String, VerificationResult> invalidByLogId = new LinkedHashMap<>();
        
        for (int i = 0; i < logs.size(); i++) {
            SysAuditLog current = logs.get(i);
            VerificationResult result = verifySingleLog(current.getId());
            
            if (!result.isValid()) {
                invalidByLogId.put(current.getId(), result);
            }
            
            // 4. 验证与前一条日志的哈希关联
            if (i > 0) {
                SysAuditLog prev = logs.get(i - 1);
                if (current.getPrevLogHash() != null && prev.getLogHash() != null) {
                    if (!current.getPrevLogHash().equals(prev.getLogHash())) {
                        invalidByLogId.merge(
                                current.getId(),
                                VerificationResult.invalid(current.getId(), "与前一条日志的哈希关联不匹配"),
                                this::mergeVerificationResult
                        );
                    }
                }
            }
        }

        List<VerificationResult> invalidResults = new ArrayList<>(invalidByLogId.values());
        int invalidCount = invalidResults.size();
        
        return ChainVerificationResult.builder()
            .chainIntact(invalidResults.isEmpty())
            .totalLogs(logs.size())
            .validLogs(logs.size() - invalidCount)
            .invalidLogs(invalidCount)
            .invalidResults(invalidResults)
            .verifiedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 计算审计日志的哈希值（与 AuditLogService 中的逻辑一致）
     */
    private String calculateLogHash(SysAuditLog log) {
        String createdTimeStr = log.getCreatedTime() != null 
            ? log.getCreatedTime().format(TIME_FORMATTER) 
            : null;
        
        String payload = buildHashPayload(
            log.getUserId(),
            log.getAction(),
            log.getObjectDigest(),
            createdTimeStr,
            log.getPrevLogHash()
        );
        
        // 使用 SM3 HMAC 计算哈希值（与 AuditLogService 逻辑一致）
        return sm3Utils.hmac(auditLogHmacKey, payload);
    }
    
    /**
     * 构建哈希载荷（与 AuditLogService.buildLogChainPayload 逻辑一致）
     */
    private String buildHashPayload(String userId, String action, String objectDigest, 
                                   String createdTime, String prevLogHash) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId != null ? userId : "").append("|");
        sb.append(action != null ? action : "").append("|");
        sb.append(objectDigest != null ? objectDigest : "").append("|");
        sb.append(createdTime != null ? createdTime : "").append("|");
        sb.append(prevLogHash != null ? prevLogHash : "");
        return sb.toString();
    }
    
    /**
     * 获取前一条日志的哈希值
     */
    private String getPreviousLogHash(SysAuditLog log) {
        // 查询前一条日志（按时间排序）
        LambdaQueryWrapper<SysAuditLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(SysAuditLog::getCreatedTime, log.getCreatedTime())
            .orderByDesc(SysAuditLog::getCreatedTime)
            .last("LIMIT 1");
        
        SysAuditLog prevLog = auditLogMapper.selectOne(queryWrapper);
        return prevLog != null ? prevLog.getLogHash() : null;
    }

    private List<String> splitViolations(String message) {
        return message.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private VerificationResult mergeVerificationResult(VerificationResult existing, VerificationResult incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }
        String mergedReason = existing.getReason();
        if (incoming.getReason() != null && !incoming.getReason().isBlank()
                && (mergedReason == null || !mergedReason.contains(incoming.getReason()))) {
            mergedReason = mergedReason == null || mergedReason.isBlank()
                    ? incoming.getReason()
                    : mergedReason + "；" + incoming.getReason();
        }
        existing.setReason(mergedReason);
        return existing;
    }
}
