package com.nexusarchive.dto.sip.report;

/**
 * 四性检测总体状态
 */
public enum OverallStatus {
    /**
     * 检测通过
     */
    PASS,
    
    /**
     * 检测失败 (阻断性问题)
     */
    FAIL,
    
    /**
     * 警告 (非阻断性问题)
     */
    WARNING
}
