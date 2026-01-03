// Input: Archive Entity, Retention Policy, Scheduled Tasks
// Output: ArchiveExpirationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;

import java.time.LocalDate;

/**
 * 到期档案自动识别服务
 * 
 * 功能：
 * 1. 定时扫描到期档案并更新状态
 * 2. 支持分布式锁，防止多实例重复执行
 * 3. 分页扫描，支持大数据量场景
 * 4. 基于 retention_start_date 计算到期时间
 */
public interface ArchiveExpirationService {
    
    /**
     * 扫描到期档案并更新状态
     * 定时任务：每天凌晨2点执行
     * 
     * @return 本次扫描发现的到期档案数量
     */
    int scanAndMarkExpired();
    
    /**
     * 检查单个档案是否到期
     * 
     * @param archive 档案对象
     * @param retentionPeriod 保管期限（如 "30Y", "10Y", "PERMANENT"）
     * @return true 如果已到期
     */
    boolean isExpired(Archive archive, String retentionPeriod);
    
    /**
     * 手动触发扫描（用于测试或紧急情况）
     * 
     * @return 本次扫描发现的到期档案数量
     */
    int manualScan();
}


