// Input: Jackson、Jakarta EE、Lombok、org.aspectj、等
// Output: ArchivalAuditAspect 类
// Pos: 后端模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.LocalAuditBuffer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 档案审计切面
 * 
 * 自动拦截所有标记了 @ArchivalAudit 的方法
 * 记录操作日志到 sys_audit_log 表
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ArchivalAuditAspect {
    
    private final AuditLogService auditLogService;
    private final LocalAuditBuffer localAuditBuffer;
    private final ObjectMapper objectMapper;
    
    @Around("@annotation(com.nexusarchive.annotation.ArchivalAudit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ArchivalAudit auditAnnotation = method.getAnnotation(ArchivalAudit.class);
        
        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = getUsernameFromAuthentication(authentication);
        String userId = getUserIdFromRequest();
        
        // 获取请求信息
        HttpServletRequest request = getHttpServletRequest();
        String clientIp = getClientIp(request);
        
        // Initialize riskLevel here, as it can be modified before the main try-catch
        String operationResult = "SUCCESS";
        String riskLevel = "LOW";

        MacAddressResult macResult = getMacAddress(request); // [FIXED P1-1: 修复值传递问题]
        String macAddress = macResult.mac();
        if (macResult.shouldElevateRisk()) {
            riskLevel = "MEDIUM"; // MAC 地址未获取时提升风险等级
        }
        String userAgent = request != null ? request.getHeader("User-Agent") : "";
        
        // 记录操作前数据
        String dataBefore = null;
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            try {
                dataBefore = objectMapper.writeValueAsString(args[0]);
            } catch (JsonProcessingException e) {
                // 序列化失败不影响业务流程，记录警告并标记风险等级
                log.warn("操作前数据序列化失败，记录为 <serialization_error>", e);
                dataBefore = "<serialization_error>";
                riskLevel = "MEDIUM";
            } catch (Exception e) {
                log.warn("无法序列化操作前数据", e);
            }
        }
        
        // 执行目标方法
        Object result = null;
        
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            operationResult = "FAILURE";
            riskLevel = "HIGH";
            throw e;
        } finally {
            // 记录操作后数据
            String dataAfter = null;
            if (result != null) {
                try {
                    dataAfter = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    log.warn("无法序列化操作后数据", e);
                }
            }
            
            // 计算对象摘要(如果是文件操作)
            String objectDigest = extractObjectDigest(args);
            
            // 创建审计日志
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setAction(auditAnnotation.operationType());
            auditLog.setResourceType(auditAnnotation.resourceType());
            auditLog.setOperationResult(operationResult);
            auditLog.setRiskLevel(riskLevel);
            auditLog.setDetails(auditAnnotation.description());
            auditLog.setDataBefore(dataBefore);
            auditLog.setDataAfter(dataAfter);
            auditLog.setClientIp(clientIp);
            auditLog.setMacAddress(macAddress);
            auditLog.setObjectDigest(objectDigest);
            auditLog.setUserAgent(userAgent);
            auditLog.setDeviceFingerprint(getDeviceFingerprint(request));
            
            // 异步保存审计日志 [FIXED P0-1: 失败时写入本地缓冲]
            try {
                auditLogService.log(auditLog);
            } catch (Exception e) {
                log.error("保存审计日志失败，写入本地缓冲", e);
                localAuditBuffer.persist(auditLog);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("审计日志已记录: 用户={}, 操作={}, 结果={}, 耗时={}ms", 
                    username, auditAnnotation.operationType(), operationResult, duration);
        }
    }
    
    /**
     * 从请求中获取用户ID
     */
    private String getUserIdFromRequest() {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            Object userId = request.getAttribute("userId");
            if (userId != null) {
                return userId.toString();
            }
        }
        return "UNKNOWN";
    }
    
    /**
     * 获取 HttpServletRequest
     */
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况(取第一个)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip != null ? ip : "UNKNOWN";
    }
    
    /**
     * [FIXED P1-1] 尝试获取MAC地址
     * 
     * 优化：从请求头 X-Client-Mac 获取客户端传递的 MAC 地址
     * 注意: Web环境下通常无法直接获取客户端MAC地址，需要客户端配合传递
     * 
     * @return MacAddressResult 包含 MAC 地址和是否需要提升风险等级
     */
    private MacAddressResult getMacAddress(HttpServletRequest request) {
        if (request == null) {
            return new MacAddressResult("UNKNOWN", true);
        }
        
        // 首先尝试可信内部网络传递的 MAC 地址
        String macAddress = request.getHeader("X-Client-Mac");
        if (isValidMacAddress(macAddress)) {
            return new MacAddressResult(macAddress.toUpperCase(), false);
        }
        
        // 其他可能的 Header（仍需校验格式）
        String[] macHeaders = {"X-MAC-Address", "X-Device-Mac", "Client-MAC"};
        for (String header : macHeaders) {
            String mac = request.getHeader(header);
            if (isValidMacAddress(mac)) {
                return new MacAddressResult(mac.toUpperCase(), false);
            }
        }
        
        // 未获取到合法 MAC，返回 UNKNOWN 并标记需要提升风险等级
        return new MacAddressResult("UNKNOWN", true);
    }

    /**
     * [FIXED P1-1] MAC 地址获取结果
     */
    private record MacAddressResult(String mac, boolean shouldElevateRisk) {}
    
    /**
     * 验证 MAC 地址格式
     */
    private boolean isValidMacAddress(String mac) {
        if (mac == null || mac.isEmpty()) {
            return false;
        }
        // 匹配 XX:XX:XX:XX:XX:XX 或 XX-XX-XX-XX-XX-XX 格式
        return mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }
    
    /**
     * 获取设备指纹
     * 
     * 基于 User-Agent、Accept-Language、Accept-Encoding 等信息计算
     */
    private String getDeviceFingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        StringBuilder fingerprint = new StringBuilder();
        fingerprint.append(request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "");
        fingerprint.append("|");
        fingerprint.append(request.getHeader("Accept-Language") != null ? request.getHeader("Accept-Language") : "");
        fingerprint.append("|");
        fingerprint.append(request.getHeader("Accept-Encoding") != null ? request.getHeader("Accept-Encoding") : "");
        fingerprint.append("|");
        // 客户端可能传递的设备指纹
        String clientFingerprint = request.getHeader("X-Device-Fingerprint");
        if (clientFingerprint != null) {
            fingerprint.append(clientFingerprint);
        }
        
        // 如果有内容则返回，否则返回 null
        String fp = fingerprint.toString();
        return fp.equals("|||") ? null : fp;
    }
    
    /**
     * 从认证信息中提取用户名
     * 确保返回纯文本用户名，并限制长度在 255 以内
     */
    private String getUsernameFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            return "SYSTEM";
        }
        
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = authentication.getName();
        }
        
        // 防御性处理：防止任何非预期情况导致的超长字符
        if (username != null && username.length() > 255) {
            return username.substring(0, 252) + "...";
        }
        
        return username != null ? username : "UNKNOWN";
    }

    /**
     * 从方法参数中提取对象摘要(哈希值)
     */
    private String extractObjectDigest(Object[] args) {
        // 尝试从参数中提取 fixityValue 字段
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg != null) {
                    try {
                        Method getFixityValue = arg.getClass().getMethod("getFixityValue");
                        Object digest = getFixityValue.invoke(arg);
                        if (digest != null) {
                            return digest.toString();
                        }
                    } catch (Exception e) {
                        // 忽略,继续尝试下一个参数
                    }
                }
            }
        }
        return null;
    }
}
