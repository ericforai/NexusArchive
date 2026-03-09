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

    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String UNKNOWN = "UNKNOWN";
    private static final Set<String> SENSITIVE_FIELD_KEYWORDS = Set.of(
            "password", "passwd", "secret", "token", "authorization", "credential",
            "idcard", "id_no", "idnumber", "certno", "bankcard", "accountno",
            "phone", "mobile", "email", "mac", "devicefingerprint",
            "authticket", "approvalticket"
    );

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
        auditLog.setMacAddress(UNKNOWN);
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLog.setRiskLevel("LOW");
        enrichWithRequestContext(auditLog);
        
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
        enrichWithRequestContext(auditLog);
        
        saveAuditLogWithHash(auditLog);
    }

    /**
     * 记录关键业务链路的 before/after 审计快照。
     */
    @Async
    public void logBusinessSnapshot(String userId, String username, String action,
                                    String resourceType, String resourceId, String result,
                                    String details, String riskLevel,
                                    Object beforeSnapshot, Object afterSnapshot,
                                    String sourceFonds, String targetFonds, String authTicketId) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(UUID.randomUUID().toString().replace("-", ""));
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOperationResult(result);
        auditLog.setDetails(details);
        auditLog.setCreatedTime(LocalDateTime.now());

        AuditRequestContext requestContext = enrichWithRequestContext(auditLog);
        auditLog.setRiskLevel(resolveRiskLevel(riskLevel, requestContext.shouldElevateRisk()));
        auditLog.setSourceFonds(firstNonBlank(sourceFonds, auditLog.getSourceFonds()));
        auditLog.setTargetFonds(firstNonBlank(targetFonds, auditLog.getTargetFonds()));
        auditLog.setAuthTicketId(firstNonBlank(authTicketId, auditLog.getAuthTicketId()));
        auditLog.setDataBefore(toMaskedAuditJson(beforeSnapshot));
        auditLog.setDataAfter(toMaskedAuditJson(afterSnapshot));
        auditLog.setDataSnapshot(buildDataSnapshot(auditLog.getDataBefore(), auditLog.getDataAfter(), requestContext,
                auditLog.getSourceFonds(), auditLog.getTargetFonds(), auditLog.getAuthTicketId()));

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
        auditLog.setMacAddress(UNKNOWN);
        auditLog.setTraceId(traceId);
        auditLog.setSourceFonds(sourceFonds);
        auditLog.setTargetFonds(targetFonds);
        auditLog.setAuthTicketId(ticketId);
        auditLog.setCreatedTime(LocalDateTime.now());
        auditLog.setRiskLevel("MEDIUM");
        AuditRequestContext requestContext = enrichWithRequestContext(auditLog);
        auditLog.setDataSnapshot(buildDataSnapshot(null, null, requestContext,
                auditLog.getSourceFonds(), auditLog.getTargetFonds(), auditLog.getAuthTicketId()));
        
        saveAuditLogWithHash(auditLog);
    }

    /**
     * 从当前请求补齐审计上下文。
     */
    public AuditRequestContext enrichWithRequestContext(SysAuditLog auditLog) {
        AuditRequestContext context = resolveRequestContext();
        auditLog.setClientIp(firstNonBlank(auditLog.getClientIp(), context.clientIp()));
        auditLog.setMacAddress(firstNonBlank(auditLog.getMacAddress(), context.macAddress()));
        auditLog.setUserAgent(firstNonBlank(auditLog.getUserAgent(), context.userAgent()));
        auditLog.setDeviceFingerprint(firstNonBlank(auditLog.getDeviceFingerprint(), context.deviceFingerprint()));
        auditLog.setTraceId(firstNonBlank(auditLog.getTraceId(), context.traceId()));
        auditLog.setSourceFonds(firstNonBlank(auditLog.getSourceFonds(), context.sourceFonds()));
        auditLog.setTargetFonds(firstNonBlank(auditLog.getTargetFonds(), context.targetFonds()));
        auditLog.setAuthTicketId(firstNonBlank(auditLog.getAuthTicketId(), context.authTicketId()));
        return context;
    }

    public String toMaskedAuditJson(Object source) {
        if (source == null) {
            return null;
        }
        try {
            Object sanitized = sanitizeValue(source, null);
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            log.warn("审计快照序列化失败，使用占位符输出", e);
            return "<serialization_error>";
        } catch (IllegalArgumentException e) {
            log.warn("审计快照脱敏失败，使用占位符输出", e);
            return "<sanitize_error>";
        }
    }

    public String buildDataSnapshot(String beforeJson, String afterJson, AuditRequestContext context,
                                    String sourceFonds, String targetFonds, String authTicketId) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("before", readJsonSilently(beforeJson));
        envelope.put("after", readJsonSilently(afterJson));

        Map<String, Object> runtimeContext = new LinkedHashMap<>();
        runtimeContext.put("traceId", context.traceId());
        runtimeContext.put("sourceFonds", firstNonBlank(sourceFonds, context.sourceFonds()));
        runtimeContext.put("targetFonds", firstNonBlank(targetFonds, context.targetFonds()));
        runtimeContext.put("authTicketId", maskValue(firstNonBlank(authTicketId, context.authTicketId())));
        runtimeContext.put("clientIp", maskValue(context.clientIp()));
        runtimeContext.put("macAddress", maskValue(context.macAddress()));
        runtimeContext.put("macSource", context.macSource());
        runtimeContext.put("macFallback", context.shouldElevateRisk());
        envelope.put("context", runtimeContext);

        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.warn("审计上下文快照序列化失败", e);
            return "{\"context\":\"<serialization_error>\"}";
        }
    }
    
    /**
     * 保存审计日志并计算哈希链
     *
     * 哈希链机制：
     * 1. 获取前一条日志的哈希值
     * 2. 计算当前日志哈希: SM3(operatorId + operationType + objectDigest + createdTime + prevHash)
     * 3. 存储当前日志和哈希值
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAuditLogWithHash(SysAuditLog auditLog) {
        if (auditLog.getCreatedTime() == null) {
            auditLog.setCreatedTime(LocalDateTime.now());
        }
        if (auditLog.getMacAddress() == null || auditLog.getMacAddress().isBlank()) {
            auditLog.setMacAddress(UNKNOWN);
        }
        if (auditLog.getClientIp() == null || auditLog.getClientIp().isBlank()) {
            auditLog.setClientIp(UNKNOWN);
        }

        populateHashChainFields(auditLog);
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

    private AuditRequestContext resolveRequestContext() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            String generatedTraceId = UUID.randomUUID().toString().replace("-", "");
            return new AuditRequestContext(generatedTraceId, null, null, null,
                    UNKNOWN, UNKNOWN, "NO_REQUEST", true, null, null);
        }

        MacAddressResolution mac = resolveMacAddress(request);
        return new AuditRequestContext(
                resolveTraceId(request),
                firstNonBlank(stringAttribute(request, "source_fonds"), request.getHeader("X-Current-Fonds-No")),
                firstNonBlank(stringAttribute(request, "target_fonds"),
                        request.getHeader("X-Target-Fonds-No"), request.getParameter("fondsNo")),
                firstNonBlank(stringAttribute(request, "auth_ticket_id"),
                        request.getHeader("X-Auth-Ticket-Id"), request.getParameter("authTicketId")),
                resolveClientIp(request),
                mac.macAddress(),
                mac.source(),
                mac.shouldElevateRisk(),
                request.getHeader("User-Agent"),
                resolveDeviceFingerprint(request)
        );
    }

    private HttpServletRequest getHttpServletRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String resolveTraceId(HttpServletRequest request) {
        return firstNonBlank(
                request.getHeader("X-Trace-Id"),
                stringAttribute(request, "trace_id"),
                UUID.randomUUID().toString().replace("-", "")
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null || ip.isBlank() ? UNKNOWN : ip;
    }

    private String resolveDeviceFingerprint(HttpServletRequest request) {
        StringBuilder fingerprint = new StringBuilder();
        appendFingerprintPart(fingerprint, request.getHeader("User-Agent"));
        appendFingerprintPart(fingerprint, request.getHeader("Accept-Language"));
        appendFingerprintPart(fingerprint, request.getHeader("Accept-Encoding"));
        appendFingerprintPart(fingerprint, request.getHeader("X-Device-Fingerprint"));
        String value = fingerprint.toString();
        return value.isBlank() ? null : value;
    }

    private void appendFingerprintPart(StringBuilder builder, String value) {
        if (builder.length() > 0) {
            builder.append('|');
        }
        builder.append(value == null ? "" : value.trim());
    }

    private MacAddressResolution resolveMacAddress(HttpServletRequest request) {
        String primary = request.getHeader("X-Client-Mac");
        if (isValidMacAddress(primary)) {
            return new MacAddressResolution(primary.toUpperCase(Locale.ROOT), "X-Client-Mac", false);
        }

        String[] fallbackHeaders = {"X-MAC-Address", "X-Device-Mac", "Client-MAC"};
        for (String header : fallbackHeaders) {
            String value = request.getHeader(header);
            if (isValidMacAddress(value)) {
                return new MacAddressResolution(value.toUpperCase(Locale.ROOT), header, false);
            }
        }
        return new MacAddressResolution(UNKNOWN, "HEADER_FALLBACK", true);
    }

    private boolean isValidMacAddress(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) {
            return false;
        }
        return macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }

    private Object sanitizeValue(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                String key = entry.getKey() == null ? "null" : entry.getKey().toString();
                sanitized.put(key, sanitizeValue(entry.getValue(), key));
            }
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new java.util.ArrayList<>();
            for (Object item : iterable) {
                sanitized.add(sanitizeValue(item, fieldName));
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> sanitized = new java.util.ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                sanitized.add(sanitizeValue(java.lang.reflect.Array.get(value, i), fieldName));
            }
            return sanitized;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof CharSequence sequence) {
            return shouldMask(fieldName) ? maskValue(sequence.toString()) : sequence.toString();
        }
        if (value instanceof Enum<?>) {
            return value.toString();
        }

        Map<String, Object> converted = objectMapper.convertValue(value, new TypeReference<LinkedHashMap<String, Object>>() {});
        return sanitizeValue(converted, fieldName);
    }

    private boolean shouldMask(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String normalized = fieldName.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_FIELD_KEYWORDS.stream()
                .map(keyword -> keyword.replace("_", ""))
                .anyMatch(normalized::contains);
    }

    private String maskValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return rawValue;
        }
        String trimmed = rawValue.trim();
        if (UNKNOWN.equalsIgnoreCase(trimmed)) {
            return UNKNOWN;
        }
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, 2) + "****" + trimmed.substring(trimmed.length() - 2);
    }

    private Object readJsonSilently(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        if (json.startsWith("<")) {
            return json;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    private String resolveRiskLevel(String requestedRiskLevel, boolean elevateRisk) {
        if (requestedRiskLevel != null && !requestedRiskLevel.isBlank()) {
            return requestedRiskLevel.toUpperCase(Locale.ROOT);
        }
        return elevateRisk ? "MEDIUM" : "LOW";
    }

    private String stringAttribute(HttpServletRequest request, String name) {
        Object attribute = request.getAttribute(name);
        return attribute == null ? null : attribute.toString();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record AuditRequestContext(String traceId,
                                      String sourceFonds,
                                      String targetFonds,
                                      String authTicketId,
                                      String clientIp,
                                      String macAddress,
                                      String macSource,
                                      boolean shouldElevateRisk,
                                      String userAgent,
                                      String deviceFingerprint) {
    }

    private record MacAddressResolution(String macAddress, String source, boolean shouldElevateRisk) {
    }

    private void populateHashChainFields(SysAuditLog auditLog) {
        try {
            String prevHash = auditLogMapper.getLatestLogHash();
            auditLog.setPrevLogHash(prevHash);

            String createdTimeStr = auditLog.getCreatedTime() != null
                    ? auditLog.getCreatedTime().format(TIME_FORMATTER)
                    : null;
            String payload = buildLogChainPayload(
                    auditLog.getUserId(),
                    auditLog.getAction(),
                    auditLog.getObjectDigest(),
                    createdTimeStr,
                    prevHash
            );

            auditLog.setLogHash(sm3Utils.hmac(auditLogHmacKey, payload));
        } catch (Exception ex) {
            auditLog.setPrevLogHash(null);
            auditLog.setLogHash(null);
            log.warn("审计日志哈希链计算失败，将以降级模式写入日志: userId={}, action={}",
                    auditLog.getUserId(), auditLog.getAction(), ex);
        }
    }
}
