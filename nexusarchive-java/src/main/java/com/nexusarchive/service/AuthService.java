// Input: Jackson、Lombok、Spring Framework、Java 标准库、等
// Output: AuthService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.entity.Role;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.service.LicenseService;
import com.nexusarchive.service.LoginAttemptService;
import com.nexusarchive.service.TokenBlacklistService;
import com.nexusarchive.util.JwtUtil;
import com.nexusarchive.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final ObjectMapper objectMapper;
    private final LicenseService licenseService;
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        if (loginAttemptService.isLocked(request.getUsername())) {
            throw new BusinessException(401, "账户已锁定，请稍后再试");
        }

        // 1. 查询用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            loginAttemptService.recordFailure(request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 2. 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(401, "用户已被禁用或锁定");
        }
        
        // 3. 验证密码
        if (!passwordUtil.verifyPassword(user.getPasswordHash(), request.getPassword())) {
            loginAttemptService.recordFailure(request.getUsername());
            int remaining = loginAttemptService.getRemainingAttempts(request.getUsername());
            if (remaining == 0) {
                throw new BusinessException(401, "密码错误次数过多，账号已锁定15分钟");
            }
            throw new BusinessException(401, "用户名或密码错误");
        }

        loginAttemptService.recordSuccess(request.getUsername());
    
    // 4. license 检查 - 移至登录后由 LicenseValidationFilter 统一处理
    // 允许登录以便管理员可以通过 /api/license/load 加载 License
    // licenseService.assertValid(userMapper.countActiveUsers());
        // 5. 生成Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        // 5. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 6. 构造响应
        LoginResponse.UserInfo userInfo = buildUserInfo(user);
        
        return new LoginResponse(token, userInfo);
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String token) {
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new BusinessException("Token 已失效，请重新登录");
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Token 无效或已过期");
        }
        licenseService.assertValid(userMapper.countActiveUsers());
        String username = jwtUtil.extractUsername(token);
        String userId = jwtUtil.extractUserId(token);
        return jwtUtil.generateToken(username, userId);
    }

    /**
     * 登出并将旧 Token 加入黑名单
     */
    public void logout(String token) {
        try {
            Date exp = jwtUtil.extractExpiration(token);
            tokenBlacklistService.blacklist(token, exp.getTime());
        } catch (Exception ignored) {
            // token 已损坏则忽略
        }
    }
    
    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
    
    /**
     * 获取当前用户信息（基于Token）
     */
    public LoginResponse.UserInfo getCurrentUser(String token) {
        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new BusinessException("Token 已失效");
        }
        String userId = getUserIdFromToken(token);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在或已被删除");
        }
        return buildUserInfo(user);
    }
    
    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);
    }

    private LoginResponse.UserInfo buildUserInfo(User user) {
        List<Role> roles = roleMapper.findByUserId(user.getId());
        List<String> roleCodes = new ArrayList<>();
        List<String> roleNames = new ArrayList<>();
        Set<String> permissions = new HashSet<>();

        for (Role role : roles) {
            roleCodes.add(role.getCode());
            roleNames.add(role.getName());
            if (role.getPermissions() != null && !role.getPermissions().isEmpty()) {
                try {
                    List<String> perms = objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {});
                    perms.stream()
                            .filter(p -> p != null && !p.isEmpty())
                            .forEach(permissions::add);
                } catch (Exception ignored) {
                    // ignore parse errors
                }
            }
        }

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setFullName(user.getFullName());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setDepartmentId(user.getOrganizationId()); // 使用 organizationId（已替换 departmentId）
        userInfo.setRoles(roleCodes);
        userInfo.setRoleNames(roleNames);
        userInfo.setPermissions(new ArrayList<>(permissions));
        userInfo.setStatus(user.getStatus());

        // 新增字段：个人资料展示
        userInfo.setPhone(user.getPhone());
        userInfo.setEmployeeId(user.getEmployeeId());
        userInfo.setJobTitle(user.getJobTitle());
        userInfo.setOrgCode(user.getOrgCode());
        userInfo.setLastLoginAt(user.getLastLoginAt());
        userInfo.setCreatedTime(user.getCreatedTime());

        return userInfo;
    }
}
