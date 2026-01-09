// Input: Spring Framework、Jakarta Servlet、Java 标准库
// Output: RequestContextFilter 类
// Pos: 配置层 - 请求上下文过滤器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求上下文过滤器
 * <p>
 * 在请求进入时注入 requestId 到 MDC，在请求结束时清理 MDC。
 * </p>
 * <p>
 * 执行顺序：
 * <ol>
 *   <li>优先级最高 (HIGHEST_PRECEDENCE)，确保最先执行</li>
 *   <li>从请求头 X-Request-ID 读取或生成新的 requestId</li>
 *   <li>请求结束时清理 MDC，避免线程池复用问题</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component("nexusRequestContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestId = extractOrGenerateRequestId(request);

        // 设置 MDC
        RequestContext.setRequestId(requestId);

        // 将 requestId 写入响应头，便于客户端追踪
        response.setHeader(RequestContext.REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC，避免线程池复用导致的数据污染
            RequestContext.clear();
        }
    }

    /**
     * 从请求头提取或生成新的请求追踪 ID
     *
     * @param request HTTP 请求
     * @return 请求追踪 ID
     */
    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(RequestContext.REQUEST_ID_HEADER);

        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = RequestContext.generateRequestId();
        } else {
            // 清理客户端传入的 requestId，防止注入攻击
            requestId = requestId.replaceAll("[^a-zA-Z0-9\\-]", "");
        }

        return requestId;
    }
}
