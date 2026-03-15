// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: AuthController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.service.AuthService;
import lombok.RequiredArgsConstructor;
import com.nexusarchive.annotation.ArchivalAudit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.nexusarchive.common.constants.HttpConstants;

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
    @ArchivalAudit(operationType = "LOGIN", resourceType = "AUTH", description = "用户登录")
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
    @ArchivalAudit(operationType = "LOGOUT", resourceType = "AUTH", description = "用户登出")
    public Result<Void> logout() {
        // 将当前Token加入黑名单
        String authHeader = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader(HttpConstants.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return Result.success("登出成功", null);
    }
    
    /**
     * 获取当前用户信息
     * 
     * GET /auth/me
     */
    @GetMapping("/me")
    public Result<LoginResponse.UserInfo> getCurrentUser(@RequestHeader(value = HttpConstants.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String token = authorization.substring(7);
        LoginResponse.UserInfo userInfo = authService.getCurrentUser(token);
        return Result.success(userInfo);
    }

    /**
     * 刷新Token
     *
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    @ArchivalAudit(operationType = "REFRESH_TOKEN", resourceType = "AUTH", description = "刷新令牌")
    public Result<LoginResponse> refresh(@RequestHeader(value = HttpConstants.AUTHORIZATION, required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String oldToken = authorization.substring(7);
        String newToken = authService.refreshToken(oldToken);
        LoginResponse.UserInfo userInfo = authService.getCurrentUser(oldToken);
        return Result.success("刷新成功", new LoginResponse(newToken, userInfo));
    }
}
