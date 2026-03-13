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
    private final com.nexusarchive.service.helper.PreviewHelper helper;
    
    @Override
    public PreviewResponse streamPreview(String archiveId, String mode, HttpServletRequest req, HttpServletResponse resp) {
        validateArchiveMainRequest(archiveId);
        return streamPreview(RESOURCE_TYPE_ARCHIVE_MAIN, archiveId, null, mode, req, resp);
    }

    @Override
    public PreviewResponse streamPreview(String type, String aid, String fid, String mode, HttpServletRequest req, HttpServletResponse resp) {
        PreviewTarget target = resolvePreviewTarget(type, aid, fid);
        String tid = watermarkGenerator.generateTraceId();
        PreviewResponse previewResponse = new PreviewResponse();
        previewResponse.setMode(mode); previewResponse.setTraceId(tid);
        previewResponse.setWatermark(watermarkGenerator.generateWatermarkMetadata(tid, target.fondsCode()));

        if ("stream".equals(mode)) handleStreamMode(target, req, resp, tid);
        else if ("rendered".equals(mode)) handleStreamMode(target, req, resp, tid);

        helper.logPreview(target.auditResourceId(), mode, tid);
        return previewResponse;
    }
    
    @Override
    public PreviewResponse generatePresignedUrl(String aid, int exp) {
        validateArchiveMainRequest(aid);
        return generatePresignedUrl(RESOURCE_TYPE_ARCHIVE_MAIN, aid, null, exp);
    }

    @Override
    public PreviewResponse generatePresignedUrl(String type, String aid, String fid, int exp) {
        PreviewTarget target = resolvePreviewTarget(type, aid, fid);
        String tid = watermarkGenerator.generateTraceId();
        PreviewResponse response = new PreviewResponse();
        response.setMode("presigned"); response.setTraceId(tid);
        response.setWatermark(watermarkGenerator.generateWatermarkMetadata(tid, target.fondsCode()));
        helper.logPreview(target.auditResourceId(), "presigned", tid);
        return response;
    }
    
    @Override
    public void renderWithWatermark(String aid, int page, HttpServletRequest req, HttpServletResponse resp) {
        try {
            validateArchiveMainRequest(aid);
            PreviewTarget target = resolvePreviewTarget(RESOURCE_TYPE_ARCHIVE_MAIN, aid, null);
            java.io.File file = fileStorageService.getFile(target.resolvedFile().storagePath());
            if (file == null || !file.exists()) { resp.setStatus(404); return; }
            String tid = watermarkGenerator.generateTraceId();
            pdfWatermarkRenderer.renderPageWithWatermark(file, page, watermarkGenerator.generateWatermarkText(tid), watermarkGenerator.generateWatermarkSubtext(tid, target.fondsCode()), tid, resp);
            helper.logPreview(target.auditResourceId(), "rendered", tid);
        } catch (Exception e) { resp.setStatus(500); }
    }

    private void handleStreamMode(PreviewTarget target, HttpServletRequest req, HttpServletResponse resp, String tid) {
        try {
            java.io.File file = fileStorageService.getFile(target.resolvedFile().storagePath());
            if (file == null || !file.exists()) { resp.setStatus(404); return; }
            org.springframework.core.io.Resource res = new org.springframework.core.io.FileSystemResource(file);
            resp.setContentType(helper.resolveType(target.resolvedFile()).toString());
            resp.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            resp.setHeader("X-Trace-Id", tid);
            resp.setHeader("X-Watermark-Text", watermarkGenerator.generateWatermarkText(tid));
            
            long len = res.contentLength();
            String range = req.getHeader(HttpHeaders.RANGE);
            if (range != null && range.startsWith("bytes=")) helper.transferRange(res, range, len, resp);
            else helper.transferFull(res, len, resp);
        } catch (IOException e) { resp.setStatus(500); }
    }

    private PreviewTarget resolvePreviewTarget(String type, String aid, String fid) {
        if (RESOURCE_TYPE_ARCHIVE_MAIN.equals(type) || !StringUtils.hasText(type)) return resolveArchiveMainTarget(aid);
        if (RESOURCE_TYPE_FILE.equals(type)) return resolveFileTarget(fid);
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private PreviewTarget resolveArchiveMainTarget(String aid) {
        validateArchiveMainRequest(aid);
        Archive a = archiveService.getArchiveById(aid);
        ResolvedPreviewFile f = filePathResolver.resolveArchiveMainFile(a.getArchiveCode());
        if (f == null) throw new IllegalArgumentException("File not found: " + aid);
        return new PreviewTarget(RESOURCE_TYPE_ARCHIVE_MAIN, a.getId(), a.getFondsNo(), f);
    }

    private PreviewTarget resolveFileTarget(String fid) {
        if (!StringUtils.hasText(fid)) throw new IllegalArgumentException("fid is empty");
        ArcFileContent af = archiveFileContentService.getFileContentById(fid, resolveCurrentUserId());
        if (af != null) {
            authorizeArchiveBackedFile(af);
            ResolvedPreviewFile rf = filePathResolver.resolveFileById(fid);
            if (rf == null) throw new IllegalArgumentException("File not found");
            return new PreviewTarget(RESOURCE_TYPE_FILE, fid, StringUtils.hasText(af.getFondsCode()) ? af.getFondsCode() : resolveFondsCode(af.getArchivalCode()), rf);
        }
        var ovf = originalVoucherService.getFileById(fid);
        if (ovf == null || ovf.getDeleted() != 0) throw new IllegalArgumentException("File not found");
        OriginalVoucher v = originalVoucherMapper.selectById(ovf.getVoucherId());
        if (v == null || !dataScopeService.canAccessOriginalVoucher(v, dataScopeService.resolve())) throw new AccessDeniedException("Access denied");
        ResolvedPreviewFile rf = filePathResolver.resolveFileById(fid);
        return new PreviewTarget(RESOURCE_TYPE_FILE, fid, v.getFondsCode(), rf);
    }

    private void validateArchiveMainRequest(String aid) {
        if (!StringUtils.hasText(aid) || aid.startsWith("FILE_") || aid.startsWith("OV_")) throw new IllegalArgumentException("Invalid aid");
    }

    private void authorizeArchiveBackedFile(ArcFileContent file) {
        String code = file.getArchivalCode();
        if (!StringUtils.hasText(code)) throw new IllegalArgumentException("No code");
        Archive a = archiveMapper.selectById(code);
        if (a == null) a = archiveMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>().eq(Archive::getArchiveCode, code).last("LIMIT 1"));
        if (a != null) {
            if (!dataScopeService.canAccessArchive(a, dataScopeService.resolve())) throw new AccessDeniedException("Access denied");
            return;
        }
        OriginalVoucher v = originalVoucherMapper.selectById(code);
        if (v == null) v = originalVoucherMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>().eq(OriginalVoucher::getVoucherNo, code).last("LIMIT 1"));
        if (v == null) v = originalVoucherMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>().eq(OriginalVoucher::getSourceDocId, code).last("LIMIT 1"));
        if (v == null) throw new IllegalArgumentException("Not found: " + code);
        if (!dataScopeService.canAccessOriginalVoucher(v, dataScopeService.resolve())) throw new AccessDeniedException("Access denied");
    }

    private String resolveFondsCode(String code) {
        if (!StringUtils.hasText(code)) return "UNKNOWN";
        Archive a = archiveMapper.selectById(code);
        if (a == null) a = archiveMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>().eq(Archive::getArchiveCode, code).last("LIMIT 1"));
        if (a != null && StringUtils.hasText(a.getFondsNo())) return a.getFondsNo();
        OriginalVoucher v = originalVoucherMapper.selectById(code);
        if (v == null) v = originalVoucherMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OriginalVoucher>().eq(OriginalVoucher::getVoucherNo, code).last("LIMIT 1"));
        return v != null && StringUtils.hasText(v.getFondsCode()) ? v.getFondsCode() : "UNKNOWN";
    }

    private String resolveCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.nexusarchive.security.CustomUserDetails details) return details.getId();
        return auth != null ? auth.getName() : null;
    }

    private record PreviewTarget(String resourceType, String auditResourceId, String fondsCode, ResolvedPreviewFile resolvedFile) {}
}
