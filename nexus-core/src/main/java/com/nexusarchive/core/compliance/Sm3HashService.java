// Input: BouncyCastle SM3 算法
// Output: 哈希计算服务（SM3 / SHA256）
// Pos: NexusCore compliance/hash
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.HexFormat;
import java.util.Objects;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
public class Sm3HashService {
    private static final String PROVIDER_BC = "BC";
    private static final String ALGORITHM_SM3 = "SM3";
    private static final String ALGORITHM_SHA256 = "SHA-256";

    static {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public String hashSm3(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return computeHash(data, ALGORITHM_SM3, PROVIDER_BC);
    }

    public String hashSm3(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return hashSm3(text.getBytes(StandardCharsets.UTF_8));
    }

    public String hashSha256(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        return computeHash(data, ALGORITHM_SHA256, null);
    }

    public String hash(byte[] data, HashAlgorithm algorithm) {
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        return switch (algorithm) {
            case SM3 -> hashSm3(data);
            case SHA256 -> hashSha256(data);
        };
    }

    private String computeHash(byte[] data, String algorithm, String provider) {
        try {
            MessageDigest md = provider != null
                    ? MessageDigest.getInstance(algorithm, provider)
                    : MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new IllegalStateException("Hash algorithm not available: " + algorithm, ex);
        }
    }
}
