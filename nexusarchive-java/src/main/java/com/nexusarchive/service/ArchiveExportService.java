package com.nexusarchive.service;

import java.io.File;
import java.io.IOException;

/**
 * 档案导出服务
 * 负责将 AIP 包导出为 ZIP 文件
 */
public interface ArchiveExportService {
    
    /**
     * 导出单个 AIP 包为 ZIP 文件
     * 
     * @param archivalCode 档号
     * @return ZIP 文件
     * @throws IOException 文件操作异常
     */
    File exportAipPackage(String archivalCode) throws IOException;
}
