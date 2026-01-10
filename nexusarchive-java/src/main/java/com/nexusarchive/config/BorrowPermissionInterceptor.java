// Input: Spring Web、Security、BorrowingFacade
// Output: BorrowPermissionInterceptor 拦截器类
// Pos: 配置层/拦截器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.modules.borrowing.app.BorrowingFacade;
import com.nexusarchive.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 借阅权限拦截器
 *
 * 功能：
 * 1. 检测 query_user 角色用户的档案访问请求
 * 2. 验证用户是否已获得借阅审批权限
 * 3. 拒绝未授权的档案访问
 *
 * PRD 来源: 2026-01-10-query-user-borrow-design.md 第5.2节
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowPermissionInterceptor implements HandlerInterceptor {

    private final BorrowingFacade borrowingFacade;

    // 需要进行权限检查的路径前缀
    private static final List<String> PROTECTED_PATH_PREFIXES = Arrays.asList(
            "/api/archives/",
            "/api/archive/"
    );

    // 下载接口路径（query_user 禁止访问）
    private static final List<String> DOWNLOAD_PATHS = Arrays.asList(
            "/api/archives/",
            "/api/archive/"
    );

    // 下载操作的关键字
    private static final String DOWNLOAD_KEYWORD = "/download";
    private static final String ROLE_QUERY_USER = "query_user";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 1. 检查当前用户是否是 query_user 角色
        if (!isQueryUser()) {
            // 非查询用户，直接放行
            return true;
        }

        // 2. 检查请求路径是否需要权限校验
        String requestUri = request.getRequestURI();
        if (!needsPermissionCheck(requestUri)) {
            return true;
        }

        // 3. 检查是否是下载请求 - query_user 禁止下载
        if (isDownloadRequest(requestUri)) {
            log.warn("查询用户尝试下载文件（已拦截）: uri={}", requestUri);
            writeForbidden(response, "查询用户无权下载档案，仅支持在线预览");
            return false;
        }

        // 4. 检查是否是档案列表请求 - 返回 403
        if (isArchiveListRequest(requestUri)) {
            log.warn("查询用户尝试访问档案列表（已拦截）: uri={}", requestUri);
            writeForbidden(response, "请通过借阅申请流程访问档案");
            return false;
        }

        // 5. 提取档案ID
        String archiveId = extractArchiveId(requestUri);
        if (archiveId == null || archiveId.isEmpty()) {
            // 无法提取档案ID，放行（让后续业务逻辑处理）
            return true;
        }

        // 4. 检查借阅权限
        String userId = resolveUserId();
        if (userId == null) {
            writeForbidden(response, "无法识别当前用户");
            return false;
        }

        boolean hasPermission = borrowingFacade.checkAccess(userId, archiveId, "VIEW");
        if (!hasPermission) {
            log.warn("查询用户无权访问档案: userId={}, archiveId={}, uri={}",
                    userId, archiveId, requestUri);
            writeForbidden(response, "无权访问该档案，请先提交借阅申请");
            return false;
        }

        log.debug("查询用户借阅权限验证通过: userId={}, archiveId={}", userId, archiveId);
        return true;
    }

    /**
     * 判断当前用户是否是 query_user 角色
     */
    private boolean isQueryUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return false;
        }

        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        return details.getAuthorities().stream()
                .anyMatch(a -> ROLE_QUERY_USER.equals(a.getAuthority())
                        || ("ROLE_" + ROLE_QUERY_USER).equals(a.getAuthority()));
    }

    /**
     * 判断请求路径是否需要权限校验
     */
    private boolean needsPermissionCheck(String requestUri) {
        return PROTECTED_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }

    /**
     * 判断是否是下载请求
     */
    private boolean isDownloadRequest(String requestUri) {
        String lowerUri = requestUri.toLowerCase();
        return lowerUri.contains(DOWNLOAD_KEYWORD)
                || lowerUri.contains("/export/")
                || lowerUri.contains("/attachment");
    }

    /**
     * 判断是否是档案列表请求（page、search 等）
     */
    private boolean isArchiveListRequest(String requestUri) {
        String lowerUri = requestUri.toLowerCase();
        return lowerUri.equals("/api/archives")
                || lowerUri.equals("/api/archives/")
                || lowerUri.contains("/page")
                || lowerUri.contains("/search")
                || requestUri.matches("/api/archives[^/]*");
    }

    /**
     * 从请求路径中提取档案ID
     * 支持: /api/archives/{id} 和 /api/archives/{id}/* 模式
     */
    private String extractArchiveId(String requestUri) {
        // 移除查询参数
        String path = requestUri.split("\\?")[0];

        // 尝试匹配 /api/archives/{id} 或 /api/archive/{id}
        for (String prefix : PROTECTED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                String remaining = path.substring(prefix.length());
                // 提取第一段作为档案ID
                String[] parts = remaining.split("/");
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    return parts[0];
                }
            }
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":403,\"message\":\"%s\"}", message));
    }
}
