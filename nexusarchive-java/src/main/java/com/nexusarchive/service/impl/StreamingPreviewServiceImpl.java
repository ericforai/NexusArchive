// Input: StreamingPreviewService, FileStorageService, ArchiveService, AuditLogService
// Output: StreamingPreviewServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.StreamingPreviewService;
import com.nexusarchive.service.preview.PdfWatermarkRenderer;
import com.nexusarchive.service.preview.PreviewFilePathResolver;
import com.nexusarchive.service.preview.WatermarkGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;

/**
 * 流式预览服务实现
 *
 * <p>职责：</p>
 * <ol>
 *   <li>协调预览流程（流式、预签名、渲染）</li>
 *   <li>记录审计日志</li>
 * </ol>
 *
 * <p>具体实现已委托给：</p>
 * <ul>
 *   <li>{@link PreviewFilePathResolver} - 文件路径解析</li>
 *   <li>{@link WatermarkGenerator} - 水印生成</li>
 *   <li>{@link PdfWatermarkRenderer} - PDF水印渲染</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingPreviewServiceImpl implements StreamingPreviewService {

    private final FileStorageService fileStorageService;
    private final ArchiveService archiveService;
    private final AuditLogService auditLogService;
    private final WatermarkGenerator watermarkGenerator;
    private final PreviewFilePathResolver filePathResolver;
    private final PdfWatermarkRenderer pdfWatermarkRenderer;
    
    @Override
    public PreviewResponse streamPreview(String archiveId, String mode,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        // 1. 查询档案
        Archive archive = archiveService.getArchiveById(archiveId);

        // 2. 生成追踪ID和水印元数据
        String traceId = watermarkGenerator.generateTraceId();
        PreviewResponse.WatermarkMetadata watermark =
            watermarkGenerator.generateWatermarkMetadata(traceId, archive.getFondsNo());

        // 3. 构建响应
        PreviewResponse previewResponse = new PreviewResponse();
        previewResponse.setMode(mode);
        previewResponse.setTraceId(traceId);
        previewResponse.setWatermark(watermark);

        // 4. 根据模式处理
        switch (mode) {
            case "stream":
                handleStreamMode(archive, request, response, traceId);
                break;
            case "presigned":
                // TODO: 实现预签名 URL 生成
                log.warn("预签名 URL 生成功能待实现");
                break;
            case "rendered":
                // 服务端渲染模式，需要按页渲染
                // TODO: 实现按页渲染逻辑
                handleStreamMode(archive, request, response, traceId);
                break;
            default:
                throw new IllegalArgumentException("不支持的预览模式: " + mode);
        }

        // 5. 记录审计日志
        recordPreviewAuditLog(archiveId, mode, traceId);

        return previewResponse;
    }
    
    @Override
    public PreviewResponse generatePresignedUrl(String archiveId, int expiresInSeconds) {
        // 1. 查询档案
        Archive archive = archiveService.getArchiveById(archiveId);

        // 2. 生成追踪ID和水印
        String traceId = watermarkGenerator.generateTraceId();
        PreviewResponse.WatermarkMetadata watermark =
            watermarkGenerator.generateWatermarkMetadata(traceId, archive.getFondsNo());

        // 3. 生成预签名 URL
        // TODO: 实现预签名 URL 生成
        String presignedUrl = null; // 占位符，待实现
        log.warn("预签名 URL 生成功能待实现: archiveId={}", archiveId);

        // 4. 构建响应
        PreviewResponse response = new PreviewResponse();
        response.setMode("presigned");
        response.setPresignedUrl(presignedUrl);
        response.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        response.setTraceId(traceId);
        response.setWatermark(watermark);

        // 5. 记录审计日志
        recordPreviewAuditLog(archiveId, "presigned", traceId);

        return response;
    }
    
    @Override
    public void renderWithWatermark(String archiveId, int pageNumber,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            // 1. 查询档案
            Archive archive = archiveService.getArchiveById(archiveId);

            // 2. 获取文件路径
            String filePath = filePathResolver.resolveArchiveFilePath(archiveId);
            if (filePath == null) {
                log.warn("档案文件不存在: archiveId={}", archiveId);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 3. 获取文件
            java.io.File file = fileStorageService.getFile(filePath);
            if (file == null || !file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // 4. 生成追踪ID和水印文本
            String traceId = watermarkGenerator.generateTraceId();
            String watermarkText = watermarkGenerator.generateWatermarkText(traceId);
            String watermarkSubtext = watermarkGenerator.generateWatermarkSubtext(traceId, archive.getFondsNo());

            // 5. 使用 PdfWatermarkRenderer 渲染指定页面并添加水印
            pdfWatermarkRenderer.renderPageWithWatermark(file, pageNumber, watermarkText,
                                                         watermarkSubtext, traceId, response);

            // 6. 记录审计日志
            recordPreviewAuditLog(archiveId, "rendered", traceId);

        } catch (Exception e) {
            log.error("服务端渲染带水印失败: archiveId={}, pageNumber={}", archiveId, pageNumber, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("渲染失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 处理流式模式
     */
    private void handleStreamMode(Archive archive, HttpServletRequest request,
                                 HttpServletResponse response, String traceId) {
        try {
            // 1. 获取文件路径
            String relativePath = filePathResolver.resolveArchiveFilePath(archive.getArchiveCode());
            log.debug("[handleStreamMode] relativePath={}", relativePath);
            if (relativePath == null) {
                log.warn("[handleStreamMode] 档案文件不存在: archiveId={}", archive.getId());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            java.io.File file = fileStorageService.getFile(relativePath);
            log.debug("[handleStreamMode] file={}, exists={}", file, file != null ? file.exists() : "null");
            if (file == null || !file.exists()) {
                log.error("[handleStreamMode] 文件不存在: relativePath={}, file={}, exists={}",
                    relativePath, file, file != null ? file.exists() : "N/A");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Resource fileResource = new org.springframework.core.io.FileSystemResource(file);

            // 2. 生成水印文本
            String watermarkText = watermarkGenerator.generateWatermarkText(traceId);
            String watermarkSubtext = watermarkGenerator.generateWatermarkSubtext(traceId, archive.getFondsNo());

            // 3. 设置响应头
            response.setContentType("application/pdf");
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Watermark-Text", watermarkText);
            response.setHeader("X-Watermark-Subtext", watermarkSubtext);
            response.setHeader("X-Watermark-Opacity", "0.3");
            response.setHeader("X-Watermark-Rotate", "-45");

            // 4. 处理 Range 请求（支持断点续传）
            long fileLength = fileResource.contentLength();
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                handleRangeRequest(fileResource, rangeHeader, fileLength, response);
            } else {
                handleFullFileTransfer(fileResource, fileLength, response);
            }
        } catch (IOException e) {
            log.error("流式预览失败: archiveId={}", archive.getId(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理 Range 请求（断点续传）
     */
    private void handleRangeRequest(Resource fileResource, String rangeHeader,
                                   long fileLength, HttpServletResponse response) throws IOException {
        // 解析 Range 请求
        String range = rangeHeader.substring(6);
        String[] ranges = range.split("-");
        long start = Long.parseLong(ranges[0]);
        long end = ranges.length > 1 && !ranges[1].isEmpty()
            ? Long.parseLong(ranges[1])
            : fileLength - 1;

        long contentLength = end - start + 1;
        response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
        response.setHeader(HttpHeaders.CONTENT_RANGE,
            String.format("bytes %d-%d/%d", start, end, fileLength));
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));

        // 流式传输指定范围
        try (InputStream inputStream = fileResource.getInputStream();
             OutputStream outputStream = response.getOutputStream()) {
            inputStream.skip(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            while (remaining > 0) {
                int read = inputStream.read(buffer, 0,
                    (int) Math.min(buffer.length, remaining));
                if (read == -1) break;
                outputStream.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    /**
     * 处理完整文件传输
     */
    private void handleFullFileTransfer(Resource fileResource, long fileLength,
                                       HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength));
        try (InputStream inputStream = fileResource.getInputStream();
             OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 记录预览审计日志
     */
    private void recordPreviewAuditLog(String archiveId, String mode, String traceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "Unknown";
        String username = "Unknown";
        if (authentication != null) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else {
                username = authentication.getName();
            }
        }

        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction("ARCHIVE_PREVIEW");
        auditLog.setResourceType("ARCHIVE");
        auditLog.setResourceId(archiveId);
        auditLog.setOperationResult("SUCCESS");
        auditLog.setDetails(String.format("预览模式: %s, TraceID: %s", mode, traceId));
        auditLog.setClientIp(getClientIp());
        auditLog.setTraceId(traceId);
        auditLogService.log(auditLog);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        // TODO: 从请求中获取客户端IP
        return "UNKNOWN";
    }
}
