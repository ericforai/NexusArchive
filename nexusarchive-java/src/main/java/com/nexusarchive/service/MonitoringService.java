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
