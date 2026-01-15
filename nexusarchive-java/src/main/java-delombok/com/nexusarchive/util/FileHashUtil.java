// Input: org.bouncycastle、Spring Framework、Java 标准库
// Output: FileHashUtil 类
// Pos: 工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.util;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

/**
 * 文件完整性校验工具类
 * 
 * 支持 SM3 (国密算法) 和 SHA-256
 * 优先使用 SM3,符合信创要求
 */
@Component
public class FileHashUtil {
    
    static {
        // 注册 BouncyCastle 提供者以支持 SM3
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * 计算文件的 SM3 哈希值
     * 
     * @param file 文件对象
     * @return SM3 哈希值 (十六进制字符串)
     */
    public String calculateSM3(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return calculateSM3(is);
        }
    }
    
    /**
     * 计算输入流的 SM3 哈希值
     */
    public String calculateSM3(InputStream inputStream) throws IOException {
        SM3Digest digest = new SM3Digest();
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        
        return bytesToHex(hash);
    }
    
    /**
     * 计算文件的 SHA-256 哈希值 (备用方案)
     */
    public String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = new FileInputStream(file)) {
            return calculateSHA256(is);
        }
    }
    
    /**
     * 计算输入流的 SHA-256 哈希值
     */
    public String calculateSHA256(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }
        
        return bytesToHex(digest.digest());
    }
    
    /**
     * 验证文件哈希值
     * 
     * @param file 文件对象
     * @param expectedHash 期望的哈希值
     * @param algorithm 算法: "SM3" 或 "SHA256"
     * @return 是否匹配
     */
    public boolean verifyHash(File file, String expectedHash, String algorithm) throws IOException, NoSuchAlgorithmException {
        String actualHash;
        
        if ("SM3".equalsIgnoreCase(algorithm)) {
            actualHash = calculateSM3(file);
        } else if ("SHA256".equalsIgnoreCase(algorithm)) {
            actualHash = calculateSHA256(file);
        } else {
            throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
        
        return actualHash.equalsIgnoreCase(expectedHash);
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
