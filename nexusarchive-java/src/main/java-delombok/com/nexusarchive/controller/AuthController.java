// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: AuthController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证控制器
 *
 * PRD 来源: 认证授权模块
 * 提供用户登录、登出、Token 刷新功能
 */
@Tag(name = "认证授权", description = """
    用户认证授权接口。

    **功能说明:**
    - 用户登录
    - 用户登出
    - 获取当前用户信息
    - 刷新访问令牌

    **认证方式:**
    - JWT Bearer Token
    - Token 存储在 Authorization 请求头

    **登录流程:**
    1. 用户提交用户名和密码
    2. 系统验证用户名和密码
    3. 生成 JWT Token
    4. 返回用户信息和 Token

    **Token 刷新:**
    - Token 过期前可刷新
    - 刷新后获取新的 Token
    - 旧 Token 加入黑名单

    **使用场景:**
    - 用户登录
    - 用户登出
    - Token 刷新
    - 获取当前用户信息
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(
        summary = "用户登录",
        description = """
            用户使用用户名和密码登录系统。

            **请求体:**
            - username: 用户名（必填）
            - password: 密码（必填）
            - fondsNo: 全宗号（多全宗系统必填）

            **返回数据包括:**
            - token: JWT 访问令牌
            - userInfo: 用户信息
              - userId: 用户ID
              - username: 用户名
              - fullName: 姓名
              - roles: 角色列表
              - permissions: 权限列表
              - allowedFonds: 可访问全宗列表

            **业务规则:**
            - 连续登录失败 5 次后锁定 15 分钟
            - 登录成功后重置失败计数
            - 登录操作会被审计记录
            - Token 有效期默认 24 小时

            **使用场景:**
            - 用户登录
            """,
        operationId = "login",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "用户名或密码错误"),
        @ApiResponse(responseCode = "423", description = "账户已锁定"),
        @ApiResponse(responseCode = "500", description = "登录失败")
    })
    @ArchivalAudit(operationType = "LOGIN", resourceType = "AUTH", description = "用户登录")
    public Result<LoginResponse> login(
            @Parameter(description = "登录请求", required = true)
            @Validated @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(
        summary = "用户登出",
        description = """
            用户登出系统，将当前 Token 加入黑名单。

            **请求头:**
            - Authorization: Bearer {token}

            **业务规则:**
            - Token 加入黑名单后立即失效
            - 登出操作会被审计记录

            **使用场景:**
            - 用户主动登出
            - 切换用户
            """,
        operationId = "logout",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "登出成功"),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    @ArchivalAudit(operationType = "LOGOUT", resourceType = "AUTH", description = "用户登出")
    public Result<Void> logout() {
        // 将当前Token加入黑名单
        String authHeader = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(
        summary = "获取当前用户信息",
        description = """
            获取当前登录用户的详细信息。

            **请求头:**
            - Authorization: Bearer {token}

            **返回数据包括:**
            - userId: 用户ID
            - username: 用户名
            - fullName: 姓名
            - email: 邮箱
            - phone: 手机号
            - roles: 角色列表
            - permissions: 权限列表
            - allowedFonds: 可访问全宗列表
            - currentFonds: 当前选中的全宗

            **使用场景:**
            - 获取当前登录用户信息
            - 验证用户身份
            - 获取用户权限
            """,
        operationId = "getCurrentUser",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录或 Token 无效")
    })
    public Result<LoginResponse.UserInfo> getCurrentUser(
            @Parameter(description = "Bearer Token", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String token = authorization.substring(7);
        LoginResponse.UserInfo userInfo = authService.getCurrentUser(token);
        return Result.success(userInfo);
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "刷新 Token",
        description = """
            刷新访问令牌，获取新的 Token。

            **请求头:**
            - Authorization: Bearer {token}

            **返回数据包括:**
            - token: 新的 JWT 访问令牌
            - userInfo: 用户信息

            **业务规则:**
            - Token 过期前可刷新
            - 刷新后旧 Token 加入黑名单
            - 刷新操作会被审计记录
            - 新 Token 有效期重新计算

            **使用场景:**
            - Token 即将过期时刷新
            - 延长用户会话
            """,
        operationId = "refreshToken",
        tags = {"认证授权"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "刷新成功"),
        @ApiResponse(responseCode = "401", description = "未登录或 Token 无效")
    })
    @ArchivalAudit(operationType = "REFRESH_TOKEN", resourceType = "AUTH", description = "刷新令牌")
    public Result<LoginResponse> refresh(
            @Parameter(description = "Bearer Token", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.unauthorized("未登录");
        }
        String oldToken = authorization.substring(7);
        String newToken = authService.refreshToken(oldToken);
        LoginResponse.UserInfo userInfo = authService.getCurrentUser(oldToken);
        return Result.success("刷新成功", new LoginResponse(newToken, userInfo));
    }
}
