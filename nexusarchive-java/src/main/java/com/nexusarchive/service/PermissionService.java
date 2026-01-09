// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: PermissionService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Permission;
import com.nexusarchive.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限服务
 * 缓存策略: 使用 permissions 缓存空间，TTL 1 小时
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    /**
     * 获取所有权限
     * 缓存键: permissions:all
     */
    @Cacheable(value = "permissions", key = "'all'")
    public List<Permission> listAll() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getGroupName)
                .orderByAsc(Permission::getPermKey));
    }

    /**
     * 创建权限
     * 清除缓存: permissions:all
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public Permission create(Permission permission) {
        // Check if key exists
        Long count = permissionMapper.selectCount(new LambdaQueryWrapper<Permission>()
                .eq(Permission::getPermKey, permission.getPermKey()));
        if (count > 0) {
            throw new RuntimeException("权限标识已存在: " + permission.getPermKey());
        }
        permissionMapper.insert(permission);
        return permission;
    }

    /**
     * 更新权限
     * 清除缓存: permissions:all
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public Permission update(String id, Permission permission) {
        Permission existing = permissionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("权限不存在");
        }
        existing.setLabel(permission.getLabel());
        existing.setGroupName(permission.getGroupName());
        // permKey usually not editable to avoid breaking code references, but let's allow if needed or keep it safe
        // existing.setPermKey(permission.getPermKey()); 
        permissionMapper.updateById(existing);
        return existing;
    }

    /**
     * 删除权限
     * 清除缓存: permissions:all
     */
    @Transactional
    @CacheEvict(value = "permissions", allEntries = true)
    public void delete(String id) {
        permissionMapper.deleteById(id);
    }
}
