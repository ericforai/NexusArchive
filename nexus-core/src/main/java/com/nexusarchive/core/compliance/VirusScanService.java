// Input: 文件路径/输入流
// Output: 病毒扫描结果
// Pos: NexusCore compliance/virus
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 病毒扫描服务接口
 * 
 * PRD 来源: PRD 3.1 - 文件入库/预览前病毒扫描
 */
public interface VirusScanService {
    /**
     * 扫描文件是否包含病毒
     * 
     * @param filePath 文件路径
     * @return 扫描结果
     */
    VirusScanResult scan(Path filePath);
    
    /**
     * 扫描输入流
     * 
     * @param inputStream 输入流
     * @param fileName 文件名 (用于日志)
     * @return 扫描结果
     */
    VirusScanResult scan(InputStream inputStream, String fileName);
}
