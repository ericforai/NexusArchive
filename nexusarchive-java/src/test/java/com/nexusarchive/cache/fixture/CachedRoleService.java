// Input: Spring Framework、MyBatis-Plus、Lombok、Java 标准库
// Output: CachedRoleService 测试辅助服务类
// Pos: 测试辅助类
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.cache.fixture;

import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 带缓存注解的角色服务（测试专用）
 * <p>
 * 此服务专门用于测试 Spring Cache + Redis 的各种注解功能。
 * 包含以下缓存注解的演示：
 * <ul>
 *   <li>@Cacheable - 缓存读取</li>
 *   <li>@CachePut - 缓存更新</li>
 *   <li>@CacheEvict - 缓存清除</li>
 *   <li>@CacheConfig - 类级别缓存配置</li>
 * </ul>
 * </p>
 */
@Service
@CacheConfig(cacheNames = "roles")
public class CachedRoleService {

    private final RoleMapper roleMapper;

    public CachedRoleService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public CachedRoleService() {
        this.roleMapper = null;
    }

    /**
     * 获取所有角色（使用默认缓存）
     * <p>
     * key = "all" 固定键，所有调用共享同一缓存
     * </p>
     */
    @Cacheable(key = "'all'")
    public List<Role> getAllRoles() {
        return roleMapper.selectList(null);
    }

    /**
     * 获取所有角色（结果为空时不缓存）
     * <p>
     * unless = "#result.isEmpty()" - 结果为空时不缓存
     * </p>
     */
    @Cacheable(key = "'all'", unless = "#result.isEmpty()")
    public List<Role> getAllRolesUnlessEmpty() {
        return roleMapper.selectList(null);
    }

    /**
     * 根据ID获取角色（使用单独的缓存命名空间）
     */
    @Cacheable(value = "role", key = "#id")
    public Role getRoleById(String id) {
        return roleMapper.selectById(id);
    }

    /**
     * 根据编码获取角色（使用单独的缓存命名空间和自定义键）
     */
    @Cacheable(value = "rolesByCode", key = "#code")
    public Role getRoleByCode(String code) {
        return roleMapper.findByCode(code);
    }

    /**
     * 条件缓存：只缓存系统角色
     * <p>
     * condition = "#type == 'system'" - 只在类型为 system 时缓存
     * </p>
     */
    @Cacheable(value = "systemRoles", key = "#id", condition = "#type == 'system'")
    public Role getRoleByTypeIfSystem(String id, String type) {
        return roleMapper.selectById(id);
    }

    /**
     * 更新角色并清除缓存
     * <p>
     * beforeInvocation = true - 方法执行前清除缓存
     * </p>
     */
    @CacheEvict(value = "role", key = "#id")
    public Role updateRole(String id, Role role) {
        roleMapper.updateById(role);
        return roleMapper.selectById(id);
    }

    /**
     * 刷新角色缓存（更新并写入新值）
     * <p>
     * @CachePut 总是执行方法并将结果写入缓存
     * </p>
     */
    @CachePut(value = "role", key = "#id")
    public Role refreshRole(String id, Role role) {
        roleMapper.updateById(role);
        return roleMapper.selectById(id);
    }

    /**
     * 清除所有角色缓存
     * <p>
     * allEntries = true - 清除缓存中的所有条目
     * </p>
     */
    @CacheEvict(allEntries = true)
    public void clearAllRolesCache() {
        // 只清除缓存，不执行数据库操作
    }

    /**
     * 根据ID删除角色并清除缓存
     */
    @CacheEvict(value = "role", key = "#id")
    public int deleteRole(String id) {
        return roleMapper.deleteById(id);
    }

    /**
     * 批量清除指定缓存命名空间
     */
    @CacheEvict(value = {"roles", "role", "rolesByCode"}, allEntries = true)
    public void clearAllRoleCaches() {
        // 清除多个缓存命名空间
    }
}
