// Input: 预览请求
// Output: 流式响应
// Pos: NexusCore service implementation
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.core.compliance.PdfWatermarkService;
import com.nexusarchive.core.compliance.WatermarkConfig;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.storage.StorageService;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultStreamingPreviewService implements StreamingPreviewService {

    private final StorageService storageService;
    private final FileContentMapper fileContentMapper;
    private final PdfWatermarkService pdfWatermarkService;

    @Override
    public ResponseEntity<Resource> preview(String archiveId,
                                            String fileId,
                                            String mode,
                                            Long rangeStart,
                                            Long rangeEnd,
                                            String traceId,
                                            String operator) throws IOException {
        // 1. Resolve FileContent
        FileContent fileContent = resolveFile(archiveId, fileId);
        if (fileContent == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        // 2. Security Check (Basic path validation, full RBAC in Controller/Aspect)
        String storagePath = fileContent.getStoragePath();
        if (!storageService.exists(storagePath)) {
            log.error("Physical file missing for metadata id={}: path={}",
                    fileContent.getId(), storagePath);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Physical file missing");
        }

        long fileSize = storageService.getLength(storagePath);
        String mimeType = determineMimeType(fileContent.getFileType());

        // 3. Handle "rendered" mode (Watermarking) -> Defer to M5, currently fallback to stream
        // 3. Handle "rendered" mode (Watermarking)
        if ("rendered".equalsIgnoreCase(mode) && "PDF".equalsIgnoreCase(fileContent.getFileType())) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String timestamp = LocalDateTime.now().format(formatter);
                String primary = operator + " " + timestamp + " " + traceId;
                String secondary = traceId + " " + fileContent.getFondsCode();
                WatermarkConfig config = WatermarkConfig.of(primary, secondary);

                InputStream sourceStream = storageService.getInputStream(storagePath);
                InputStream watermarkedStream = pdfWatermarkService.addWatermark(sourceStream, config);
                
                if (watermarkedStream instanceof PdfWatermarkService.TempFileInputStream tempStream) {
                    // Switch storagePath to temp file for Range handling
                    storagePath = tempStream.getOutputPath().toString();
                    fileSize = java.nio.file.Files.size(tempStream.getOutputPath());

                    final InputStream masterStream = watermarkedStream; // Holds the delete-on-close lock
                    
                    // Use Range logic on temp path
                    long start = rangeStart != null ? rangeStart : 0;
                    long end = rangeEnd != null ? rangeEnd : fileSize - 1;
                    long contentLength = end - start + 1;
                    
                    FileInputStream fis = new FileInputStream(storagePath);
                    fis.skip(start);
                    InputStream rangeStream = new BufferedInputStream(new InputStream() {
                         private final InputStream in = fis;
                         private long remaining = contentLength;
                         @Override public int read() throws IOException {
                             if (remaining-- <= 0) {
                                 return -1;
                             }
                             return in.read();
                         }
                         @Override public int read(byte[] b, int off, int len) throws IOException {
                             if (remaining <= 0) {
                                  return -1;
                             }
                             int r = in.read(b, off, (int)Math.min(len, remaining));
                             if (r > 0) {
                                 remaining -= r;
                             }
                             return r;
                         }
                         @Override public void close() throws IOException {
                             in.close();
                             masterStream.close(); // Triggers delete
                         }
                    });

                    InputStreamResource resource = new InputStreamResource(rangeStream);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(mimeType));
                    headers.setContentLength(contentLength);
                    headers.set("Accept-Ranges", "bytes");
                    String fileName = "watermarked_" + fileContent.getFileName();
                    headers.set("Content-Disposition", "inline; filename=\"" + fileName + "\"");

                    if (rangeStart != null || rangeEnd != null) {
                         headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
                         return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).headers(headers).body(resource);
                    } else {
                         return ResponseEntity.ok().headers(headers).body(resource);
                    }
                }
            } catch (Exception e) {
                 log.error("Failed to apply watermark", e);
                 // Fallback to raw
            }
        }

        // 4. Handle Range Request
        long start = rangeStart != null ? rangeStart : 0;
        long end = rangeEnd != null ? rangeEnd : fileSize - 1;
        long contentLength = end - start + 1;

        InputStream inputStream = storageService.getInputStream(storagePath, start, contentLength);
        InputStreamResource resource = new InputStreamResource(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setContentLength(contentLength);
        headers.set("Accept-Ranges", "bytes");
        headers.set("Content-Disposition",
                "inline; filename=\"" + fileContent.getFileName() + "\"");

        if (rangeStart != null || rangeEnd != null) {
            headers.set("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);
        } else {
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        }
    }

    private FileContent resolveFile(String archiveId, String fileId) {
        if (StringUtils.hasText(fileId)) {
            return fileContentMapper.selectById(fileId);
        }
        if (StringUtils.hasText(archiveId)) {
            // Find main file (priority: PDF > OFD > XML)
            List<FileContent> files = fileContentMapper.selectList(new LambdaQueryWrapper<FileContent>()
                    .eq(FileContent::getItemId, archiveId)
                    .orderByDesc(FileContent::getFileType)); // Rough priority default
            return files.isEmpty() ? null : files.get(0);
        }
        return null;
    }

    private String determineMimeType(String fileType) {
        if (fileType == null) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        switch (fileType.toUpperCase()) {
            case "PDF": return MediaType.APPLICATION_PDF_VALUE;
            case "OFD": return "application/ofd"; // Standardize OFD mime
            case "XML": return MediaType.APPLICATION_XML_VALUE;
            case "JPG":
            case "JPEG": return MediaType.IMAGE_JPEG_VALUE;
            case "PNG": return MediaType.IMAGE_PNG_VALUE;
            default: return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
