// Input: Lombok、Spring Framework、Jackson、Java 标准库、本地模块
// Output: AuditLogService 类（含上下文补齐、脱敏快照、哈希链）
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.util.SM3Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private final ObjectMapper objectMapper;
    private final com.nexusarchive.service.helper.AuditLogHelper helper;

    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String UNKNOWN = "UNKNOWN";

    @Async
    public void log(String userId, String username, String action, String resourceType, String resourceId, String result, String details, String ip) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId); auditLog.setUsername(username); auditLog.setAction(action);
        auditLog.setResourceType(resourceType); auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result); auditLog.setDetails(details); auditLog.setClientIp(ip);
        auditLog.setMacAddress(UNKNOWN); auditLog.setCreatedTime(LocalDateTime.now()); auditLog.setRiskLevel("LOW");
        enrichWithRequestContext(auditLog);
        saveAuditLogWithHash(auditLog);
    }
    
    @Async
    public void log(SysAuditLog auditLog) {
        if (auditLog.getId() == null) auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        if (auditLog.getCreatedTime() == null) auditLog.setCreatedTime(LocalDateTime.now());
        enrichWithRequestContext(auditLog);
        saveAuditLogWithHash(auditLog);
    }

    @Async
    public void logBusinessSnapshot(String userId, String username, String action, String resourceType, String resourceId, String result, String details, String riskLevel, Object before, Object after, String sourceFonds, String targetFonds, String ticketId) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId); auditLog.setUsername(username); auditLog.setAction(action);
        auditLog.setResourceType(resourceType); auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result); auditLog.setDetails(details); auditLog.setCreatedTime(LocalDateTime.now());

        AuditRequestContext ctx = enrichWithRequestContext(auditLog);
        auditLog.setRiskLevel(riskLevel != null ? riskLevel.toUpperCase() : (ctx.shouldElevateRisk() ? "MEDIUM" : "LOW"));
        auditLog.setSourceFonds(firstNonBlank(sourceFonds, auditLog.getSourceFonds()));
        auditLog.setTargetFonds(firstNonBlank(targetFonds, auditLog.getTargetFonds()));
        auditLog.setAuthTicketId(firstNonBlank(ticketId, auditLog.getAuthTicketId()));
        auditLog.setDataBefore(toMaskedAuditJson(before));
        auditLog.setDataAfter(toMaskedAuditJson(after));
        auditLog.setDataSnapshot(buildDataSnapshot(auditLog.getDataBefore(), auditLog.getDataAfter(), ctx, auditLog.getSourceFonds(), auditLog.getTargetFonds(), auditLog.getAuthTicketId()));
        saveAuditLogWithHash(auditLog);
    }

    @Async
    public void logCrossFondsAccess(String userId, String username, String sourceFonds, String targetFonds, String ticketId, String action, String resourceId, String result, String clientIp, String traceId, String message) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId); auditLog.setUsername(username); auditLog.setAction(action);
        auditLog.setResourceType("CROSS_FONDS"); auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result); auditLog.setDetails(message);
        auditLog.setClientIp(clientIp); auditLog.setTraceId(traceId);
        auditLog.setSourceFonds(sourceFonds); auditLog.setTargetFonds(targetFonds); auditLog.setAuthTicketId(ticketId);
        auditLog.setCreatedTime(LocalDateTime.now()); auditLog.setRiskLevel("MEDIUM");
        AuditRequestContext ctx = enrichWithRequestContext(auditLog);
        auditLog.setDataSnapshot(buildDataSnapshot(null, null, ctx, sourceFonds, targetFonds, ticketId));
        saveAuditLogWithHash(auditLog);
    }

    public AuditRequestContext enrichWithRequestContext(SysAuditLog auditLog) {
        AuditRequestContext ctx = helper.resolveRequestContext();
        auditLog.setClientIp(firstNonBlank(auditLog.getClientIp(), ctx.clientIp()));
        auditLog.setMacAddress(firstNonBlank(auditLog.getMacAddress(), ctx.macAddress()));
        auditLog.setUserAgent(firstNonBlank(auditLog.getUserAgent(), ctx.userAgent()));
        auditLog.setTraceId(firstNonBlank(auditLog.getTraceId(), ctx.traceId()));
        auditLog.setSourceFonds(firstNonBlank(auditLog.getSourceFonds(), ctx.sourceFonds()));
        auditLog.setTargetFonds(firstNonBlank(auditLog.getTargetFonds(), ctx.targetFonds()));
        auditLog.setAuthTicketId(firstNonBlank(auditLog.getAuthTicketId(), ctx.authTicketId()));
        return ctx;
    }

    public String toMaskedAuditJson(Object source) {
        if (source == null) return null;
        try {
            return objectMapper.writeValueAsString(helper.sanitize(source, null));
        } catch (Exception e) {
            return "<serialization_error>";
        }
    }

    public String buildDataSnapshot(String before, String after, AuditRequestContext ctx, String sf, String tf, String tid) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("before", readJsonSilently(before));
        envelope.put("after", readJsonSilently(after));
        Map<String, Object> rt = new LinkedHashMap<>();
        rt.put("traceId", ctx.traceId()); rt.put("sourceFonds", firstNonBlank(sf, ctx.sourceFonds()));
        rt.put("targetFonds", firstNonBlank(tf, ctx.targetFonds())); rt.put("authTicketId", tid);
        rt.put("clientIp", ctx.clientIp()); rt.put("macAddress", ctx.macAddress());
        envelope.put("context", rt);
        try { return objectMapper.writeValueAsString(envelope); } catch (Exception e) { return "{\"error\":\"serialization\"}"; }
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void saveAuditLogWithHash(SysAuditLog auditLog) {
        if (auditLog.getCreatedTime() == null) auditLog.setCreatedTime(LocalDateTime.now());
        populateHashChainFields(auditLog);
        auditLogMapper.insert(auditLog);
    }
    
    public LogChainVerifyResult verifyLogChain(LocalDate start, LocalDate end) {
        List<SysAuditLog> logs = auditLogMapper.findByDateRange(start, end);
        if (logs.isEmpty()) return LogChainVerifyResult.builder().valid(true).totalLogs(0).build();
        int verified = 0; StringBuilder errors = new StringBuilder();
        for (int i = 0; i < logs.size(); i++) {
            SysAuditLog cur = logs.get(i);
            if (i > 0 && cur.getPrevLogHash() != null && !logs.get(i - 1).getLogHash().equals(cur.getPrevLogHash()))
                errors.append("链条断裂: ").append(cur.getId()).append("\n");
            String payload = buildLogChainPayload(cur.getUserId(), cur.getAction(), cur.getObjectDigest(), 
                cur.getCreatedTime() != null ? cur.getCreatedTime().format(TIME_FORMATTER) : null, cur.getPrevLogHash());
            if (cur.getLogHash() != null && !cur.getLogHash().equals(sm3Utils.hmac(auditLogHmacKey, payload)))
                errors.append("篡改: ").append(cur.getId()).append("\n");
            else verified++;
        }
        return LogChainVerifyResult.builder().valid(errors.length() == 0).totalLogs(logs.size()).verifiedLogs(verified).message(errors.toString()).build();
    }
    
    @lombok.Builder @lombok.Data
    public static class LogChainVerifyResult { private boolean valid; private int totalLogs; private int verifiedLogs; private String message; }

    private String buildLogChainPayload(String u, String a, String d, String t, String p) {
        return (u != null ? u : "") + "|" + (a != null ? a : "") + "|" + (d != null ? d : "") + "|" + (t != null ? t : "") + "|" + (p != null ? p : "");
    }

    private Object readJsonSilently(String json) {
        if (json == null || json.isBlank() || json.startsWith("<")) return json;
        try { return objectMapper.readValue(json, Object.class); } catch (Exception e) { return json; }
    }

    private String firstNonBlank(String... vs) {
        for (String v : vs) if (v != null && !v.isBlank() && !"null".equals(v)) return v.trim();
        return null;
    }

    public record AuditRequestContext(String traceId, String sourceFonds, String targetFonds, String authTicketId, String clientIp, String macAddress, String macSource, boolean shouldElevateRisk, String userAgent, String deviceFingerprint) {}

    private void populateHashChainFields(SysAuditLog auditLog) {
        try {
            String prev = auditLogMapper.getLatestLogHash();
            auditLog.setPrevLogHash(prev);
            String payload = buildLogChainPayload(auditLog.getUserId(), auditLog.getAction(), auditLog.getObjectDigest(), 
                auditLog.getCreatedTime() != null ? auditLog.getCreatedTime().format(TIME_FORMATTER) : null, prev);
            auditLog.setLogHash(sm3Utils.hmac(auditLogHmacKey, payload));
        } catch (Exception ex) {
            log.warn("哈希链计算失败: {}", auditLog.getId());
        }
    }
}
