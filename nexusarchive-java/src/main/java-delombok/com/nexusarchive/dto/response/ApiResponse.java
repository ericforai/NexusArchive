// Input: Lombok、Jakarta EE、Java 标准库
// Output: ApiResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API 统一响应包装器
 * <p>
 * 增强版响应对象，支持 traceId 追踪，用于替代直接返回 Entity
 * </p>
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "API统一响应结果")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码 (200=成功, 4xx/5xx=错误)
     */
    @Schema(description = "响应码", example = "200")
    private String code;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    /**
     * 响应数据 (使用 DTO 隐藏敏感字段)
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 追踪ID (用于日志关联)
     */
    @Schema(description = "追踪ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String traceId;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳", example = "1704067200000")
    private Long timestamp;

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>builder()
                .code("200")
                .message("操作成功")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .message("操作成功")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code("200")
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error() {
        return ApiResponse.<T>builder()
                .code("500")
                .message("操作失败")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code("500")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 未授权
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return ApiResponse.<T>builder()
                .code("401")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 禁止访问
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return ApiResponse.<T>builder()
                .code("403")
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 从请求中提取 traceId 并设置
     */
    public ApiResponse<T> withTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = request.getHeader("traceId");
        }
        if (traceId == null || traceId.isEmpty()) {
            traceId = java.util.UUID.randomUUID().toString();
        }
        this.traceId = traceId;
        return this;
    }

    /**
     * 手动设置 traceId
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
