// Input: Spring Web、AuthTicketValidationService、AuditLogService、AuthScope
// Output: CrossFondsAccessInterceptor 拦截器类
// Pos: Web 配置/拦截器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketValidationResult;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.AuthTicketValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 跨全宗访问拦截器
 * 
 * 功能：
 * 1. 检测跨全宗访问请求
 * 2. 验证授权票据
 * 3. 将票据信息存入请求上下文，供后续审计日志使用
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossFondsAccessInterceptor implements HandlerInterceptor {
    
    private final AuthTicketValidationService authTicketValidationService;
    private final AuditLogService auditLogService;
    
    // 请求头名称
    private static final String AUTH_TICKET_HEADER = "X-Auth-Ticket-Id";
    private static final String TARGET_FONDS_PARAM = "fondsNo";
    private static final String TARGET_FONDS_HEADER = "X-Target-Fonds-No";
    private static final String CURRENT_FONDS_ATTRIBUTE = "current_fonds_no";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                           Object handler) throws Exception {
        // 1. 提取目标全宗号（从请求参数或 Header）
        String targetFonds = extractTargetFonds(request);
        String currentFonds = getCurrentFonds(request);
        
        // 2. 判断是否为跨全宗访问
        if (targetFonds != null && !targetFonds.equals(currentFonds)) {
            log.debug("检测到跨全宗访问: currentFonds={}, targetFonds={}", currentFonds, targetFonds);
            
            // 3. 提取授权票据ID（从 Header 或请求参数）
            String ticketId = extractAuthTicketId(request);
            
            if (ticketId == null || ticketId.isEmpty()) {
                log.warn("跨全宗访问未提供授权票据: targetFonds={}, uri={}", 
                        targetFonds, request.getRequestURI());
                recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "FAILURE",
                        "跨全宗访问必须提供授权票据");
                writeForbidden(response, "跨全宗访问必须提供授权票据");
                return false;
            }
            
            // 4. 提取访问范围（从请求参数）
            AuthScope accessScope = extractAccessScope(request);
            
            // 5. 验证授权票据
            AuthTicketValidationResult result = authTicketValidationService
                .validateTicket(ticketId, targetFonds, accessScope);
            
            if (!result.isValid()) {
                log.warn("授权票据验证失败: ticketId={}, reason={}", ticketId, result.getReason());
                recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "FAILURE",
                        "授权票据无效: " + result.getReason());
                writeForbidden(response, "授权票据无效: " + result.getReason());
                return false;
            }

            // 5.1 校验票据源全宗与当前全宗一致
            if (currentFonds == null || currentFonds.isBlank()) {
                recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "FAILURE",
                        "无法识别当前全宗");
                writeForbidden(response, "无法识别当前全宗");
                return false;
            }
            if (result.getSourceFonds() != null && !result.getSourceFonds().equals(currentFonds)) {
                recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "FAILURE",
                        "授权票据源全宗与当前全宗不一致");
                writeForbidden(response, "授权票据源全宗与当前全宗不一致");
                return false;
            }

            // 5.2 校验票据申请人与当前用户一致（默认严格）
            String currentUserId = resolveUserId(request);
            if (currentUserId != null && result.getApplicantId() != null
                    && !currentUserId.equals(result.getApplicantId())) {
                recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "FAILURE",
                        "授权票据申请人与当前用户不一致");
                writeForbidden(response, "授权票据申请人与当前用户不一致");
                return false;
            }
            
            // 6. 将票据信息存入请求上下文，供后续审计日志使用
            request.setAttribute("auth_ticket_id", ticketId);
            request.setAttribute("auth_ticket_info", result);
            request.setAttribute("cross_fonds_access", true);
            request.setAttribute("source_fonds", result.getSourceFonds());
            request.setAttribute("target_fonds", result.getTargetFonds());
            
            recordCrossFondsAudit(request, currentFonds, targetFonds, ticketId, "SUCCESS", "授权票据验证通过");
            log.debug("跨全宗访问授权票据验证通过: ticketId={}, sourceFonds={}, targetFonds={}", 
                    ticketId, result.getSourceFonds(), result.getTargetFonds());
        }
        
        return true;
    }
    
    /**
     * 提取目标全宗号
     */
    private String extractTargetFonds(HttpServletRequest request) {
        // 优先从 Header 获取
        String targetFonds = request.getHeader(TARGET_FONDS_HEADER);
        if (targetFonds != null && !targetFonds.isEmpty()) {
            return targetFonds;
        }
        
        // 从请求参数获取
        targetFonds = request.getParameter(TARGET_FONDS_PARAM);
        if (targetFonds != null && !targetFonds.isEmpty()) {
            return targetFonds;
        }
        
        return null;
    }
    
    /**
     * 获取当前用户所属全宗号
     */
    private String getCurrentFonds(HttpServletRequest request) {
        String currentFonds = (String) request.getAttribute(CURRENT_FONDS_ATTRIBUTE);
        if (currentFonds != null && !currentFonds.isEmpty()) {
            return currentFonds;
        }
        
        // 如果无法获取，返回 null（表示无法判断是否为跨全宗访问）
        return null;
    }
    
    /**
     * 提取授权票据ID
     */
    private String extractAuthTicketId(HttpServletRequest request) {
        // 优先从 Header 获取
        String ticketId = request.getHeader(AUTH_TICKET_HEADER);
        if (ticketId != null && !ticketId.isEmpty()) {
            return ticketId;
        }
        
        // 从请求参数获取
        ticketId = request.getParameter("authTicketId");
        if (ticketId != null && !ticketId.isEmpty()) {
            return ticketId;
        }
        
        return null;
    }
    
    /**
     * 提取访问范围
     */
    private AuthScope extractAccessScope(HttpServletRequest request) {
        // 从请求参数中提取访问范围
        AuthScope scope = new AuthScope();
        
        // 归档年度
        String archiveYearsParam = request.getParameter("archiveYears");
        if (archiveYearsParam != null && !archiveYearsParam.isEmpty()) {
            try {
                String[] years = archiveYearsParam.split(",");
                scope.setArchiveYears(java.util.Arrays.stream(years)
                    .map(Integer::parseInt)
                    .collect(java.util.stream.Collectors.toList()));
            } catch (Exception e) {
                log.warn("解析归档年度失败: {}", archiveYearsParam, e);
            }
        }
        
        // 档案类型
        String docTypesParam = request.getParameter("docTypes");
        if (docTypesParam != null && !docTypesParam.isEmpty()) {
            scope.setDocTypes(java.util.Arrays.asList(docTypesParam.split(",")));
        }
        
        // 关键词
        String keywordsParam = request.getParameter("keywords");
        if (keywordsParam != null && !keywordsParam.isEmpty()) {
            scope.setKeywords(java.util.Arrays.asList(keywordsParam.split(",")));
        }
        
        // 访问类型
        String accessType = request.getParameter("accessType");
        if (accessType != null && !accessType.isEmpty()) {
            scope.setAccessType(accessType);
        }
        
        return scope;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":403,\"message\":\"%s\"}", message));
    }

    private void recordCrossFondsAudit(HttpServletRequest request, String sourceFonds, String targetFonds,
                                       String ticketId, String result, String message) {
        String userId = resolveUserId(request);
        String username = resolveUsername();
        String resourceId = resolveResourceId(request);
        String clientIp = resolveClientIp(request);
        String traceId = resolveTraceId(request);
        auditLogService.logCrossFondsAccess(
                userId,
                username,
                sourceFonds,
                targetFonds,
                ticketId,
                "CROSS_FONDS_ACCESS",
                resourceId,
                result,
                clientIp,
                traceId,
                message
        );
    }

    private String resolveUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        return "UNKNOWN";
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            if (details.getFullName() != null && !details.getFullName().isBlank()) {
                return details.getFullName();
            }
            return details.getUsername();
        }
        return "UNKNOWN";
    }

    private String resolveResourceId(HttpServletRequest request) {
        String resourceId = request.getParameter("archiveId");
        if (resourceId != null && !resourceId.isBlank()) {
            return resourceId;
        }
        resourceId = request.getParameter("id");
        if (resourceId != null && !resourceId.isBlank()) {
            return resourceId;
        }
        return request.getRequestURI();
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
        return ip != null ? ip : "UNKNOWN";
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId != null && !traceId.isBlank()) {
            return traceId.trim();
        }
        Object attribute = request.getAttribute("trace_id");
        if (attribute != null) {
            return attribute.toString();
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
