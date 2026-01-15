// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: AdminUserController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.ResetPasswordRequest;
import com.nexusarchive.dto.request.UpdateUserFondsScopeRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import com.nexusarchive.dto.request.UpdateUserStatusRequest;
import com.nexusarchive.dto.response.FondsScopeResponse;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * 用户管理接口（管理员）
 *
 * PRD 来源: 用户管理模块
 * 提供用户的全生命周期管理功能
 */
@Tag(name = "用户管理", description = """
    用户管理接口（管理员）。

    **功能说明:**
    - 创建用户
    - 更新用户信息
    - 删除用户
    - 获取用户详情
    - 分页查询用户列表
    - 重置用户密码
    - 修改用户状态
    - 获取用户全宗权限范围
    - 更新用户全宗权限范围

    **用户状态:**
    - ACTIVE: 活跃
    - INACTIVE: 停用
    - LOCKED: 锁定
    - EXPIRED: 已过期

    **全宗权限:**
    - 用户必须分配至少一个全宗
    - 超级管理员分配全宗后可访问所有全宗数据
    - 普通用户只能访问分配的全宗数据

    **使用场景:**
    - 用户管理维护
    - 用户权限分配
    - 用户状态管理

    **权限要求:**
    - manage_users: 用户管理权限
    - SYSTEM_ADMIN: 系统管理员
    - nav:all: 全部导航权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('manage_users') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AdminUserController {

    private final UserService userService;

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(
        summary = "创建用户",
        description = """
            创建新的系统用户。

            **请求体:**
            - username: 用户名（必填，唯一）
            - password: 密码（必填）
            - fullName: 姓名（必填）
            - email: 邮箱（必填，唯一）
            - phone: 手机号
            - orgId: 所属组织ID
            - roleIds: 角色ID列表

            **密码策略:**
            - 最小长度 8 位
            - 必须包含大小写字母、数字、特殊字符

            **业务规则:**
            - 用户名、邮箱必须唯一
            - 新用户默认状态为 ACTIVE
            - 创建操作会被审计记录

            **使用场景:**
            - 新增用户
            - 批量创建用户
            """,
        operationId = "createUser",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或用户名/邮箱重复"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "CREATE", resourceType = "USER", description = "创建用户")
    public Result<UserResponse> createUser(
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新用户",
        description = """
            更新用户基本信息。

            **路径参数:**
            - id: 用户ID

            **请求体:**
            - fullName: 姓名
            - email: 邮箱
            - phone: 手机号
            - orgId: 所属组织ID
            - roleIds: 角色ID列表

            **注意:**
            - 用户名不可修改
            - 更新操作会被审计记录

            **使用场景:**
            - 修改用户信息
            - 调整用户角色
            """,
        operationId = "updateUser",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "USER", description = "更新用户信息")
    public Result<UserResponse> updateUser(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id,
            @Parameter(description = "用户信息", required = true)
            @Valid @RequestBody UpdateUserRequest request) {
        request.setId(id);
        return Result.success(userService.updateUser(request));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除用户",
        description = """
            删除指定用户。

            **路径参数:**
            - id: 用户ID

            **业务规则:**
            - 不能删除自己
            - 删除操作会被审计记录

            **使用场景:**
            - 删除离职用户
            """,
        operationId = "deleteUser",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "不能删除自己"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "USER", description = "删除用户")
    public Result<Void> deleteUser(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取用户详情",
        description = """
            获取指定用户的详细信息。

            **路径参数:**
            - id: 用户ID

            **返回数据包括:**
            - id: 用户ID
            - username: 用户名
            - fullName: 姓名
            - email: 邮箱
            - phone: 手机号
            - orgId: 所属组织ID
            - status: 用户状态
            - roles: 角色列表
            - allowedFonds: 可访问全宗列表

            **使用场景:**
            - 用户详情查看
            - 用户编辑预填充
            """,
        operationId = "getUser",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public Result<UserResponse> getUser(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id) {
        return Result.success(userService.getUserById(id));
    }

    /**
     * 分页查询用户
     */
    @GetMapping
    @Operation(
        summary = "分页查询用户",
        description = """
            分页查询用户列表，支持搜索和状态过滤。

            **查询参数:**
            - page: 页码（从 1 开始）
            - limit: 每页条数
            - search: 搜索关键词（用户名、姓名、邮箱）
            - status: 状态过滤

            **返回数据包括:**
            - records: 用户记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **使用场景:**
            - 用户列表展示
            - 用户搜索
            """,
        operationId = "listUsers",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Page<UserResponse>> listPaged(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String search,
            @Parameter(description = "状态过滤", example = "ACTIVE")
            @RequestParam(required = false) String status) {
        return Result.success(userService.listPaged(page, limit, search, status));
    }

    /**
     * 重置用户密码
     */
    @PostMapping("/{id}/reset-password")
    @Operation(
        summary = "重置用户密码",
        description = """
            重置指定用户的密码。

            **路径参数:**
            - id: 用户ID

            **请求体:**
            - newPassword: 新密码（必填）

            **密码策略:**
            - 最小长度 8 位
            - 必须包含大小写字母、数字、特殊字符

            **业务规则:**
            - 新密码不能与旧密码相同
            - 重置操作会被审计记录
            - 重置后用户需下次登录时修改密码

            **使用场景:**
            - 用户忘记密码
            - 强制密码重置
            """,
        operationId = "resetUserPassword",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "400", description = "密码不符合策略"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @ArchivalAudit(operationType = "RESET_PASSWORD", resourceType = "USER", description = "重置用户密码")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id,
            @Parameter(description = "重置密码请求", required = true)
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success("密码已重置", null);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/{id}/status")
    @Operation(
        summary = "修改用户状态",
        description = """
            修改指定用户的状态。

            **路径参数:**
            - id: 用户ID

            **请求体:**
            - status: 新状态（必填）

            **用户状态:**
            - ACTIVE: 活跃（可登录）
            - INACTIVE: 停用（不可登录）
            - LOCKED: 锁定（不可登录）
            - EXPIRED: 已过期（不可登录）

            **业务规则:**
            - 不能修改自己的状态
            - 停用用户后其 Token 立即失效
            - 状态修改会被审计记录

            **使用场景:**
            - 停用离职用户
            - 解锁用户
            """,
        operationId = "updateUserStatus",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "状态已更新"),
        @ApiResponse(responseCode = "400", description = "不能修改自己的状态"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "USER", description = "修改用户状态")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id,
            @Parameter(description = "状态更新请求", required = true)
            @Valid @RequestBody UpdateUserStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success("状态已更新", null);
    }

    /**
     * 获取用户全宗权限范围
     */
    @GetMapping("/{id}/fonds-scope")
    @Operation(
        summary = "获取用户全宗权限范围",
        description = """
            获取用户的全宗权限范围信息。

            **路径参数:**
            - id: 用户ID

            **返回数据包括:**
            - assignedFonds: 已分配的全宗列表
            - availableFonds: 所有可用的全宗列表
            - canAccessAll: 是否可访问所有全宗

            **业务规则:**
            - 用户必须分配至少一个全宗
            - 超级管理员可访问所有全宗

            **使用场景:**
            - 用户全宗权限查看
            - 全宗分配参考
            """,
        operationId = "getUserFondsScope",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public Result<FondsScopeResponse> getUserFondsScope(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id) {
        return Result.success(userService.getUserFondsScope(id));
    }

    /**
     * 更新用户全宗权限范围
     */
    @PutMapping("/{id}/fonds-scope")
    @Operation(
        summary = "更新用户全宗权限范围",
        description = """
            更新用户的全宗权限范围。

            **路径参数:**
            - id: 用户ID

            **请求体:**
            - fondsNos: 全宗编号列表

            **业务规则:**
            - 用户必须分配至少一个全宗
            - 更新后立即生效
            - 更新操作会被审计记录

            **使用场景:**
            - 分配用户全宗权限
            - 调整用户数据访问范围
            """,
        operationId = "updateUserFondsScope",
        tags = {"用户管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "全宗权限已更新"),
        @ApiResponse(responseCode = "400", description = "必须分配至少一个全宗"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "用户或全宗不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "USER_FONDS_SCOPE", description = "更新用户全宗权限")
    public Result<Void> updateUserFondsScope(
            @Parameter(description = "用户ID", required = true, example = "user-001")
            @PathVariable String id,
            @Parameter(description = "全宗权限更新请求", required = true)
            @RequestBody UpdateUserFondsScopeRequest request) {
        userService.updateUserFondsScope(id, request);
        return Result.success("全宗权限已更新", null);
    }
}
