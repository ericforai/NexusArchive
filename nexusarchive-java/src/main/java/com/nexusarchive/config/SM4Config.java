// Input: Spring Framework、Lombok、Hutool、SLF4J
// Output: SM4Config 配置类
// Pos: 配置模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * SM4 国密加密配置类
 *
 * 功能:
 * - 使用 Spring Environment 正确检测生产环境 profile
 * - 在应用启动时验证 SM4_KEY 配置
 * - 初始化 SM4 加密实例并注入到 SM4Utils
 *
 * 修复问题:
 * - 静态初始化无法感知 Spring 运行时 profile 变化
 * - @Value 只能读取配置文件，无法读取环境变量设置的 profile
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SM4Config {

    private final Environment environment;

    /**
     * 在应用准备就绪后初始化 SM4
     * 此时 Spring Environment 已完全准备好
     *
     * @param event ApplicationReadyEvent
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeSM4(ApplicationReadyEvent event) {
        log.info("SM4 配置初始化开始...");

        // 使用 Spring Environment 检测活跃的 profiles
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.contains("prod"));

        // 从环境变量读取密钥
        String envKey = System.getenv("SM4_KEY");
        String resolvedKey = null;

        if (envKey != null && !envKey.isEmpty()) {
            if (isValidKey(envKey)) {
                resolvedKey = envKey;
                log.info("SM4 加密: 使用环境变量配置的密钥");
            } else {
                log.error("SM4 加密: 环境变量 SM4_KEY 格式无效（需要 32 位 16 进制字符串）");
            }
        }

        if (resolvedKey == null) {
            // 尝试从系统属性读取
            String propKey = System.getProperty("sm4.key");
            if (propKey != null && !propKey.isEmpty() && isValidKey(propKey)) {
                resolvedKey = propKey;
                log.info("SM4 加密: 使用系统属性配置的密钥");
            }
        }

        // 根据环境决定如何处理未配置的密钥
        if (resolvedKey == null) {
            if (isProd) {
                // 生产环境必须配置 SM4_KEY
                String msg = "生产环境 SM4_KEY 未配置！请设置环境变量 SM4_KEY（32位16进制字符串）";
                log.error("🚨 {}", msg);
                throw new IllegalStateException(msg);
            } else {
                // 开发/测试环境使用默认密钥
                resolvedKey = "0123456789abcdef0123456789abcdef";
                log.warn("⚠️ SM4_KEY 未配置，使用默认密钥进行加解密。请在生产环境配置有效的 SM4_KEY。");
            }
        }

        // 初始化 SM4 实例
        SymmetricCrypto sm4 = SmUtil.sm4(HexUtil.decodeHex(resolvedKey));

        // 将密钥和实例设置到 SM4Utils
        SM4Utils.initialize(resolvedKey, sm4);

        log.info("SM4 配置初始化成功,密钥哈希: {}, 环境: {}", getKeyHash(resolvedKey), isProd ? "生产" : "开发/测试");
    }

    /**
     * 验证密钥格式
     *
     * @param key 待验证的密钥
     * @return 是否为有效的 32 位 16 进制字符串
     */
    private boolean isValidKey(String key) {
        if (key == null || key.length() != 32) {
            return false;
        }
        return key.matches("^[0-9a-fA-F]{32}$");
    }

    /**
     * 获取密钥哈希（用于日志，不暴露真实密钥）
     *
     * @param key 密钥
     * @return 密钥哈希的前 8 位
     */
    private String getKeyHash(String key) {
        try {
            if (key == null) {
                return "missing";
            }
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(key.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString() + "...";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
