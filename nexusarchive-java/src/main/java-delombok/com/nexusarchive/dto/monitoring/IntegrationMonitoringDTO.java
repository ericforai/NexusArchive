// Input: Lombok、Java 标准库
// Output: IntegrationMonitoringDTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.monitoring;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 集成中心全链路监控 DTO
 */
@Data
@Builder
public class IntegrationMonitoringDTO {
    
    // 1. 核心流量指标
    private long totalSyncCount;      // 总同步笔数
    private long successSyncCount;    // 成功同步笔数
    private double successRate;       // 同步成功率
    
    // 2. 存储与合规指标
    private long totalArchivedItems;  // 已归档档案总数
    private long totalAttachedFiles;  // 物理附件总数
    private double evidenceCoverage;  // 证据覆盖率 (有档且有证)
    
    // 3. 健康度指标
    private String erpHealthStatus;   // ERP 连接总体健康度 (HEALTHY/WARNING/CRITICAL)
    private List<SystemHealthStatus> adapterStatus;
    
    // 4. 时效性/趋势数据 (最近7天)
    private List<DataPoint> dailySyncTrend;
    
    @Data
    @Builder
    public static class SystemHealthStatus {
        private String adapterName;
        private boolean isAlive;
        private double lastResponseTime; // 平均响应耗时
    }
    
    @Data
    @Builder
    public static class DataPoint {
        private String date;
        private long value;
        private String type; // e.g. "SUCCESS", "FAIL"
    }
}
