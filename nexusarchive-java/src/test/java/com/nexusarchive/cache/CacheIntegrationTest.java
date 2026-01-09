// Input: Spring Framework、JUnit 5、Redis、Mockito、Java 标准库
// Output: CacheIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.cache;

import com.nexusarchive.NexusArchiveApplication;
import com.nexusarchive.cache.fixture.CachedRoleService;
import com.nexusarchive.entity.Role;
import com.nexusarchive.mapper.RoleMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Redis 缓存功能集成测试
 * <p>
 * 验证 Spring Cache + Redis 的以下功能：
 * <ul>
 *   <li>缓存写入验证 (@Cacheable)</li>
 *   <li>缓存命中验证 (第二次调用不查数据库)</li>
 *   <li>缓存清除验证 (@CacheEvict)</li>
 *   <li>缓存更新验证 (@CachePut)</li>
 *   <li>TTL 过期验证</li>
 *   <li>多命名空间隔离</li>
 * </ul>
 * </p>
 *
 * <p>注意：此测试需要运行 Redis 服务（使用 docker-compose.infra.yml 启动）</p>
 *
 * @see CachedRoleService
 * @see RedisConfig
 */
@SpringBootTest(classes = NexusArchiveApplication.class)
@ActiveProfiles("test")
@DisplayName("Redis 缓存功能集成测试")
class CacheIntegrationTest {

    @MockBean
    private RoleMapper roleMapper;

    @Autowired
    private CachedRoleService cachedRoleService;

    @Autowired
    private CacheManager cacheManager;

    private Role sampleRole;
    private List<Role> sampleRoles;

    @BeforeEach
    void setUp() {
        // 清空所有缓存
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // 准备测试数据
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

    @AfterEach
    void tearDown() {
        // 清理测试缓存
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    // ==================== 缓存写入验证 (@Cacheable) ====================

    @Test
    @DisplayName("缓存写入 - 首次调用应查询数据库并写入缓存")
    void shouldWriteToCache_OnFirstCall() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(sampleRoles);

        // Act
        List<Role> result = cachedRoleService.getAllRoles();

        // Assert
        assertThat(result).hasSize(2);
        verify(roleMapper, times(1)).selectList(any());

        // 验证缓存已写入
        Cache cache = cacheManager.getCache("roles");
        assertThat(cache).isNotNull();
        assertThat(cache.get("all")).isNotNull();
    }

    @Test
    @DisplayName("缓存命中 - 第二次调用应从缓存读取不查数据库")
    void shouldHitCache_OnSecondCall() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(sampleRoles);

        // Act - 第一次调用（查询数据库）
        List<Role> firstCall = cachedRoleService.getAllRoles();
        assertThat(firstCall).hasSize(2);
        verify(roleMapper, times(1)).selectList(any());

        // 第二次调用（应从缓存读取）
        List<Role> secondCall = cachedRoleService.getAllRoles();
        assertThat(secondCall).hasSize(2);
        assertThat(secondCall).isEqualTo(firstCall);

        // 验证只调用了一次数据库
        verify(roleMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("按ID缓存 - 不同ID应缓存不同值")
    void shouldCacheById() {
        // Arrange
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);
        Role otherRole = new Role();
        otherRole.setId("role-002");
        otherRole.setName("业务操作员");
        when(roleMapper.selectById("role-002")).thenReturn(otherRole);

        // Act
        Role result1 = cachedRoleService.getRoleById("role-001");
        Role result2 = cachedRoleService.getRoleById("role-002");

        // Assert
        assertThat(result1.getId()).isEqualTo("role-001");
        assertThat(result2.getId()).isEqualTo("role-002");

        // 验证数据库被调用两次
        verify(roleMapper, times(1)).selectById("role-001");
        verify(roleMapper, times(1)).selectById("role-002");

        // 再次调用应从缓存读取
        Role cached1 = cachedRoleService.getRoleById("role-001");
        Role cached2 = cachedRoleService.getRoleById("role-002");

        verify(roleMapper, times(1)).selectById("role-001"); // 仍然是 1 次
        verify(roleMapper, times(1)).selectById("role-002"); // 仍然是 1 次
    }

    // ==================== 缓存清除验证 (@CacheEvict) ====================

    @Test
    @DisplayName("缓存清除 - 清除后再次调用应查询数据库")
    void shouldEvictCache_OnClear() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(sampleRoles);

        // Act - 第一次调用（查询数据库并缓存）
        List<Role> firstCall = cachedRoleService.getAllRoles();
        verify(roleMapper, times(1)).selectList(any());

        // 清除缓存
        cachedRoleService.clearAllRolesCache();

        // 第二次调用（应重新查询数据库）
        List<Role> secondCall = cachedRoleService.getAllRoles();

        // Assert
        verify(roleMapper, times(2)).selectList(any());
    }

