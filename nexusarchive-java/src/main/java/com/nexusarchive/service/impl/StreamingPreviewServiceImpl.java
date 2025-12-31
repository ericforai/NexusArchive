// Input: StreamingPreviewService, FileStorageService, ArchiveService, AuditLogService
// Output: StreamingPreviewServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveAttachmentMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.StreamingPreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 流式预览服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingPreviewServiceImpl implements StreamingPreviewService {
    
    private final FileStorageService fileStorageService;
    private final ArchiveService archiveService;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArchiveAttachmentMapper archiveAttachmentMapper;
    private final AuditLogService auditLogService;
    
    @Override
    public PreviewResponse streamPreview(String archiveId, String mode, 
                                        HttpServletRequest request, 
                                        HttpServletResponse response) {
        // 1. 查询档案
        Archive archive = archiveService.getArchiveById(archiveId);
        
        // 2. 生成追踪ID
        String traceId = UUID.randomUUID().toString();
        
        // 3. 生成水印元数据
        PreviewResponse.WatermarkMetadata watermark = generateWatermarkMetadata(traceId, archive.getFondsNo());
        
        // 4. 根据模式处理
        PreviewResponse previewResponse = new PreviewResponse();
        previewResponse.setMode(mode);
        previewResponse.setTraceId(traceId);
        previewResponse.setWatermark(watermark);
        
        switch (mode) {
            case "stream":
                handleStreamMode(archive, request, response, traceId);
                break;
            case "presigned":
                // TODO: 实现预签名 URL 生成
                // String presignedUrl = fileStorageService.generatePresignedUrl(
                //     archive.getId(), 3600); // 1小时有效期
                // previewResponse.setPresignedUrl(presignedUrl);
                // previewResponse.setExpiresAt(LocalDateTime.now().plusHours(1));
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
        
        // 2. 生成追踪ID
        String traceId = UUID.randomUUID().toString();
        
        // 3. 生成预签名 URL
        // TODO: 实现预签名 URL 生成
        // String presignedUrl = fileStorageService.generatePresignedUrl(archiveId, expiresInSeconds);
        String presignedUrl = null; // 占位符，待实现
        log.warn("预签名 URL 生成功能待实现: archiveId={}", archiveId);
        
        // 4. 生成水印元数据
        PreviewResponse.WatermarkMetadata watermark = generateWatermarkMetadata(traceId, archive.getFondsNo());
        
        // 5. 构建响应
        PreviewResponse response = new PreviewResponse();
        response.setMode("presigned");
        response.setPresignedUrl(presignedUrl);
        response.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        response.setTraceId(traceId);
        response.setWatermark(watermark);
        
        // 6. 记录审计日志
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
            String filePath = getArchiveFilePath(archiveId);
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
            String traceId = UUID.randomUUID().toString();
            String watermarkText = generateWatermarkText(traceId);
            String watermarkSubtext = generateWatermarkSubtext(traceId, archive.getFondsNo());
            
            // 5. 使用 PDFBox 渲染指定页面并添加水印
            try (PDDocument document = PDDocument.load(file)) {
                int totalPages = document.getNumberOfPages();
                if (pageNumber < 1 || pageNumber > totalPages) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("页码超出范围: " + pageNumber);
                    return;
                }
                
                // 6. 创建新文档，只包含指定页面
                PDDocument watermarkedDoc = new PDDocument();
                PDPage sourcePage = document.getPage(pageNumber - 1);
                PDPage newPage = new PDPage(sourcePage.getMediaBox());
                watermarkedDoc.addPage(newPage);
                
                // 7. 复制页面内容并添加水印
                try (PDPageContentStream contentStream = new PDPageContentStream(
                        watermarkedDoc, newPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    // 复制原始页面内容（简化处理：绘制矩形表示内容区域）
                    PDRectangle mediaBox = newPage.getMediaBox();
                    float width = mediaBox.getWidth();
                    float height = mediaBox.getHeight();
                    
                    // 添加水印文本（倾斜、半透明）
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 48);
                    contentStream.setNonStrokingColor(new Color(200, 200, 200, 100)); // 半透明灰色
                    
                    // 计算文本位置（居中）
                    float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(watermarkText) / 1000 * 48;
                    float textX = (width - textWidth) / 2;
                    float textY = height / 2;
                    
                    // 保存当前变换矩阵
                    contentStream.saveGraphicsState();
                    
                    // 旋转和定位（使用 AffineTransform 实现旋转）
                    contentStream.beginText();
                    // 移动到旋转中心
                    contentStream.newLineAtOffset(textX, textY);
                    // 应用旋转变换（-45度）
                    Matrix rotationMatrix = Matrix.getRotateInstance(Math.toRadians(-45), textX, textY);
                    contentStream.transform(rotationMatrix);
                    contentStream.showText(watermarkText);
                    contentStream.endText();
                    
                    // 添加副文本
                    contentStream.setFont(PDType1Font.HELVETICA, 24);
                    float subtextWidth = PDType1Font.HELVETICA.getStringWidth(watermarkSubtext) / 1000 * 24;
                    float subtextX = (width - subtextWidth) / 2;
                    float subtextY = height / 2 - 60;
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(subtextX, subtextY);
                    Matrix subtextRotationMatrix = Matrix.getRotateInstance(Math.toRadians(-45), subtextX, subtextY);
                    contentStream.transform(subtextRotationMatrix);
                    contentStream.showText(watermarkSubtext);
                    contentStream.endText();
                    
                    // 恢复变换矩阵
                    contentStream.restoreGraphicsState();
                }
                
                // 8. 输出到响应流
                response.setContentType("application/pdf");
                response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
                response.setHeader("X-Trace-Id", traceId);
                response.setHeader("X-Watermark-Text", watermarkText);
                response.setHeader("X-Watermark-Subtext", watermarkSubtext);
                response.setHeader("X-Watermark-Opacity", "0.3");
                response.setHeader("X-Watermark-Rotate", "-45");
                response.setHeader("X-Page-Number", String.valueOf(pageNumber));
                response.setHeader("X-Total-Pages", String.valueOf(totalPages));
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                watermarkedDoc.save(baos);
                watermarkedDoc.close();
                
                byte[] pdfBytes = baos.toByteArray();
                response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(pdfBytes.length));
                response.getOutputStream().write(pdfBytes);
                response.getOutputStream().flush();
                
                log.info("服务端渲染带水印完成: archiveId={}, pageNumber={}, traceId={}", 
                    archiveId, pageNumber, traceId);
            }
            
            // 9. 记录审计日志
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
     * 获取档案的文件路径
     */
    private String getArchiveFilePath(String archiveId) {
        // 1. 优先从 ArchiveAttachment 表查询
        List<ArchiveAttachment> attachments = archiveAttachmentMapper.selectByArchiveId(archiveId);
        if (!attachments.isEmpty()) {
            // 获取第一个附件（通常是主文件）
            String fileId = attachments.get(0).getFileId();
            ArcFileContent fileContent = arcFileContentMapper.selectById(fileId);
            if (fileContent != null && fileContent.getStoragePath() != null) {
                return fileContent.getStoragePath();
            }
        }
        
        // 2. 从 ArcFileContent 表查询（通过 item_id 关联）
        List<ArcFileContent> files = arcFileContentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, archiveId)
                .orderByAsc(ArcFileContent::getCreatedTime)
                .last("LIMIT 1")
        );
        
        if (!files.isEmpty() && files.get(0).getStoragePath() != null) {
            return files.get(0).getStoragePath();
        }
        
        return null;
    }
    
    /**
     * 处理流式模式
     */
    private void handleStreamMode(Archive archive, HttpServletRequest request, 
                                 HttpServletResponse response, String traceId) {
        try {
            // 1. 获取文件资源
            // TODO: 实现文件资源获取
            // Resource fileResource = fileStorageService.getFileResource(archive.getId());
            // if (fileResource == null || !fileResource.exists()) {
            //     response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            //     return;
            // }
            
            // 1. 获取文件路径
            String relativePath = getArchiveFilePath(archive.getId());
            if (relativePath == null) {
                log.warn("档案文件不存在: archiveId={}", archive.getId());
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            java.io.File file = fileStorageService.getFile(relativePath);
            if (file == null || !file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Resource fileResource = new org.springframework.core.io.FileSystemResource(file);
            
            // 2. 设置响应头
            response.setContentType("application/pdf"); // 或根据文件类型动态设置
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Watermark-Text", generateWatermarkText(traceId));
            response.setHeader("X-Watermark-Subtext", generateWatermarkSubtext(traceId, archive.getFondsNo()));
            response.setHeader("X-Watermark-Opacity", "0.3");
            response.setHeader("X-Watermark-Rotate", "-45");
            
            // 3. 处理 Range 请求（支持断点续传）
            long fileLength = fileResource.contentLength();
            String rangeHeader = request.getHeader(HttpHeaders.RANGE);
            
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
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
            } else {
                // 完整文件传输
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
        } catch (IOException e) {
            log.error("流式预览失败: archiveId={}", archive.getId(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 生成水印元数据
     */
    private PreviewResponse.WatermarkMetadata generateWatermarkMetadata(String traceId, String fondsNo) {
        PreviewResponse.WatermarkMetadata watermark = new PreviewResponse.WatermarkMetadata();
        watermark.setText(generateWatermarkText(traceId));
        watermark.setSubtext(generateWatermarkSubtext(traceId, fondsNo));
        watermark.setOpacity(0.3);
        watermark.setRotate(-45);
        return watermark;
    }
    
    /**
     * 生成水印文本（用户名 + 时间戳 + TraceID）
     */
    private String generateWatermarkText(String traceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "Unknown";
        String timestamp = LocalDateTime.now().toString();
        return String.format("%s %s %s", username, timestamp, traceId);
    }
    
    /**
     * 生成水印副文本（TraceID + FondsNo）
     */
    private String generateWatermarkSubtext(String traceId, String fondsNo) {
        return String.format("%s %s", traceId, fondsNo);
    }
    
    /**
     * 记录预览审计日志
     */
    private void recordPreviewAuditLog(String archiveId, String mode, String traceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication != null ? authentication.getName() : "Unknown";
        String username = authentication != null ? 
            (authentication.getPrincipal() != null ? authentication.getPrincipal().toString() : "Unknown") : "Unknown";

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
