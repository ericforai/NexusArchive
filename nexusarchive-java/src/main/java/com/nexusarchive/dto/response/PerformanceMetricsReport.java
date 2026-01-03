// Input: Java 标准库
// Output: PerformanceMetricsReport DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.Map;

/**
 * 性能指标报告 DTO
 */
@Data
public class PerformanceMetricsReport {
    
    /**
     * 单全宗容量（GB）
     */
    private Double fondsCapacityGB;
    
    /**
     * 最大并发检索数
     */
    private Integer maxConcurrentSearch;
    
    /**
     * 最大文件大小（MB）
     */
    private Double maxFileSizeMB;
    
    /**
     * 平均预览首屏时间（毫秒）
     */
    private Long avgPreviewTimeMs;
    
    /**
     * 日志留存周期（天）
     */
    private Integer logRetentionDays;
    
    /**
     * 检索性能统计
     */
    private Map<String, SearchPerformanceStats> searchStats;
    
    /**
     * 存储容量统计
     */
    private Map<String, StorageCapacityStats> storageStats;
    
    /**
     * 检索性能统计
     */
    @Data
    public static class SearchPerformanceStats {
        private Long avgDurationMs;
        private Long maxDurationMs;
        private Long minDurationMs;
        private Integer totalSearches;
        private Integer avgResultCount;
    }
    
    /**
     * 存储容量统计
     */
    @Data
    public static class StorageCapacityStats {
        private Double totalSizeGB;
        private Double usedSizeGB;
        private Double usagePercentage;
        private Long fileCount;
    }
}


