// Input: Spring Web、StreamingPreviewService
// Output: ArchivePreviewController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.service.StreamingPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

/**
 * 档案预览控制器
 * 
 * PRD 来源: Section 2.2 - 流式预览与动态水印
 */
@Slf4j
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Tag(name = "档案预览", description = "流式预览、预签名URL、服务端渲染接口")
public class ArchivePreviewController {
    
    private final StreamingPreviewService streamingPreviewService;
    
    @PostMapping("/preview")
    @Operation(summary = "流式预览")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public void preview(
            @RequestParam String archiveId,
            @RequestParam(defaultValue = "stream") String mode,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            streamingPreviewService.streamPreview(archiveId, mode, request, response);
        } catch (IllegalArgumentException e) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("预览失败: archiveId={}, mode={}", archiveId, mode, e);
            writeErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "预览失败");
        }
    }
    
    @PostMapping("/preview/presigned")
    @Operation(summary = "生成预签名URL")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public Result<PreviewResponse> generatePresignedUrl(
            @RequestParam String archiveId,
            @RequestParam(defaultValue = "3600") int expiresInSeconds) {

        try {
            PreviewResponse previewResponse = streamingPreviewService.generatePresignedUrl(
                archiveId, expiresInSeconds);
            return Result.success(previewResponse);
        } catch (IllegalArgumentException e) {
            return Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        } catch (AccessDeniedException e) {
            return Result.fail(HttpStatus.FORBIDDEN.value(), e.getMessage());
        } catch (Exception e) {
            log.error("生成预签名URL失败: archiveId={}", archiveId, e);
            return Result.fail("生成预签名URL失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/preview/render")
    @Operation(summary = "服务端渲染带水印内容（高敏模式）")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public void renderWithWatermark(
            @RequestParam String archiveId,
            @RequestParam int pageNumber,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            streamingPreviewService.renderWithWatermark(archiveId, pageNumber, request, response);
        } catch (IllegalArgumentException e) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (AccessDeniedException e) {
            writeErrorResponse(response, HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            log.error("服务端渲染失败: archiveId={}, pageNumber={}", archiveId, pageNumber, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, HttpStatus status, String message) {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        try {
            response.getWriter().write(String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), escapeJson(message)));
        } catch (Exception writeException) {
            log.warn("预览失败响应写入异常", writeException);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

