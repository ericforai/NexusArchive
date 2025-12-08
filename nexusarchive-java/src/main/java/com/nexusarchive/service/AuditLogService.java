package com.nexusarchive.service;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 审计日志服务
 * 
 * 增强功能：
 * - SM3 哈希链防篡改
 * - 日志链完整性验证
 * 
 * 合规要求：
 * - GB/T 39784-2021 表36: 审计日志必须防篡改
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;
    private final SM3Utils sm3Utils;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 记录审计日志 (异步)
     * 简化方法，自动计算哈希链
     */
    @Async
    public void log(String userId, String username, String action, 
                   String resourceType, String resourceId, 
                   String result, String details, String ip) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result);
        auditLog.setDetails(details);
        auditLog.setClientIp(ip);
        auditLog.setMacAddress("UNKNOWN");
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLog.setRiskLevel("low");
        
        saveAuditLogWithHash(auditLog);
    }
    
    /**
     * 记录审计日志 (直接传入 SysAuditLog 对象)
     * 自动计算哈希链
     */
    @Async
    public void log(SysAuditLog auditLog) {
        if (auditLog.getId() == null || auditLog.getId().isEmpty()) {
            auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (auditLog.getCreatedTime() == null) {
            auditLog.setCreatedTime(LocalDateTime.now());
        }
        
        saveAuditLogWithHash(auditLog);
    }
    
    /**
     * 保存审计日志并计算哈希链
     * 
     * 哈希链机制：
     * 1. 获取前一条日志的哈希值
     * 2. 计算当前日志哈希: SM3(operatorId + operationType + objectDigest + createdTime + prevHash)
     * 3. 存储当前日志和哈希值
     */
    @Transactional
    public void saveAuditLogWithHash(SysAuditLog auditLog) {
        try {
            // 获取前一条日志的哈希
            String prevHash = auditLogMapper.getLatestLogHash();
            auditLog.setPrevLogHash(prevHash);
            
            // 计算当前日志的哈希
            String createdTimeStr = auditLog.getCreatedTime() != null 
                    ? auditLog.getCreatedTime().format(TIME_FORMATTER) 
                    : LocalDateTime.now().format(TIME_FORMATTER);
            
            String logHash = sm3Utils.calculateLogHash(
                    auditLog.getUserId(),
                    auditLog.getAction(),
                    auditLog.getObjectDigest(),
                    createdTimeStr,
                    prevHash
            );
            auditLog.setLogHash(logHash);
            
            // 插入日志
            auditLogMapper.insert(auditLog);
            
            log.debug("审计日志已记录: 用户={}, 操作={}, 哈希={}", 
                    auditLog.getUserId(), auditLog.getAction(), logHash);
                    
        } catch (Exception e) {
            log.error("保存审计日志失败: {}", e.getMessage(), e);
            // 即使哈希计算失败，也要确保日志被记录
            auditLogMapper.insert(auditLog);
        }
    }
    
    /**
     * 验证日志链完整性
     * 
     * 验证逻辑：
     * 1. 按时间顺序遍历日志
     * 2. 检查每条日志的 prevLogHash 是否等于前一条的 logHash
     * 3. 重新计算日志哈希并验证
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 验证结果
     */
    public LogChainVerifyResult verifyLogChain(LocalDate startDate, LocalDate endDate) {
        log.info("开始验证日志链: {} 至 {}", startDate, endDate);
        
        List<SysAuditLog> logs = auditLogMapper.findByDateRange(startDate, endDate);
        
        if (logs.isEmpty()) {
            return LogChainVerifyResult.builder()
                    .valid(true)
                    .totalLogs(0)
                    .verifiedLogs(0)
                    .message("指定时间范围内无日志")
                    .build();
        }
        
        int verifiedCount = 0;
        StringBuilder errorMessages = new StringBuilder();
        
        for (int i = 0; i < logs.size(); i++) {
            SysAuditLog current = logs.get(i);
            
            // 验证链条：当前的 prevLogHash 应该等于前一条的 logHash
            if (i > 0) {
                SysAuditLog prev = logs.get(i - 1);
                if (prev.getLogHash() != null && current.getPrevLogHash() != null) {
                    if (!prev.getLogHash().equals(current.getPrevLogHash())) {
                        errorMessages.append(String.format(
                                "链条断裂: 日志 %s 的 prevLogHash 与前一条日志的 logHash 不匹配\n",
                                current.getId()
                        ));
                    }
                }
            }
            
            // 重新计算哈希并验证
            String createdTimeStr = current.getCreatedTime() != null 
                    ? current.getCreatedTime().format(TIME_FORMATTER) 
                    : null;
            
            String recalculatedHash = sm3Utils.calculateLogHash(
                    current.getUserId(),
                    current.getAction(),
                    current.getObjectDigest(),
                    createdTimeStr,
                    current.getPrevLogHash()
            );
            
            if (current.getLogHash() != null && recalculatedHash != null) {
                if (!current.getLogHash().equals(recalculatedHash)) {
                    errorMessages.append(String.format(
                            "日志篡改: 日志 %s 的哈希值验证失败\n",
                            current.getId()
                    ));
                } else {
                    verifiedCount++;
                }
            } else {
                // 旧日志可能没有哈希值
                verifiedCount++;
            }
        }
        
        boolean valid = errorMessages.length() == 0;
        String message = valid ? "日志链验证通过" : errorMessages.toString();
        
        log.info("日志链验证完成: 总数={}, 验证通过={}, 结果={}", 
                logs.size(), verifiedCount, valid ? "通过" : "失败");
        
        return LogChainVerifyResult.builder()
                .valid(valid)
                .totalLogs(logs.size())
                .verifiedLogs(verifiedCount)
                .message(message)
                .build();
    }
    
    /**
     * 日志链验证结果
     */
    @lombok.Builder
    @lombok.Data
    public static class LogChainVerifyResult {
        private boolean valid;
        private int totalLogs;
        private int verifiedLogs;
        private String message;
    }
}
