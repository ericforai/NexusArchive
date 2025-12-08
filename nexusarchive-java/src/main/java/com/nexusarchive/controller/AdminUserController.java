package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.ResetPasswordRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import com.nexusarchive.dto.request.UpdateUserStatusRequest;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.service.UserService;
import com.nexusarchive.annotation.ArchivalAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口（管理员）
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('manage_users') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ArchivalAudit(operationType = "CREATE", resourceType = "USER", description = "创建用户")
    public Result<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "USER", description = "更新用户信息")
    public Result<UserResponse> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        request.setId(id);
        return Result.success(userService.updateUser(request));
    }

    @DeleteMapping("/{id}")
    @ArchivalAudit(operationType = "DELETE", resourceType = "USER", description = "删除用户")
    public Result<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    @GetMapping
    public Result<Page<UserResponse>> listPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return Result.success(userService.listPaged(page, limit, search, status));
    }

    @PostMapping("/{id}/reset-password")
    @ArchivalAudit(operationType = "RESET_PASSWORD", resourceType = "USER", description = "重置用户密码")
    public Result<Void> resetPassword(@PathVariable String id, @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.getNewPassword());
        return Result.success("密码已重置", null);
    }

    @PutMapping("/{id}/status")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "USER", description = "修改用户状态")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody UpdateUserStatusRequest request) {
        userService.updateStatus(id, request.getStatus());
        return Result.success("状态已更新", null);
    }
}
