package com.nexusarchive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS 跨站脚本攻击过滤器
 * 安全加固 - 过滤危险的 XSS 字符和脚本
 */
@Component
@Order(2)
@Slf4j
public class XssFilter extends OncePerRequestFilter {

    @Value("${security.xss.enabled:true}")
    private boolean enabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(new XssRequestWrapper(request), response);
    }

    /**
     * XSS 请求包装器
     * 对所有请求参数进行 XSS 过滤
     */
    public static class XssRequestWrapper extends HttpServletRequestWrapper {

        // XSS 攻击模式
        private static final Pattern[] XSS_PATTERNS = {
            // Script 标签
            Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // eval 表达式
            Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // expression 表达式
            Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // javascript: 协议
            Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
            
            // vbscript: 协议
            Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
            
            // data: 协议（可用于 XSS）
            Pattern.compile("data:\\s*text/html", Pattern.CASE_INSENSITIVE),
            
            // onXXX 事件处理器
            Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
            
            // src 属性可能加载恶意内容
            Pattern.compile("src\\s*=\\s*['\"]\\s*javascript:", Pattern.CASE_INSENSITIVE),
            
            // iframe 标签
            Pattern.compile("<iframe(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // object 标签
            Pattern.compile("<object(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // embed 标签
            Pattern.compile("<embed(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            
            // base 标签（可用于重定向）
            Pattern.compile("<base\\s+href", Pattern.CASE_INSENSITIVE),
            
            // form action 劫持
            Pattern.compile("<form(.*?)action(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
        };

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }

            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = sanitize(values[i]);
            }
            return sanitizedValues;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return sanitize(value);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            // Cookie 和 Authorization 头不过滤（可能包含特殊字符）
            if ("Cookie".equalsIgnoreCase(name) || "Authorization".equalsIgnoreCase(name)) {
                return value;
            }
            return sanitize(value);
        }

        /**
         * 过滤 XSS 攻击字符
         */
        private String sanitize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            String sanitized = value;
            for (Pattern pattern : XSS_PATTERNS) {
                sanitized = pattern.matcher(sanitized).replaceAll("");
            }

            // HTML 实体编码特殊字符
            sanitized = sanitized
                .replace("<", "&lt;")
                .replace(">", "&gt;");

            // 如果内容被修改，记录警告
            if (!sanitized.equals(value)) {
                // 避免在日志中记录原始恶意内容
                log.warn("🛡️ 检测到潜在 XSS 攻击，已过滤危险内容");
            }

            return sanitized;
        }
    }
}
