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
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 支持全景视图中凭证与附件的关联管理
 * 所有返回值使用 DTO，避免直接暴露 Entity
 */
@Slf4j
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "附件管理", description = "档案附件上传与关联管理")
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final DtoMapper dtoMapper;

    /**
     * 获取档案关联的所有附件
     */
    @GetMapping("/by-archive/{archiveId}")
    @Operation(summary = "获取档案附件", description = "获取指定档案关联的所有附件文件列表")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveFileResponse>> getAttachmentsByArchive(@PathVariable String archiveId) {
        log.info("获取档案附件: archiveId={}", archiveId);
        List<ArcFileContent> files = attachmentService.getAttachmentsByArchive(archiveId);
        return Result.success(dtoMapper.toArchiveFileResponseList(files));
    }

    /**
     * 获取档案附件关联记录
     */
    @GetMapping("/links/{archiveId}")
    @Operation(summary = "获取关联记录", description = "获取档案的附件关联记录详情")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveAttachmentResponse>> getAttachmentLinks(@PathVariable String archiveId) {
        List<ArchiveAttachment> links = attachmentService.getAttachmentLinks(archiveId);
        return Result.success(dtoMapper.toArchiveAttachmentResponseList(links));
    }

    /**
     * 关联已有文件到档案
     */
    @PostMapping("/link")
    @Operation(summary = "关联附件", description = "将已上传的文件关联到档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "LINK_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "关联附件到档案")
    public Result<ArchiveAttachmentResponse> linkAttachment(
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
    @Operation(summary = "上传附件", description = "上传文件并自动关联到指定档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "UPLOAD_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "上传并关联附件")
    public Result<ArchiveFileResponse> uploadAndLink(
            @RequestParam("file") MultipartFile file,
            @RequestParam("archiveId") String archiveId,
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
    @Operation(summary = "删除关联", description = "删除附件与档案的关联关系")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "UNLINK_ATTACHMENT", resourceType = "ARCHIVE_ATTACHMENT", description = "删除附件关联")
    public Result<Void> unlinkAttachment(@PathVariable String attachmentId) {
        attachmentService.unlinkAttachment(attachmentId);
        return Result.success("删除成功", null);
    }
}
