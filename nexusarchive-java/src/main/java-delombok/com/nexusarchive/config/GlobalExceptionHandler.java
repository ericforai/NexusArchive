// Input: Lombok、Spring Framework、Spring Security、Java 标准库、等
// Output: GlobalExceptionHandler 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * <p>
 * Standardized Exception Handling for Commercial Viability.
 * Ensures all responses follow the Result&lt;T&gt; schema.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     * Returns error with HTTP status derived from BusinessException code (defaults to 400).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Map<String, String>>> handleBusinessException(BusinessException e) {
        log.warn("Business Exception: {}", e.getMessage());

        // Standardize the "data" field to contain ref info
        Map<String, String> errorDetails = new HashMap<>();
        String codeStr = String.valueOf(e.getCode());
        if (!codeStr.startsWith("EAA_")) {
            codeStr = "EAA_" + codeStr;
        }
        errorDetails.put("errCode", codeStr);
        errorDetails.put("ref", "DA/T 104-2024");

        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }

        // Fix: Pass errorDetails to the Result and honor custom status code
        Result<Map<String, String>> body = new Result<>(status.value(), e.getMessage(), errorDetails);
        return ResponseEntity.status(status).body(body);
    }
    
    /**
     * 处理参数验证异常
     * Returns 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation Failed: {}", e.getMessage());
        
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null ? error.getDefaultMessage() : "参数验证失败";
        String field = error != null ? error.getField() : "unknown";
        
        String detailedMsg = String.format("Field '%s': %s", field, message);
        
        Map<String, String> details = new HashMap<>();
        details.put("field", field);
        details.put("violation", message);
        details.put("ref", "DA/T 94-2022");
        
        return new Result<>(400, detailedMsg, details);
    }

    /**
     * 处理空/不可读请求体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("Body unreadable or missing: {}", e.getMessage());
        Map<String, String> details = new HashMap<>();
        details.put("ref", "DA/T 104-2024");
        details.put("violation", "请求体不能为空或格式错误");
        return new Result<>(400, "请求体不能为空或格式错误", details);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return Result.forbidden("无权限访问");
    }
    
    @ExceptionHandler({AuthenticationCredentialsNotFoundException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthErrors(RuntimeException e) {
        log.warn("Auth Error: {}", e.getMessage());
        return Result.unauthorized("未登录或登录已过期");
    }
    
    /**
     * 处理其他异常
     * Catches all unhandled exceptions.
     * SECURITY: Does NOT leak stack trace or internal message to client.
     */
    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<Result<?>> handleException(Exception e) {
        // 若异常链中包含 BusinessException，则按业务异常处理（可返回 4xx/409 等）
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof com.nexusarchive.common.exception.BusinessException be) {
                // 直接复用业务异常处理逻辑
                @SuppressWarnings("unchecked")
                org.springframework.http.ResponseEntity<Result<?>> delegated =
                        (org.springframework.http.ResponseEntity<Result<?>>) (org.springframework.http.ResponseEntity<?>) handleBusinessException(be);
                return delegated;
            }
            cause = cause.getCause();
        }
        // Log the full stack trace server-side
        log.error("Unhandled System Exception", e);
        // Return a generic safe message to client with 500
        return org.springframework.http.ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "系统内部错误，请联系管理员"));
    }
}
