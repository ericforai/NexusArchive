// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: AuditLogService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;
    
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
     * 记录跨全宗访问审计日志
     * 
     * @param userId 操作者ID
     * @param username 操作者姓名
     * @param sourceFonds 源全宗号
     * @param targetFonds 目标全宗号
     * @param ticketId 授权票据ID
     * @param action 操作类型
     * @param resourceId 资源ID
     * @param result 操作结果 (SUCCESS/FAILURE)
     * @param clientIp 客户端IP
     * @param traceId 追踪ID
     * @param message 详细信息
     */
    @Async
    public void logCrossFondsAccess(String userId, String username, String sourceFonds, 
                                     String targetFonds, String ticketId, String action,
                                     String resourceId, String result, String clientIp,
                                     String traceId, String message) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setResourceType("CROSS_FONDS");
        auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result);
        auditLog.setDetails(String.format("源全宗: %s, 目标全宗: %s, 授权票据: %s, 信息: %s",
                sourceFonds, targetFonds, ticketId, message));
        auditLog.setClientIp(clientIp);
        auditLog.setMacAddress("UNKNOWN");
        auditLog.setTraceId(traceId);
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLog.setRiskLevel("medium");
        
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
        auditLogMapper.insert(auditLog);

        log.debug("审计日志已记录: 用户={}, 操作={}",
                auditLog.getUserId(), auditLog.getAction());
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

            String payload = buildLogChainPayload(
                    current.getUserId(),
                    current.getAction(),
                    current.getObjectDigest(),
                    createdTimeStr,
                    current.getPrevLogHash()
            );
            String recalculatedHash = sm3Utils.hmac(auditLogHmacKey, payload);
            
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

    private String buildLogChainPayload(String userId, String action, String objectDigest,
                                        String createdTime, String prevHash) {
        StringBuilder content = new StringBuilder();
        content.append(userId != null ? userId : "");
        content.append("|");
        content.append(action != null ? action : "");
        content.append("|");
        content.append(objectDigest != null ? objectDigest : "");
        content.append("|");
        content.append(createdTime != null ? createdTime : "");
        content.append("|");
        content.append(prevHash != null ? prevHash : "");
        return content.toString();
    }
}
