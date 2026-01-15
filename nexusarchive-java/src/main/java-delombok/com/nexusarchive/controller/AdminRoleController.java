// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: AdminRoleController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Role;
import com.nexusarchive.service.RoleService;
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
 * 角色管理控制器
 *
 * PRD 来源: 角色权限管理模块
 * 提供系统角色的 CRUD 操作
 */
@Tag(name = "角色管理", description = """
    系统角色管理接口。

    **功能说明:**
    - 分页查询角色列表
    - 获取所有角色（下拉选择）
    - 获取角色详情
    - 创建新角色
    - 更新角色信息
    - 删除角色
    - 获取权限列表

    **系统角色:**
    - SYSTEM_ADMIN: 系统管理员（所有权限）
    - SECURITY_ADMIN: 安全保密员
    - AUDIT_ADMIN: 安全审计员
    - BUSINESS_USER: 业务操作员

    **三员分立 (GB/T 39784-2021):**
    - 系统管理员、安全保密员、安全审计员不能分配给同一用户
    - 三员角色互斥

    **使用场景:**
    - 角色管理维护
    - 用户角色分配
    - 权限体系构建

    **权限要求:**
    - manage_roles: 角色管理权限
    - SYSTEM_ADMIN: 系统管理员
    - nav:all: 全部导航权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('manage_roles') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AdminRoleController {

    private final RoleService roleService;

    /**
     * 获取角色列表（分页）
     */
    @GetMapping
    @Operation(
        summary = "获取角色列表（分页）",
        description = """
            分页查询角色列表，支持搜索。

            **查询参数:**
            - page: 页码（从 1 开始）
            - limit: 每页条数
            - search: 搜索关键词（角色名称、编码）

            **返回数据包括:**
            - records: 角色记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **使用场景:**
            - 角色列表展示
            - 角色搜索
            """,
        operationId = "getRoles",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Page<Role>> getRoles(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "搜索关键词")
            @RequestParam(required = false) String search) {
        Page<Role> result = roleService.getRoles(page, limit, search);
        return Result.success(result);
    }

    /**
     * 获取所有角色（不分页）
     */
    @GetMapping("/all")
    @Operation(
        summary = "获取所有角色",
        description = """
            获取所有角色列表（不分页），用于下拉选择。

            **返回数据包括:**
            - id: 角色ID
            - code: 角色编码
            - name: 角色名称

            **使用场景:**
            - 角色选择器
            - 用户角色分配
            """,
        operationId = "getAllRoles",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取角色详情",
        description = """
            获取指定角色的详细信息。

            **路径参数:**
            - id: 角色ID

            **返回数据包括:**
            - id: 角色ID
            - code: 角色编码
            - name: 角色名称
            - description: 角色描述
            - permissions: 关联的权限列表

            **使用场景:**
            - 角色详情查看
            - 角色编辑预填充
            """,
        operationId = "getRole",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    public Result<Role> getRole(
            @Parameter(description = "角色ID", required = true, example = "role-001")
            @PathVariable String id) {
        Role role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(
        summary = "创建角色",
        description = """
            创建新的系统角色。

            **请求体:**
            - code: 角色编码（必填）
            - name: 角色名称（必填）
            - description: 角色描述
            - permissions: 关联的权限ID列表

            **角色编码规范:**
            - 系统角色: SYSTEM_ADMIN, SECURITY_ADMIN, AUDIT_ADMIN
            - 自定义角色: 使用前缀如 ROLE_CUSTOM_XXX

            **业务规则:**
            - 角色编码必须唯一
            - 创建操作会被审计记录

            **使用场景:**
            - 新增自定义角色
            - 创建业务角色
            """,
        operationId = "createRole",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或编码重复"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "CREATE", resourceType = "ROLE", description = "创建角色")
    public Result<Role> createRole(
            @Parameter(description = "角色信息", required = true)
            @Valid @RequestBody Role role) {
        Role created = roleService.createRole(role);
        return Result.success("角色创建成功", created);
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新角色",
        description = """
            更新现有角色的信息。

            **路径参数:**
            - id: 角色ID

            **请求体:**
            - name: 角色名称
            - description: 角色描述
            - permissions: 关联的权限ID列表

            **注意:**
            - 角色编码不可修改
            - 更新操作会被审计记录

            **使用场景:**
            - 修改角色信息
            - 调整角色权限
            """,
        operationId = "updateRole",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ROLE", description = "更新角色")
    public Result<Void> updateRole(
            @Parameter(description = "角色ID", required = true, example = "role-001")
            @PathVariable String id,
            @Parameter(description = "角色信息", required = true)
            @Valid @RequestBody Role role) {
        roleService.updateRole(id, role);
        return Result.success("角色更新成功", null);
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除角色",
        description = """
            删除指定的系统角色。

            **路径参数:**
            - id: 角色ID

            **业务规则:**
            - 系统内置角色不可删除
            - 被用户使用的角色不可删除
            - 删除操作会被审计记录

            **使用场景:**
            - 删除自定义角色
            """,
        operationId = "deleteRole",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "角色被使用或不可删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "角色不存在")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "ROLE", description = "删除角色")
    public Result<Void> deleteRole(
            @Parameter(description = "角色ID", required = true, example = "role-001")
            @PathVariable String id) {
        roleService.deleteRole(id);
        return Result.success("角色删除成功", null);
    }

    /**
     * 获取权限列表
     */
    @GetMapping("/permissions")
    @Operation(
        summary = "获取权限列表",
        description = """
            获取系统中所有可用的权限列表，用于角色权限分配。

            **返回数据包括:**
            - id: 权限ID
            - code: 权限编码
            - name: 权限名称
            - category: 权限分类

            **权限分类:**
            - 档案管理: archive:read, archive:manage, archive:export
            - 用户管理: user:read, user:manage
            - 系统管理: system:config, system:log

            **使用场景:**
            - 角色权限分配
            - 权限列表展示
            """,
        operationId = "getPermissions",
        tags = {"角色管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<RoleService.Permission>> getPermissions() {
        List<RoleService.Permission> permissions = roleService.getPermissions();
        return Result.success(permissions);
    }
}
