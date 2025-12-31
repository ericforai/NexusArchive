// Input: HTTP 请求头/属性
// Output: ArchiveYearContext 注入与清理
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
public class ArchiveYearContextFilter extends OncePerRequestFilter {
    private static final String HEADER_NAME = "X-Archive-Year";
    private static final String ALT_HEADER_NAME = "X-ArchiveYear";
    private static final String ATTRIBUTE_NAME = "archive_year";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Integer archiveYear = resolveArchiveYear(request);
        if (archiveYear != null) {
            ArchiveYearContext.setArchiveYear(archiveYear);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            ArchiveYearContext.clear();
        }
    }

    private Integer resolveArchiveYear(HttpServletRequest request) {
        String header = firstNonBlank(
                request.getHeader(HEADER_NAME),
                request.getHeader(ALT_HEADER_NAME));
        if (header != null) {
            return parseYear(header, "archive_year header");
        }
        Object attribute = request.getAttribute(ATTRIBUTE_NAME);
        if (attribute instanceof Integer) {
            return (Integer) attribute;
        }
        if (attribute instanceof String) {
            return parseYear((String) attribute, "archive_year attribute");
        }
        return null;
    }

    private Integer parseYear(String value, String source) {
        String trimmed = Optional.ofNullable(value).orElse("").trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            throw new FondsIsolationException("Invalid " + source);
        }
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
