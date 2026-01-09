// Input: Lombok、Spring Framework、Spring Security、Java 标准库、本地模块
// Output: UserController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 普通用户接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/permissions")
    public Result<Map<String, Object>> getPermissions() {
        // 从当前认证用户获取实际权限
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new HashMap<>();

        if (authentication != null && authentication.isAuthenticated()) {
            // 获取实际权限列表
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            List<String> permissions = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            result.put("permissions", permissions);

            // 获取用户信息
            if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                result.put("userId", userDetails.getId());
                result.put("username", userDetails.getUsername());
                result.put("fullName", userDetails.getFullName());
                result.put("allowedFonds", userDetails.getAllowedFonds());
            }

            // 获取角色信息（从权限中提取 ROLE_ 前缀的）
            List<Map<String, String>> roles = permissions.stream()
                    .filter(p -> p.startsWith("ROLE_"))
                    .map(p -> {
                        Map<String, String> role = new HashMap<>();
                        role.put("code", p.substring(5)); // 去掉 ROLE_ 前缀
                        return role;
                    })
                    .collect(Collectors.toList());

            result.put("roles", roles);
        } else {
            // 未认证用户
            result.put("permissions", List.of());
            result.put("roles", List.of());
        }

        return Result.success(result);
    }
}
