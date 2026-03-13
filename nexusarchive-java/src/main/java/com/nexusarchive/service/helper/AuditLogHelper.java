package com.nexusarchive.service.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.service.AuditLogService.AuditRequestContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogHelper {

    private final ObjectMapper objectMapper;
    private static final String UNKNOWN = "UNKNOWN";
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "secret", "token", "authorization", "credential", "phone", "email"
    );

    public AuditRequestContext resolveRequestContext() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) return new AuditRequestContext(UUID.randomUUID().toString().replace("-", ""), null, null, null, UNKNOWN, UNKNOWN, "NO_REQUEST", true, null, null);

        String mac = resolveMac(request);
        return new AuditRequestContext(
                resolveTraceId(request),
                firstNonBlank(String.valueOf(request.getAttribute("source_fonds")), request.getHeader("X-Current-Fonds-No")),
                firstNonBlank(String.valueOf(request.getAttribute("target_fonds")), request.getHeader("X-Target-Fonds-No"), request.getParameter("fondsNo")),
                firstNonBlank(String.valueOf(request.getAttribute("auth_ticket_id")), request.getHeader("X-Auth-Ticket-Id")),
                resolveIp(request), mac, "HEADER", false, request.getHeader("User-Agent"), null
        );
    }

    public Object sanitize(Object value, String field) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> s = new LinkedHashMap<>();
            map.forEach((k, v) -> s.put(String.valueOf(k), sanitize(v, String.valueOf(k))));
            return s;
        }
        if (value instanceof Iterable<?> it) {
            List<Object> s = new ArrayList<>();
            it.forEach(i -> s.add(sanitize(i, field)));
            return s;
        }
        if (value instanceof CharSequence seq) return shouldMask(field) ? mask(seq.toString()) : seq.toString();
        return value;
    }

    private boolean shouldMask(String field) {
        if (field == null) return false;
        String n = field.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(n::contains);
    }

    private String mask(String v) {
        if (v == null || v.length() <= 4) return "****";
        return v.substring(0, 2) + "****" + v.substring(v.length() - 2);
    }

    private String firstNonBlank(String... vs) {
        for (String v : vs) if (v != null && !v.isBlank() && !"null".equals(v)) return v.trim();
        return null;
    }

    private HttpServletRequest getHttpServletRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return null;
        return attrs.getRequest();
    }

    private String resolveIp(HttpServletRequest r) {
        String ip = r.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = r.getRemoteAddr();
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String resolveMac(HttpServletRequest r) {
        String m = r.getHeader("X-Client-Mac");
        return (m != null && m.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")) ? m : UNKNOWN;
    }

    private String resolveTraceId(HttpServletRequest r) {
        String t = r.getHeader("X-Trace-Id");
        return t != null ? t : UUID.randomUUID().toString().replace("-", "");
    }
}
