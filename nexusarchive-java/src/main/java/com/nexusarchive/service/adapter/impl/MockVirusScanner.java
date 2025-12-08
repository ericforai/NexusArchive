package com.nexusarchive.service.adapter.impl;

import com.nexusarchive.service.adapter.VirusScanAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 病毒扫描器
 * 目前默认返回安全，预留 ClamAV 集成接口
 */
@Slf4j
@Component
public class MockVirusScanner implements VirusScanAdapter {

    @Override
    public boolean scan(byte[] fileContent, String fileName) {
        log.info("SecurityEvent: Executing virus scan for file: {} (Size: {} bytes)", fileName, fileContent.length);
        
        // 模拟扫描过程
        // 在实际生产环境中，这里会调用 ClamAV 或其他杀毒引擎的 API
        
        // 简单的脚本检测 (Script Detection)
        // Reject files containing <script>, javascript: if inside XML/HTML wrappers
        // 这里做一个简单的字符串检查作为示例
        if (fileName.toLowerCase().endsWith(".xml") || fileName.toLowerCase().endsWith(".html")) {
            String content = new String(fileContent);
            if (content.contains("<script>") || content.contains("javascript:")) {
                log.error("SecurityEvent: Malicious script detected in file: {}", fileName);
                return false;
            }
        }
        
        return true; // Default to SAFE
    }
}
