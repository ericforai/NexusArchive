package com.nexusarchive.config;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.result.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        Map<String, Object> errorMap = new HashMap<>();
        // 如果 code 是数字，转换为 EAA_ 前缀格式，或者直接使用
        String codeStr = String.valueOf(e.getCode());
        if (!codeStr.startsWith("EAA_")) {
            codeStr = "EAA_" + codeStr;
        }
        
        errorMap.put("code", codeStr);
        errorMap.put("msg", e.getMessage());
        errorMap.put("ref", "DA/T 104-2024"); // 默认引用接口规范
        
        return new ResponseEntity<>(errorMap, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 处理参数验证异常
     * Return strict JSON: { "code": "EAA_400", "msg": "...", "ref": "..." }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("code", "EAA_400");
        
        // 获取第一个错误信息
        FieldError error = e.getBindingResult().getFieldError();
        String message = error != null ? error.getDefaultMessage() : "参数验证失败";
        String field = error != null ? error.getField() : "unknown";
        
        errorMap.put("msg", String.format("Field '%s' error: %s", field, message));
        
        // 尝试从消息中提取引用，如果没有则默认
        if (message != null && message.contains("DA/T")) {
            // 简单提取，实际可优化
            errorMap.put("ref", "DA/T 94-2022");
        } else {
            errorMap.put("ref", "DA/T 94-2022");
        }
        
        return errorMap;
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.forbidden("无权限访问");
    }
    
    @ExceptionHandler({AuthenticationCredentialsNotFoundException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthErrors(RuntimeException e) {
        return Result.unauthorized("未登录或登录已过期");
    }
    
    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("系统错误: " + e.getMessage());
    }
}
