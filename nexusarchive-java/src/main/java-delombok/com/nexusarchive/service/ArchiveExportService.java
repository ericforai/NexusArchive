// Input: Java 标准库
// Output: ArchiveExportService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
