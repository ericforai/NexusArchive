// Input: JUnit 5、Mockito、Java 标准库
// Output: CacheAnnotationTest 单元测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.cache;

import com.nexusarchive.cache.fixture.CachedRoleService;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 缓存注解单元测试
 * <p>
 * 验证 CachedRoleService 中缓存注解的正确性，使用 Mock 验证调用次数。
 * </p>
 *
 * <p>测试场景：</p>
 * <ul>
 *   <li>验证 @Cacheable 注解是否正确应用</li>
 *   <li>验证 @CacheEvict 注解是否正确应用</li>
 *   <li>验证 @CachePut 注解是否正确应用</li>
 *   <li>验证条件缓存参数</li>
 * </ul>
 *
 * @see CachedRoleService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("缓存注解单元测试")
class CacheAnnotationTest {

    @Mock
    private RoleMapper roleMapper;

    private CachedRoleService cachedRoleService;

    private Role sampleRole;
    private List<Role> sampleRoles;

    @BeforeEach
    void setUp() {
        cachedRoleService = new CachedRoleService(roleMapper);

        sampleRole = new Role();
        sampleRole.setId("role-001");
        sampleRole.setName("系统管理员");
        sampleRole.setCode("system_admin");
        sampleRole.setRoleCategory("system_admin");
        sampleRole.setIsExclusive(true);
        sampleRole.setType("system");

        Role role2 = new Role();
        role2.setId("role-002");
        role2.setName("业务操作员");
        role2.setCode("business_user");
        role2.setRoleCategory("business_user");
        role2.setIsExclusive(false);
        role2.setType("custom");

        sampleRoles = Arrays.asList(sampleRole, role2);
    }

    // ==================== 注解存在性验证 ====================

    @Test
    @DisplayName("CachedRoleService 应该有 @CacheConfig 注解")
    void shouldHaveCacheConfigAnnotation() {
        // 验证类级别的 @CacheConfig 注解
        CacheConfig annotation = cachedRoleService.getClass().getAnnotation(CacheConfig.class);
        assertNotNull(annotation, "CachedRoleService 应该有 @CacheConfig 注解");
        assertArrayEquals(new String[]{"roles"}, annotation.cacheNames(), "缓存名称应该是 'roles'");
    }

    @Test
    @DisplayName("getAllRoles 方法应该有 @Cacheable 注解")
    void shouldHaveCacheableAnnotation() throws NoSuchMethodException {
        // 验证方法级别的 @Cacheable 注解
        Method method = cachedRoleService.getClass().getMethod("getAllRoles");
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        assertNotNull(annotation, "getAllRoles 方法应该有 @Cacheable 注解");
        assertEquals("'all'", annotation.key(), "缓存键应该是 'all'");
    }

    @Test
    @DisplayName("clearAllRolesCache 方法应该有 @CacheEvict 注解")
    void shouldHaveCacheEvictAnnotation() throws NoSuchMethodException {
        Method method = cachedRoleService.getClass().getMethod("clearAllRolesCache");
        CacheEvict annotation = method.getAnnotation(CacheEvict.class);
        assertNotNull(annotation, "clearAllRolesCache 方法应该有 @CacheEvict 注解");
        assertTrue(annotation.allEntries(), "应该清除所有条目");
    }

    @Test
    @DisplayName("refreshRole 方法应该有 @CachePut 注解")
    void shouldHaveCachePutAnnotation() throws NoSuchMethodException {
        Method method = cachedRoleService.getClass().getMethod("refreshRole", String.class, Role.class);
        CachePut annotation = method.getAnnotation(CachePut.class);
        assertNotNull(annotation, "refreshRole 方法应该有 @CachePut 注解");
        assertEquals("role", annotation.value()[0], "缓存命名空间应该是 'role'");
        assertEquals("#id", annotation.key(), "缓存键应该是 '#id'");
    }

    // ==================== 基本功能测试 ====================

    @Test
    @DisplayName("getAllRoles - 应该调用 mapper.selectList")
    void getAllRoles_shouldCallMapper() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(sampleRoles);

        // Act
        List<Role> result = cachedRoleService.getAllRoles();

        // Assert
        assertEquals(2, result.size());
        assertEquals("系统管理员", result.get(0).getName());
        verify(roleMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getRoleById - 应该调用 mapper.selectById")
    void getRoleById_shouldCallMapper() {
        // Arrange
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);

        // Act
        Role result = cachedRoleService.getRoleById("role-001");

        // Assert
        assertNotNull(result);
        assertEquals("role-001", result.getId());
        verify(roleMapper, times(1)).selectById("role-001");
    }

    @Test
    @DisplayName("getRoleByCode - 应该调用 mapper.findByCode")
    void getRoleByCode_shouldCallMapper() {
        // Arrange
        when(roleMapper.findByCode("system_admin")).thenReturn(sampleRole);

        // Act
        Role result = cachedRoleService.getRoleByCode("system_admin");

        // Assert
        assertNotNull(result);
        assertEquals("system_admin", result.getCode());
        verify(roleMapper, times(1)).findByCode("system_admin");
    }

