// Input: Spring 配置
// Output: 病毒扫描服务自动配置
// Pos: NexusCore compliance/config
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 病毒扫描服务配置
 * 
 * 根据配置自动选择 ClamAV 或 Mock 实现：
 * - nexus.compliance.virus-scan.mode=clamav (默认): 使用 ClamAV
 * - nexus.compliance.virus-scan.mode=mock: 使用 Mock 实现
 */
@Configuration
public class VirusScanConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirusScanConfiguration.class);

    @Bean
    @Primary
    @ConditionalOnProperty(name = "nexus.compliance.virus-scan.mode", havingValue = "mock")
    public VirusScanService mockVirusScanService() {
        LOGGER.warn("⚠️ 使用 Mock 病毒扫描服务 - 仅检测 EICAR 测试签名");
        return new MockVirusScanService();
    }

    @Bean
    @ConditionalOnProperty(name = "nexus.compliance.virus-scan.mode", havingValue = "clamav", matchIfMissing = true)
    public VirusScanService clamAvVirusScanService(
            @Value("${nexus.compliance.clamav.host:localhost}") String host,
            @Value("${nexus.compliance.clamav.port:3310}") int port,
            @Value("${nexus.compliance.clamav.timeout:60000}") int timeout) {
        LOGGER.info("使用 ClamAV 病毒扫描服务: {}:{}", host, port);
        return new ClamAvVirusScanService(host, port, timeout);
    }
}
