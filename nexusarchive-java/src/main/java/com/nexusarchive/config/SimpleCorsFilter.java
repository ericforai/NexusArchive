package com.nexusarchive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 简单 CORS 过滤器
 * 继承 OncePerRequestFilter 以兼容 Spring Security FilterChain
 * 直接设置 CORS 响应头，绕过 Spring 的 CORS 验证机制
 */
@Slf4j
public class SimpleCorsFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:5173",
        "http://localhost:3000",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:3000"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String origin = request.getHeader("Origin");
        log.info("[SimpleCorsFilter] Request: {} {} Origin: {}", request.getMethod(), request.getRequestURI(), origin);
        
        // 设置 CORS 响应头
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With");
            response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Disposition");
            response.setHeader("Access-Control-Max-Age", "3600");
            log.info("[SimpleCorsFilter] CORS headers set for origin: {}", origin);
        }
        
        // 对 OPTIONS 预检请求直接返回 200
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.info("[SimpleCorsFilter] Handling OPTIONS preflight, returning 200");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(request, response);
    }
}
