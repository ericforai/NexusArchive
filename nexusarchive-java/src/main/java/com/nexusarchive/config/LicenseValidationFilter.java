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

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/auth",
            "/api/license/load",
            "/api/license",
            "/api/health",
            "/api/health/self-check"
    );

    private final LicenseService licenseService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // DEV MODE: 临时禁用 License 验证以便开发调试
        // TODO: 生产环境需要删除此行并恢复正常验证
        if (true) return true;
        
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