    @Test
    @DisplayName("updateRole - 应该调用 mapper.updateById")
    void updateRole_shouldCallMapper() {
        // Arrange
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        // Act
        Role result = cachedRoleService.updateRole("role-001", sampleRole);

        // Assert
        verify(roleMapper, times(1)).updateById(any(Role.class));
        verify(roleMapper, times(1)).selectById("role-001");
    }

    @Test
    @DisplayName("deleteRole - 应该调用 mapper.deleteById")
    void deleteRole_shouldCallMapper() {
        // Arrange
        when(roleMapper.deleteById("role-001")).thenReturn(1);

        // Act
        int result = cachedRoleService.deleteRole("role-001");

        // Assert
        assertEquals(1, result);
        verify(roleMapper, times(1)).deleteById("role-001");
    }

    @Test
    @DisplayName("refreshRole - 应该更新并返回角色")
    void refreshRole_shouldUpdateAndReturn() {
        // Arrange
        Role updatedRole = new Role();
        updatedRole.setId("role-001");
        updatedRole.setName("超级管理员");

        when(roleMapper.updateById(any(Role.class))).thenReturn(1);
        when(roleMapper.selectById("role-001")).thenReturn(updatedRole);

        // Act
        Role result = cachedRoleService.refreshRole("role-001", updatedRole);

        // Assert
        assertEquals("超级管理员", result.getName());
        verify(roleMapper, times(1)).updateById(any(Role.class));
        verify(roleMapper, times(1)).selectById("role-001");
    }

    @Test
    @DisplayName("clearAllRolesCache - 清除缓存不应抛出异常")
    void clearAllRolesCache_shouldNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> cachedRoleService.clearAllRolesCache());
    }

    @Test
    @DisplayName("clearAllRoleCaches - 清除多个缓存不应抛出异常")
    void clearAllRoleCaches_shouldNotThrow() {
        // Act & Assert
        assertDoesNotThrow(() -> cachedRoleService.clearAllRoleCaches());
    }

    // ==================== 条件缓存测试 ====================

    @Test
    @DisplayName("getRoleByTypeIfSystem - 系统角色应该调用查询")
    void getRoleByTypeIfSystem_systemRole_shouldCallMapper() {
        // Arrange
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);

        // Act
        Role result = cachedRoleService.getRoleByTypeIfSystem("role-001", "system");

        // Assert
        assertNotNull(result);
        verify(roleMapper, times(1)).selectById("role-001");
    }

    @Test
    @DisplayName("getRoleByTypeIfSystem - 自定义角色应该调用查询")
    void getRoleByTypeIfSystem_customRole_shouldCallMapper() {
        // Arrange
        when(roleMapper.selectById("role-002")).thenReturn(sampleRoles.get(1));

        // Act
        Role result = cachedRoleService.getRoleByTypeIfSystem("role-002", "custom");

        // Assert
        assertNotNull(result);
        verify(roleMapper, times(1)).selectById("role-002");
    }

    @Test
    @DisplayName("getAllRolesUnlessEmpty - 空列表应该返回空")
    void getAllRolesUnlessEmpty_emptyList_shouldReturnEmpty() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(Arrays.asList());

        // Act
        List<Role> result = cachedRoleService.getAllRolesUnlessEmpty();

        // Assert
        assertTrue(result.isEmpty());
        verify(roleMapper, times(1)).selectList(any());
    }

    // ==================== 注解参数验证 ====================

    @Test
    @DisplayName("getRoleByCode - 验证缓存命名空间")
    void getRoleByCode_shouldHaveCustomCacheNamespace() throws NoSuchMethodException {
        Method method = cachedRoleService.getClass().getMethod("getRoleByCode", String.class);
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        assertNotNull(annotation);
        assertEquals("rolesByCode", annotation.value()[0], "缓存命名空间应该是 'rolesByCode'");
    }

    @Test
    @DisplayName("getRoleByTypeIfSystem - 验证条件缓存参数")
    void getRoleByTypeIfSystem_shouldHaveCondition() throws NoSuchMethodException {
        Method method = cachedRoleService.getClass().getMethod("getRoleByTypeIfSystem", String.class, String.class);
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        assertNotNull(annotation);
        assertEquals("#type == 'system'", annotation.condition(), "条件应该是 #type == 'system'");
    }

    @Test
    @DisplayName("getAllRolesUnlessEmpty - 验证 unless 参数")
    void getAllRolesUnlessEmpty_shouldHaveUnless() throws NoSuchMethodException {
        Method method = cachedRoleService.getClass().getMethod("getAllRolesUnlessEmpty");
        Cacheable annotation = method.getAnnotation(Cacheable.class);
        assertNotNull(annotation);
        assertEquals("#result.isEmpty()", annotation.unless(), "unless 应该是 #result.isEmpty()");
    }
}
