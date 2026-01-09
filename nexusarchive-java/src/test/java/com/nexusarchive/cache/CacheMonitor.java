// Input: Spring Framework、Java 标准库
// Output: CacheMonitor 缓存监控工具类
// Pos: 测试辅助工具
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存监控工具类
 * <p>
 * 用于监控和统计 Spring Cache 的使用情况，包括：
 * <ul>
 *   <li>缓存命中率统计</li>
 *   <li>缓存大小监控</li>
 *   <li>缓存访问日志</li>
 *   <li>性能指标计算</li>
 * </ul>
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * CacheMonitor monitor = new CacheMonitor(cacheManager);
 * monitor.recordAccess("roles", true);  // 记录缓存命中
 * monitor.recordAccess("roles", false); // 记录缓存未命中
 * CacheStats stats = monitor.getStats("roles");
 * System.out.println("命中率: " + stats.getHitRate());
 * }</pre>
 */
public class CacheMonitor {

    private final CacheManager cacheManager;
    private final Map<String, CacheStats> statsMap;

    public CacheMonitor(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.statsMap = new ConcurrentHashMap<>();
    }

    /**
     * 记录缓存访问
     *
     * @param cacheName 缓存名称
     * @param hit       是否命中
     */
    public void recordAccess(String cacheName, boolean hit) {
        statsMap.computeIfAbsent(cacheName, k -> new CacheStats(k))
                .recordAccess(hit);
    }

    /**
     * 获取指定缓存的统计信息
     *
     * @param cacheName 缓存名称
     * @return 缓存统计信息
     */
    public CacheStats getStats(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        CacheStats stats = statsMap.getOrDefault(cacheName, new CacheStats(cacheName));

        if (cache != null) {
            stats.setCacheExists(true);
        }

        return stats;
    }

    /**
     * 获取所有缓存的统计信息
     *
     * @return 缓存名称到统计信息的映射
     */
    public Map<String, CacheStats> getAllStats() {
        Map<String, CacheStats> result = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            result.put(cacheName, getStats(cacheName));
        }

        return result;
    }

    /**
     * 重置指定缓存的统计信息
     *
     * @param cacheName 缓存名称
     */
    public void resetStats(String cacheName) {
        statsMap.remove(cacheName);
    }

    /**
     * 重置所有统计信息
     */
    public void resetAllStats() {
        statsMap.clear();
    }

    /**
     * 打印统计报告
     */
    public void printReport() {
        System.out.println("\n========== 缓存统计报告 ==========");
        System.out.println("生成时间: " + LocalDateTime.now());
        System.out.println("----------------------------------------");

        Map<String, CacheStats> allStats = getAllStats();

        if (allStats.isEmpty()) {
            System.out.println("暂无缓存统计数据");
        } else {
            for (CacheStats stats : allStats.values()) {
                System.out.println(stats);
            }
        }

        System.out.println("========================================\n");
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        private final String cacheName;
        private long hitCount = 0;
        private long missCount = 0;
        private LocalDateTime firstAccess;
        private LocalDateTime lastAccess;
        private boolean cacheExists = false;

        public CacheStats(String cacheName) {
            this.cacheName = cacheName;
        }

        public void recordAccess(boolean hit) {
            if (hit) {
                hitCount++;
            } else {
                missCount++;
            }

            LocalDateTime now = LocalDateTime.now();
            if (firstAccess == null) {
                firstAccess = now;
            }
            lastAccess = now;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public long getTotalAccess() {
            return hitCount + missCount;
        }

        /**
         * 计算缓存命中率
         *
         * @return 命中率（0-1之间），无访问记录返回0
         */
        public double getHitRate() {
            long total = getTotalAccess();
            return total == 0 ? 0 : (double) hitCount / total;
        }

        /**
         * 计算缓存命中率百分比
         *
         * @return 命中率百分比（0-100）
         */
        public double getHitRatePercentage() {
            return getHitRate() * 100;
        }

        public String getCacheName() {
            return cacheName;
        }

        public LocalDateTime getFirstAccess() {
            return firstAccess;
        }

        public LocalDateTime getLastAccess() {
            return lastAccess;
        }

        public boolean isCacheExists() {
            return cacheExists;
        }

        public void setCacheExists(boolean cacheExists) {
            this.cacheExists = cacheExists;
        }

        public Duration getAccessDuration() {
            if (firstAccess == null || lastAccess == null) {
                return Duration.ZERO;
            }
            return Duration.between(firstAccess, lastAccess);
        }

        @Override
        public String toString() {
            return String.format(
                    "缓存[%s]: 命中率=%.2f%% (%d/%d), 存在=%s, 时间跨度=%s",
                    cacheName,
                    getHitRatePercentage(),
                    hitCount,
                    getTotalAccess(),
                    cacheExists,
                    formatDuration(getAccessDuration())
            );
        }

        private String formatDuration(Duration duration) {
            long seconds = duration.getSeconds();
            if (seconds < 60) {
                return seconds + "秒";
            } else if (seconds < 3600) {
                return (seconds / 60) + "分钟";
            } else {
                return (seconds / 3600) + "小时";
            }
        }

        /**
         * 获取详细统计信息（JSON格式）
         */
        public String toJson() {
            return String.format(
                    "{\"cacheName\":\"%s\",\"hitCount\":%d,\"missCount\":%d," +
                    "\"hitRate\":%.4f,\"totalAccess\":%d,\"cacheExists\":%b}",
                    cacheName, hitCount, missCount, getHitRate(), getTotalAccess(), cacheExists
            );
        }
    }
}
