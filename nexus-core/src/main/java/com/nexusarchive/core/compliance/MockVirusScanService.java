// Input: Mock 病毒扫描（EICAR 测试签名）
// Output: 模拟扫描结果
// Pos: NexusCore compliance/virus
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock 病毒扫描服务 (开发/测试用)
 * 
 * 当 ClamAV 不可用时使用此实现。
 * 通过检测 EICAR 测试签名模拟病毒检测。
 */
public class MockVirusScanService implements VirusScanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockVirusScanService.class);
    
    // EICAR 标准测试签名 (分段存储避免误报)
    private static final String EICAR_PREFIX = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$";
    private static final String EICAR_SUFFIX = "EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
    private static final String EICAR_PARTIAL = "EICAR-STANDARD-ANTIVIRUS-TEST-FILE";
    private static final String EICAR_SIGNATURE = EICAR_PREFIX + EICAR_SUFFIX;

    @Override
    public VirusScanResult scan(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        
        long startTime = System.currentTimeMillis();
        try {
            String content = Files.readString(filePath);
            long duration = System.currentTimeMillis() - startTime;
            
            if (content.contains(EICAR_SIGNATURE)
                    || content.contains(EICAR_PARTIAL)
                    || content.contains(EICAR_SUFFIX)) {
                LOGGER.warn("[MOCK] 检测到 EICAR 测试病毒: {}", filePath);
                return VirusScanResult.infected("Eicar-Test-Signature", duration);
            }
            
            LOGGER.info("[MOCK] 文件扫描通过: {}", filePath);
            return VirusScanResult.clean(duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.warn("[MOCK] 文件读取失败，默认返回 clean: {}", filePath);
            return VirusScanResult.clean(duration);
        }
    }

    @Override
    public VirusScanResult scan(InputStream inputStream, String fileName) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        
        long startTime = System.currentTimeMillis();
        try {
            byte[] bytes = inputStream.readAllBytes();
            String content = new String(bytes);
            long duration = System.currentTimeMillis() - startTime;
            
            if (content.contains(EICAR_SIGNATURE)
                    || content.contains(EICAR_PARTIAL)
                    || content.contains(EICAR_SUFFIX)) {
                LOGGER.warn("[MOCK] 检测到 EICAR 测试病毒: {}", fileName);
                return VirusScanResult.infected("Eicar-Test-Signature", duration);
            }
            
            LOGGER.info("[MOCK] 文件扫描通过: {}", fileName);
            return VirusScanResult.clean(duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.warn("[MOCK] 流读取失败，默认返回 clean: {}", fileName);
            return VirusScanResult.clean(duration);
        }
    }
}
