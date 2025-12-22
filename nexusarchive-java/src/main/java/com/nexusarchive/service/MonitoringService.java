// Input: Java 标准库、本地模块
// Output: MonitoringService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.monitoring.IntegrationMonitoringDTO;

/**
 * 全链路监控服务接口
 */
public interface MonitoringService {
    
    /**
     * 获取集成中心核心运营指标
     */
    IntegrationMonitoringDTO getIntegrationMetrics();
    
    /**
     * 手动触发一次 ERP 健康巡检
     */
    void runHealthCheck();
}
