// Input: Jackson、Jakarta EE、Lombok、Spring Framework、等
// Output: LicenseValidationFilter 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
     * [FIXED P1-1] 使用精确匹配，防止前缀绕过
     */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            // /api 前缀路径 - 精确匹配
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/license/load",  // [FIXED] 仅允许 /load 端点
            "/api/health",
            "/api/health/self-check",
            // 非 /api 前缀路径
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/license/load",
            "/health"
    );

    private final LicenseService licenseService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // OPTIONS 请求（CORS 预检）不需要 License 检查
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        
        // [FIXED P1-1] 使用精确匹配
        return EXCLUDED_PATHS.contains(path);
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
