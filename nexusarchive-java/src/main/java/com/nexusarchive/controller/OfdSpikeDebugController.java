// Input: Spring Web、Lombok、MultipartFile、OfdConverterHelper
// Output: OfdSpikeDebugController 类（OFD 技术路线调试接口）
// Pos: 调试接口层 (仅限内部/开发使用)
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.service.converter.OfdConverterHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/debug/ofd-spike")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "OFD Spike Debug")
@Slf4j
public class OfdSpikeDebugController {

    private final OfdConverterHelper ofdConverterHelper;

    @PostMapping(value = "/convert-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "将上传的 PDF 通过 ofdrw 转为 OFD（调试接口）")
    public ResponseEntity<ByteArrayResource> convertPdf(@RequestParam("file") MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename() == null ? "debug-upload.pdf" : file.getOriginalFilename();
        String lowerName = originalFilename.toLowerCase();
        String contentType = file.getContentType();

        if (!lowerName.endsWith(".pdf") && !MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("仅支持 PDF 文件进行 ofdrw spike 转换");
        }

        Path tempPdf = Files.createTempFile("ofd-spike-", ".pdf");
        Path tempOfd = Files.createTempFile("ofd-spike-", ".ofd");

        try {
            file.transferTo(tempPdf);
            ofdConverterHelper.convertToOfd(tempPdf, tempOfd);

            byte[] bytes = Files.readAllBytes(tempOfd);
            String outputFileName = originalFilename.replaceAll("(?i)\\.pdf$", ".ofd");

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFileName + "\"")
                .header("X-OFD-Spike-Source", "ofdrw")
                .body(new ByteArrayResource(bytes));
        } finally {
            try {
                Files.deleteIfExists(tempPdf);
            } catch (Exception cleanupError) {
                log.warn("清理临时 PDF 失败: {}", tempPdf, cleanupError);
            }
            try {
                Files.deleteIfExists(tempOfd);
            } catch (Exception cleanupError) {
                log.warn("清理临时 OFD 失败: {}", tempOfd, cleanupError);
            }
        }
    }
}
