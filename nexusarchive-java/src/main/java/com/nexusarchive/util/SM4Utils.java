// Input: cn.hutool、SLF4J、Java 标准库
// Output: SM4Utils 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
 * - 由 SM4Config 在应用启动时初始化
 * - 使用 Spring Environment 正确检测生产环境
 * - 生产环境强制要求配置 SM4_KEY
 *
 * @author Agent B - 合规开发工程师
 */
public class SM4Utils {

    private static final Logger log = LoggerFactory.getLogger(SM4Utils.class);

    // 实际使用的密钥
    private static String ACTIVE_KEY;

    // SM4 加密实例
    private static SymmetricCrypto sm4;

    // 是否已初始化
    private static boolean initialized = false;

    /**
     * 由 SM4Config 调用，初始化 SM4 加密实例
     * 此方法在 Spring Environment 准备好后被调用
     *
     * @param key 加密密钥
     * @param crypto SM4 加密实例
     */
    public static void initialize(String key, SymmetricCrypto crypto) {
        ACTIVE_KEY = key;
        sm4 = crypto;
        initialized = true;
        log.debug("SM4Utils 初始化完成");
    }

    /**
     * 检查是否已初始化
     *
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
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
     * 解密字符串 (兼容模式 - 仅用于数据迁移)
     * ⚠️ 生产环境应使用 decryptStrict()
     * ✅ P0 修复: 标记为 @Deprecated,提升日志级别
     * 
     * @param hex 加密后的十六进制字符串
     * @return 解密后的原始内容
     */
    @Deprecated
    public static String decrypt(String hex) {
        if (StrUtil.isEmpty(hex)) {
            return hex;
        }
        ensureReady();
        try {
            return sm4.decryptStr(hex, CharsetUtil.CHARSET_UTF_8);
        } catch (Exception e) {
            // ✅ 提升日志级别为 WARN,并记录完整堆栈
            log.warn("SM4 解密失败,返回原始内容 (兼容模式): hex={}, error={}", 
                hex.substring(0, Math.min(8, hex.length())) + "...", e.getMessage(), e);
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
        if (!initialized || sm4 == null) {
            throw new IllegalStateException("SM4 未初始化，请确保 SM4Config 已正确配置");
        }
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
