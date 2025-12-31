// Input: storage root path config
// Output: Local File IO implementation
// Pos: NexusCore storage/local
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.storage;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@Primary
public class LocalFileStorageService implements StorageService {

    private final Path rootPath;

    public LocalFileStorageService(@Value("${nexus.storage.local.root-path:./data/archives}") String rootPathStr) {
        this.rootPath = Paths.get(rootPathStr).toAbsolutePath().normalize();
        log.info("LocalFileStorageService initialized with root: {}", this.rootPath);
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            log.error("Failed to create storage root directory", e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        return Files.exists(resolveAndValidate(storagePath));
    }

    @Override
    public long getLength(String storagePath) throws IOException {
        Path path = resolveAndValidate(storagePath);
        return Files.size(path);
    }

    @Override
    public InputStream getInputStream(String storagePath) throws IOException {
        Path path = resolveAndValidate(storagePath);
        return new BufferedInputStream(Files.newInputStream(path));
    }

    @Override
    public InputStream getInputStream(String storagePath, long offset, long length) throws IOException {
        Path path = resolveAndValidate(storagePath);
        long fileSize = Files.size(path);
        
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }
        if (offset >= fileSize) {
            throw new IOException("Offset out of bounds: " + offset + " >= " + fileSize);
        }
        
        long actualLength;
        if (length < 0 || offset + length > fileSize) {
            actualLength = fileSize - offset;
        } else {
            actualLength = length;
        }

        FileInputStream fis = new FileInputStream(path.toFile());
        long skipped = fis.skip(offset);
        if (skipped != offset) {
            fis.close();
            throw new IOException("Failed to skip " + offset + " bytes");
        }
        
        // Wrap with LimitedInputStream to ensure strictly reading 'actualLength' bytes
        return new BufferedInputStream(new LimitedInputStream(fis, actualLength));
    }

    private Path resolveAndValidate(String storagePath) {
        Assert.hasText(storagePath, "Storage path must not be empty");
        
        // [P0-FIX] 增强目录遍历防护：处理 URL 编码绕过
        String decodedPath = storagePath;
        try {
            decodedPath = java.net.URLDecoder.decode(storagePath, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 解码失败则使用原始路径
        }
        
        // Prevent directory traversal (检查原始和解码后的路径)
        if (storagePath.contains("..") || decodedPath.contains("..")) {
            throw new IllegalArgumentException("Invalid path sequence: " + storagePath);
        }
        
        // 支持绝对路径与相对路径混合逻辑
        // 如果数据库存的是相对路径，则 resolve。如果是绝对路径 (历史数据)，则校验是否在 root 下。
        Path requested = Paths.get(decodedPath);
        Path target;
        if (requested.isAbsolute()) {
            target = requested.normalize();
        } else {
            target = this.rootPath.resolve(decodedPath).normalize();
        }

        // [P0-FIX] Security Check: Path Access Control - 必须启用
        if (!target.startsWith(this.rootPath)) {
            log.warn("路径访问被拒绝: 请求路径 {} 超出存储根目录 {}", target, this.rootPath);
            throw new SecurityException("Access denied: Path is outside storage root");
        }
        
        // [P0-FIX] 符号链接检查：解析真实路径后再次校验
        try {
            Path realPath = target.toRealPath();
            if (!realPath.startsWith(this.rootPath.toRealPath())) {
                log.warn("符号链接绕过被拒绝: 真实路径 {} 超出存储根目录", realPath);
                throw new SecurityException("Access denied: Symbolic link points outside storage root");
            }
        } catch (java.nio.file.NoSuchFileException e) {
            // 文件不存在时跳过符号链接检查（可能是检查路径是否存在）
        } catch (IOException e) {
            log.warn("无法解析真实路径: {}", target, e);
        }
        
        return target;
    }

    /**
     * Simple internal LimitedInputStream to enforce read limit
     */
    private static class LimitedInputStream extends InputStream {
        private final InputStream in;
        private final long limit;
        private long bytesRead = 0;

        public LimitedInputStream(InputStream in, long limit) {
            this.in = in;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            if (bytesRead >= limit) {
                return -1;
            }
            int b = in.read();
            if (b != -1) {
                bytesRead++;
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bytesRead >= limit) {
                return -1;
            }
            long remaining = limit - bytesRead;
            int lenToRead = (int) Math.min(len, remaining);
            int numRead = in.read(b, off, lenToRead);
            if (numRead != -1) {
                bytesRead += numRead;
            }
            return numRead;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
