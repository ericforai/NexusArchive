// Input: Jakarta EE、Lombok、Spring Framework、Java 标准库
// Output: SecurityConfigValidator 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
public class SecurityConfigValidator {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SecurityConfigValidator.class);

    private final Environment environment;

    @Value("${jwt.public-key-location:}")
    private String jwtPublicKeyLocation;

    @Value("${jwt.private-key-location:}")
    private String jwtPrivateKeyLocation;

    @Value("${yonsuite.app-key:}")
    private String yonsuiteAppKey;

    @Value("${yonsuite.app-secret:}")
    private String yonsuiteAppSecret;

    @Value("${virus.scan.type:mock}")
    private String virusScanType;

    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;

    @Value("${signature.keystore.path:}")
    private String signatureKeystorePath;

    @Value("${signature.keystore.password:}")
    private String signatureKeystorePassword;

    @Value("${timestamp.fallback-on-error:true}")
    private boolean timestampFallbackOnError;

    public SecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateSecurityConfig() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.startsWith("prod"));

        // JWT RSA 密钥验证
        if (isProduction) {
            if (jwtPublicKeyLocation == null || jwtPublicKeyLocation.isBlank()
                    || jwtPrivateKeyLocation == null || jwtPrivateKeyLocation.isBlank()) {
                throw new IllegalStateException("【安全错误】生产环境必须配置 JWT 公私钥路径 (JWT_PUBLIC_KEY_LOCATION/JWT_PRIVATE_KEY_LOCATION)");
            }
        } else if ((jwtPublicKeyLocation == null || jwtPublicKeyLocation.isBlank())
                || (jwtPrivateKeyLocation == null || jwtPrivateKeyLocation.isBlank())) {
            log.warn("⚠️ JWT 公私钥路径未配置，JWT 将无法正常签发/校验");
        }

        // YonSuite 配置验证（仅当需要使用时）
        if (isProduction) {
            if ((yonsuiteAppKey == null || yonsuiteAppKey.isBlank()) &&
                (yonsuiteAppSecret == null || yonsuiteAppSecret.isBlank())) {
                log.info("\ud83d\udccb YonSuite 集成未配置 - 如需使用用友ERP集成，请设置 YONSUITE_APP_KEY 和 YONSUITE_APP_SECRET");
            } else if (yonsuiteAppKey == null || yonsuiteAppKey.isBlank() ||
                       yonsuiteAppSecret == null || yonsuiteAppSecret.isBlank()) {
                log.warn("⚠️ YonSuite 配置不完整 - 请同时设置 YONSUITE_APP_KEY 和 YONSUITE_APP_SECRET");
            }
        }

        // 病毒扫描配置验证：生产环境禁止 mock（允许 skip 跳过或 clamav）
        if (isProduction && "mock".equalsIgnoreCase(virusScanType)) {
            throw new IllegalStateException("【安全错误】生产环境必须启用真实病毒扫描 (virus.scan.type!=mock，可用 skip 或 clamav)");
        }

        // 审计日志 HMAC 关键密钥校验
        if (isProduction && (auditLogHmacKey == null || auditLogHmacKey.isBlank())) {
            throw new IllegalStateException("【安全错误】生产环境必须设置审计日志 HMAC 密钥 (AUDIT_LOG_HMAC_KEY)");
        }

        // SM4 密钥校验
        if (isProduction && com.nexusarchive.util.SM4Utils.isKeyMissing()) {
            throw new IllegalStateException("【安全错误】生产环境必须设置 SM4_KEY，用于敏感字段加解密");
        }

        // 签章密钥库校验（配置了路径则要求密码）
        if (isProduction && signatureKeystorePath != null && !signatureKeystorePath.isBlank()) {
            if (signatureKeystorePassword == null || signatureKeystorePassword.isBlank()) {
                throw new IllegalStateException("【安全错误】签章密钥库已配置，但密码为空");
            }
            if ("changeit".equalsIgnoreCase(signatureKeystorePassword)) {
                throw new IllegalStateException("【安全错误】生产环境禁止使用默认签章密钥库口令");
            }
        }

        // 时间戳降级策略
        if (isProduction && timestampFallbackOnError) {
            throw new IllegalStateException("【安全错误】生产环境禁止时间戳服务降级到本地时间");
        }

        log.info("✅ 安全配置验证完成，当前环境: {}", 
            activeProfiles.length > 0 ? String.join(", ", activeProfiles) : "default");
    }
}
