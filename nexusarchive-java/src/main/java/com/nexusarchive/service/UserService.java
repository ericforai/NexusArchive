// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: UserService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import com.nexusarchive.dto.response.UserResponse;
import com.nexusarchive.entity.User;
import com.nexusarchive.mapper.RoleMapper;
import com.nexusarchive.mapper.UserMapper;
import com.nexusarchive.service.RoleValidationService;
import com.nexusarchive.util.PasswordPolicyValidator;
import com.nexusarchive.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
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
        PasswordPolicyValidator.validate(request.getPassword());
        String passwordHash = passwordUtil.hashPassword(request.getPassword());
        // 构建实体 - 应用 XSS 过滤
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        user.setUsername(request.getUsername()); // 用户名保持原样
        user.setPasswordHash(passwordHash);
        user.setFullName(com.nexusarchive.util.XssFilter.clean(request.getFullName())); // XSS 过滤
        user.setEmail(com.nexusarchive.util.XssFilter.clean(request.getEmail())); // XSS 过滤
        user.setPhone(com.nexusarchive.util.XssFilter.clean(request.getPhone())); // XSS 过滤
        user.setAvatar(request.getAvatar());
        user.setDepartmentId(request.getDepartmentId());
        user.setStatus("active");
        user.setCreatedTime(LocalDateTime.now());
        user.setLastModifiedTime(LocalDateTime.now());
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
        existing.setFullName(com.nexusarchive.util.XssFilter.clean(request.getFullName()));
        existing.setEmail(com.nexusarchive.util.XssFilter.clean(request.getEmail()));
        existing.setPhone(com.nexusarchive.util.XssFilter.clean(request.getPhone()));
        existing.setAvatar(request.getAvatar());
        existing.setDepartmentId(request.getDepartmentId());
        existing.setLastModifiedTime(LocalDateTime.now());
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
        user.setLastModifiedTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 根据 ID 查询用户
     */
    public UserResponse getUserById(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return toResponse(user);
    }

    /**
     * 分页查询用户（简化实现）
     */
    public Page<UserResponse> listPaged(int page, int limit, String search, String status) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        if (StringUtils.hasText(search)) {
            wrapper.and(w -> w.like("username", search).or().like("full_name", search));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_time");

        Page<User> pageObj = new Page<>(page, limit);
        Page<User> userPage = userMapper.selectPage(pageObj, wrapper);

        Page<UserResponse> respPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        respPage.setRecords(userPage.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
        respPage.setPages(userPage.getPages());
        return respPage;
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

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(String userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        PasswordPolicyValidator.validate(newPassword);
        String hash = passwordUtil.hashPassword(newPassword);
        user.setPasswordHash(hash);
        user.setLastModifiedTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public void updateStatus(String userId, String status) {
        if (!Arrays.asList("active", "disabled", "locked").contains(status)) {
            throw new BusinessException("非法状态值");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        user.setLastModifiedTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
