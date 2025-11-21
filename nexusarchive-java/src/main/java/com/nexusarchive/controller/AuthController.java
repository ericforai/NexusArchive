package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 
 * 路径: /auth
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     * 
     * POST /auth/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }
    
    /**
     * 用户登出
     * 
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT是无状态的，登出只需前端删除Token即可
        // 如果需要服务端登出，可以使用Redis黑名单
        return Result.success("登出成功", null);
    }
    
    /**
     * 获取当前用户信息
     * 
     * GET /auth/me
     */
    @GetMapping("/me")
    public Result<String> getCurrentUser(@RequestHeader("Authorization") String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            String username = authService.getUsernameFromToken(token);
            return Result.success(username);
        }
        return Result.unauthorized("未登录");
    }
}
