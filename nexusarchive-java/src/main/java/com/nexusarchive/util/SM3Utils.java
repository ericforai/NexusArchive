// Input: org.bouncycastle、Spring Framework、Java 标准库
// Output: SM3Utils 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * SM3 哈希工具类
 * 
 * 基于 BouncyCastle 实现国密 SM3 摘要算法
 * 用于审计日志防篡改哈希链
 * 
 * 合规要求：
 * - 信创环境必须使用国密算法
 * - 审计日志必须防篡改（GB/T 39784-2021）
 * 
 * @author Agent B - 合规开发工程师
 */
@Component
public class SM3Utils {
    
    static {
        // 注册 BouncyCastle 提供者
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * 计算字符串的 SM3 哈希值
     * 
     * @param content 待哈希内容
     * @return SM3 哈希值 (十六进制字符串)
     */
    public String hash(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        return hash(content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 计算字节数组的 SM3 哈希值
     * 
     * @param data 待哈希数据
     * @return SM3 哈希值 (十六进制字符串)
     */
    public String hash(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        
        byte[] result = new byte[digest.getDigestSize()];
        digest.doFinal(result, 0);
        
        return bytesToHex(result);
    }
    
    /**
     * 计算审计日志哈希链
     * 
     * 哈希内容组成：operatorId + operationType + objectDigest + createdTime + prevLogHash
     * 
     * @param operatorId 操作者ID
     * @param operationType 操作类型
     * @param objectDigest 对象摘要
     * @param createdTime 创建时间
     * @param prevLogHash 前一条日志哈希
     * @return 当前日志哈希值
     */
    public String calculateLogHash(String operatorId, String operationType, 
                                   String objectDigest, String createdTime, 
                                   String prevLogHash) {
        StringBuilder content = new StringBuilder();
        content.append(operatorId != null ? operatorId : "");
        content.append("|");
        content.append(operationType != null ? operationType : "");
        content.append("|");
        content.append(objectDigest != null ? objectDigest : "");
        content.append("|");
        content.append(createdTime != null ? createdTime : "");
        content.append("|");
        content.append(prevLogHash != null ? prevLogHash : "");
        
        return hash(content.toString());
    }

    /**
     * 计算 SM3 HMAC
     *
     * @param key HMAC 密钥
     * @param content 待摘要内容
     * @return HMAC 值（十六进制）
     */
    public String hmac(String key, String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        if (key == null || key.isEmpty()) {
            return hash(content);
        }

        HMac hmac = new HMac(new SM3Digest());
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        hmac.init(new KeyParameter(keyBytes));

        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        hmac.update(data, 0, data.length);

        byte[] out = new byte[hmac.getMacSize()];
        hmac.doFinal(out, 0);
        return bytesToHex(out);
    }
    
    /**
     * 验证日志哈希
     * 
     * @param expectedHash 期望的哈希值
     * @param actualHash 实际的哈希值
     * @return 是否匹配
     */
    public boolean verifyHash(String expectedHash, String actualHash) {
        if (expectedHash == null || actualHash == null) {
            return expectedHash == actualHash;
        }
        return expectedHash.equalsIgnoreCase(actualHash);
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
