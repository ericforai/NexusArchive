package com.nexusarchive.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 路径安全工具类
 * 安全加固 - 防止路径遍历攻击
 */
@Component
@Slf4j
public class PathSecurityUtils {

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    @Value("${archive.temp.path:/tmp/nexusarchive}")
    private String tempPath;

    /**
     * 验证并规范化文件路径
     * 确保路径在允许的根目录内
     *
     * @param userPath 用户提供的路径
     * @param allowedRoot 允许的根目录
     * @return 规范化后的安全路径
     * @throws SecurityException 如果路径不安全
     */
    public Path validateAndNormalize(String userPath, String allowedRoot) {
        if (userPath == null || userPath.isEmpty()) {
            throw new SecurityException("路径不能为空");
        }

        // 检查危险字符
        if (containsDangerousChars(userPath)) {
            log.warn("🚫 检测到路径中包含危险字符: {}", sanitizeForLog(userPath));
            throw new SecurityException("路径包含非法字符");
        }

        try {
            // 规范化根目录路径
            Path rootPath = Paths.get(allowedRoot).toAbsolutePath().normalize();
            
            // 规范化用户路径
            Path resolvedPath = rootPath.resolve(userPath).toAbsolutePath().normalize();
            
            // 确保解析后的路径仍在根目录内
            if (!resolvedPath.startsWith(rootPath)) {
                log.warn("🚫 检测到路径遍历攻击: {} 尝试访问 {}", 
                    sanitizeForLog(userPath), resolvedPath);
                throw new SecurityException("非法的文件路径");
            }
            
            return resolvedPath;
            
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("路径验证失败: {}", e.getMessage());
            throw new SecurityException("无效的文件路径");
        }
    }

    /**
     * 验证档案存储路径
     */
    public Path validateArchivePath(String relativePath) {
        return validateAndNormalize(relativePath, archiveRootPath);
    }

    /**
     * 验证临时文件路径
     */
    public Path validateTempPath(String relativePath) {
        return validateAndNormalize(relativePath, tempPath);
    }

    /**
     * 检查路径是否包含危险字符
     */
    private boolean containsDangerousChars(String path) {
        // 检查路径遍历模式
        String[] dangerousPatterns = {
            "..",           // 父目录遍历
            "..\\",         // Windows 父目录遍历
            "../",          // Unix 父目录遍历
            "..%2f",        // URL 编码的 ../
            "..%5c",        // URL 编码的 ..\
            "%2e%2e",       // URL 编码的 ..
            "..%252f",      // 双重 URL 编码
            "....//",       // 过滤绕过尝试
            "....\\\\",     // 过滤绕过尝试
            "\0",           // 空字符注入
            "%00",          // URL 编码的空字符
        };

        String lowerPath = path.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerPath.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 为日志输出清理路径（防止日志注入）
     */
    private String sanitizeForLog(String path) {
        if (path == null) return "null";
        // 限制长度并移除换行符
        String sanitized = path.replaceAll("[\\r\\n]", "");
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        return sanitized;
    }

    /**
     * 获取安全的文件名（移除路径部分）
     */
    public String getSafeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new SecurityException("文件名不能为空");
        }

        // 移除路径分隔符，只保留文件名
        String safeName = Paths.get(fileName).getFileName().toString();
        
        // 检查是否包含危险字符
        if (containsDangerousChars(safeName)) {
            throw new SecurityException("文件名包含非法字符");
        }

        // 检查文件扩展名
        String extension = getFileExtension(safeName);
        if (isDangerousExtension(extension)) {
            log.warn("🚫 检测到危险文件类型: {}", extension);
            throw new SecurityException("不允许的文件类型: " + extension);
        }

        return safeName;
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 检查是否是危险的文件扩展名
     */
    private boolean isDangerousExtension(String extension) {
        String[] dangerous = {
            "exe", "bat", "cmd", "com", "msi", "scr", "pif",  // Windows 可执行
            "sh", "bash", "zsh",                               // Unix 脚本
            "php", "jsp", "asp", "aspx", "cgi",               // Web 脚本
            "jar", "war", "ear",                              // Java 归档
            "dll", "so", "dylib",                             // 动态库
            "vbs", "vbe", "js", "jse", "wsf", "wsh",          // 脚本
            "ps1", "psm1",                                    // PowerShell
            "hta", "htc",                                     // HTML 应用
        };

        for (String ext : dangerous) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证文件类型是否在允许列表中
     * 符合 DA/T 94-2022 对电子档案格式的要求
     */
    public boolean isAllowedArchiveFormat(String extension) {
        String[] allowed = {
            "pdf", "ofd",           // 版式文件（优先）
            "xml", "json",          // 结构化数据
            "jpg", "jpeg", "png", "tiff", "tif", "bmp",  // 图像
            "doc", "docx", "xls", "xlsx", "ppt", "pptx", // Office 文档
            "txt", "csv",           // 纯文本
            "zip", "rar", "7z"      // 压缩包
        };

        for (String ext : allowed) {
            if (ext.equals(extension.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
