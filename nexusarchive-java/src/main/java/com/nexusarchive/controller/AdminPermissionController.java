// Input: Lombok、Spring Security、Spring Framework、Java 标准库、等
// Output: AdminPermissionController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Permission;
import com.nexusarchive.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/permissions")
@PreAuthorize("hasAuthority('manage_roles') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class AdminPermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Permission>> listAll() {
        return Result.success(permissionService.listAll());
    }

    @PostMapping
    public Result<Permission> create(@RequestBody Permission permission) {
        return Result.success("创建成功", permissionService.create(permission));
    }

    @PutMapping("/{id}")
    public Result<Permission> update(@PathVariable String id, @RequestBody Permission permission) {
        return Result.success("更新成功", permissionService.update(id, permission));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        permissionService.delete(id);
        return Result.success("删除成功", null);
    }
}
