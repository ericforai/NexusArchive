// Input: ClamAV 扫描引擎
// Output: 病毒扫描实现
// Pos: NexusCore compliance/virus
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import fi.solita.clamav.ClamAVClient;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClamAV 病毒扫描实现
 * 
 * Bean 由 VirusScanConfiguration 管理
 */
public class ClamAvVirusScanService implements VirusScanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClamAvVirusScanService.class);
    private static final String CLEAN_RESPONSE = "OK";

    private final String clamAvHost;
    private final int clamAvPort;
    private final int timeout;

    public ClamAvVirusScanService(String clamAvHost, int clamAvPort, int timeout) {
        this.clamAvHost = clamAvHost;
        this.clamAvPort = clamAvPort;
        this.timeout = timeout;
    }

    @Override
    public VirusScanResult scan(Path filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))) {
            return scan(is, filePath.getFileName().toString());
        } catch (IOException ex) {
            LOGGER.error("文件打开失败: {}", filePath, ex);
            return VirusScanResult.error("文件打开失败: " + ex.getMessage());
        }
    }

    @Override
    public VirusScanResult scan(InputStream inputStream, String fileName) {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        
        long startTime = System.currentTimeMillis();
        try {
            ClamAVClient client = new ClamAVClient(clamAvHost, clamAvPort, timeout);
            byte[] reply = client.scan(inputStream);
            String response = new String(reply).trim();
            long duration = System.currentTimeMillis() - startTime;
            
            LOGGER.info("病毒扫描完成: file={}, response={}, duration={}ms", 
                    fileName, response, duration);
            
            if (response.contains(CLEAN_RESPONSE)) {
                return VirusScanResult.clean(duration);
            }
            
            // 解析病毒名称
            String virusName = parseVirusName(response);
            return VirusScanResult.infected(virusName, duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            LOGGER.error("病毒扫描异常: file={}, error={}", fileName, ex.getMessage(), ex);
            return VirusScanResult.error(ex.getMessage());
        }
    }

    private String parseVirusName(String response) {
        // ClamAV 响应格式: "stream: Eicar-Test-Signature FOUND"
        if (response.contains("FOUND")) {
            int endIndex = response.lastIndexOf("FOUND");
            String part = response.substring(0, endIndex).trim();
            int colonIndex = part.lastIndexOf(":");
            if (colonIndex >= 0) {
                return part.substring(colonIndex + 1).trim();
            }
            return part;
        }
        return response;
    }
}
