// nexusarchive-java/src/main/java/com/nexusarchive/config/AiProperties.java
package com.nexusarchive.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.claude")
public class AiProperties {
    private String apiKey;
    private String model = "claude-3-5-sonnet-20241022";
    private int maxTokens = 8192;
    private double temperature = 0.3;
    private int timeout = 60000;
    private boolean enabled = true;
}
