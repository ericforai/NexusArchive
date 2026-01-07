// Input: Spring Web, Spring Security, Lombok, Jakarta Validation
// Output: ScanWorkspaceController
// Pos: Controller Layer

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.service.ScanWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 扫描工作区控制器
 *
 * 提供扫描工作区的REST API:
 * - 查询工作区文件列表
 * - 上传文件到工作区
 * - 触发OCR识别
 * - 更新OCR识别结果
 * - 提交到预归档池
 * - 删除工作区项
 */
@RestController
@RequestMapping("/api/scan/workspace")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ScanWorkspaceController {

    private final ScanWorkspaceService scanWorkspaceService;

    /**
     * 获取当前用户的工作区文件列表
     *
     * GET /api/scan/workspace
     *
     * @param user 当前认证用户
     * @return 工作区文件列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('scan:view', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ScanWorkspace>> getWorkspaceFiles(
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        List<ScanWorkspace> files = scanWorkspaceService.getUserWorkspaceFiles(userId);

        log.info("查询工作区文件: userId={}, count={}", userId, files.size());
        return Result.success(files);
    }

    /**
     * 上传文件到工作区
     *
     * POST /api/scan/workspace/upload
     *
     * @param file 上传的文件
     * @param uploadSource 上传来源（scan/upload/folder）
     * @param sessionId 会话ID（用于批量操作，可选）
     * @param user 当前认证用户
     * @return 创建的工作区项
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyAuthority('scan:upload', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_UPLOAD", resourceType = "SCAN_WORKSPACE",
                  description = "上传文件到扫描工作区")
    public Result<ScanWorkspace> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadSource", defaultValue = "upload") String uploadSource,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String userId = user != null ? user.getId() : "system";
        ScanWorkspace workspace = scanWorkspaceService.uploadFile(file, uploadSource, sessionId, userId);

        log.info("文件上传到工作区: userId={}, fileName={}, id={}", userId, file.getOriginalFilename(), workspace.getId());
        return Result.success("文件上传成功", workspace);
    }

    /**
     * 触发OCR识别
     *
     * POST /api/scan/workspace/{id}/ocr
     *
     * @param id 工作区项ID
     * @param engine OCR引擎类型（tesseract/paddle/baidu，可选，默认使用系统配置）
     * @param user 当前认证用户
     * @return 更新后的工作区项
     */
    @PostMapping("/{id}/ocr")
    @PreAuthorize("hasAnyAuthority('scan:ocr', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_OCR", resourceType = "SCAN_WORKSPACE",
                  description = "触发OCR识别")
    public Result<ScanWorkspace> triggerOcr(
            @PathVariable Long id,
            @RequestParam(value = "engine", required = false) String engine,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        ScanWorkspace workspace = scanWorkspaceService.triggerOcr(id, engine, userId);

        log.info("OCR识别已触发: userId={}, workspaceId={}, engine={}", userId, id, engine);
        return Result.success("OCR识别已启动", workspace);
    }

    /**
     * 更新OCR识别结果（用户编辑后）
     *
     * PUT /api/scan/workspace/{id}
     *
     * @param id 工作区项ID
     * @param workspace 更新的工作区数据（包含ocrResult、docType、overallScore等）
     * @param user 当前认证用户
     * @return 更新后的工作区项
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_UPDATE", resourceType = "SCAN_WORKSPACE",
                  description = "更新扫描工作区数据")
    public Result<ScanWorkspace> updateWorkspace(
            @PathVariable Long id,
            @Valid @RequestBody ScanWorkspace workspace,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        workspace.setId(id);
        ScanWorkspace updated = scanWorkspaceService.updateWorkspace(workspace, userId);

        log.info("工作区数据已更新: userId={}, workspaceId={}", userId, id);
        return Result.success("更新成功", updated);
    }

    /**
     * 提交到预归档池
     *
     * POST /api/scan/workspace/{id}/submit
     *
     * @param id 工作区项ID
     * @param user 当前认证用户
     * @return 提交结果（包含预归档池ID等信息）
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('scan:submit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_SUBMIT", resourceType = "SCAN_WORKSPACE",
                  description = "提交工作区项到预归档池")
    public Result<ScanWorkspaceService.SubmitResult> submitToPreArchive(
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        ScanWorkspaceService.SubmitResult result = scanWorkspaceService.submitToPreArchive(id, userId);

        log.info("提交到预归档池: userId={}, workspaceId={}, archiveId={}", userId, id, result.getArchiveId());
        return Result.success("提交成功", result);
    }

    /**
     * 删除工作区项
     *
     * DELETE /api/scan/workspace/{id}
     *
     * @param id 工作区项ID
     * @param user 当前认证用户
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:delete', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_DELETE", resourceType = "SCAN_WORKSPACE",
                  description = "删除工作区项")
    public Result<Void> deleteWorkspace(
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        scanWorkspaceService.deleteWorkspace(id, userId);

        log.info("工作区项已删除: userId={}, workspaceId={}", userId, id);
        return Result.success("删除成功");
    }
}
