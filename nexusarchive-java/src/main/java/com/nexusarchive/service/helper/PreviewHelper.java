// Input: Spring Resource, Jakarta Servlet, AuditLogService
// Output: PreviewHelper (预览辅助类)
// Pos: Service Helper Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.preview.PreviewFilePathResolver.ResolvedPreviewFile;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.nexusarchive.common.constants.HttpConstants;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreviewHelper {

    private final AuditLogService auditLogService;

    public void transferRange(Resource res, String range, long len, HttpServletResponse resp) throws IOException {
        if (range == null || !range.startsWith("bytes=")) {
            throw new IllegalArgumentException("无效的 Range 头格式");
        }

        String[] parts;
        long start, end;

        try {
            parts = range.substring(6).split("-");
            if (parts.length == 0 || parts[0].isEmpty()) {
                throw new IllegalArgumentException("Range 起始位置不能为空");
            }
            start = Long.parseLong(parts[0]);

            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            } else {
                end = len - 1;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的 Range 格式: " + range, e);
        }
        long clen = end - start + 1;
        resp.setStatus(HttpStatus.PARTIAL_CONTENT.value());
        resp.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + len);
        resp.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(clen));
        try (InputStream is = res.getInputStream(); OutputStream os = resp.getOutputStream()) {
            is.skip(start);
            byte[] buf = new byte[8192];
            long rem = clen;
            while (rem > 0) {
                int r = is.read(buf, 0, (int) Math.min(buf.length, rem));
                if (r == -1) break;
                os.write(buf, 0, r);
                rem -= r;
            }
        }
    }

    public void transferFull(Resource res, long len, HttpServletResponse resp) throws IOException {
        resp.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(len));
        try (InputStream is = res.getInputStream(); OutputStream os = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
        }
    }

    public MediaType resolveType(ResolvedPreviewFile f) {
        String type = f.fileType();
        String name = f.fileName();
        if (StringUtils.hasText(type)) {
            return switch (type.toLowerCase()) {
                case "pdf" -> MediaType.APPLICATION_PDF;
                case "ofd" -> MediaType.parseMediaType(HttpConstants.APPLICATION_OFD);
                case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
                case "png" -> MediaType.IMAGE_PNG;
                case "xml" -> MediaType.APPLICATION_XML;
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
        }
        if (name != null) {
            String n = name.toLowerCase();
            if (n.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
            if (n.endsWith(".ofd")) return MediaType.parseMediaType(HttpConstants.APPLICATION_OFD);
            if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
            if (n.endsWith(".png")) return MediaType.IMAGE_PNG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    public void logPreview(String rid, String mode, String tid) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String uid = auth != null ? auth.getName() : "Unknown";
        SysAuditLog log = new SysAuditLog();
        log.setUserId(uid); log.setUsername(uid); log.setAction("ARCHIVE_PREVIEW");
        log.setResourceType("ARCHIVE"); log.setResourceId(rid); log.setOperationResult(OperationResult.SUCCESS);
        log.setDetails("Mode: " + mode + ", Trace: " + tid); log.setTraceId(tid);
        auditLogService.log(log);
    }
}
