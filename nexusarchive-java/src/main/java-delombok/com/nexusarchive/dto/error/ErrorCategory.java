// Input: Lombok、Java 标准库
// Output: ErrorCategory 枚举
// Pos: DTO - 错误分类
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.error;

import lombok.Getter;

/**
 * 错误分类枚举
 * <p>
 * 用于将错误按类型分类，便于前端统一处理和日志聚合分析。
 * </p>
 */
@Getter
public enum ErrorCategory {

    /**
     * 业务异常 (4xx)
     * 用户操作不当或业务规则不满足
     */
    BUSINESS_ERROR("BUSINESS_ERROR", "业务异常", 400),

    /**
     * 验证异常 (400)
     * 请求参数校验失败
     */
    VALIDATION_ERROR("VALIDATION_ERROR", "参数验证失败", 400),

    /**
     * 认证异常 (401)
     * 未登录或登录已过期
     */
    AUTHENTICATION_ERROR("AUTHENTICATION_ERROR", "认证失败", 401),

    /**
     * 授权异常 (403)
     * 无权限访问
     */
    AUTHORIZATION_ERROR("AUTHORIZATION_ERROR", "授权失败", 403),

    /**
     * 资源不存在 (404)
     */
    NOT_FOUND_ERROR("NOT_FOUND_ERROR", "资源不存在", 404),

    /**
     * 冲突异常 (409)
     * 资源冲突（如重复创建）
     */
    CONFLICT_ERROR("CONFLICT_ERROR", "资源冲突", 409),

    /**
     * 限流异常 (429)
     * 请求过于频繁
     */
    RATE_LIMIT_ERROR("RATE_LIMIT_ERROR", "请求过于频繁", 429),

    /**
     * 系统异常 (500)
     * 服务器内部错误
     */
    SYSTEM_ERROR("SYSTEM_ERROR", "系统内部错误", 500),

    /**
     * 服务不可用 (503)
     */
    SERVICE_UNAVAILABLE_ERROR("SERVICE_UNAVAILABLE_ERROR", "服务暂时不可用", 503),

    /**
     * 安全异常 (4xx/5xx)
     * 涉及安全相关的异常（如病毒检测失败、签名验证失败等）
     */
    SECURITY_ERROR("SECURITY_ERROR", "安全异常", 500);

    private final String code;
    private final String description;
    private final int defaultHttpStatusCode;

    ErrorCategory(String code, String description, int defaultHttpStatusCode) {
        this.code = code;
        this.description = description;
        this.defaultHttpStatusCode = defaultHttpStatusCode;
    }

    /**
     * 根据异常类型获取错误分类
     */
    public static ErrorCategory fromException(Throwable exception) {
        if (exception == null) {
            return SYSTEM_ERROR;
        }

        String className = exception.getClass().getSimpleName();

        // 认证异常
        if (className.contains("Authentication") ||
            className.contains("Login") ||
            className.contains("Credentials") ||
            className.contains("AuthTicket")) {
            return AUTHENTICATION_ERROR;
        }

        // 授权异常
        if (className.contains("AccessDenied") ||
            className.contains("Authorization")) {
            return AUTHORIZATION_ERROR;
        }

        // 验证异常
        if (className.contains("Validation") ||
            className.contains("ConstraintViolation") ||
            className.contains("MethodArgumentNotValid")) {
            return VALIDATION_ERROR;
        }

        // 资源不存在
        if (className.contains("NotFound") ||
            className.contains("NoSuch")) {
            return NOT_FOUND_ERROR;
        }

        // 冲突异常
        if (className.contains("AlreadyExists") ||
            className.contains("Duplicate") ||
            className.contains("Conflict")) {
            return CONFLICT_ERROR;
        }

        // 安全异常
        if (className.contains("Security") ||
            className.contains("Virus") ||
            className.contains("Signature") ||
            className.contains("Ciphertext")) {
            return SECURITY_ERROR;
        }

        // 业务异常
        if (className.contains("Business") ||
            className.contains("Rule") ||
            className.contains("NotAllowed")) {
            return BUSINESS_ERROR;
        }

        // 默认为系统异常
        return SYSTEM_ERROR;
    }
}
