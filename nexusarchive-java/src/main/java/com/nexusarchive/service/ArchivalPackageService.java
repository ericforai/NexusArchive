package com.nexusarchive.service;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.ArcFileContent;

import java.io.IOException;
import java.util.List;

/**
 * AIP (Archival Information Package) 封装服务
 * 
 * 负责将 SIP 转换为 AIP 并进行物理归档
 * Reference: OAIS (ISO 14721) / DA/T 94-2022
 */
public interface ArchivalPackageService {
    
    /**
     * 执行 AIP 封装和物理归档
     * 
     * @param sip SIP 数据包
     * @param tempPath 临时文件存放路径 (包含所有附件)
     * @return 归档文件列表 (包含数据库 ID)
     * @throws IOException 如果文件操作失败
     */
    List<ArcFileContent> archivePackage(AccountingSipDto sip, String tempPath) throws IOException;
}
