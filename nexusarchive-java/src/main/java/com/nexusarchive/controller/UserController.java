// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: UserController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 普通用户接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/permissions")
    public Result<Map<String, Object>> getPermissions() {
        // 构建前端期望的数据结构: { permissions: [], roles: [] }
        Map<String, Object> result = new HashMap<>();
        
        // 权限列表 - 包含所有导航权限
        List<String> permissions = new ArrayList<>();
        permissions.add("nav:portal");
        permissions.add("nav:panorama");
        permissions.add("nav:pre_archive");
        permissions.add("nav:collection");
        permissions.add("nav:archive_mgmt");
        permissions.add("nav:query");
        permissions.add("nav:borrowing");
        permissions.add("nav:destruction");
        permissions.add("nav:warehouse");
        permissions.add("nav:stats");
        permissions.add("nav:settings");
        permissions.add("nav:all");  // 超级权限
        permissions.add("user:read");
        permissions.add("system_admin");
        
        result.put("permissions", permissions);
        
        // 角色列表
        List<Map<String, String>> roles = new ArrayList<>();
        Map<String, String> adminRole = new HashMap<>();
        adminRole.put("id", "1");
        adminRole.put("name", "系统管理员");
        adminRole.put("code", "super_admin");
        roles.add(adminRole);
        
        result.put("roles", roles);
        
        return Result.success(result);
    }
}
