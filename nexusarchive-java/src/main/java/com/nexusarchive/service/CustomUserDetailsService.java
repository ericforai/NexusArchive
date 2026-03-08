// Input: Jackson、Lombok、Spring Security、Spring Framework、等
// Output: CustomUserDetailsService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Role;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.SysUserFondsScopeMapper;
import com.nexusarchive.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 从数据库加载用户、角色、权限
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final SysUserFondsScopeMapper userFondsScopeMapper;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildUserDetails(user);
    }

    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildUserDetails(user);
    }

    private UserDetails buildUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        List<Role> roles = roleMapper.findByUserId(user.getId());

        for (Role role : roles) {
            // 角色前缀 ROLE_ 以符合 Spring Security 习惯
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode()));
            String upperRoleCode = role.getCode() != null ? role.getCode().toUpperCase() : null;
            if (upperRoleCode != null && !upperRoleCode.equals(role.getCode())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + upperRoleCode));
            }
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                try {
                    List<String> perms = objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {});
                    perms.stream()
                            .filter(p -> p != null && !p.isEmpty())
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                } catch (Exception ignored) {
                    // 权限格式异常时，不阻断登录，但不加入额外权限
                }
            }
        }

        boolean locked = "locked".equalsIgnoreCase(user.getStatus());
        boolean disabled = "disabled".equalsIgnoreCase(user.getStatus());

        // 获取用户允许访问的全宗列表
        List<String> allowedFonds = userFondsScopeMapper.findFondsNoByUserId(user.getId());

        return new com.nexusarchive.security.CustomUserDetails(
                user.getUsername(),
                user.getPasswordHash(),
                authorities,
                user.getId(),
                user.getFullName(),
                user.getOrgCode(),
                user.getOrganizationId(), // 组织ID（用于用户归属，不参与数据隔离）
                allowedFonds, // 允许访问的全宗列表（数据隔离键）
                true,
                !locked,
                true,
                !disabled
        );
    }
}
