// Input: cn.hutool、Lombok、Spring Framework、Javax、等
// Output: YonSuiteEventCrypto 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.security;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * YonSuite事件加解密工具类
 *
 * <p><b>安全设计说明</b>：</p>
 * <ul>
 *   <li>IV (初始化向量) 处理：按照 YonSuite 官方协议规范，IV 从 AES Key 的前 16 位派生
 *       这是由 YonSuite 协议固定的，消息本身已包含 16 字节随机数</li>
 *   <li>密钥管理：AES Key 从配置中心动态获取，支持定期轮换</li>
 *   <li>传输安全：所有通信必须通过 HTTPS 传输</li>
 * </ul>
 *
 * <p><b>SonarQube 规则说明</b>：</p>
 * <ul>
 *   <li>java:S3329 (固定 IV) - 误报：IV 派生方式由 YonSuite 协议固定，不可修改</li>
 *   <li>代码位置：YonSuiteEventCrypto.java:58</li>
 * </ul>
 *
 * <p><b>协议格式</b>：</p>
 * <pre>
 * random(16B) + msgLen(4B) + msg + appKey
 * </pre>
 *
 * @reference https://gitee.com/yycloudopen/isv-demo/blob/master/src/main/java/com/yonyou/iuap/isv/demo/crypto/IsvEventCrypto.java
 * @author Agent B - 合规开发工程师
 */
@Component
@Slf4j
public class YonSuiteEventCrypto {

    @Value("${yonsuite.encoding-aes-key:}")
    private String encodingAesKey;

    @Value("${yonsuite.app-key:}")
    private String appKey;

    /**
     * 解密事件消息
     *
     * @param encrypt 密文
     * @return 解密后的明文 (JSON)
     */
    public String decrypt(String encrypt) {
        if (encodingAesKey == null || encodingAesKey.isEmpty()) {
            throw new RuntimeException("EncodingAESKey is not configured");
        }

        try {
            // 1. Base64 解码 EncodingAESKey 得到 AES 密钥
            byte[] aesKey = Base64.decode(encodingAesKey + "=");

            // 2. Base64 解码 密文
            byte[] encryptBytes = Base64.decode(encrypt);

            // 3. AES 解密 (CBC 模式, PKCS5Padding / PKCS7Padding)
            // IV 是 AESKey 的前 16 位 (YonSuite 协议规范，不可修改)
            // NOSONAR: java:S3329 - IV 派生方式由 YonSuite 协议固定，消息本身包含 16B 随机数
            byte[] iv = Arrays.copyOfRange(aesKey, 0, 16);

            AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] original = aes.decrypt(encryptBytes);

            // 4. 去除补位字符 (参考官方 Demo)
            // 协议格式: random(16B) + msgLen(4B) + msg + appKey
            // 我们只需要提取 msg 部分

            // 获取 XML/JSON 内容长度 (前16位是随机串，16-20位是长度)
            byte[] lengthBytes = Arrays.copyOfRange(original, 16, 20);
            int xmlLength = recoverNetworkBytesOrder(lengthBytes);

            String fromAppKey = new String(Arrays.copyOfRange(original, 20 + xmlLength, original.length), StandardCharsets.UTF_8);
            if (!fromAppKey.equals(appKey)) {
                 log.warn("AppKey mismatch in decrypted data. Expected: {}, Actual: {}", appKey, fromAppKey);
                 // throw new RuntimeException("AppKey mismatch"); 
            }

            return new String(Arrays.copyOfRange(original, 20, 20 + xmlLength), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to decrypt YonSuite event", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // 还原 4字节的网络字节序
    private int recoverNetworkBytesOrder(byte[] orderBytes) {
        int sourceNumber = 0;
        for (int i = 0; i < 4; i++) {
            sourceNumber <<= 8;
            sourceNumber |= orderBytes[i] & 0xff;
        }
        return sourceNumber;
    }
}
