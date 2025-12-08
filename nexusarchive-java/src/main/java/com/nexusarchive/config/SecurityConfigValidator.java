package com.nexusarchive.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 启动时配置验证
 * 确保生产环境必须正确设置关键安全配置
 */
@Configuration
@Slf4j
public class SecurityConfigValidator {

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${yonsuite.app-key:}")
    private String yonsuiteAppKey;

    @Value("${yonsuite.app-secret:}")
    private String yonsuiteAppSecret;

    public SecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecurityConfig() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.startsWith("prod"));

        // JWT 密钥验证
        if (jwtSecret == null || jwtSecret.isBlank()) {
            if (isProduction) {
                throw new IllegalStateException(
                    "【安全错误】生产环境必须设置 JWT_SECRET 环境变量！" +
                    "请使用 'openssl rand -base64 32' 生成强密钥。");
            } else {
                log.warn("⚠️ JWT_SECRET 未设置，使用临时密钥（仅限开发环境）");
                // 开发环境使用临时密钥，但会在日志中警告
            }
        } else if (jwtSecret.length() < 32) {
            log.warn("⚠️ JWT_SECRET 长度不足32位，建议使用更强的密钥");
        }

        // YonSuite 配置验证（仅当需要使用时）
        if (isProduction) {
            if ((yonsuiteAppKey == null || yonsuiteAppKey.isBlank()) &&
                (yonsuiteAppSecret == null || yonsuiteAppSecret.isBlank())) {
                log.info("📋 YonSuite 集成未配置 - 如需使用用友ERP集成，请设置 YONSUITE_APP_KEY 和 YONSUITE_APP_SECRET");
            } else if (yonsuiteAppKey == null || yonsuiteAppKey.isBlank() ||
                       yonsuiteAppSecret == null || yonsuiteAppSecret.isBlank()) {
                log.warn("⚠️ YonSuite 配置不完整 - 请同时设置 YONSUITE_APP_KEY 和 YONSUITE_APP_SECRET");
            }
        }

        log.info("✅ 安全配置验证完成，当前环境: {}", 
            activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");
    }
}