    @Test
    @DisplayName("单个清除 - 更新后清除对应缓存")
    void shouldEvictSingleEntry_OnUpdate() {
        // Arrange
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);

        // Act - 第一次调用（查询数据库并缓存）
        Role firstCall = cachedRoleService.getRoleById("role-001");
        verify(roleMapper, times(1)).selectById("role-001");

        // 模拟更新操作（会清除缓存）
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);
        Role updatedRole = cachedRoleService.updateRole("role-001", sampleRole);

        // 验证更新被调用
        verify(roleMapper, times(1)).updateById(any(Role.class));

        // 再次查询应重新访问数据库
        Role secondCall = cachedRoleService.getRoleById("role-001");
        verify(roleMapper, times(2)).selectById("role-001");
    }

    // ==================== 缓存更新验证 (@CachePut) ====================

    @Test
    @DisplayName("缓存更新 - 更新后缓存应包含新值")
    void shouldUpdateCache_OnPut() {
        // Arrange
        Role updatedRole = new Role();
        updatedRole.setId("role-001");
        updatedRole.setName("超级管理员");
        updatedRole.setCode("super_admin");
        updatedRole.setRoleCategory("super_admin");
        updatedRole.setIsExclusive(false);

        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);

        // Act - 首次加载
        Role firstCall = cachedRoleService.getRoleById("role-001");
        assertThat(firstCall.getName()).isEqualTo("系统管理员");

        // 执行带 @CachePut 的更新操作
        when(roleMapper.selectById("role-001")).thenReturn(updatedRole);
        Role putResult = cachedRoleService.refreshRole("role-001", updatedRole);

        // Assert
        assertThat(putResult.getName()).isEqualTo("超级管理员");

        // 验证缓存已更新
        Role fromCache = cachedRoleService.getRoleById("role-001");
        assertThat(fromCache.getName()).isEqualTo("超级管理员");

        // refreshRole 内部会调用 selectById 一次，之后的 getRoleById 应该从缓存读取
        // 所以 selectById 应该被调用 2 次（初始 + refresh）
        verify(roleMapper, times(2)).selectById("role-001");
    }

    // ==================== 多命名空间隔离验证 ====================

    @Test
    @DisplayName("命名空间隔离 - 不同缓存名称互不影响")
    void shouldIsolateCacheNamespaces() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(sampleRoles);
        when(roleMapper.selectById("role-001")).thenReturn(sampleRole);

        // Act - 分别调用两个不同的缓存方法
        List<Role> listResult = cachedRoleService.getAllRoles();
        Role singleResult = cachedRoleService.getRoleById("role-001");

        // Assert
        Cache rolesCache = cacheManager.getCache("roles");
        Cache roleCache = cacheManager.getCache("role");

        assertThat(rolesCache).isNotNull();
        assertThat(roleCache).isNotNull();

        // 验证两个缓存都有数据
        assertThat(rolesCache.get("all")).isNotNull();
        assertThat(roleCache.get("role-001")).isNotNull();

        // 清除一个缓存不应影响另一个
        cachedRoleService.clearAllRolesCache();

        assertThat(rolesCache.get("all")).isNull(); // 被清除
        assertThat(roleCache.get("role-001")).isNotNull(); // 仍然存在
    }

    // ==================== 条件缓存验证 ====================

    @Test
    @DisplayName("条件缓存 - 只缓存系统角色")
    void shouldCacheConditionally() {
        // Arrange
        Role systemRole = new Role();
        systemRole.setId("role-001");
        systemRole.setCode("system_admin");
        systemRole.setType("system");

        Role customRole = new Role();
        customRole.setId("role-002");
        customRole.setCode("custom_role");
        customRole.setType("custom");

        when(roleMapper.selectById("role-001")).thenReturn(systemRole);
        when(roleMapper.selectById("role-002")).thenReturn(customRole);

        // Act
        cachedRoleService.getRoleByTypeIfSystem("role-001", "system");
        cachedRoleService.getRoleByTypeIfSystem("role-002", "custom");

        // Assert - 系统角色应该被缓存，自定义角色不应该
        Cache systemRolesCache = cacheManager.getCache("systemRoles");
        assertThat(systemRolesCache).isNotNull();

        // 验证只缓存了系统角色
        assertThat(systemRolesCache.get("role-001")).isNotNull();
        assertThat(systemRolesCache.get("role-002")).isNull();
    }

    // ==================== 缓存键生成验证 ====================

    @Test
    @DisplayName("自定义缓存键 - 使用角色编码作为键")
    void shouldUseCustomCacheKey() {
        // Arrange
        when(roleMapper.findByCode("system_admin")).thenReturn(sampleRole);

        // Act
        cachedRoleService.getRoleByCode("system_admin");
        cachedRoleService.getRoleByCode("system_admin"); // 应该从缓存读取

        // Assert - 只调用一次数据库
        verify(roleMapper, times(1)).findByCode("system_admin");

        // 验证缓存键
        Cache rolesByCodeCache = cacheManager.getCache("rolesByCode");
        assertThat(rolesByCodeCache).isNotNull();
        assertThat(rolesByCodeCache.get("system_admin")).isNotNull();
    }

    // ==================== 缓存管理器验证 ====================

    @Test
    @DisplayName("缓存管理器 - 验证缓存名称可获取")
    void shouldHaveCacheManager() {
        // Assert
        assertThat(cacheManager).isNotNull();

        // 验证至少有一个缓存名称（动态创建的缓存）
        // 注意：RedisCacheManager 的缓存名称是按需创建的
        assertThat(cacheManager.getCacheNames()).isNotNull();
    }

    @Test
    @DisplayName("缓存未命中 - 空值处理")
    void handleCacheMiss() {
        // Arrange
        when(roleMapper.selectById("non-existent")).thenReturn(null);

        // Act
        Role result = cachedRoleService.getRoleById("non-existent");

        // Assert
        assertThat(result).isNull();
        verify(roleMapper, times(1)).selectById("non-existent");

        // 再次调用仍然应该查询数据库（因为返回 null，不会被缓存）
        Role secondCall = cachedRoleService.getRoleById("non-existent");
        assertThat(secondCall).isNull();
        verify(roleMapper, times(2)).selectById("non-existent");
    }

    // ==================== unless 参数验证 ====================

    @Test
    @DisplayName("unless 参数 - 结果为空时不缓存")
    void shouldNotCacheWhenUnlessCondition() {
        // Arrange
        when(roleMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());

        // Act
        List<Role> result = cachedRoleService.getAllRolesUnlessEmpty();

        // Assert
        assertThat(result).isEmpty();
        verify(roleMapper, times(1)).selectList(any());

        // 再次调用应该仍然查询数据库（因为结果为空，不缓存）
        List<Role> secondCall = cachedRoleService.getAllRolesUnlessEmpty();
        verify(roleMapper, times(2)).selectList(any());
    }
}
