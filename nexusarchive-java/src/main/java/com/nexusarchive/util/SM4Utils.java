package com.nexusarchive.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SM4 国密加密工具类
 * 
 * 基于 Hutool (BouncyCastle wrapper) 实现 SM4-ECB 加密
 * 符合信创环境国密标准
 * 
 * 密钥管理优化：
 * - 优先从环境变量 SM4_KEY 读取密钥
 * - 密钥格式：32 位 16 进制字符串（128-bit）
 * - 生产环境警告：使用默认密钥会输出警告日志
 * 
 * @author Agent B - 合规开发工程师
 */
public class SM4Utils {

    private static final Logger log = LoggerFactory.getLogger(SM4Utils.class);

    // 实际使用的密钥
    private static final String ACTIVE_KEY;

    // SM4 加密实例
    private static final SymmetricCrypto sm4;

    static {
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

        ACTIVE_KEY = resolvedKey;
        sm4 = (ACTIVE_KEY != null) ? SmUtil.sm4(HexUtil.decodeHex(ACTIVE_KEY)) : null;
        if (sm4 == null) {
            log.error("SM4 加密: 未配置有效密钥，相关加解密功能将不可用");
        }
    }

    /**
     * 加密字符串
     * 
     * @param content 待加密内容
     * @return 加密后的十六进制字符串
     */
    public static String encrypt(String content) {
        if (StrUtil.isEmpty(content)) {
            return content;
        }
        ensureReady();
        return sm4.encryptHex(content);
    }

    /**
     * 解密字符串
     * 
     * @param hex 加密后的十六进制字符串
     * @return 解密后的原始内容
     */
    public static String decrypt(String hex) {
        if (StrUtil.isEmpty(hex)) {
            return hex;
        }
        ensureReady();
        try {
            return sm4.decryptStr(hex, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            // 容错处理：如果解密失败，返回原始内容
            // 这对于旧数据迁移非常有用
            log.debug("SM4 解密失败，返回原始内容: {}", e.getMessage());
            return hex;
        }
    }

    /**
     * 解密字符串 (严格模式) - 修复 High #7
     * 
     * @param hex 加密后的十六进制字符串
     * @return 解密后的原始内容
     * @throws RuntimeException 当解密失败时抛出异常
     */
    public static String decryptStrict(String hex) {
        if (StrUtil.isEmpty(hex)) {
            return hex;
        }
        ensureReady();
        try {
            return sm4.decryptStr(hex, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            log.error("SM4 严格解密失败: {}", e.getMessage());
            throw new RuntimeException("SM4解密失败", e);
        }
    }

    private static void ensureReady() {
        if (sm4 == null) {
            throw new IllegalStateException("SM4_KEY 未配置或格式非法，无法执行加解密操作");
        }
    }

    /**
     * 验证密钥格式
     * 
     * @param key 待验证的密钥
     * @return 是否为有效的 32 位 16 进制字符串
     */
    private static boolean isValidKey(String key) {
        if (key == null || key.length() != 32) {
            return false;
        }
        // 验证是否为有效的 16 进制字符串
        return key.matches("^[0-9a-fA-F]{32}$");
    }

    /**
     * 检查是否使用默认密钥
     * 
     * @return 是否使用默认密钥
     */
    public static boolean isKeyMissing() {
        return ACTIVE_KEY == null || ACTIVE_KEY.isEmpty();
    }

    /**
     * 获取当前密钥的哈希（用于日志，不暴露真实密钥）
     * 
     * @return 密钥哈希的前 8 位
     */
    public static String getKeyHash() {
        try {
            if (ACTIVE_KEY == null) {
                return "missing";
            }
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(ACTIVE_KEY.getBytes());
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
