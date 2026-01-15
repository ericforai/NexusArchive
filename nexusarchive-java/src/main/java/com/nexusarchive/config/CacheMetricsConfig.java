// Input: Spring Framework、Micrometer
// Output: CacheMetricsConfig 类 - 缓存指标监控配置
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存指标监控配置
 * <p>
 * 为所有缓存命名空间注册 Micrometer 指标，便于监控缓存命中率。
 * </p>
 *
 * <p>监控的缓存命名空间：</p>
 * <ul>
 *   <li>permissions - 用户权限缓存</li>
 *   <li>roles - 角色数据缓存</li>
 *   <li>stats - 统计数据缓存</li>
 *   <li>fonds - 全宗数据缓存</li>
 *   <li>users - 用户信息缓存</li>
 *   <li>loginAttempts - 登录尝试记录</li>
 *   <li>rateLimit - 请求限流计数</li>
 *   <li>archiveStats - 归档统计</li>
 *   <li>menuCache - 菜单数据</li>
 *   <li>dictCache - 数据字典</li>
 *   <li>systemConfig - 系统配置</li>
 *   <li>orgTree - 组织架构树</li>
 *   <li>fondsScope - 全宗权限范围</li>
 *   <li>erpConfig - ERP 配置</li>
 *   <li>entityConfig - 法人配置</li>
 *   <li>archiveVoucherMapping - 档案→凭证映射</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * // 在 Prometheus/Grafana 中查询缓存指标
 * cache_requests_total{cache="permissions",result="unknown"}
 * cache_requests_total{cache="roles",result="unknown"}
 * }</pre>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>Redis 的 size/hit/miss 统计需要从 Redis INFO stats 获取</li>
 *   <li>当前实现注册缓存存在性的基础指标</li>
 *   <li>如需完整统计，需配合 Redis INFO 命令实现</li>
 * </ul>
 *
 * @see RedisConfig 缓存命名空间与 TTL 配置
 */
@Slf4j
@Configuration
public class CacheMetricsConfig {

    /**
     * 为所有缓存注册指标
     * <p>
     * 注意：Redis 的 size/hit/miss 统计需要从 Redis INFO stats 获取
     * 这里我们注册缓存存在性的基础指标
     * </p>
     *
     * @param cacheManager 缓存管理器
     * @param meterRegistry Micrometer 指标注册表
     * @return 缓存指标绑定器列表
     */
    @Bean
    public List<CacheMeterBinder> cacheMeterBinders(CacheManager cacheManager, MeterRegistry meterRegistry) {
        log.info("注册缓存指标，缓存数量: {}", cacheManager.getCacheNames().size());
        List<CacheMeterBinder> binders = new ArrayList<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                binders.add(new CacheMeterBinder(cache, cacheName, List.of("cache", cacheName)) {
                    @Override
                    protected Long size() {
                        // Redis 不支持直接获取 size，返回 null
                        return null;
                    }

                    @Override
                    protected long hitCount() {
                        // 从 Redis INFO stats 获取，这里返回 0
                        return 0;
                    }

                    @Override
                    protected Long missCount() {
                        // 从 Redis INFO stats 获取，这里返回 null
                        return null;
                    }

                    @Override
                    protected Long evictionCount() {
                        return null;
                    }

                    @Override
                    protected long putCount() {
                        return 0;
                    }

                    @Override
                    protected void bindImplementationSpecificMetrics(MeterRegistry registry) {
                        // 为每个缓存注册一个简单的计数器
                        // 实际的 hit/miss 统计可以从 Redis INFO stats 获取
                        Counter.builder("cache.requests")
                            .tag("cache", cacheName)
                            .tag("result", "unknown")
                            .description("Cache requests (note: hit/miss stats from Redis INFO stats)")
                            .register(registry);
                    }
                });
            }
        }
        return binders;
    }
}
