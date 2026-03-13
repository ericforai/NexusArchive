// Input: Spring Web、StreamingPreviewService
// Output: PreviewController 类
// Pos: Web 控制器层

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.service.StreamingPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一预览控制器。
 */
@Slf4j
@RestController
@RequestMapping("/preview")
@RequiredArgsConstructor
@Tag(name = "统一预览", description = "按资源类型统一处理档案主文件和附件文件预览")
public class PreviewController {

    private final StreamingPreviewService streamingPreviewService;

    @GetMapping
    @Operation(summary = "统一流式预览（浏览器直连）")
    @PreAuthorize("hasAnyAuthority('archive:read', 'archive:view', 'archive:preview', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public void previewByGet(
        @RequestParam String resourceType,
        @RequestParam(required = false) String archiveId,
        @RequestParam(required = false) String fileId,
        @RequestParam(defaultValue = "stream") String mode,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        handlePreview(resourceType, archiveId, fileId, mode, request, response);
    }

    @PostMapping
    @Operation(summary = "统一流式预览")
    @PreAuthorize("hasAnyAuthority('archive:read', 'archive:view', 'archive:preview', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public void preview(
        @RequestParam String resourceType,
        @RequestParam(required = false) String archiveId,
        @RequestParam(required = false) String fileId,
        @RequestParam(defaultValue = "stream") String mode,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        handlePreview(resourceType, archiveId, fileId, mode, request, response);
    }

    private void handlePreview(
        String resourceType,
        String archiveId,
        String fileId,
        String mode,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        try {
            streamingPreviewService.streamPreview(resourceType, archiveId, fileId, mode, request, response);
        } catch (IllegalArgumentException e) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("统一预览失败: resourceType={}, archiveId={}, fileId={}, mode={}", resourceType, archiveId, fileId, mode, e);
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "预览失败");
        }
    }

    @PostMapping("/presigned")
    @Operation(summary = "统一预签名预览")
    @PreAuthorize("hasAnyAuthority('archive:read', 'archive:view', 'archive:preview', 'archive:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<PreviewResponse> generatePresignedUrl(
        @RequestParam String resourceType,
        @RequestParam(required = false) String archiveId,
        @RequestParam(required = false) String fileId,
        @RequestParam(defaultValue = "3600") int expiresInSeconds
    ) {
        try {
            return Result.success(streamingPreviewService.generatePresignedUrl(resourceType, archiveId, fileId, expiresInSeconds));
        } catch (IllegalArgumentException e) {
            return Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        } catch (AccessDeniedException e) {
            return Result.fail(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            log.error("统一预签名预览失败: resourceType={}, archiveId={}, fileId={}", resourceType, archiveId, fileId, e);
            return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "预览失败");
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), escapeJson(message)));
        } catch (Exception writeException) {
            log.warn("统一预览错误响应写入异常", writeException);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
