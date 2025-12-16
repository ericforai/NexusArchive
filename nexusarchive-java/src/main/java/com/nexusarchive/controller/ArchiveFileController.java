package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
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
    private final FileStorageService fileStorageService;

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

        try {
            Path filePath = fileStorageService.resolvePath(content.getStoragePath());
            
            if (!fileStorageService.exists(content.getStoragePath())) {
                throw new BusinessException("Physical file not found: " + content.getStoragePath());
            }

            FileInputStream fis = new FileInputStream(filePath.toFile());
            InputStreamResource resource = new InputStreamResource(fis);

            // Determine content type
            String contentType = determineContentType(content.getFileType(), content.getFileName());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.error("File not found: {}", content.getStoragePath(), e);
            throw new BusinessException("File not accessible");
        }
    }

    /**
     * 通过文件 ID 直接下载文件
     * 用于全景视图附件预览（不需要额外权限校验，已登录即可）
     */
    @GetMapping("/files/download/{fileId}")
    @Operation(summary = "通过文件ID下载文件")
    public ResponseEntity<Resource> downloadByFileId(@PathVariable String fileId) {
        // 直接通过文件 ID 查询
        ArcFileContent content = fileContentMapper.selectById(fileId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException("File not found: " + fileId);
        }

        try {
            Path filePath = fileStorageService.resolvePath(content.getStoragePath());
            
            if (!fileStorageService.exists(content.getStoragePath())) {
                throw new BusinessException("Physical file not found: " + content.getStoragePath());
            }

            FileInputStream fis = new FileInputStream(filePath.toFile());
            InputStreamResource resource = new InputStreamResource(fis);

            String contentType = determineContentType(content.getFileType(), content.getFileName());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + content.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.error("File not found: {}", content.getStoragePath(), e);
            throw new BusinessException("File not accessible");
        }
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
}
