// Input: io.swagger、Lombok、Spring Security、Spring Framework、DtoMapper、ArchiveAttachmentResponse
// Output: AttachmentController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.ArchiveAttachmentResponse;
import com.nexusarchive.dto.response.ArchiveFileResponse;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import com.nexusarchive.annotation.ArchivalAudit;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 档案附件关联控制器
 *
 * PRD 来源: 附件管理模块
 * 支持全景视图中凭证与附件的关联管理
 *
 * <p>所有返回值使用 DTO，避免直接暴露 Entity</p>
 */
@Tag(name = "附件管理", description = """
    档案附件上传与关联管理接口。

    **功能说明:**
    - 获取档案关联的所有附件
    - 获取档案附件关联记录
    - 关联已有文件到档案
    - 上传附件并关联到档案
    - 删除附件关联

    **附件类型:**
    - voucher: 凭证附件
    - invoice: 发票附件
    - contract: 合同附件
    - other: 其他附件（默认）

    **关联方式:**
    1. 关联已有文件：将已上传的文件关联到档案
    2. 上传并关联：上传新文件并自动关联

    **使用场景:**
    - 全景视图凭证附件关联
    - 档案补充附件上传
    - 附件关系管理

    **权限要求:**
    - archive:read: 查看权限
    - archive:manage: 管理权限
    - nav:all: 全部导航权限
    - SYSTEM_ADMIN: 系统管理员

    **业务规则:**
    - 关联操作会被审计记录
    - 删除关联不会删除物理文件
    - 一个文件可以关联到多个档案
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final DtoMapper dtoMapper;

    /**
     * 获取档案关联的所有附件
     */
    @GetMapping("/by-archive/{archiveId}")
    @Operation(
        summary = "获取档案附件",
        description = """
            获取指定档案关联的所有附件文件列表。

            **路径参数:**
            - archiveId: 档案ID

            **返回数据包括:**
            - fileId: 文件ID
            - fileName: 文件名
            - fileType: 文件类型
            - fileSize: 文件大小
            - storagePath: 存储路径
            - attachmentType: 附件类型

            **使用场景:**
            - 档案附件列表展示
            - 全景视图附件预览
            """,
        operationId = "getArchiveAttachments",
        tags = {"附件管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveFileResponse>> getAttachmentsByArchive(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String archiveId) {
        log.info("获取档案附件: archiveId={}", archiveId);
        List<ArcFileContent> files = attachmentService.getAttachmentsByArchive(archiveId);
        return Result.success(dtoMapper.toArchiveFileResponseList(files));
    }

    /**
     * 获取档案附件关联记录
     */
    @GetMapping("/links/{archiveId}")
    @Operation(
        summary = "获取关联记录",
        description = """
            获取档案的附件关联记录详情。

            **路径参数:**
            - archiveId: 档案ID

            **返回数据包括:**
            - id: 关联记录ID
            - archiveId: 档案ID
            - fileId: 文件ID
            - attachmentType: 附件类型
            - createdAt: 关联时间
            - createdBy: 关联人

            **使用场景:**
            - 查看附件关联历史
            - 关联关系追溯
            """,
        operationId = "getAttachmentLinks",
        tags = {"附件管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveAttachmentResponse>> getAttachmentLinks(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String archiveId) {
        List<ArchiveAttachment> links = attachmentService.getAttachmentLinks(archiveId);
        return Result.success(dtoMapper.toArchiveAttachmentResponseList(links));
    }

    /**
     * 关联已有文件到档案
     */
    @PostMapping("/link")
    @Operation(
        summary = "关联附件",
        description = """
            将已上传的文件关联到档案。

            **请求体:**
            - archiveId: 档案ID（必填）
            - fileId: 文件ID（必填）
            - attachmentType: 附件类型（可选，默认 other）

            **返回数据:**
            - 创建的关联记录详情

            **附件类型:**
            - voucher: 凭证附件
            - invoice: 发票附件
            - contract: 合同附件
            - other: 其他附件

            **业务规则:**
            - 文件必须已存在于系统中
            - 一个文件可以关联到多个档案
            - 关联操作会被审计记录

            **使用场景:**
            - 关联已有文件到档案
            """,
        operationId = "linkAttachment",
        tags = {"附件管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "关联成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或文件不存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案或文件不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "LINK_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "关联附件到档案")
    public Result<ArchiveAttachmentResponse> linkAttachment(
            @Parameter(description = "关联请求", required = true)
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails user) {
        String archiveId = request.get("archiveId");
        String fileId = request.get("fileId");
        String attachmentType = request.getOrDefault("attachmentType", "other");

        String userId = user != null ? user.getId() : "system";
        ArchiveAttachment attachment = attachmentService.linkAttachment(archiveId, fileId, attachmentType, userId);
        return Result.success("关联成功", dtoMapper.toArchiveAttachmentResponse(attachment));
    }

    /**
     * 上传附件并关联到档案
     */
    @PostMapping("/upload")
    @Operation(
        summary = "上传附件",
        description = """
            上传文件并自动关联到指定档案。

            **请求参数 (multipart/form-data):**
            - file: 文件内容（必填）
            - archiveId: 档案ID（必填）
            - attachmentType: 附件类型（可选，默认 other）

            **返回数据:**
            - 上传的文件详情

            **支持的文件类型:**
            - PDF: 便携式文档格式
            - OFD: 版式文档
            - 图片: JPG、PNG
            - Office 文档: DOC、DOCX、XLS、XLSX

            **文件大小限制:**
            - 默认最大 50MB
            - 可通过配置调整

            **业务规则:**
            - 自动计算文件哈希值
            - 自动记录上传人信息
            - 上传操作会被审计记录

            **使用场景:**
            - 上传档案附件
            - 补充凭证附件
            """,
        operationId = "uploadAttachment",
        tags = {"附件管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或文件过大"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "UPLOAD_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "上传并关联附件")
    public Result<ArchiveFileResponse> uploadAndLink(
            @Parameter(description = "文件内容", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @RequestParam("archiveId") String archiveId,
            @Parameter(description = "附件类型", example = "voucher")
            @RequestParam(value = "attachmentType", defaultValue = "other") String attachmentType,
            @AuthenticationPrincipal CustomUserDetails user) {
        log.info("上传附件: archiveId={}, fileName={}, type={}", archiveId, file.getOriginalFilename(), attachmentType);

        String userId = user != null ? user.getId() : "system";
        ArcFileContent fileContent = attachmentService.uploadAndLink(archiveId, file, attachmentType, userId);
        return Result.success("上传成功", dtoMapper.toArchiveFileResponse(fileContent));
    }

    /**
     * 删除附件关联
     */
    @DeleteMapping("/{attachmentId}")
    @Operation(
        summary = "删除关联",
        description = """
            删除附件与档案的关联关系。

            **路径参数:**
            - attachmentId: 关联记录ID

            **业务规则:**
            - 仅删除关联关系
            - 不删除物理文件
            - 如果文件仅被此档案关联，文件会被保留
            - 删除操作会被审计记录

            **使用场景:**
            - 移除错误关联
            - 清理不需要的附件
            """,
        operationId = "unlinkAttachment",
        tags = {"附件管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "关联记录不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "UNLINK_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "删除附件关联")
    public Result<Void> unlinkAttachment(
            @Parameter(description = "关联记录ID", required = true, example = "att-001")
            @PathVariable String attachmentId) {
        attachmentService.unlinkAttachment(attachmentId);
        return Result.success("删除成功", null);
    }
}
