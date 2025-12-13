package com.nexusarchive.config;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     * Returns 400 Bad Request with standardized error code structure.
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Map<String, String>> handleBusinessException(BusinessException e) {
        log.warn("Business Exception: {}", e.getMessage());

        // Standardize the "data" field to contain ref info
        Map<String, String> errorDetails = new HashMap<>();
        String codeStr = String.valueOf(e.getCode());
        if (!codeStr.startsWith("EAA_")) {
            codeStr = "EAA_" + codeStr;
        }
        errorDetails.put("errCode", codeStr);
        errorDetails.put("ref", "DA/T 104-2024");
        
        // Fix: Pass errorDetails to the Result
        return new Result<>(400, e.getMessage(), errorDetails);
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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        // Log the full stack trace server-side
        log.error("Unhandled System Exception", e);
        // Return a generic safe message to client
        return Result.error(500, "系统内部错误，请联系管理员");
    }
}
