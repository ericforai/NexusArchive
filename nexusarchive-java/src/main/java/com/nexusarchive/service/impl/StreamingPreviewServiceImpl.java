// Input: StreamingPreviewService, FileStorageService, ArchiveService, AuditLogService
// Output: StreamingPreviewServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.OriginalVoucherService;
import com.nexusarchive.service.StreamingPreviewService;
import com.nexusarchive.service.preview.PdfWatermarkRenderer;
import com.nexusarchive.service.preview.PreviewFilePathResolver;
import com.nexusarchive.service.preview.PreviewFilePathResolver.ResolvedPreviewFile;
import com.nexusarchive.service.preview.WatermarkGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final ArchiveFileContentService archiveFileContentService;
    private final OriginalVoucherService originalVoucherService;
    private final AuditLogService auditLogService;
    private final DataScopeService dataScopeService;
    private final ArchiveMapper archiveMapper;
    private final OriginalVoucherMapper originalVoucherMapper;
    private final WatermarkGenerator watermarkGenerator;
    private final PreviewFilePathResolver filePathResolver;
    private final PdfWatermarkRenderer pdfWatermarkRenderer;
    
    @Override
    public PreviewResponse streamPreview(String archiveId, String mode,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        validateArchiveMainRequest(archiveId);
        return streamPreview(RESOURCE_TYPE_ARCHIVE_MAIN, archiveId, null, mode, request, response);
    }

    @Override
    public PreviewResponse streamPreview(String resourceType, String archiveId, String fileId, String mode,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        PreviewTarget target = resolvePreviewTarget(resourceType, archiveId, fileId);

        // 2. 生成追踪ID和水印元数据
        String traceId = watermarkGenerator.generateTraceId();
        PreviewResponse.WatermarkMetadata watermark =
            watermarkGenerator.generateWatermarkMetadata(traceId, target.fondsCode());

        // 3. 构建响应
        PreviewResponse previewResponse = new PreviewResponse();
        previewResponse.setMode(mode);
        previewResponse.setTraceId(traceId);
        previewResponse.setWatermark(watermark);

        // 4. 根据模式处理
        switch (mode) {
            case "stream":
                handleStreamMode(target, request, response, traceId);
                break;
            case "presigned":
                // TODO: 实现预签名 URL 生成
                log.warn("预签名 URL 生成功能待实现");
                break;
            case "rendered":
                // 服务端渲染模式，需要按页渲染
                // TODO: 实现按页渲染逻辑
                handleStreamMode(target, request, response, traceId);
                break;
            default:
                throw new IllegalArgumentException("不支持的预览模式: " + mode);
        }

        // 5. 记录审计日志
        recordPreviewAuditLog(target.auditResourceId(), mode, traceId);

        return previewResponse;
    }
    
    @Override
    public PreviewResponse generatePresignedUrl(String archiveId, int expiresInSeconds) {
        validateArchiveMainRequest(archiveId);
        return generatePresignedUrl(RESOURCE_TYPE_ARCHIVE_MAIN, archiveId, null, expiresInSeconds);
    }

    @Override
    public PreviewResponse generatePresignedUrl(String resourceType, String archiveId, String fileId, int expiresInSeconds) {
        PreviewTarget target = resolvePreviewTarget(resourceType, archiveId, fileId);

        // 2. 生成追踪ID和水印
        String traceId = watermarkGenerator.generateTraceId();
        PreviewResponse.WatermarkMetadata watermark =
            watermarkGenerator.generateWatermarkMetadata(traceId, target.fondsCode());

        // 3. 生成预签名 URL
        // TODO: 实现预签名 URL 生成
        String presignedUrl = null; // 占位符，待实现
        log.warn("预签名 URL 生成功能待实现: resourceType={}, archiveId={}, fileId={}", resourceType, archiveId, fileId);

        // 4. 构建响应
        PreviewResponse response = new PreviewResponse();
        response.setMode("presigned");
        response.setPresignedUrl(presignedUrl);
        response.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
        response.setTraceId(traceId);
        response.setWatermark(watermark);

        // 5. 记录审计日志
        recordPreviewAuditLog(target.auditResourceId(), "presigned", traceId);

        return response;
    }
    
    @Override
    public void renderWithWatermark(String archiveId, int pageNumber,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            validateArchiveMainRequest(archiveId);
            PreviewTarget target = resolvePreviewTarget(RESOURCE_TYPE_ARCHIVE_MAIN, archiveId, null);

            // 2. 获取文件路径
            String filePath = target.resolvedFile().storagePath();
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
            String watermarkSubtext = watermarkGenerator.generateWatermarkSubtext(traceId, target.fondsCode());

            // 5. 使用 PdfWatermarkRenderer 渲染指定页面并添加水印
            pdfWatermarkRenderer.renderPageWithWatermark(file, pageNumber, watermarkText,
                                                         watermarkSubtext, traceId, response);

            // 6. 记录审计日志
            recordPreviewAuditLog(target.auditResourceId(), "rendered", traceId);

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
    private void handleStreamMode(PreviewTarget target, HttpServletRequest request,
                                 HttpServletResponse response, String traceId) {
        try {
            String relativePath = target.resolvedFile().storagePath();
            log.debug("[handleStreamMode] relativePath={}", relativePath);
            if (relativePath == null) {
                log.warn("[handleStreamMode] 预览文件不存在: resourceId={}", target.auditResourceId());
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
            String watermarkSubtext = watermarkGenerator.generateWatermarkSubtext(traceId, target.fondsCode());

            // 3. 设置响应头
            response.setContentType(resolveMediaType(target.resolvedFile()).toString());
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
            log.error("流式预览失败: resourceId={}", target.auditResourceId(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private PreviewTarget resolvePreviewTarget(String resourceType, String archiveId, String fileId) {
        String normalizedMode = StringUtils.hasText(resourceType) ? resourceType : RESOURCE_TYPE_ARCHIVE_MAIN;
        return switch (normalizedMode) {
            case RESOURCE_TYPE_ARCHIVE_MAIN -> resolveArchiveMainTarget(archiveId);
            case RESOURCE_TYPE_FILE -> resolveFileTarget(fileId);
            default -> throw new IllegalArgumentException("不支持的资源类型: " + resourceType);
        };
    }

    private PreviewTarget resolveArchiveMainTarget(String archiveId) {
        validateArchiveMainRequest(archiveId);
        Archive archive = archiveService.getArchiveById(archiveId);
        ResolvedPreviewFile resolvedFile = filePathResolver.resolveArchiveMainFile(archive.getArchiveCode());
        if (resolvedFile == null) {
            throw new IllegalArgumentException("未找到档案主文件: " + archiveId);
        }
        return new PreviewTarget(RESOURCE_TYPE_ARCHIVE_MAIN, archive.getId(), archive.getFondsNo(), resolvedFile);
    }

    private PreviewTarget resolveFileTarget(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            throw new IllegalArgumentException("fileId 不能为空");
        }

        String operatorId = resolveCurrentUserId();
        ArcFileContent archiveFile = archiveFileContentService.getFileContentById(fileId, operatorId);
        if (archiveFile != null) {
            authorizeArchiveBackedFile(archiveFile);
            ResolvedPreviewFile resolvedFile = filePathResolver.resolveFileById(fileId);
            if (resolvedFile == null) {
                throw new IllegalArgumentException("未找到文件: " + fileId);
            }
            String fondsCode = StringUtils.hasText(archiveFile.getFondsCode()) ? archiveFile.getFondsCode() : resolveFondsCode(archiveFile.getArchivalCode());
            return new PreviewTarget(RESOURCE_TYPE_FILE, fileId, fondsCode, resolvedFile);
        }

        var originalVoucherFile = originalVoucherService.getFileById(fileId);
        if (originalVoucherFile == null || originalVoucherFile.getDeleted() != 0) {
            throw new IllegalArgumentException("未找到文件: " + fileId);
        }
        OriginalVoucher voucher = originalVoucherMapper.selectById(originalVoucherFile.getVoucherId());
        if (voucher == null) {
            throw new IllegalArgumentException("未找到原始凭证文件归属: " + fileId);
        }
        if (!dataScopeService.canAccessOriginalVoucher(voucher, dataScopeService.resolve())) {
            throw new AccessDeniedException("无权访问该原始凭证文件");
        }
        ResolvedPreviewFile resolvedFile = filePathResolver.resolveFileById(fileId);
        if (resolvedFile == null) {
            throw new IllegalArgumentException("未找到文件: " + fileId);
        }
        return new PreviewTarget(RESOURCE_TYPE_FILE, fileId, voucher.getFondsCode(), resolvedFile);
    }

    private void validateArchiveMainRequest(String archiveId) {
        if (!StringUtils.hasText(archiveId)) {
            throw new IllegalArgumentException("archiveId 不能为空");
        }
        if (archiveId.startsWith("FILE_") || archiveId.startsWith("OV_")) {
            throw new IllegalArgumentException("archive/preview 仅支持真实档案主文件预览");
        }
    }

    private void authorizeArchiveBackedFile(ArcFileContent file) {
        String archivalCode = file.getArchivalCode();
        if (!StringUtils.hasText(archivalCode)) {
            throw new IllegalArgumentException("文件未绑定档案资源");
        }
        Archive archive = archiveMapper.selectById(archivalCode);
        if (archive == null) {
            archive = archiveMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                    .eq(Archive::getArchiveCode, archivalCode)
                    .last("LIMIT 1")
            );
        }
        if (archive != null) {
            if (!dataScopeService.canAccessArchive(archive, dataScopeService.resolve())) {
                throw new AccessDeniedException("无权访问该档案文件");
            }
            return;
        }

        OriginalVoucher voucher = originalVoucherMapper.selectById(archivalCode);
        if (voucher == null) {
            voucher = originalVoucherMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>()
                    .eq(OriginalVoucher::getVoucherNo, archivalCode)
                    .last("LIMIT 1")
            );
        }
        if (voucher == null) {
            voucher = originalVoucherMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>()
                    .eq(OriginalVoucher::getSourceDocId, archivalCode)
                    .last("LIMIT 1")
            );
        }
        if (voucher == null) {
            throw new IllegalArgumentException("无法解析文件归属资源: " + archivalCode);
        }
        if (!dataScopeService.canAccessOriginalVoucher(voucher, dataScopeService.resolve())) {
            throw new AccessDeniedException("无权访问该原始凭证文件");
        }
    }

    private String resolveFondsCode(String archivalCode) {
        if (!StringUtils.hasText(archivalCode)) {
            return "UNKNOWN";
        }
        Archive archive = archiveMapper.selectById(archivalCode);
        if (archive == null) {
            archive = archiveMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                    .eq(Archive::getArchiveCode, archivalCode)
                    .last("LIMIT 1")
            );
        }
        if (archive != null && StringUtils.hasText(archive.getFondsNo())) {
            return archive.getFondsNo();
        }
        OriginalVoucher voucher = originalVoucherMapper.selectById(archivalCode);
        if (voucher == null) {
            voucher = originalVoucherMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>()
                    .eq(OriginalVoucher::getVoucherNo, archivalCode)
                    .last("LIMIT 1")
            );
        }
        return voucher != null && StringUtils.hasText(voucher.getFondsCode()) ? voucher.getFondsCode() : "UNKNOWN";
    }

    private MediaType resolveMediaType(ResolvedPreviewFile resolvedFile) {
        String fileType = resolvedFile.fileType();
        String fileName = resolvedFile.fileName();
        if (StringUtils.hasText(fileType)) {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> MediaType.APPLICATION_PDF;
                case "ofd", "application/ofd" -> MediaType.parseMediaType("application/ofd");
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "xml" -> MediaType.APPLICATION_XML;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
        }
        if (StringUtils.hasText(fileName)) {
            String lowerName = fileName.toLowerCase();
            if (lowerName.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
            if (lowerName.endsWith(".ofd")) return MediaType.parseMediaType("application/ofd");
            if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
            if (lowerName.endsWith(".png")) return MediaType.IMAGE_PNG;
            if (lowerName.endsWith(".xml")) return MediaType.APPLICATION_XML;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.nexusarchive.security.CustomUserDetails details) {
            return details.getId();
        }
        return authentication != null ? authentication.getName() : null;
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

    private record PreviewTarget(
        String resourceType,
        String auditResourceId,
        String fondsCode,
        ResolvedPreviewFile resolvedFile
    ) {
    }
}
