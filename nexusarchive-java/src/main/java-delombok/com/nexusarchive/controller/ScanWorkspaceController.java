// Input: Spring Web, Spring Security, Lombok, Swagger/OpenAPI, Jakarta Validation
// Output: ScanWorkspaceController (含移动端会话管理)
// Pos: Controller Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.service.ScanWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 扫描工作区控制器
 *
 * PRD 来源: 扫描集成模块
 * 提供扫描工作区的 REST API
 */
@Tag(name = "扫描工作区", description = """
    扫描工作区管理接口。

    **功能说明:**
    - 查询工作区文件列表
    - 上传文件到工作区
    - 触发 OCR 识别
    - 更新 OCR 识别结果
    - 提交到预归档池
    - 删除工作区项
    - 移动端会话管理

    **文件来源:**
    - scan: 扫描仪上传
    - upload: Web 上传
    - folder: 文件夹监控
    - mobile: 移动端扫码

    **文档类型识别:**
    - VAT_INVOICE: 增值税发票
    - CONTRACT: 合同协议
    - BANK_RECEIPT: 银行回单
    - ID_CARD: 身份/资质证件
    - OTHER: 其他类型

    **OCR 引擎:**
    - tesseract: 开源 OCR 引擎
    - paddle: 百度 PaddleOCR
    - baidu: 百度 OCR API

    **工作区状态:**
    - UPLOADED: 已上传
    - OCR_PROCESSING: OCR 处理中
    - OCR_COMPLETED: OCR 完成
    - EDITED: 已编辑
    - SUBMITTED: 已提交

    **移动端会话:**
    - 会话有效期: 30 分钟
    - 会话格式: UUID
    - 二维码扫码上传

    **使用场景:**
    - 扫描工作区文件管理
    - OCR 智能识别
    - 移动端扫码上传
    - 预归档数据处理

    **权限要求:**
    - scan:view 查看权限
    - scan:upload 上传权限
    - scan:ocr OCR 权限
    - scan:edit 编辑权限
    - scan:delete 删除权限
    - scan:submit 提交权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/scan/workspace")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ScanWorkspaceController {

    private final ScanWorkspaceService scanWorkspaceService;

    /**
     * 获取当前用户的工作区文件列表
     */
    @GetMapping
    @Operation(
        summary = "获取工作区文件列表",
        description = """
            获取当前用户扫描工作区的所有文件。

            **返回数据包括:**
            - id: 工作区项 ID
            - fileName: 文件名
            - fileSize: 文件大小
            - uploadSource: 上传来源
            - docType: 文档类型
            - ocrResult: OCR 识别结果
            - overallScore: 置信度分数
            - status: 处理状态
            - createTime: 创建时间

            **业务规则:**
            - 只返回当前用户的文件
            - 按创建时间倒序排列
            - 支持状态过滤

            **使用场景:**
            - 扫描工作区首页
            - 文件列表展示
            """,
        operationId = "getScanWorkspaceFiles",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
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
     */
    @GetMapping("/file/{id}")
    @Operation(
        summary = "获取工作区文件内容",
        description = """
            获取指定工作区项的文件内容，用于预览。

            **路径参数:**
            - id: 工作区项 ID

            **返回数据:**
            - 文件流（自动探测 MIME 类型）

            **支持格式:**
            - 图片: JPG, PNG, TIFF
            - 文档: PDF, OFD

            **业务规则:**
            - 只能访问当前用户的文件
            - 文件不存在时返回 404

            **使用场景:**
            - 文件预览
            - 图片查看
            """,
        operationId = "getScanWorkspaceFile",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @PreAuthorize("hasAnyAuthority('scan:view', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_VIEW_FILE", resourceType = "SCAN_WORKSPACE",
                  description = "预览工作区文件")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getFile(
            @Parameter(description = "工作区项 ID", required = true)
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
     */
    @PostMapping("/upload")
    @Operation(
        summary = "上传文件到工作区",
        description = """
            上传文件到扫描工作区。

            **请求参数:**
            - file: 上传的文件（MultipartFile）
            - uploadSource: 上传来源（默认 upload）
              - scan: 扫描仪上传
              - upload: Web 上传
              - folder: 文件夹监控
            - sessionId: 会话 ID（可选，用于批量操作）

            **支持格式:**
            - 图片: JPG, PNG, TIFF
            - 文档: PDF, OFD

            **返回数据:**
            - id: 工作区项 ID
            - fileName: 文件名
            - fileSize: 文件大小
            - uploadSource: 上传来源
            - status: 处理状态

            **限制:**
            - 单文件最大 50MB

            **使用场景:**
            - Web 文件上传
            - 扫描仪文件接收
            """,
        operationId = "uploadToScanWorkspace",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件为空或格式不支持"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('scan:upload', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_UPLOAD", resourceType = "SCAN_WORKSPACE",
                  description = "上传文件到扫描工作区")
    public Result<ScanWorkspace> uploadFile(
            @Parameter(description = "上传的文件", required = true,
                    content = @Content(mediaType = "multipart/form-data"))
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传来源", example = "upload",
                    schema = @Schema(allowableValues = {"scan", "upload", "folder"}))
            @RequestParam(value = "uploadSource", defaultValue = "upload") String uploadSource,
            @Parameter(description = "会话 ID（用于批量操作）")
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
     */
    @PostMapping("/{id}/ocr")
    @Operation(
        summary = "触发 OCR 识别",
        description = """
            对指定工作区项触发 OCR 识别。

            **路径参数:**
            - id: 工作区项 ID

            **查询参数:**
            - engine: OCR 引擎类型（可选，默认使用系统配置）
              - tesseract: 开源 OCR 引擎
              - paddle: 百度 PaddleOCR
              - baidu: 百度 OCR API

            **识别结果包括:**
            - 文本内容
            - 文档类型（发票/合同/回单等）
            - 置信度分数
            - 关键字段提取

            **业务规则:**
            - 异步执行，立即返回
            - 识别完成后更新工作区状态

            **使用场景:**
            - OCR 智能识别
            - 发票信息提取
            """,
        operationId = "triggerOcrRecognition",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OCR 任务已触发"),
        @ApiResponse(responseCode = "400", description = "文件状态不允许 OCR"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "工作区项不存在")
    })
    @PreAuthorize("hasAnyAuthority('scan:ocr', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_OCR", resourceType = "SCAN_WORKSPACE",
                  description = "触发OCR识别")
    public Result<Void> triggerOcr(
            @Parameter(description = "工作区项 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "OCR 引擎类型", example = "tesseract",
                    schema = @Schema(allowableValues = {"tesseract", "paddle", "baidu"}))
            @RequestParam(value = "engine", required = false) String engine,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        scanWorkspaceService.triggerOcr(id, engine, userId);

        log.info("OCR识别已触发: userId={}, workspaceId={}, engine={}", userId, id, engine);
        return Result.success();
    }

    /**
     * 更新OCR识别结果（用户编辑后）
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新工作区数据",
        description = """
            更新工作区项的数据，通常用于用户编辑 OCR 结果后。

            **路径参数:**
            - id: 工作区项 ID

            **请求参数:**
            完整的工作区对象，包括:
            - ocrResult: OCR 识别结果
            - docType: 文档类型
            - overallScore: 置信度分数

            **业务规则:**
            - 只能更新当前用户的项
            - 更新后标记为已编辑状态

            **使用场景:**
            - OCR 结果修正
            - 手动标注文档类型
            """,
        operationId = "updateScanWorkspace",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "工作区项不存在")
    })
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_UPDATE", resourceType = "SCAN_WORKSPACE",
                  description = "更新扫描工作区数据")
    public Result<ScanWorkspace> updateWorkspace(
            @Parameter(description = "工作区项 ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "更新的工作区数据", required = true)
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
     */
    @PostMapping("/{id}/submit")
    @Operation(
        summary = "提交到预归档池",
        description = """
            将工作区项提交到预归档池，等待归档审批。

            **路径参数:**
            - id: 工作区项 ID

            **返回数据包括:**
            - archiveId: 创建的预归档记录 ID
            - status: 提交状态

            **业务规则:**
            - 提交后工作区项状态变更为 SUBMITTED
            - 创建对应的预归档记录
            - 进入归档审批流程

            **使用场景:**
            - 完成数据处理
            - 提交归档审批
            """,
        operationId = "submitToPreArchive",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许提交"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "工作区项不存在")
    })
    @PreAuthorize("hasAnyAuthority('scan:submit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_SUBMIT", resourceType = "SCAN_WORKSPACE",
                  description = "提交工作区项到预归档池")
    public Result<ScanWorkspaceService.SubmitResult> submitToPreArchive(
            @Parameter(description = "工作区项 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {

        String userId = user != null ? user.getId() : "system";
        ScanWorkspaceService.SubmitResult result = scanWorkspaceService.submitToPreArchive(id, userId);

        log.info("提交到预归档池: userId={}, workspaceId={}, archiveId={}", userId, id, result.archiveId());
        return Result.success("提交成功", result);
    }

    /**
     * 删除工作区项
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除工作区项",
        description = """
            删除指定的工作区项及其关联文件。

            **路径参数:**
            - id: 工作区项 ID

            **业务规则:**
            - 只能删除当前用户的项
            - 已提交的项不允许删除
            - 删除操作不可逆

            **使用场景:**
            - 清理无效文件
            - 取消错误上传
            """,
        operationId = "deleteScanWorkspace",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "工作区项不存在")
    })
    @PreAuthorize("hasAnyAuthority('scan:delete', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_DELETE", resourceType = "SCAN_WORKSPACE",
                  description = "删除工作区项")
    public Result<Void> deleteWorkspace(
            @Parameter(description = "工作区项 ID", required = true)
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
    @io.swagger.v3.oas.annotations.media.Schema(description = "移动端扫描会话响应")
    public static class MobileSessionResponse {
        @io.swagger.v3.oas.annotations.media.Schema(description = "会话 ID（UUID 格式）", example = "550e8400-e29b-41d4-a716-446655440000")
        private String sessionId;

        @io.swagger.v3.oas.annotations.media.Schema(description = "过期时间（秒）", example = "1800")
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
     */
    @PostMapping("/mobile/session")
    @Operation(
        summary = "创建移动端扫描会话",
        description = """
            创建移动端扫描会话，供手机扫码上传文件使用。

            **返回数据包括:**
            - sessionId: 会话 ID（UUID 格式）
            - expiresInSeconds: 过期时间（默认 1800 秒）

            **业务流程:**
            1. 调用此接口创建会话
            2. 生成二维码（包含 sessionId）
            3. 用户手机扫码二维码
            4. 手机通过 sessionId 上传文件到工作区

            **会话规则:**
            - 会话有效期: 30 分钟（1800 秒）
            - 过期后自动失效
            - 同一用户可创建多个会话
            - 会话与当前用户绑定

            **使用场景:**
            - 移动端扫码上传
            - 手机拍照归档
            """,
        operationId = "createMobileScanSession",
        tags = {"扫描工作区"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "会话创建成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
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
}
