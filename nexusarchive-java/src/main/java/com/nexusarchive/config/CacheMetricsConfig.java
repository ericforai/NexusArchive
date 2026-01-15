// Input: Spring Framework、Micrometer
// Output: CacheMetricsConfig 类
// Pos: 配置层
package com.nexusarchive.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
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
 */
@Configuration
public class CacheMetricsConfig {

    /**
     * 为所有缓存注册指标
     * 注意：Redis 的 size/hit/miss 统计需要从 Redis INFO stats 获取
     * 这里我们注册缓存存在性的基础指标
     */
    @Bean
    public List<CacheMeterBinder> cacheMeterBinders(CacheManager cacheManager, MeterRegistry meterRegistry) {
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
