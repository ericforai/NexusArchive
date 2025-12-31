// Input: Spring Web、FondsScopeService、SecurityContext
// Output: FondsContextFilter 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.FondsScopeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FondsContextFilter extends OncePerRequestFilter {

    private static final String CURRENT_FONDS_ATTRIBUTE = "current_fonds_no";
    private static final String ALLOWED_FONDS_ATTRIBUTE = "allowed_fonds";
    private static final String HEADER_NAME = "X-Fonds-No";
    private static final String ALT_HEADER_NAME = "X-FondsNo";

    private final FondsScopeService fondsScopeService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String userId = resolveUserId(request);
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        List<String> allowedFonds = fondsScopeService.getAllowedFonds(userId);
        if (allowedFonds.isEmpty()) {
            deny(response, "当前用户未配置任何可访问全宗");
            return;
        }

        String currentFonds = resolveCurrentFonds(request, allowedFonds);
        if (currentFonds == null) {
            deny(response, "未授权的全宗访问");
            return;
        }

        request.setAttribute(CURRENT_FONDS_ATTRIBUTE, currentFonds);
        request.setAttribute(ALLOWED_FONDS_ATTRIBUTE, List.copyOf(allowedFonds));

        FondsContext.setCurrentFondsNo(currentFonds);
        FondsContext.setAllowedFonds(allowedFonds);
        try {
            filterChain.doFilter(request, response);
        } finally {
            FondsContext.clear();
        }
    }

    private String resolveCurrentFonds(HttpServletRequest request, List<String> allowedFonds) {
        Object existing = request.getAttribute(CURRENT_FONDS_ATTRIBUTE);
        if (existing instanceof String existingValue && !existingValue.isBlank()) {
            return allowedFonds.contains(existingValue.trim()) ? existingValue.trim() : null;
        }

        String header = firstNonBlank(request.getHeader(HEADER_NAME), request.getHeader(ALT_HEADER_NAME));
        if (header != null) {
            String trimmed = header.trim();
            return allowedFonds.contains(trimmed) ? trimmed : null;
        }

        if (allowedFonds.size() == 1) {
            return allowedFonds.get(0);
        }

        return null;
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
        return null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private void deny(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":403,\"message\":\"%s\"}", message));
    }
}
