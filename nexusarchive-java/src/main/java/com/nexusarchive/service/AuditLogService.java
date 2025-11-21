package com.nexusarchive.service;

import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * 记录审计日志 (异步)
     */
    @Async
    public void log(String userId, String username, String action, 
                   String resourceType, String resourceId, 
                   String result, String details, String ip) {
        SysAuditLog log = new SysAuditLog();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOperationResult(result);
        log.setDetails(details);
        log.setIpAddress(ip);
        log.setCreatedAt(LocalDateTime.now());
        
        // 默认风险等级
        log.setRiskLevel("low");
        
        auditLogMapper.insert(log);
    }
}
