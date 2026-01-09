// Input: Java 标准库、Spring Framework
// Output: FileHashService 类
// Pos: 业务服务层

package com.nexusarchive.service.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 文件哈希计算服务
 * <p>
 * 支持 SM3 (国密) 和 SHA-256 算法
 * 优先使用 SM3，回退到 SHA-256
 * </p>
 */
@Service
@Slf4j
public class FileHashService {

    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * 计算文件哈希值
     * 优先使用 SM3 算法，不支持则回退到 SHA-256
     *
     * @param fileBytes 文件字节数组
     * @return 哈希结果 (包含算法和值)
     */
    public FileHashResult calculateHash(byte[] fileBytes) {
        try {
            // 优先使用 SM3 (国密算法)
            MessageDigest sm3 = MessageDigest.getInstance("SM3");
            byte[] hashBytes = sm3.digest(fileBytes);
            String hashValue = HEX_FORMAT.formatHex(hashBytes);
            log.debug("Calculated SM3 hash: {}", hashValue.substring(0, 8) + "...");
            return new FileHashResult("SM3", hashValue);
        } catch (java.security.NoSuchAlgorithmException e) {
            // 回退到 SHA-256
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = sha256.digest(fileBytes);
                String hashValue = HEX_FORMAT.formatHex(hashBytes);
                log.debug("Calculated SHA-256 hash: {} (SM3 not available)", hashValue.substring(0, 8) + "...");
                return new FileHashResult("SHA-256", hashValue);
            } catch (java.security.NoSuchAlgorithmException ex) {
                log.error("No hash algorithm available", ex);
                throw new RuntimeException("No hash algorithm available", ex);
            }
        }
    }

    /**
     * 哈希计算结果
     */
    public record FileHashResult(String algorithm, String hashValue) {
    }
}
