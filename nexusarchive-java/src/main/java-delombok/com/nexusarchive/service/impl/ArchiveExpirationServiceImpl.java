// Input: ArchiveExpirationService, ArchiveMapper, DistributedLock, Scheduled
// Output: ArchiveExpirationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 到期档案自动识别服务实现
 * 
 * 实现要点：
 * 1. 使用分布式锁（Redis）防止多实例重复执行
 * 2. 分页扫描，每批处理 1000 条
 * 3. 支持断点续扫（记录最后扫描的 ID）
 * 4. 基于 retention_start_date 计算到期时间
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveExpirationServiceImpl implements ArchiveExpirationService {
    
    private static final String LOCK_KEY = "archive:expiration:scan:lock";
    private static final String LAST_SCAN_ID_KEY = "archive:expiration:scan:last_id";
    private static final int BATCH_SIZE = 1000;
    private static final int LOCK_TIMEOUT_SECONDS = 3600; // 1小时超时
    
    private final ArchiveMapper archiveMapper;
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 定时任务：每天凌晨2点执行
     * 使用分布式锁确保仅单实例执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledScan() {
        if (tryLock()) {
            try {
                log.info("开始执行到期档案扫描任务");
                int count = scanAndMarkExpired();
                log.info("到期档案扫描完成，发现 {} 条到期档案", count);
            } finally {
                releaseLock();
            }
        } else {
            log.debug("其他实例正在执行扫描任务，跳过本次执行");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndMarkExpired() {
        int totalExpired = 0;
        String lastScanId = getLastScanId();
        int page = 1;
        
        while (true) {
            // 分页查询正常状态的档案（排除已销毁、已冻结的）
            Page<Archive> pageParam = new Page<>(page, BATCH_SIZE);
            LambdaQueryWrapper<Archive> queryWrapper = new LambdaQueryWrapper<Archive>()
                    .eq(Archive::getDeleted, 0)
                    .and(wrapper -> wrapper
                        .isNull(Archive::getDestructionStatus)
                        .or()
                        .eq(Archive::getDestructionStatus, "NORMAL")
                        .or()
                        .eq(Archive::getDestructionStatus, "EXPIRED")
                    )
                    .isNotNull(Archive::getRetentionPeriod)
                    .isNotNull(Archive::getRetentionStartDate)
                    .orderByAsc(Archive::getId);
            
            // 断点续扫：从上次扫描的 ID 之后继续
            if (lastScanId != null) {
                queryWrapper.gt(Archive::getId, lastScanId);
            }
            
            Page<Archive> result = archiveMapper.selectPage(pageParam, queryWrapper);
            List<Archive> archives = result.getRecords();
            
            if (archives.isEmpty()) {
                break;
            }
            
            // 处理当前批次
            int expiredCount = processBatch(archives);
            totalExpired += expiredCount;
            
            // 更新最后扫描的 ID
            String lastId = archives.get(archives.size() - 1).getId();
            setLastScanId(lastId);
            
            log.debug("处理批次 {}，发现 {} 条到期档案，最后ID: {}", page, expiredCount, lastId);
            
            // 如果当前批次数量小于批次大小，说明已处理完
            if (archives.size() < BATCH_SIZE) {
                break;
            }
            
            page++;
        }
        
        // 重置最后扫描 ID
        clearLastScanId();
        
        return totalExpired;
    }
    
    /**
     * 处理一批档案，标记到期状态
     */
    private int processBatch(List<Archive> archives) {
        int expiredCount = 0;
        
        for (Archive archive : archives) {
            if (isExpired(archive, archive.getRetentionPeriod())) {
                // 更新状态为 EXPIRED
                LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<Archive>()
                        .eq(Archive::getId, archive.getId())
                        .and(wrapper -> wrapper
                            .isNull(Archive::getDestructionStatus)
                            .or()
                            .eq(Archive::getDestructionStatus, "NORMAL")
                        ) // 仅更新 NORMAL 或 null 状态的，避免并发问题
                        .set(Archive::getDestructionStatus, "EXPIRED");
                
                int updated = archiveMapper.update(null, updateWrapper);
                if (updated > 0) {
                    expiredCount++;
                    log.debug("档案 {} 已标记为到期（保管期限: {}, 起算日期: {}）", 
                            archive.getArchiveCode(), archive.getRetentionPeriod(), archive.getRetentionStartDate());
                }
            }
        }
        
        return expiredCount;
    }
    
    @Override
    public boolean isExpired(Archive archive, String retentionPeriod) {
        if (archive.getRetentionStartDate() == null) {
            log.warn("档案 {} 缺少 retention_start_date，无法判断是否到期", archive.getArchiveCode());
            return false;
        }
        
        // 永久保管的档案不会到期
        if ("PERMANENT".equalsIgnoreCase(retentionPeriod) || 
            "永久".equals(retentionPeriod)) {
            return false;
        }
        
        // 解析保管期限（格式：10Y, 30Y 等）
        int years = parseRetentionYears(retentionPeriod);
        if (years <= 0) {
            log.warn("无法解析保管期限: {}", retentionPeriod);
            return false;
        }
        
        // 计算到期日期
        LocalDate expirationDate = archive.getRetentionStartDate().plusYears(years);
        LocalDate today = LocalDate.now();
        
        // 如果今天 >= 到期日期，则已到期
        return !today.isBefore(expirationDate);
    }
    
    /**
     * 解析保管期限年数
     * 支持格式：10Y, 30Y, 15Y 等
     */
    private int parseRetentionYears(String retentionPeriod) {
        if (retentionPeriod == null || retentionPeriod.isEmpty()) {
            return 0;
        }
        
        // 移除空格，转大写
        String period = retentionPeriod.trim().toUpperCase();
        
        // 永久保管
        if (period.contains("PERMANENT") || period.contains("永久")) {
            return Integer.MAX_VALUE;
        }
        
        // 提取数字部分
        try {
            // 移除 "Y" 后缀
            if (period.endsWith("Y")) {
                period = period.substring(0, period.length() - 1);
            }
            return Integer.parseInt(period);
        } catch (NumberFormatException e) {
            log.warn("无法解析保管期限: {}", retentionPeriod);
            return 0;
        }
    }
    
    @Override
    public int manualScan() {
        return scanAndMarkExpired();
    }
    
    /**
     * 尝试获取分布式锁
     */
    private boolean tryLock() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "locked", LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }
    
    /**
     * 释放分布式锁
     */
    private void releaseLock() {
        redisTemplate.delete(LOCK_KEY);
    }
    
    /**
     * 获取最后扫描的 ID
     */
    private String getLastScanId() {
        return redisTemplate.opsForValue().get(LAST_SCAN_ID_KEY);
    }
    
    /**
     * 设置最后扫描的 ID
     */
    private void setLastScanId(String lastId) {
        redisTemplate.opsForValue().set(LAST_SCAN_ID_KEY, lastId, 24, TimeUnit.HOURS);
    }
    
    /**
     * 清除最后扫描的 ID
     */
    private void clearLastScanId() {
        redisTemplate.delete(LAST_SCAN_ID_KEY);
    }
}

