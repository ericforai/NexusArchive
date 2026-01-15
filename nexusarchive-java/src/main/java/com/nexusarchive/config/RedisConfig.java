// Input: Spring Framework、Java 标准库
// Output: RedisConfig 类 - Redis 缓存配置，支持多命名空间与 TTL
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置
 *
 * <p>缓存命名空间与 TTL 配置：</p>
 * <table>
 *   <tr><th>命名空间</th><th>TTL</th><th>用途</th></tr>
 *   <tr><td>permissions</td><td>1 小时</td><td>用户权限缓存</td></tr>
 *   <tr><td>roles</td><td>1 小时</td><td>角色数据缓存</td></tr>
 *   <tr><td>stats</td><td>5 分钟</td><td>统计数据缓存</td></tr>
 *   <tr><td>fonds</td><td>30 分钟</td><td>全宗数据缓存</td></tr>
 *   <tr><td>users</td><td>15 分钟</td><td>用户信息缓存</td></tr>
 *   <tr><td>loginAttempts</td><td>15 分钟</td><td>登录尝试记录</td></tr>
 *   <tr><td>rateLimit</td><td>1 分钟</td><td>请求限流计数</td></tr>
 *   <tr><td>archiveStats</td><td>10 分钟</td><td>归档统计</td></tr>
 *   <tr><td>menuCache</td><td>1 小时</td><td>菜单数据</td></tr>
 *   <tr><td>dictCache</td><td>2 小时</td><td>数据字典</td></tr>
 *   <tr><td>systemConfig</td><td>30 分钟</td><td>系统配置</td></tr>
 *   <tr><td>orgTree</td><td>30 分钟</td><td>组织架构树</td></tr>
 *   <tr><td>fondsScope</td><td>30 分钟</td><td>全宗权限范围</td></tr>
 *   <tr><td>erpConfig</td><td>30 分钟</td><td>ERP 配置</td></tr>
 *   <tr><td>entityConfig</td><td>30 分钟</td><td>法人配置</td></tr>
 *   <tr><td>archiveVoucherMapping</td><td>30 分钟</td><td>档案→凭证映射（穿透联查）</td></tr>
 *   <tr><td>default</td><td>30 分钟</td><td>默认缓存</td></tr>
 * </table>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Cacheable(value = "permissions", key = "#userId")
 * public Set<String> getUserPermissions(Long userId) { ... }
 * }</pre>
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    /**
     * 默认缓存配置
     */
    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
    }

    /**
     * RedisTemplate 配置，用于直接操作 Redis
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager 配置，支持多命名空间与不同 TTL
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // permissions 缓存 - 1 小时
        cacheConfigurations.put("permissions",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // roles 缓存 - 1 小时
        cacheConfigurations.put("roles",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // stats 缓存 - 5 分钟
        cacheConfigurations.put("stats",
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // fonds 缓存 - 30 分钟
        cacheConfigurations.put("fonds",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // users 缓存 - 15 分钟
        cacheConfigurations.put("users",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // loginAttempts 缓存 - 15 分钟（用于登录锁定）
        cacheConfigurations.put("loginAttempts",
                defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // rateLimit 缓存 - 1 分钟（用于请求限流）
        cacheConfigurations.put("rateLimit",
                defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // archiveStats 缓存 - 10 分钟（归档统计）
        cacheConfigurations.put("archiveStats",
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // menuCache 缓存 - 1 小时（菜单数据）
        cacheConfigurations.put("menuCache",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // dictCache 缓存 - 2 小时（数据字典）
        cacheConfigurations.put("dictCache",
                defaultConfig.entryTtl(Duration.ofHours(2)));

        // systemConfig 缓存 - 30 分钟（系统配置）
        cacheConfigurations.put("systemConfig",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // orgTree 缓存 - 30 分钟（组织架构树）
        cacheConfigurations.put("orgTree",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // fondsScope 缓存 - 30 分钟（全宗权限范围）
        cacheConfigurations.put("fondsScope",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // erpConfig 缓存 - 30 分钟（ERP 配置）
        cacheConfigurations.put("erpConfig",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // entityConfig 缓存 - 30 分钟（法人配置）
        cacheConfigurations.put("entityConfig",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // archiveVoucherMapping 缓存 - 30 分钟（档案→凭证映射，用于穿透联查）
        cacheConfigurations.put("archiveVoucherMapping",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * 自定义 Key 生成器
     * 格式: 类名.方法名.参数Hash
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(".");
            sb.append(method.getName());
            sb.append(".");
            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString().hashCode());
                }
            }
            return sb.toString();
        };
    }

    /**
     * 缓存异常处理器
     * 缓存异常时不影响业务流程
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // 缓存读取失败时，直接返回 null，不影响业务
                // 生产环境应记录日志
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                // 缓存写入失败时，忽略异常，不影响业务
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                // 缓存删除失败时，忽略异常
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                // 缓存清空失败时，忽略异常
            }
        };
    }

    // 不提供自定义 CacheResolver，使用默认实现
    @Override
    public CacheResolver cacheResolver() {
        return null;
    }
}
