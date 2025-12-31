// Input: 大文件流式哈希
// Output: 文件哈希计算服务（避免 OOM）
// Pos: NexusCore compliance/hash
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.HexFormat;
import java.util.Objects;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
public class FileHashService {
    private static final String PROVIDER_BC = "BC";
    private static final int BUFFER_SIZE = 8192;

    static {
        if (Security.getProvider(PROVIDER_BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public String hashFile(Path filePath, HashAlgorithm algorithm) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        try (InputStream is = new BufferedInputStream(Files.newInputStream(filePath))) {
            return hashStream(is, algorithm);
        }
    }

    public String hashStream(InputStream inputStream, HashAlgorithm algorithm) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream must not be null");
        Objects.requireNonNull(algorithm, "algorithm must not be null");
        MessageDigest md = createDigest(algorithm);
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private MessageDigest createDigest(HashAlgorithm algorithm) {
        try {
            return switch (algorithm) {
                case SM3 -> MessageDigest.getInstance("SM3", PROVIDER_BC);
                case SHA256 -> MessageDigest.getInstance("SHA-256");
            };
        } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
            throw new IllegalStateException("Hash algorithm not available: " + algorithm, ex);
        }
    }
}
