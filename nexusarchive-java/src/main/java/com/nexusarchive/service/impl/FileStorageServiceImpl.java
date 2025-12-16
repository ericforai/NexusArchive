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
        if (relativePath == null) {
            throw new BusinessException("File path cannot be null");
        }
        // 防止路径遍历攻击
        if (relativePath.contains("..")) {
            throw new BusinessException("Invalid file path: " + relativePath);
        }
        
        // 处理以 ./ 开头的相对路径（相对于工作目录）
        if (relativePath.startsWith("./")) {
            return Paths.get(relativePath).toAbsolutePath().normalize();
        }
        
        return Paths.get(archiveRootPath, relativePath).toAbsolutePath();
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
