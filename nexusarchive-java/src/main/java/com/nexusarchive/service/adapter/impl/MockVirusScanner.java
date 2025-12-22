// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: MockVirusScanner 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.adapter.impl;

import java.io.InputStream;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 病毒扫描器
 * 目前默认返回安全，预留 ClamAV 集成接口
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "virus.scan.type", havingValue = "mock", matchIfMissing = true)
public class MockVirusScanner implements VirusScanAdapter {

    @Override
    public boolean scan(java.io.InputStream inputStream, String fileName) {
        log.info("SecurityEvent: Executing mock virus scan for file: {}", fileName);
        
        // 模拟扫描过程
        if (fileName.toLowerCase().endsWith(".xml") || fileName.toLowerCase().endsWith(".html")) {
            // [FIXED] 避免读取全部字节，仅读取开头部分进行演示，或使用流式检测
            try {
                byte[] sample = new byte[1024];
                int read = inputStream.read(sample);
                if (read > 0) {
                    String contentHead = new String(sample, 0, read);
                    if (contentHead.contains("<script>") || contentHead.contains("javascript:")) {
                        log.error("SecurityEvent: Malicious script detected in file header: {}", fileName);
                        return false;
                    }
                }
            } catch (java.io.IOException e) {
                log.error("Error reading stream for mock scan: {}", e.getMessage());
            }
        }
        
        return true; // Default to SAFE
    }
}
