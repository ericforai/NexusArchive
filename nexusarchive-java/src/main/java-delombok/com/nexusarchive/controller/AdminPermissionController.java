// Input: Lombok、Spring Security、Spring Framework、Java 标准库、等
// Output: AdminPermissionController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Permission;
import com.nexusarchive.service.PermissionService;
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
 * 权限管理控制器
 *
 * PRD 来源: 权限管理模块
 * 提供系统权限的 CRUD 操作
 */
@Tag(name = "权限管理", description = """
    系统权限管理接口。

    **功能说明:**
    - 获取所有权限列表
    - 创建新权限
    - 更新权限信息
    - 删除权限

    **权限类型:**
    - 菜单权限: 控制菜单访问
    - 操作权限: 控制功能操作
    - 数据权限: 控制数据访问范围

    **权限编码规范:**
    - 格式: 资源:操作 (如 archive:read)
    - 特殊: nav:all 表示全部导航权限

    **使用场景:**
    - 权限管理维护
    - 角色权限分配
    - 权限体系构建

    **权限要求:**
    - manage_roles: 角色管理权限
    - SYSTEM_ADMIN: 系统管理员
    - nav:all: 全部导航权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/permissions")
@PreAuthorize("hasAuthority('manage_roles') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    /**
     * 获取所有权限
     */
    @GetMapping
    @Operation(
        summary = "获取所有权限",
        description = """
            获取系统中所有可用的权限列表。

            **返回数据包括:**
            - id: 权限ID
            - code: 权限编码
            - name: 权限名称
            - description: 权限描述
            - type: 权限类型

            **权限类型:**
            - MENU: 菜单权限
            - ACTION: 操作权限
            - DATA: 数据权限

            **使用场景:**
            - 权限列表展示
            - 角色权限分配
            """,
        operationId = "listAllPermissions",
        tags = {"权限管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<Permission>> listAll() {
        return Result.success(permissionService.listAll());
    }

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(
        summary = "创建权限",
        description = """
            创建新的系统权限。

            **请求体:**
            - code: 权限编码（必填）
            - name: 权限名称（必填）
            - description: 权限描述
            - type: 权限类型

            **权限编码规范:**
            - 格式: 资源:操作
            - 示例: archive:read, user:manage

            **业务规则:**
            - 权限编码必须唯一
            - 创建操作会被审计记录

            **使用场景:**
            - 新增功能权限
            - 扩展权限体系
            """,
        operationId = "createPermission",
        tags = {"权限管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或编码重复"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "CREATE", resourceType = "PERMISSION", description = "创建权限")
    public Result<Permission> create(
            @Parameter(description = "权限信息", required = true)
            @Valid @RequestBody Permission permission) {
        return Result.success("创建成功", permissionService.create(permission));
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新权限",
        description = """
            更新现有权限的信息。

            **路径参数:**
            - id: 权限ID

            **请求体:**
            - name: 权限名称
            - description: 权限描述
            - type: 权限类型

            **注意:**
            - 权限编码不可修改

            **使用场景:**
            - 修改权限名称
            - 更新权限描述
            """,
        operationId = "updatePermission",
        tags = {"权限管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "权限不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "PERMISSION", description = "更新权限")
    public Result<Permission> update(
            @Parameter(description = "权限ID", required = true, example = "perm-001")
            @PathVariable String id,
            @Parameter(description = "权限信息", required = true)
            @Valid @RequestBody Permission permission) {
        return Result.success("更新成功", permissionService.update(id, permission));
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除权限",
        description = """
            删除指定的系统权限。

            **路径参数:**
            - id: 权限ID

            **业务规则:**
            - 被角色使用的权限不可删除
            - 删除操作会被审计记录

            **使用场景:**
            - 清理无用权限
            """,
        operationId = "deletePermission",
        tags = {"权限管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "权限被使用，无法删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "权限不存在")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "PERMISSION", description = "删除权限")
    public Result<Void> delete(
            @Parameter(description = "权限ID", required = true, example = "perm-001")
            @PathVariable String id) {
        permissionService.delete(id);
        return Result.success("删除成功", null);
    }
}
