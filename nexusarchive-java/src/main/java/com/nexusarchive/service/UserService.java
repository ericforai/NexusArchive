package com.nexusarchive.service;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.util.PasswordUtil;
import com.nexusarchive.service.RoleValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordUtil passwordUtil;
    private final RoleValidationService roleValidationService;

    /**
     * 创建用户
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        // 三员互斥校验
        roleValidationService.validateThreeRoleExclusion(null, request.getRoleIds());
        // 密码哈希
        String passwordHash = passwordUtil.hashPassword(request.getPassword());
        // 构建实体
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordHash);
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setDepartmentId(request.getDepartmentId());
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        // 保存用户
        userMapper.insert(user);
        // 关联角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            userMapper.insertUserRoles(user.getId(), request.getRoleIds());
        }
        return toResponse(user);
    }

    /**
     * 更新用户信息（不包括密码）
     */
    @Transactional
    public UserResponse updateUser(UpdateUserRequest request) {
        User existing = userMapper.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        // 三员互斥校验（排除自身已有角色）
        roleValidationService.validateThreeRoleExclusion(request.getId(), request.getRoleIds());
        // 更新字段
        existing.setFullName(request.getFullName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setAvatar(request.getAvatar());
        existing.setDepartmentId(request.getDepartmentId());
        existing.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(existing);
        // 更新角色关联
        userMapper.deleteUserRoles(existing.getId());
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            userMapper.insertUserRoles(existing.getId(), request.getRoleIds());
        }
        return toResponse(existing);
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional
    public void deleteUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 分页查询用户（简化实现）
     */
    public List<UserResponse> listAll() {
        List<User> users = userMapper.selectAll();
        return users.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private UserResponse toResponse(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setFullName(user.getFullName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setAvatar(user.getAvatar());
        resp.setDepartmentId(user.getDepartmentId());
        resp.setStatus(user.getStatus());
        // 角色列表
        List<String> roleIds = userMapper.selectRoleIdsByUserId(user.getId());
        resp.setRoleIds(roleIds);
        return resp;
    }
}
