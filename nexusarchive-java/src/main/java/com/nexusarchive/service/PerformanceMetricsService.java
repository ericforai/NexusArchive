// Input: Performance Metrics Entities
// Output: PerformanceMetricsService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.response.PerformanceMetricsReport;

import java.util.Map;

/**
 * 性能指标服务
 * 
 * 功能：
 * 1. 收集性能指标（单全宗容量、并发检索、最大文件大小、预览首屏时间、日志留存周期）
 * 2. 生成性能报告
 * 3. 性能告警
 * 
 * PRD 来源: Section 7.5 - 非功能指标
 */
public interface PerformanceMetricsService {
    
    /**
     * 记录单全宗容量
     * 
     * @param fondsNo 全宗号
     * @param capacityGB 容量（GB）
     */
    void recordFondsCapacity(String fondsNo, double capacityGB);
    
    /**
     * 记录并发检索数
     * 
     * @param concurrentCount 并发数
     */
    void recordConcurrentSearch(int concurrentCount);
    
    /**
     * 记录最大文件大小
     * 
     * @param fileSizeMB 文件大小（MB）
     */
    void recordMaxFileSize(double fileSizeMB);
    
    /**
     * 记录预览首屏时间
     * 
     * @param previewTimeMs 预览时间（毫秒）
     */
    void recordPreviewTime(long previewTimeMs);
    
    /**
     * 记录日志留存周期
     * 
     * @param retentionDays 留存天数
     */
    void recordLogRetention(int retentionDays);
    
    /**
     * 记录检索性能
     * 
     * @param fondsNo 全宗号
     * @param searchType 检索类型
     * @param durationMs 耗时（毫秒）
     * @param resultCount 结果数量
     * @param userId 用户ID
     */
    void recordSearchPerformance(String fondsNo, String searchType, 
                                long durationMs, int resultCount, String userId);
    
    /**
     * 获取性能指标报告
     * 
     * @param fondsNo 全宗号（可选）
     * @return 性能报告
     */
    PerformanceMetricsReport getPerformanceReport(String fondsNo);
    
    /**
     * 获取当前性能指标快照
     * 
     * @return 指标映射
     */
    Map<String, Object> getCurrentMetrics();
}


