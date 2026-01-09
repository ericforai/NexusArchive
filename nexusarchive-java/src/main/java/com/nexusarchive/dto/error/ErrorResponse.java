// Input: Lombok、Java 标准库
// Output: ErrorResponse 类
// Pos: DTO - 错误响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一错误响应格式
 * <p>
 * 所有异常响应都使用此格式，确保前后端接口一致。
 * 生产环境不会暴露堆栈跟踪或敏感内部信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码（业务错误码，如 "EAA_400", "EAA_404" 等）
     */
    private String code;

    /**
     * 用户友好的错误信息
     */
    private String message;

    /**
     * 请求追踪 ID，用于日志关联
     */
    private String requestId;

    /**
     * 错误分类
     */
    private ErrorCategory category;

    /**
     * 时间戳 (ISO-8601)
     */
    private String timestamp;

    /**
     * 详细错误信息（仅开发环境返回）
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Detail detail;

    /**
     * 路径信息
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String path;

    /**
     * 详细错误信息（仅开发环境）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Detail implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 异常类型
         */
        private String exceptionType;

        /**
         * 原始错误消息（开发调试用）
         */
        private String debugMessage;

        /**
         * 验证错误详情（参数校验失败时）
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private ValidationError validationError;

        /**
         * 堆栈跟踪（仅开发环境）
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String[] stackTrace;
    }

    /**
     * 验证错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 字段名
         */
        private String field;

        /**
         * 拒绝值
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Object rejectedValue;

        /**
         * 错误消息
         */
        private String message;
    }

    /**
     * 快速创建方法
     */
    public static ErrorResponse of(String code, String message, String requestId, ErrorCategory category) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .requestId(requestId)
                .category(category)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * 带详细信息的创建方法（开发环境）
     */
    public static ErrorResponse withDetail(String code, String message, String requestId,
                                           ErrorCategory category, Detail detail) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .requestId(requestId)
                .category(category)
                .timestamp(Instant.now().toString())
                .detail(detail)
                .build();
    }
}
