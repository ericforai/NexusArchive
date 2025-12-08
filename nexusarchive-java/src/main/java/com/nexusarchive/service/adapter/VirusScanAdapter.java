package com.nexusarchive.service.adapter;

/**
 * 病毒扫描适配器接口
 * Reference: DA/T 92-2022 Clause 6.4 Safety Check
 */
public interface VirusScanAdapter {
    
    /**
     * 扫描文件内容
     * @param fileContent 文件字节内容
     * @param fileName 文件名
     * @return true if safe, false if infected
     */
    boolean scan(byte[] fileContent, String fileName);
}
