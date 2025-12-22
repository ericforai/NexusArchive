// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: FileStorageServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    @Override
    public Path resolvePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            throw new BusinessException("File path cannot be null or empty");
        }
        
        // [FIXED P0-2] 1. URL 解码（防止 %2e%2e 绕过）
        try {
            relativePath = java.net.URLDecoder.decode(relativePath, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("Invalid file path encoding: " + relativePath);
        }
        
        // [FIXED P0-2] 2. 禁止绝对路径和 ./ 开头
        if (relativePath.startsWith("/") || relativePath.startsWith("./") || relativePath.startsWith("../")) {
            throw new BusinessException("Absolute paths and relative paths are not allowed: " + relativePath);
        }
        
        // [FIXED P0-2] 3. 检查所有路径遍历模式
        if (relativePath.contains("..") || relativePath.contains("./") || relativePath.contains("\\")) {
            throw new BusinessException("Path traversal detected: " + relativePath);
        }
        
        // [FIXED P0-2] 4. 先拼接再 normalize，然后校验
        Path basePath = Paths.get(archiveRootPath).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(relativePath).normalize();
        
        // [FIXED P0-2] 5. 确保最终路径在 archiveRootPath 内
        if (!targetPath.startsWith(basePath)) {
            log.error("Path traversal attempt blocked: {} -> {}", relativePath, targetPath);
            throw new BusinessException("Access denied: path outside archive root");
        }
        
        return targetPath;
    }

    @Override
    public String saveFile(InputStream inputStream, String relativePath) {
        try {
            Path targetPath = resolvePath(relativePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return relativePath;
        } catch (IOException e) {
            log.error("Failed to save file: {}", relativePath, e);
            throw new BusinessException("Failed to save file: " + e.getMessage());
        }
    }

    @Override
    public boolean exists(String relativePath) {
        try {
            Path path = resolvePath(relativePath);
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }
}
