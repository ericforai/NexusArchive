// Input: MyBatis-Plus、io.swagger、Lombok、Spring Framework、等
// Output: ArchiveFileController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.dto.VoucherDataDto;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
 * 档案文件内容控制器
 *
 * PRD 来源: 档案文件管理模块
 * 提供档案文件内容的获取和下载功能
 *
 * <p>支持 PDF/OFD 等多种格式</p>
 */
@Tag(name = "档案文件内容", description = """
    档案文件内容获取和下载接口。

    **功能说明:**
    - 获取档案文件内容
    - 通过文件ID下载文件
    - 获取档案关联的凭证分录数据

    **支持的文件格式:**
    - PDF: 便携式文档格式 (application/pdf)
    - OFD: 版式文档 (application/ofd)
    - XML: 元数据文件 (application/xml)
    - JPG/PNG: 图片 (image/jpeg, image/png)

    **内容类型判定:**
    - 优先使用 fileType 字段
    - 其次使用 fileName 后缀
    - 默认 application/octet-stream

    **数据关联方式:**
    1. 通过 item_id 关联查询（acc_archive -> arc_file_content）
    2. 直接通过 id 查询（arc_file_content 自身记录）

    **使用场景:**
    - 档案文件在线预览
    - 档案文件下载
    - 全景视图附件预览
    - 凭证分录数据展示

    **权限要求:**
    - archive:read 权限
    - 自动验证档案访问权限（DataScopeService）
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Slf4j
public class ArchiveFileController {

    private final ArchiveFileContentService archiveFileContentService;
    private final ArchiveMapper archiveMapper;
    private final FileStorageService fileStorageService;
    private final DataScopeService dataScopeService;

    /**
     * 获取档案文件内容
     */
    @GetMapping("/{id}/content")
    @Operation(
        summary = "获取档案文件内容",
        description = """
            获取档案关联的文件内容，支持在线预览。

            **路径参数:**
            - id: 档案ID或文件ID

            **返回数据:**
            - 文件流（PDF/OFD/图片等）
            - Content-Type: 根据文件类型自动设置
            - Content-Disposition: inline（在线预览）
            - Content-Length: 文件大小

            **文件名编码:**
            - 使用 RFC 5987 标准编码
            - 支持中文文件名

            **业务规则:**
            - 验证用户对档案的访问权限
            - 检查物理文件是否存在
            - 自动管理资源生命周期

            **使用场景:**
            - 档案在线预览
            - 文件内容获取
            """,
        operationId = "getArchiveFileContent",
        tags = {"档案文件内容"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> getFileContent(
            @Parameter(description = "档案ID或文件ID", required = true, example = "arc-001")
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);

        // 1. Query file content record by item_id
        ArcFileContent content = archiveFileContentService.getFileContentByItemId(id, operatorId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND_FOR_ARCHIVE, id);
        }
        authorizeArchiveAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());

        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException(ErrorCode.PHYSICAL_FILE_NOT_FOUND, content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());

        String contentType = determineContentType(content.getFileType(), content.getFileName());

        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(content.getFileName(), "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedFileName = content.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.getFileSize())  // [ADDED] 设置 Content-Length
                .body(resource);
    }

    /**
     * 通过文件ID下载文件
     */
    @GetMapping("/files/download/{fileId}")
    @Operation(
        summary = "通过文件ID下载文件",
        description = """
            直接通过文件ID下载文件，用于全景视图附件预览。

            **路径参数:**
            - fileId: 文件ID

            **返回数据:**
            - 文件流
            - Content-Type: 根据文件类型自动设置
            - Content-Disposition: inline（在线预览）

            **特点:**
            - 不需要额外权限校验（已登录即可）
            - 直接通过文件ID查询，更高效

            **使用场景:**
            - 全景视图附件预览
            - 快速文件获取
            """,
        operationId = "downloadFileById",
        tags = {"档案文件内容"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<Resource> downloadByFileId(
            @Parameter(description = "文件ID", required = true, example = "file-001")
            @PathVariable String fileId,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);

        // 直接通过文件 ID 查询
        ArcFileContent content = archiveFileContentService.getFileContentById(fileId, operatorId);

        if (content == null || content.getStoragePath() == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, fileId);
        }
        authorizeArchiveAccess(content.getArchivalCode());

        Path filePath = fileStorageService.resolvePath(content.getStoragePath());

        if (!fileStorageService.exists(content.getStoragePath())) {
            throw new BusinessException(ErrorCode.PHYSICAL_FILE_NOT_FOUND, content.getStoragePath());
        }

        // [FIXED P0-3] 使用 FileSystemResource，自动管理资源生命周期
        Resource resource = new org.springframework.core.io.FileSystemResource(filePath.toFile());

        String contentType = determineContentType(content.getFileType(), content.getFileName());

        // [FIXED] 使用 RFC 5987 编码处理中文文件名
        String encodedFileName;
        try {
            encodedFileName = java.net.URLEncoder.encode(content.getFileName(), "UTF-8").replace("+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encodedFileName = content.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.getFileSize())  // [ADDED] 设置 Content-Length
                .body(resource);
    }

    /**
     * 获取档案关联的凭证分录数据
     */
    @GetMapping("/{id}/voucher-data")
    @Operation(
        summary = "获取档案关联的凭证分录数据",
        description = """
            获取档案关联的凭证分录数据，用于凭证预览标签页。

            **路径参数:**
            - id: 档案ID或文件ID

            **返回数据包括:**
            - fileId: 文件ID
            - sourceData: 凭证分录数据（JSON）
            - voucherWord: 凭证字
            - summary: 摘要
            - docDate: 日期
            - creator: 创建人

            **数据关联方式:**
            1. 通过 item_id 关联查询（acc_archive -> arc_file_content）
            2. 直接通过 id 查询（arc_file_content 自身记录）

            **业务规则:**
            - 先尝试通过 item_id 查询
            - 未找到则直接查询文件表
            - 未找到数据返回 null

            **使用场景:**
            - 凭证预览标签页
            - 全景视图凭证展示
            """,
        operationId = "getVoucherData",
        tags = {"档案文件内容"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAuthority('archive:read')")
    public ResponseEntity<com.nexusarchive.common.result.Result<com.nexusarchive.dto.VoucherDataDto>> getVoucherData(
            @Parameter(description = "档案ID或文件ID", required = true, example = "arc-001")
            @PathVariable String id,
            jakarta.servlet.http.HttpServletRequest request) {
        String operatorId = resolveUserId(request);
        log.info("获取凭证分录数据: id={}", id);

        ArcFileContent content = null;

        // 1. 先尝试通过 item_id 关联查询（acc_archive -> arc_file_content）
        content = archiveFileContentService.getFileContentByItemId(id, operatorId);

        // 2. 如果未找到，尝试直接通过 id 查询 arc_file_content 自身
        if (content == null) {
            log.info("通过 item_id 未找到，尝试直接查询 arc_file_content: id={}", id);
            content = archiveFileContentService.getFileContentById(id, operatorId);
        }

        if (content == null) {
            log.warn("未找到凭证分录数据: id={}", id);
            return ResponseEntity.ok(com.nexusarchive.common.result.Result.success(null));
        }

        com.nexusarchive.dto.VoucherDataDto dto = new com.nexusarchive.dto.VoucherDataDto();
        dto.setFileId(content.getId());
        dto.setSourceData(content.getSourceData());
        dto.setVoucherWord(content.getVoucherWord());
        dto.setSummary(content.getSummary());
        dto.setDocDate(content.getDocDate() != null ? content.getDocDate().toString() : null);
        dto.setCreator(content.getCreator());

        log.info("返回凭证分录数据: fileId={}, hasSourceData={}",
                 content.getId(), content.getSourceData() != null);
        return ResponseEntity.ok(com.nexusarchive.common.result.Result.success(dto));
    }

    private String determineContentType(String fileType, String fileName) {
        if (fileType != null) {
            switch (fileType.toLowerCase()) {
                case "ofd":
                    return "application/ofd";
                case "pdf":
                    return "application/pdf";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "xml":
                    return "application/xml";
            }
        }

        if (fileName != null) {
            if (fileName.endsWith(".ofd")) return "application/ofd";
            if (fileName.endsWith(".pdf")) return "application/pdf";
        }

        return "application/octet-stream";
    }

    private void authorizeArchiveAccess(String archivalCode) {
        if (archivalCode == null || archivalCode.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_BOUND_TO_ARCHIVE);
        }

        // 兼容两种数据格式：
        // 1. archival_code 存储的是 acc_archive.id (如 UUID格式)
        // 2. archival_code 存储的是 acc_archive.archive_code (如 BR-GROUP-2024-30Y-FIN-AC01-0002)
        Archive archive = archiveMapper.selectById(archivalCode);
        if (archive == null) {
            // 尝试按 archive_code 查找
            archive = archiveMapper.selectOne(
                    new LambdaQueryWrapper<Archive>()
                            .eq(Archive::getArchiveCode, archivalCode)
                            .last("LIMIT 1")
            );
        }

        if (archive == null) {
            throw new BusinessException(ErrorCode.ARCHIVE_NOT_FOUND, archivalCode);
        }

        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        if (!dataScopeService.canAccessArchive(archive, scope)) {
            throw new BusinessException(ErrorCode.ARCHIVE_ACCESS_DENIED);
        }
    }

    private String resolveUserId(jakarta.servlet.http.HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.nexusarchive.security.CustomUserDetails details) {
            return details.getId();
        }
        return null;
    }
}
