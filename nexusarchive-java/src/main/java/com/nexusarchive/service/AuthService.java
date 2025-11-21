package com.nexusarchive.service;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.response.LoginResponse;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.util.JwtUtil;
import com.nexusarchive.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 2. 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("用户已被禁用或锁定");
        }
        
        // 3. 验证密码
        if (!passwordUtil.verifyPassword(user.getPasswordHash(), request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 4. 生成Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        // 5. 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 6. 构造响应
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatar(),
                user.getDepartmentId()
        );
        
        return new LoginResponse(token, userInfo);
    }
    
    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
    
    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        return jwtUtil.extractUserId(token);
    }
}
