// Input: HTTP 请求头/属性
// Output: FondsContext 注入与清理
// Pos: NexusCore Web 过滤器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FondsContextFilter extends OncePerRequestFilter {
    private static final String HEADER_NAME = "X-Fonds-No";
    private static final String ALT_HEADER_NAME = "X-FondsNo";
    private static final String ATTRIBUTE_NAME = "fonds_no";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String fondsNo = resolveFondsNo(request);
        try {
            if (fondsNo != null) {
                FondsContext.setFondsNo(fondsNo);
                FondsContext.requireFondsNo();
            }
            filterChain.doFilter(request, response);
        } finally {
            FondsContext.clear();
        }
    }

    private String resolveFondsNo(HttpServletRequest request) {
        String header = firstNonBlank(
                request.getHeader(HEADER_NAME),
                request.getHeader(ALT_HEADER_NAME));
        if (header != null) {
            return normalize(header);
        }
        Object attribute = request.getAttribute(ATTRIBUTE_NAME);
        if (attribute instanceof String) {
            return normalize((String) attribute);
        }
        return null;
    }

    private String normalize(String value) {
        String trimmed = Optional.ofNullable(value).orElse("").trim();
        return trimmed.isEmpty() ? null : trimmed;
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
}
