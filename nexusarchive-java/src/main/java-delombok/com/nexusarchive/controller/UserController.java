// Input: Lombok、Spring Framework、Spring Security、Java 标准库、本地模块
// Output: UserController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *
 * PRD 来源: 用户模块
 * 提供当前用户的权限和角色信息查询
 */
@Tag(name = "用户信息", description = """
    当前用户信息查询接口。

    **功能说明:**
    - 获取当前用户权限列表
    - 获取当前用户角色列表

    **返回数据包括:**
    - permissions: 权限列表（如 archive:read, user:manage）
    - roles: 角色列表（如 SYSTEM_ADMIN, BUSINESS_USER）
    - userId: 用户ID
    - username: 用户名
    - fullName: 姓名
    - allowedFonds: 可访问全宗列表

    **权限格式:**
    - 资源权限: {资源}:{操作} (如 archive:read)
    - 角色权限: ROLE_{角色编码} (如 ROLE_SYSTEM_ADMIN)
    - 特殊权限: nav:all (全部导航权限)

    **使用场景:**
    - 前端权限控制
    - 按钮显示/隐藏判断
    - 路由权限校验
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    /**
     * 获取当前用户权限
     */
    @GetMapping("/permissions")
    @Operation(
        summary = "获取当前用户权限",
        description = """
            获取当前登录用户的权限和角色信息。

            **请求头:**
            - Authorization: Bearer {token}

            **返回数据包括:**
            - permissions: 权限列表
              - 资源权限: archive:read, user:manage
              - 角色权限: ROLE_SYSTEM_ADMIN
            - roles: 角色列表
              - code: 角色编码
            - userId: 用户ID
            - username: 用户名
            - fullName: 姓名
            - allowedFonds: 可访问全宗列表

            **权限说明:**
            - 资源权限: 控制功能访问
            - 角色权限: 标识用户角色
            - nav:all: 全部导航权限

            **使用场景:**
            - 前端权限控制
            - 按钮权限判断
            - 路由守卫
            """,
        operationId = "getUserPermissions",
        tags = {"用户信息"}
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登录")
    })
    public Map<String, Object> getPermissions() {
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

        return result;
    }
}
