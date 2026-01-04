// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: AdminRoleController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Role;
import com.nexusarchive.service.RoleService;
import com.nexusarchive.annotation.ArchivalAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * 角色管理控制器
 * 
 * 路径: /admin/roles
 */
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('manage_roles') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AdminRoleController {
    
    private final RoleService roleService;
    
    /**
     * 获取角色列表（分页）
     * 
     * GET /admin/roles?page=1&limit=10&search=xxx
     */
    @GetMapping
    public Result<Page<Role>> getRoles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        Page<Role> result = roleService.getRoles(page, limit, search);
        return Result.success(result);
    }
    
    /**
     * 获取所有角色（不分页，用于下拉选择）
     * 
     * GET /admin/roles/all
     */
    @GetMapping("/all")
    public Result<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return Result.success(roles);
    }
    
    /**
     * 获取单个角色详情
     * 
     * GET /admin/roles/{id}
     */
    @GetMapping("/{id}")
    public Result<Role> getRole(@PathVariable String id) {
        Role role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 创建角色
     * 
     * POST /admin/roles
     */
    @PostMapping
    @ArchivalAudit(operationType = "CREATE", resourceType = "ROLE", description = "创建角色")
    public Result<Role> createRole(@Valid @RequestBody Role role) {
        Role created = roleService.createRole(role);
        return Result.success("角色创建成功", created);
    }

    /**
     * 更新角色
     * 
     * PUT /admin/roles/{id}
     */
    @PutMapping("/{id}")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ROLE", description = "更新角色")
    public Result<Void> updateRole(@PathVariable String id, @Valid @RequestBody Role role) {
        roleService.updateRole(id, role);
        return Result.success("角色更新成功", null);
    }

    /**
     * 删除角色
     * 
     * DELETE /admin/roles/{id}
     */
    @DeleteMapping("/{id}")
    @ArchivalAudit(operationType = "DELETE", resourceType = "ROLE", description = "删除角色")
    public Result<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return Result.success("角色删除成功", null);
    }
    
    /**
     * 获取权限列表
     * 
     * GET /admin/roles/permissions
     */
    @GetMapping("/permissions")
    public Result<List<RoleService.Permission>> getPermissions() {
        List<RoleService.Permission> permissions = roleService.getPermissions();
        return Result.success(permissions);
    }
}
