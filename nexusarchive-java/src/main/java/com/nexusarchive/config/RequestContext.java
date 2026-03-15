// Input: Spring Framework、Java 标准库
// Output: RequestContext 类
// Pos: 配置层 - 请求上下文管理
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 请求上下文工具类
 * <p>
 * 提供统一的请求上下文访问，包括：
 * <ul>
 *   <li>请求追踪 ID (requestId)</li>
 *   <li>当前用户信息</li>
 *   <li>请求路径和参数</li>
 * </ul>
 * </p>
 * <p>
 * requestId 在请求进入时通过 {@link RequestContextFilter} 注入 MDC，
 * 在日志中可通过 %X{requestId} 引用。
 * </p>
 */
@Slf4j
public final class RequestContext {

    /**
     * MDC 键名：请求追踪 ID
     */
    public static final String REQUEST_ID_KEY = "requestId";

    /**
     * MDC 键名：用户 ID
     */
    public static final String USER_ID_KEY = "userId";

    /**
     * MDC 键名：用户名
     */
    public static final String USERNAME_KEY = "username";

    /**
     * MDC 键名：全宗代码
     */
    public static final String FONDS_CODE_KEY = "fondsCode";

    /**
     * 请求头名称：请求追踪 ID
     * 允许客户端传递请求 ID 用于链路追踪
     */
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    /**
     * 请求头名称：真实 IP
     * 用于代理场景下获取真实客户端 IP
     */
    public static final String REAL_IP_HEADER = "X-Real-IP";

    /**
     * 请求头名称：转发 IP
     * 用于代理场景下获取原始客户端 IP
     */
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private RequestContext() {
        // 工具类，禁止实例化
    }

    /**
     * 获取或生成当前请求的追踪 ID
     *
     * @return 请求追踪 ID
     */
    public static String getRequestId() {
        String requestId = MDC.get(REQUEST_ID_KEY);
        if (requestId == null) {
            requestId = generateRequestId();
            MDC.put(REQUEST_ID_KEY, requestId);
        }
        return requestId;
    }

    /**
     * 设置当前请求的追踪 ID
     *
     * @param requestId 请求追踪 ID
     */
    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID_KEY, requestId);
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，如果未登录返回 null
     */
    public static String getUserId() {
        return MDC.get(USER_ID_KEY);
    }

    /**
     * 获取当前用户 ID，如果未登录抛出异常
     *
     * @return 用户 ID
     * @throws IllegalStateException 如果用户未认证
     */
    public static String getRequiredUserId() {
        String userId = getUserId();
        if (userId == null) {
            throw new IllegalStateException("用户未认证");
        }
        return userId;
    }

    /**
     * 设置当前用户 ID
     *
     * @param userId 用户 ID
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId);
        } else {
            MDC.remove(USER_ID_KEY);
        }
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，如果未登录返回 null
     */
    public static String getUsername() {
        return MDC.get(USERNAME_KEY);
    }

    /**
     * 获取当前用户名，如果未设置抛出异常
     *
     * @return 用户名
     * @throws IllegalStateException 如果用户名未设置
     */
    public static String getRequiredUsername() {
        String username = getUsername();
        if (username == null) {
            throw new IllegalStateException("用户名未设置");
        }
        return username;
    }

    /**
     * 设置当前用户名
     *
     * @param username 用户名
     */
    public static void setUsername(String username) {
        if (username != null) {
            MDC.put(USERNAME_KEY, username);
        } else {
            MDC.remove(USERNAME_KEY);
        }
    }

    /**
     * 获取当前全宗代码
     *
     * @return 全宗代码
     */
    public static String getFondsCode() {
        return MDC.get(FONDS_CODE_KEY);
    }

    /**
     * 设置当前全宗代码
     *
     * @param fondsCode 全宗代码
     */
    public static void setFondsCode(String fondsCode) {
        if (fondsCode != null) {
            MDC.put(FONDS_CODE_KEY, fondsCode);
        } else {
            MDC.remove(FONDS_CODE_KEY);
        }
    }

    /**
     * 获取当前请求的 HTTP Servlet Request
     *
     * @return HttpServletRequest，如果不在请求上下文中返回 null
     */
    public static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取请求路径
     *
     * @return 请求路径，如果不在请求上下文中返回空字符串
     */
    public static String getRequestPath() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getRequestURI() : "";
    }

    /**
     * 获取请求方法
     *
     * @return 请求方法 (GET, POST, etc.)，如果不在请求上下文中返回空字符串
     */
    public static String getRequestMethod() {
        HttpServletRequest request = getCurrentRequest();
        return request != null ? request.getMethod() : "";
    }

    /**
     * 获取客户端 IP 地址
     * <p>
     * 优先从 X-Real-IP 获取，其次从 X-Forwarded-For 获取，最后使用 getRemoteAddr()
     * </p>
     *
     * @return 客户端 IP 地址
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader(REAL_IP_HEADER);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader(FORWARDED_FOR_HEADER);
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            int index = ip.indexOf(',');
            if (index > 0) {
                ip = ip.substring(0, index).trim();
            }
            return ip;
        }

        return request.getRemoteAddr();
    }

    /**
     * 生成新的请求追踪 ID
     *
     * @return 请求追踪 ID
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清除当前请求的 MDC 上下文
     * <p>
     * 应在请求结束时调用，避免线程池复用导致的 MDC 污染
     * </p>
     */
    public static void clear() {
        MDC.remove(REQUEST_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(USERNAME_KEY);
        MDC.remove(FONDS_CODE_KEY);
    }

    /**
     * 创建请求上下文摘要（用于日志记录）
     *
     * @return 请求上下文摘要字符串
     */
    public static String toSummaryString() {
        return String.format("[requestId=%s, userId=%s, fondsCode=%s, path=%s, method=%s, ip=%s]",
                getRequestId(),
                getUserId() != null ? getUserId() : "N/A",
                getFondsCode() != null ? getFondsCode() : "N/A",
                getRequestPath(),
                getRequestMethod(),
                getClientIp());
    }
}
