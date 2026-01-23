// Input: Spring Web, Spring Security, Lombok, Jakarta Validation
// Output: ScanWorkspaceController (含移动端会话管理)
// Pos: Controller Layer

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.service.ScanSessionService;
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
 * - 移动端扫码会话管理（创建、验证、上传）
 */
@RestController
@RequestMapping("/scan/workspace")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ScanWorkspaceController {

    private final ScanWorkspaceService scanWorkspaceService;
    private final ScanSessionService scanSessionService;

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
     * 获取工作区文件内容 (用于预览)
     * 
     * GET /api/scan/workspace/file/{id}
     * 
     * @param id 工作区项ID
     * @param user 当前认证用户
     * @return 文件流
     */
    @GetMapping("/file/{id}")
    @PreAuthorize("hasAnyAuthority('scan:view', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_VIEW_FILE", resourceType = "SCAN_WORKSPACE",
                  description = "预览工作区文件")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getFile(
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
            
        String userId = user != null ? user.getId() : "system";
        java.io.File file = scanWorkspaceService.getFile(id, userId);
        
        if (!file.exists()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        
        org.springframework.core.io.FileSystemResource resource = new org.springframework.core.io.FileSystemResource(file);
        
        // 自动探测 MIME 类型
        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files.probeContentType(file.toPath());
        } catch (java.io.IOException e) {
            log.warn("无法探测文件类型: {}", file.getName());
        }
        
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .body(resource);
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
    public Result<Void> triggerOcr(
            @PathVariable Long id,
            @RequestParam(value = "engine", required = false) String engine,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        scanWorkspaceService.triggerOcr(id, engine, userId);

        log.info("OCR识别已触发: userId={}, workspaceId={}, engine={}", userId, id, engine);
        return Result.success();
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

        log.info("提交到预归档池: userId={}, workspaceId={}, archiveId={}", userId, id, result.archiveId());
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
        return Result.success();
    }

    /**
     * 移动端会话记录
     */
    public static class MobileSessionResponse {
        private String sessionId;
        private Integer expiresInSeconds;

        public MobileSessionResponse(String sessionId, Integer expiresInSeconds) {
            this.sessionId = sessionId;
            this.expiresInSeconds = expiresInSeconds;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Integer getExpiresInSeconds() {
            return expiresInSeconds;
        }

        public void setExpiresInSeconds(Integer expiresInSeconds) {
            this.expiresInSeconds = expiresInSeconds;
        }
    }

    /**
     * 创建移动端扫描会话
     *
     * POST /api/scan/workspace/mobile/session
     *
     * 生成唯一的会话ID，供移动端通过扫码上传文件使用。
     * 会话有效期为 30 分钟，存储在 Redis 中。
     *
     * @param user 当前认证用户
     * @return 会话信息（sessionId 和过期时间）
     */
    @PostMapping("/mobile/session")
    @PreAuthorize("hasAnyAuthority('scan:upload', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_SESSION_CREATE", resourceType = "SCAN_WORKSPACE",
                  description = "创建移动端扫描会话")
    public Result<MobileSessionResponse> createMobileSession(
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        String sessionId = scanWorkspaceService.createSession(userId);

        log.info("创建移动端扫描会话: userId={}, sessionId={}", userId, sessionId);

        // 会话有效期 30 分钟 (1800 秒)
        MobileSessionResponse response = new MobileSessionResponse(sessionId, 1800);
        return Result.success("会话创建成功", response);
    }

    /**
     * 验证移动端扫描会话
     *
     * GET /api/scan/workspace/mobile/session/{sessionId}/validate
     *
     * 验证移动端扫码后访问的会话是否有效。
     *
     * <p>注意：此端点无需认证，因为移动端用户未登录。</p>
     * <p>会话有效性由 Redis 中的记录和 TTL 保证。</p>
     *
     * @param sessionId 会话ID
     * @return 验证结果（valid 字段表示是否有效）
     */
    @GetMapping("/mobile/session/{sessionId}/validate")
    public Result<java.util.Map<String, Boolean>> validateMobileSession(
            @PathVariable String sessionId) {

        boolean valid = scanWorkspaceService.validateSession(sessionId);
        log.info("验证扫描会话: sessionId={}, valid={}", sessionId, valid);

        return Result.success(java.util.Map.of("valid", valid));
    }

    /**
     * 移动端文件上传
     *
     * POST /api/scan/workspace/mobile/upload
     *
     * 移动端扫码后上传文件的专用端点，使用 sessionId 进行身份验证。
     *
     * <p>注意：此端点无需 JWT 认证，使用 sessionId 关联会话创建者。</p>
     *
     * @param file 上传的文件
     * @param sessionId 会话ID（从二维码获取）
     * @return 创建的工作区项
     */
    @PostMapping("/mobile/upload")
    public Result<ScanWorkspace> mobileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId) {

        // 1. 验证会话有效性
        if (!scanWorkspaceService.validateSession(sessionId)) {
            log.warn("移动端上传失败：会话无效或已过期, sessionId={}", sessionId);
            return Result.error("会话无效或已过期");
        }

        // 2. 从会话获取用户ID
        String userId = scanSessionService.getSessionUserId(sessionId);

        if (userId == null) {
            log.error("会话存在但无法获取用户ID: sessionId={}", sessionId);
            return Result.error("会话数据异常");
        }

        // 3. 执行上传
        ScanWorkspace workspace = scanWorkspaceService.uploadFile(file, "mobile", sessionId, userId);

        log.info("移动端上传成功: sessionId={}, userId={}, fileName={}",
                sessionId, userId, file.getOriginalFilename());

        return Result.success("文件上传成功", workspace);
    }
}
