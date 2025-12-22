// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework、等
// Output: RoleService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.enums.RoleCategory;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.PermissionMapper;
import com.nexusarchive.mapper.RoleMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 角色服务
 */
@Service
@RequiredArgsConstructor
public class RoleService {
    
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;
    private final PermissionMapper permissionMapper;
    
    /**
     * 分页查询角色列表
     */
    public Page<Role> getRoles(int page, int limit, String search) {
        Page<Role> pageObj = new Page<>(page, limit);
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        
        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w.like("name", search).or().like("code", search));
        }
        
        wrapper.orderByDesc("created_at");
        
        return roleMapper.selectPage(pageObj, wrapper);
    }
    
    /**
     * 获取所有角色（不分页）
     */
    public List<Role> getAllRoles() {
        return roleMapper.selectList(null);
    }
    
    /**
     * 根据ID获取角色
     */
    public Role getRoleById(String id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }
    
    /**
     * 根据编码获取角色
     */
    public Role getRoleByCode(String code) {
        return roleMapper.findByCode(code);
    }
    
    /**
     * 创建角色
     */
    public Role createRole(Role role) {
        // 验证角色编码唯一性
        Role existing = roleMapper.findByCode(role.getCode());
        if (existing != null) {
            throw new BusinessException("角色编码已存在");
        }
        
        // 验证角色类别
        if (role.getRoleCategory() == null || role.getRoleCategory().isEmpty()) {
            role.setRoleCategory(RoleCategory.BUSINESS_USER.getCode());
        }
        
        // 自动设置三员角色的互斥标志
        RoleCategory category = RoleCategory.fromCode(role.getRoleCategory());
        role.setIsExclusive(category.isExclusive());
        
        // 设置默认值
        if (role.getType() == null) {
            role.setType("custom");
        }
        if (role.getDataScope() == null) {
            role.setDataScope("self");
        }
        
        roleMapper.insert(role);
        return role;
    }
    
    /**
     * 更新角色
     */
    public void updateRole(String id, Role role) {
        Role existing = getRoleById(id);
        
        // 系统角色不允许修改
        if ("system".equals(existing.getType())) {
            throw new BusinessException("系统角色不允许修改");
        }
        
        // 如果修改了角色编码，检查唯一性
        if (!existing.getCode().equals(role.getCode())) {
            Role codeCheck = roleMapper.findByCode(role.getCode());
            if (codeCheck != null) {
                throw new BusinessException("角色编码已存在");
            }
        }
        
        // 自动设置三员角色的互斥标志
        if (role.getRoleCategory() != null) {
            RoleCategory category = RoleCategory.fromCode(role.getRoleCategory());
            role.setIsExclusive(category.isExclusive());
        }
        
        role.setId(id);
        roleMapper.updateById(role);
    }
    
    /**
     * 删除角色
     */
    public void deleteRole(String id) {
        Role role = getRoleById(id);
        
        // 系统角色不允许删除
        if ("system".equals(role.getType())) {
            throw new BusinessException("系统角色不允许删除");
        }
        
        // TODO: 检查是否有用户使用此角色
        
        roleMapper.deleteById(id);
    }
    
    /**
     * 获取权限列表
     */
    /**
     * 获取权限列表
     */
    public List<Permission> getPermissions() {
        List<com.nexusarchive.entity.Permission> dbPerms = permissionMapper.selectList(null);
        if (dbPerms != null && !dbPerms.isEmpty()) {
            return dbPerms.stream()
                    .map(p -> new Permission(p.getPermKey(), p.getLabel(), p.getGroupName()))
                    .toList();
        }

        // fallback to defaults
        return Arrays.asList(
                new Permission("manage_org", "组织架构管理", "组织架构"),
                new Permission("view_org", "查看组织架构", "组织架构"),
                new Permission("manage_users", "用户管理", "用户管理"),
                new Permission("view_users", "查看用户", "用户管理"),
                new Permission("reset_password", "重置密码", "用户管理"),
                new Permission("manage_roles", "角色管理", "角色权限"),
                new Permission("view_roles", "查看角色", "角色权限"),
                new Permission("manage_archives", "档案管理", "档案管理"),
                new Permission("view_archives", "查看档案", "档案管理"),
                new Permission("export_archives", "导出档案", "档案管理"),
                new Permission("delete_archives", "删除档案", "档案管理"),
                new Permission("borrow_archives", "借阅申请", "档案借阅"),
                new Permission("approve_borrow", "借阅审批", "档案借阅"),
                new Permission("view_borrow", "查看借阅记录", "档案借阅"),
                new Permission("audit_logs", "审计日志查看", "审计日志"),
                new Permission("export_logs", "导出日志", "审计日志"),
                new Permission("manage_settings", "系统设置", "系统设置"),
                new Permission("view_dashboard", "查看仪表盘", "系统设置")
        );
    }
    
    /**
     * 权限定义
     */
    public static class Permission {
        public String key;
        public String label;
        public String group;
        
        public Permission(String key, String label, String group) {
            this.key = key;
            this.label = label;
            this.group = group;
        }
    }
}
