package com.nexusarchive.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.service.LicenseService;
import com.nexusarchive.common.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LicenseValidationFilter extends OncePerRequestFilter {

    /**
     * 白名单路径 - 这些路径不需要 License 校验
     * 
     * 【修复】同时支持 /api 前缀和非前缀路径，确保登录和激活流程可用
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            // /api 前缀路径
            "/api/auth",
            "/api/license/load",
            "/api/license",
            "/api/health",
            "/api/health/self-check",
            // 非 /api 前缀路径（部分前端/网关可能直接访问）
            "/auth",
            "/license/load",
            "/license",
            "/health"
    );

    private final LicenseService licenseService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            int activeUsers = userMapper.countActiveUsers();
            licenseService.assertValid(activeUsers);
            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(Result.forbidden(e.getMessage())));
        }
    }
}
