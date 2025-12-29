// Input: MyBatis-Plus、io.swagger、Lombok、Spring Framework、等
// Output: ArchiveFileController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "档案文件内容")
public class ArchiveFileController {

    private final ArcFileContentMapper fileContentMapper;
    private final ArchiveMapper archiveMapper;
    private final FileStorageService fileStorageService;
    private final DataScopeService dataScopeService;

    @GetMapping("/{id}/content")
    @Operation(summary = "获取档案文件内容（支持 PDF/OFD 等）")
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> getFileContent(@PathVariable String id) {
        // 1. Query file content record by item_id
        ArcFileContent content = fileContentMapper.selectOne(
                new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getItemId, id)
                        .orderByDesc(ArcFileContent::getCreatedTime)
                        .last("LIMIT 1")
        );

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException("File not found for archive: " + id);
        }
        authorizeArchiveAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());
        
        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException("Physical file not found: " + content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());
        
        String contentType = determineContentType(content.getFileType(), content.getFileName());
        
        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(content.getFileName(), "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedFileName = content.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.getFileSize())  // [ADDED] 设置 Content-Length
                .body(resource);
    }

    /**
     * 通过文件 ID 直接下载文件
     * 用于全景视图附件预览（不需要额外权限校验，已登录即可）
     */
    @GetMapping("/files/download/{fileId}")
    @Operation(summary = "通过文件ID下载文件")
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> downloadByFileId(@PathVariable String fileId) {
        // 直接通过文件 ID 查询
        ArcFileContent content = fileContentMapper.selectById(fileId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException("File not found: " + fileId);
        }
        authorizeArchiveAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());
        
        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException("Physical file not found: " + content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());
        
        String contentType = determineContentType(content.getFileType(), content.getFileName());
        
        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(content.getFileName(), "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedFileName = content.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.getFileSize())  // [ADDED] 设置 Content-Length
                .body(resource);
    }

    private String determineContentType(String fileType, String fileName) {
        if (fileType != null) {
            switch (fileType.toLowerCase()) {
                case "ofd":
                    return "application/ofd";
                case "pdf":
                    return "application/pdf";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "xml":
                    return "application/xml";
            }
        }
        
        if (fileName != null) {
            if (fileName.endsWith(".ofd")) return "application/ofd";
            if (fileName.endsWith(".pdf")) return "application/pdf";
        }
        
        return "application/octet-stream";
    }

    private void authorizeArchiveAccess(String archivalCode) {
        if (archivalCode == null || archivalCode.isEmpty()) {
            throw new BusinessException("File not bound to an archive");
        }

        // 兼容两种数据格式：
        // 1. archival_code 存储的是 acc_archive.id (如 UUID格式)
        // 2. archival_code 存储的是 acc_archive.archive_code (如 BR-GROUP-2024-30Y-FIN-AC01-0002)
        Archive archive = archiveMapper.selectById(archivalCode);
        if (archive == null) {
            // 尝试按 archive_code 查找
            archive = archiveMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                            .eq(Archive::getArchiveCode, archivalCode)
                            .last("LIMIT 1")
            );
        }
        
        if (archive == null) {
            throw new BusinessException("Archive not found: " + archivalCode);
        }

        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException("无权访问该档案文件");
        }
    }
}
