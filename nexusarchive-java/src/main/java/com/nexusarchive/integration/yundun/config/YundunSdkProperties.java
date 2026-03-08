// Input: Spring Boot ConfigurationProperties、Lombok
// Output: YundunSdkProperties 配置类
// Pos: 云盾 SDK 集成配置层

package com.nexusarchive.integration.yundun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.yundun.sdk")
public class YundunSdkProperties {

    /**
     * 云盾 SDK 集成开关
     */
    private boolean enabled = false;

    /**
     * SDK 解密私钥（由运维通过环境变量注入）
     */
    private String privateKey;

    /**
     * 可选：覆盖 config.properties 中的 IDP 域名前缀（如 https://idp.example.com）
     */
    private String idpBaseUrl;
}
