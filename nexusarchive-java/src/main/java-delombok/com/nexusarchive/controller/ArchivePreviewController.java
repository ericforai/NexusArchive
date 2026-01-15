// Input: Spring Web、StreamingPreviewService
// Output: ArchivePreviewController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.PreviewResponse;
import com.nexusarchive.service.StreamingPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 档案预览控制器
 *
 * PRD 来源: Section 2.2 - 流式预览与动态水印
 * 提供档案文件的流式预览功能
 *
 * <p>支持在线预览、预签名URL、服务端渲染带水印内容</p>
 */
@Tag(name = "档案预览", description = """
    档案流式预览接口。

    **功能说明:**
    - 流式预览档案内容
    - 生成预签名URL（临时访问）
    - 服务端渲染带水印内容（高敏模式）

    **预览模式:**
    - stream: 流式传输（默认）
    - render: 服务端渲染
    - redirect: 重定向到存储URL

    **水印支持:**
    - 用户ID水印
    - 时间戳水印
    - 访问追踪码

    **预签名URL:**
    - 临时访问令牌
    - 可设置过期时间
    - 支持私有文件访问

    **使用场景:**
    - 档案在线预览
    - 临时分享
    - 高敏档案安全预览

    **权限要求:**
    - archive:view 权限
    - archive:preview 权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class ArchivePreviewController {

    private final StreamingPreviewService streamingPreviewService;

    /**
     * 流式预览
     */
    @PostMapping("/preview")
    @Operation(
        summary = "流式预览",
        description = """
            流式传输档案内容用于在线预览。

            **请求参数:**
            - archiveId: 档案ID
            - mode: 预览模式（stream/render/redirect）

            **返回数据:**
            - stream 模式: 直接流式传输文件
            - render 模式: 渲染后的内容
            - redirect 模式: 重定向到存储URL

            **业务规则:**
            - 支持断点续传
            - 自动添加访问追踪
            - 高敏模式自动加水印

            **使用场景:**
            - 档案在线预览
            - PDF/OFD 查看
            """,
        operationId = "streamPreview",
        tags = {"档案预览"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "预览内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "500", description = "预览失败")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public void preview(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @RequestParam String archiveId,
            @Parameter(description = "预览模式", example = "stream")
            @RequestParam(defaultValue = "stream") String mode,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            streamingPreviewService.streamPreview(archiveId, mode, request, response);
        } catch (Exception e) {
            log.error("预览失败: archiveId={}, mode={}", archiveId, mode, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"预览失败\"}");
            } catch (Exception writeException) {
                log.warn("预览失败响应写入异常", writeException);
            }
        }
    }

    /**
     * 生成预签名URL
     */
    @PostMapping("/preview/presigned")
    @Operation(
        summary = "生成预签名URL",
        description = """
            生成临时访问URL，用于直接访问档案文件。

            **请求参数:**
            - archiveId: 档案ID
            - expiresInSeconds: 过期时间（秒，默认 3600）

            **返回数据包括:**
            - url: 预签名URL
            - expiresAt: 过期时间
            - fileType: 文件类型

            **业务规则:**
            - URL 包含临时访问令牌
            - 过期后自动失效
            - 支持私有文件访问

            **使用场景:**
            - 临时分享
            - 前端直接访问
            - 第三方集成
            """,
        operationId = "generatePresignedUrl",
        tags = {"档案预览"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public Result<PreviewResponse> generatePresignedUrl(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @RequestParam String archiveId,
            @Parameter(description = "过期时间（秒）", example = "3600")
            @RequestParam(defaultValue = "3600") int expiresInSeconds) {

        try {
            PreviewResponse previewResponse = streamingPreviewService.generatePresignedUrl(
                archiveId, expiresInSeconds);
            return Result.success(previewResponse);
        } catch (Exception e) {
            log.error("生成预签名URL失败: archiveId={}", archiveId, e);
            return Result.fail("生成预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 服务端渲染带水印内容
     */
    @GetMapping("/preview/render")
    @Operation(
        summary = "服务端渲染带水印内容",
        description = """
            服务端渲染档案内容并添加水印（高敏模式）。

            **请求参数:**
            - archiveId: 档案ID
            - pageNumber: 页码

            **返回数据:**
            - 渲染后的图片/PDF
            - 包含用户水印
            - 包含时间戳水印

            **水印内容:**
            - 用户ID/姓名
            - 访问时间
            - 追踪码

            **业务规则:**
            - 仅高敏档案使用
            - 水印不可去除
            - 记录访问日志

            **使用场景:**
            - 高敏档案预览
            - 防泄密追踪
            """,
        operationId = "renderWithWatermark",
        tags = {"档案预览"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "渲染内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "渲染失败")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:preview')")
    public void renderWithWatermark(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @RequestParam String archiveId,
            @Parameter(description = "页码", example = "1")
            @RequestParam int pageNumber,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            streamingPreviewService.renderWithWatermark(archiveId, pageNumber, request, response);
        } catch (Exception e) {
            log.error("服务端渲染失败: archiveId={}, pageNumber={}", archiveId, pageNumber, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
