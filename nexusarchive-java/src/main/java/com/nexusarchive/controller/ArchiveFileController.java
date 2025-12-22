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
    @PreAuthorize("hasAuthority('archive:query')")
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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
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
    @PreAuthorize("hasAuthority('archive:query')")
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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
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

        Archive archive = archiveMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Archive>()
                        .eq("archive_code", archivalCode)
                        .last("LIMIT 1")
        );
        if (archive == null) {
            throw new BusinessException("Archive not found for code: " + archivalCode);
        }

        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException("无权访问该档案文件");
        }
    }
}
