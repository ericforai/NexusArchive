// Input: Spring Framework、Lombok、SLF4J
// Output: SecurityConfigValidator 类
// Pos: 配置模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 安全配置验证器
 * 应用启动时验证关键安全配置
 *
 * 功能:
 * - 检查 SM4_KEY 配置状态
 * - 生产环境安全警告
 *
 * 修复: 使用 Spring Environment 正确检测生产环境 profile
 */
@Component
@Slf4j
public class SecurityConfigValidator {

    private final Environment environment;

    public SecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateSecurityConfiguration() {
        log.info("开始验证安全配置...");

        // 使用 Spring Environment 正确检测生产环境
        // 支持从配置文件、环境变量、系统属性等多种方式设置的 profile
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.contains("prod"));

        if (isProd) {
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("生产环境安全检查");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            log.warn("请确保以下安全配置已正确设置:");
            log.warn("  - SM4_KEY 环境变量 (国密加密密钥)");
            log.warn("  - JWT_SECRET 环境变量 (JWT 签名密钥)");
            log.warn("  - 数据库密码已更新");
            log.warn("  - Redis 密码已配置");
            log.warn("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            // 检查 SM4_KEY 是否配置
            String sm4Key = System.getenv("SM4_KEY");
            if (sm4Key == null || sm4Key.isEmpty()) {
                log.error("生产环境 SM4_KEY 未配置！应用可能无法正常启动。");
            } else {
                log.info("SM4_KEY 已配置");
            }

            // 检查 JWT_SECRET 是否配置
            String jwtSecret = System.getenv("JWT_SECRET");
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.warn("JWT_SECRET 未配置，使用默认值（不推荐用于生产）");
            } else {
                log.info("JWT_SECRET 已配置");
            }
        } else {
            log.info("开发环境模式 - 安全配置检查通过");
        }

        log.info("安全配置验证完成");
    }
}
