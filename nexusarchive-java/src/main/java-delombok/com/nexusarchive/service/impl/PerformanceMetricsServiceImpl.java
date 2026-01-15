// Input: PerformanceMetricsService, SystemPerformanceMetrics Entity
// Output: PerformanceMetricsServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.response.PerformanceMetricsReport;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.SystemPerformanceMetrics;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.mapper.SystemPerformanceMetricsMapper;
import com.nexusarchive.service.PerformanceMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 性能指标服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMetricsServiceImpl implements PerformanceMetricsService {
    
    private final SystemPerformanceMetricsMapper metricsMapper;
    private final BasFondsMapper basFondsMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final StringRedisTemplate redisTemplate;
    
    // 性能指标阈值
    private static final double MAX_FONDS_CAPACITY_GB = 1000.0; // 单全宗最大容量 1TB
    private static final int MAX_CONCURRENT_SEARCH = 100; // 最大并发检索数
    private static final double MAX_FILE_SIZE_MB = 500.0; // 最大文件大小 500MB
    private static final long MAX_PREVIEW_TIME_MS = 5000; // 最大预览首屏时间 5秒
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFondsCapacity(String fondsNo, double capacityGB) {
        saveMetric("FONDS_CAPACITY", "单全宗容量", capacityGB, "GB", fondsNo);
        
        // 检查是否超过阈值
        if (capacityGB > MAX_FONDS_CAPACITY_GB) {
            log.warn("全宗容量超过阈值: fondsNo={}, capacity={}GB, threshold={}GB", 
                fondsNo, capacityGB, MAX_FONDS_CAPACITY_GB);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordConcurrentSearch(int concurrentCount) {
        saveMetric("CONCURRENT_SEARCH", "并发检索数", concurrentCount, "count", null);
        
        // 检查是否超过阈值
        if (concurrentCount > MAX_CONCURRENT_SEARCH) {
            log.warn("并发检索数超过阈值: count={}, threshold={}", 
                concurrentCount, MAX_CONCURRENT_SEARCH);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordMaxFileSize(double fileSizeMB) {
        saveMetric("FILE_SIZE", "最大文件大小", fileSizeMB, "MB", null);
        
        // 检查是否超过阈值
        if (fileSizeMB > MAX_FILE_SIZE_MB) {
            log.warn("文件大小超过阈值: size={}MB, threshold={}MB", 
                fileSizeMB, MAX_FILE_SIZE_MB);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPreviewTime(long previewTimeMs) {
        saveMetric("PREVIEW_TIME", "预览首屏时间", previewTimeMs, "ms", null);
        
        // 检查是否超过阈值
        if (previewTimeMs > MAX_PREVIEW_TIME_MS) {
            log.warn("预览首屏时间超过阈值: time={}ms, threshold={}ms", 
                previewTimeMs, MAX_PREVIEW_TIME_MS);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordLogRetention(int retentionDays) {
        saveMetric("LOG_RETENTION", "日志留存周期", retentionDays, "days", null);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSearchPerformance(String fondsNo, String searchType, 
                                        long durationMs, int resultCount, String userId) {
        // TODO: 保存到 search_performance_stats 表
        log.debug("记录检索性能: fondsNo={}, type={}, duration={}ms, results={}", 
            fondsNo, searchType, durationMs, resultCount);
    }
    
    @Override
    public PerformanceMetricsReport getPerformanceReport(String fondsNo) {
        PerformanceMetricsReport report = new PerformanceMetricsReport();
        
        // 1. 获取单全宗容量
        Double fondsCapacity = getLatestMetric("FONDS_CAPACITY", fondsNo);
        report.setFondsCapacityGB(fondsCapacity);
        
        // 2. 获取最大并发检索数
        Integer maxConcurrent = getLatestMetric("CONCURRENT_SEARCH", null).intValue();
        report.setMaxConcurrentSearch(maxConcurrent);
        
        // 3. 获取最大文件大小
        Double maxFileSize = getLatestMetric("FILE_SIZE", null);
        report.setMaxFileSizeMB(maxFileSize);
        
        // 4. 获取平均预览首屏时间
        Long avgPreviewTime = getAverageMetric("PREVIEW_TIME", null).longValue();
        report.setAvgPreviewTimeMs(avgPreviewTime);
        
        // 5. 获取日志留存周期
        Integer logRetention = getLatestMetric("LOG_RETENTION", null).intValue();
        report.setLogRetentionDays(logRetention);
        
        // TODO: 填充检索性能统计和存储容量统计
        
        return report;
    }
    
    @Override
    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("fondsCapacityGB", getLatestMetric("FONDS_CAPACITY", null));
        metrics.put("maxConcurrentSearch", getLatestMetric("CONCURRENT_SEARCH", null));
        metrics.put("maxFileSizeMB", getLatestMetric("FILE_SIZE", null));
        metrics.put("avgPreviewTimeMs", getAverageMetric("PREVIEW_TIME", null));
        metrics.put("logRetentionDays", getLatestMetric("LOG_RETENTION", null));
        
        return metrics;
    }
    
    /**
     * 定时任务：收集性能指标
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void collectMetrics() {
        log.info("开始收集性能指标");
        
        try {
            // 1. 收集各全宗的容量
            collectFondsCapacities();
            
            // 2. 收集并发检索数
            collectConcurrentSearchCount();
            
            // 3. 收集最大文件大小
            collectMaxFileSize();
            
            log.info("性能指标收集完成");
        } catch (Exception e) {
            log.error("性能指标收集失败", e);
        }
    }
    
    /**
     * 收集各全宗的容量
     */
    private void collectFondsCapacities() {
        // 查询所有全宗（BasFonds 可能没有 status 字段，查询所有）
        List<BasFonds> fondsList = basFondsMapper.selectList(null);
        
        for (BasFonds fonds : fondsList) {
            try {
                // 查询该全宗下的所有文件大小总和
                List<ArcFileContent> files = arcFileContentMapper.selectList(
                    new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getFondsCode, fonds.getFondsCode())
                );
                
                long totalBytes = files.stream()
                    .mapToLong(file -> file.getFileSize() != null ? file.getFileSize() : 0L)
                    .sum();
                
                // 转换为 GB
                double capacityGB = totalBytes / (1024.0 * 1024.0 * 1024.0);
                
                // 记录指标
                recordFondsCapacity(fonds.getFondsCode(), capacityGB);
                
                log.debug("全宗容量统计: fondsNo={}, capacity={}GB", fonds.getFondsCode(), capacityGB);
            } catch (Exception e) {
                log.warn("收集全宗容量失败: fondsNo={}", fonds.getFondsCode(), e);
            }
        }
    }
    
    /**
     * 收集并发检索数
     */
    private void collectConcurrentSearchCount() {
        try {
            // 从 Redis 获取当前活跃的检索请求数
            // 使用 Redis Set 存储活跃的检索请求（在检索开始时添加，结束时移除）
            String key = "search:active:requests";
            Long count = redisTemplate.opsForSet().size(key);
            
            int concurrentCount = count != null ? count.intValue() : 0;
            recordConcurrentSearch(concurrentCount);
            
            log.debug("并发检索数统计: count={}", concurrentCount);
        } catch (Exception e) {
            log.warn("收集并发检索数失败", e);
            // 降级：使用默认值 0
            recordConcurrentSearch(0);
        }
    }
    
    /**
     * 收集最大文件大小
     */
    private void collectMaxFileSize() {
        try {
            // 查询所有文件，找出最大文件大小
            List<ArcFileContent> files = arcFileContentMapper.selectList(
                new LambdaQueryWrapper<ArcFileContent>()
                    .orderByDesc(ArcFileContent::getFileSize)
                    .last("LIMIT 1")
            );
            
            if (!files.isEmpty() && files.get(0).getFileSize() != null) {
                long maxBytes = files.get(0).getFileSize();
                // 转换为 MB
                double maxFileSizeMB = maxBytes / (1024.0 * 1024.0);
                recordMaxFileSize(maxFileSizeMB);
                
                log.debug("最大文件大小统计: size={}MB", maxFileSizeMB);
            } else {
                recordMaxFileSize(0.0);
            }
        } catch (Exception e) {
            log.warn("收集最大文件大小失败", e);
            recordMaxFileSize(0.0);
        }
    }
    
    /**
     * 保存指标
     */
    private void saveMetric(String metricType, String metricName, 
                           double metricValue, String metricUnit, String fondsNo) {
        SystemPerformanceMetrics metric = new SystemPerformanceMetrics();
        metric.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        metric.setMetricType(metricType);
        metric.setMetricName(metricName);
        metric.setMetricValue(BigDecimal.valueOf(metricValue));
        metric.setMetricUnit(metricUnit);
        metric.setFondsNo(fondsNo);
        metric.setRecordedAt(LocalDateTime.now());
        metric.setCreatedAt(LocalDateTime.now());
        
        metricsMapper.insert(metric);
        
        log.debug("保存性能指标: type={}, name={}, value={}, unit={}, fondsNo={}", 
            metricType, metricName, metricValue, metricUnit, fondsNo);
    }
    
    /**
     * 获取最新指标值
     */
    private Double getLatestMetric(String metricType, String fondsNo) {
        LambdaQueryWrapper<SystemPerformanceMetrics> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemPerformanceMetrics::getMetricType, metricType)
               .orderByDesc(SystemPerformanceMetrics::getRecordedAt)
               .last("LIMIT 1");
        
        if (fondsNo != null) {
            wrapper.eq(SystemPerformanceMetrics::getFondsNo, fondsNo);
        }
        
        SystemPerformanceMetrics metric = metricsMapper.selectOne(wrapper);
        return metric != null && metric.getMetricValue() != null 
            ? metric.getMetricValue().doubleValue() : 0.0;
    }
    
    /**
     * 获取平均指标值
     */
    private Double getAverageMetric(String metricType, String fondsNo) {
        try {
            // 查询最近 24 小时的指标数据
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            LambdaQueryWrapper<SystemPerformanceMetrics> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SystemPerformanceMetrics::getMetricType, metricType)
                   .ge(SystemPerformanceMetrics::getRecordedAt, since);
            
            if (fondsNo != null) {
                wrapper.eq(SystemPerformanceMetrics::getFondsNo, fondsNo);
            }
            
            List<SystemPerformanceMetrics> metrics = metricsMapper.selectList(wrapper);
            
            if (metrics.isEmpty()) {
                return getLatestMetric(metricType, fondsNo);
            }
            
            // 计算平均值
            double sum = metrics.stream()
                .filter(m -> m.getMetricValue() != null)
                .mapToDouble(m -> m.getMetricValue().doubleValue())
                .sum();
            
            return sum / metrics.size();
        } catch (Exception e) {
            log.warn("计算平均指标值失败: type={}, fondsNo={}", metricType, fondsNo, e);
            // 降级：返回最新值
            return getLatestMetric(metricType, fondsNo);
        }
    }
}

