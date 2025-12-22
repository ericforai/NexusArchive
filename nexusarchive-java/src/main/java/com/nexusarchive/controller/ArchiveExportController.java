// Input: Lombok、Spring Framework、Spring Security、Java 标准库、等
// Output: ArchiveExportController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.service.ArchiveExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.nexusarchive.annotation.ArchivalAudit;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;

/**
 * 档案导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ArchiveExportController {
    
    private final ArchiveExportService exportService;
    
    /**
     * 导出 AIP 包为 ZIP 文件
     * 
     * @param archivalCode 档号
     * @return ZIP 文件流
     */
    @GetMapping("/aip/{archivalCode}")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:export','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "EXPORT", resourceType = "ARCHIVE", description = "导出 AIP 包")
    public ResponseEntity<Resource> downloadAipPackage(@PathVariable String archivalCode) throws IOException {
        log.info("收到 AIP 包导出请求: {}", archivalCode);
        
        // 生成 ZIP 文件
        File zipFile = exportService.exportAipPackage(archivalCode);
        
        // 使用 FileSystemResource 避免流被多次读取的问题
        FileSystemResource resource = new FileSystemResource(zipFile);
        
        // 设置响应头
        String filename = archivalCode + ".zip";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipFile.length())
                .body(resource);
    }
}
