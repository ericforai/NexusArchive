package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.IngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Web 端归档控制器
 * 处理前端直接上传的文件
 */
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@Slf4j
public class WebIngestController {

    private final IngestService ingestService;

    /**
     * 文件上传接口
     * @param file 上传的文件
     * @return 上传后的文件路径或ID
     */
    @PostMapping("/upload")
    public Result<com.nexusarchive.dto.FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        log.info("收到文件上传请求: {}", file.getOriginalFilename());
        com.nexusarchive.dto.FileUploadResponse response = ingestService.handleFileUpload(file);
        return Result.success("上传成功", response);
    }
}
