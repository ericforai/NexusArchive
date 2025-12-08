package com.nexusarchive.service;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.report.FourNatureReport;

import java.util.Map;

/**
 * 四性检测服务接口
 * Reference: DA/T 92-2022 Clause 6
 */
public interface FourNatureCheckService {
    
    /**
     * 执行完整的四性检测
     * 严格按照顺序执行: 真实性 -> 完整性 -> 可用性 -> 安全性
     * 如果真实性或安全性检测失败，应立即停止并报告失败
     * 
     * @param sip SIP 数据包
     * @param fileStreams 文件字节流 Map (Key: FileName, Value: FileBytes)
     * @return 四性检测报告
     */
    FourNatureReport performFullCheck(AccountingSipDto sip, Map<String, byte[]> fileStreams);

    /**
     * 执行档案健康巡检 (四性检测 - 存量档案)
     * 
     * @param archive 档案元数据
     * @param files 档案关联的文件实体
     * @return 四性检测报告
     */
    FourNatureReport performHealthCheck(com.nexusarchive.entity.Archive archive, java.util.List<com.nexusarchive.entity.ArcFileContent> files);
}
