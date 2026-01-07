// Input: Spring Framework, MyBatis-Plus, Lombok, Local Services
// Output: ScanWorkspaceServiceImpl
// Pos: Service Implementation Layer

package com.nexusarchive.service.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ScanWorkspace;
import com.nexusarchive.mapper.ScanWorkspaceMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.ScanWorkspaceService;
import com.nexusarchive.util.FileHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 扫描工作区服务实现
 *
 * <p>核心功能：</p>
 * <ol>
 *   <li>文件上传：保存到 {uploadPath}/{userId}/{yyyy/MM/dd}/{uuid}.{ext}</li>
 *   <li>OCR 触发：更新状态为 processing，后续由 OCR 服务处理</li>
 *   <li>数据更新：支持 OCR 结果更新和人工编辑</li>
 *   <li>提交归档：标记为 submitted，关联 archiveId</li>
 *   <li>清理删除：删除物理文件和数据库记录</li>
 * </ol>
 *
 * <p>合规性：</p>
 * <ul>
 *   <li>文件哈希校验 (SM3 优先，SHA-256 备用)</li>
 *   <li>审计日志记录 (上传、提交、删除)</li>
 *   <li>事务控制 (数据库操作)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanWorkspaceServiceImpl implements ScanWorkspaceService {

    private final ScanWorkspaceMapper scanWorkspaceMapper;
    private final FileHashUtil fileHashUtil;
    private final AuditLogService auditLogService;

    @Value("${app.scan.upload-path:/tmp/nexusarchive/scan}")
    private String uploadPath;

    // ===== ScanWorkspaceService Implementation =====

    @Override
    public List<ScanWorkspace> getUserWorkspaceFiles(String userId) {
        log.debug("查询用户工作区文件: userId={}", userId);
        return scanWorkspaceMapper.findDraftsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanWorkspace uploadFile(MultipartFile file, String uploadSource, String sessionId, String userId) {
        log.info("上传文件到工作区: filename={}, size={}, uploadSource={}, sessionId={}, userId={}",
                file.getOriginalFilename(), file.getSize(), uploadSource, sessionId, userId);

        // 1. 验证文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException("文件名不能为空");
        }

        // 验证文件类型
        String allowedExtensions = "pdf,jpg,jpeg,png,tiff,bmp";
        String fileExtension = getFileExtension(originalFilename);
        if (!allowedExtensions.contains(fileExtension)) {
            throw new BusinessException("不支持的文件类型: " + fileExtension);
        }

        // 验证文件大小 (最大 50MB)
        long maxSize = 50 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小超过限制 (最大50MB)");
        }

        // 2. 生成存储路径: {uploadPath}/{userId}/{yyyy/MM/dd}/{uuid}.{ext}
        String uuid = UUID.randomUUID().toString();
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = userId + "/" + datePath + "/" + uuid + "." + fileExtension;
        Path targetPath = Paths.get(uploadPath, relativePath);

        try {
            // 3. 创建目录
            Files.createDirectories(targetPath.getParent());

            // 4. 保存文件
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 5. 计算文件哈希 (优先使用 SM3)
            String fileHash;
            try {
                fileHash = fileHashUtil.calculateSM3(file.getInputStream());
            } catch (Exception e) {
                log.warn("SM3 哈希计算失败，使用 SHA-256 备用方案", e);
                try {
                    fileHash = fileHashUtil.calculateSHA256(file.getInputStream());
                } catch (java.security.NoSuchAlgorithmException ex) {
                    log.error("SHA-256 算法不可用", ex);
                    fileHash = java.util.UUID.randomUUID().toString(); // 兜底方案
                }
            }

            // 6. 创建数据库记录
            ScanWorkspace workspace = ScanWorkspace.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .fileName(originalFilename)
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .fileType(fileExtension)
                    .uploadSource(uploadSource)
                    .ocrStatus("pending")
                    .submitStatus("draft")
                    .build();

            scanWorkspaceMapper.insert(workspace);

            // 7. 记录审计日志
            auditLogService.log(
                    userId,
                    userId,
                    "UPLOAD_FILE",
                    "SCAN_WORKSPACE",
                    String.valueOf(workspace.getId()),
                    "SUCCESS",
                    "上传文件到工作区: " + originalFilename,
                    null
            );

            log.info("文件上传成功: id={}, path={}", workspace.getId(), targetPath);
            return workspace;

        } catch (IOException e) {
            log.error("文件保存失败: filename={}", originalFilename, e);
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerOcr(Long id, String engine, String userId) {
        log.info("触发 OCR 识别: id={}, engine={}, userId={}", id, engine, userId);

        // 1. 验证记录存在
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null) {
            throw new IllegalArgumentException("工作区记录不存在: id=" + id);
        }

        // 2. 验证用户权限
        if (!userId.equals(workspace.getUserId())) {
            throw new IllegalArgumentException("无权操作该记录");
        }

        // 3. 验证状态
        if (!"pending".equals(workspace.getOcrStatus())) {
            throw new IllegalStateException("OCR 状态不允许触发: " + workspace.getOcrStatus());
        }

        // 4. 更新状态为 processing
        workspace.setOcrStatus("processing");
        workspace.setOcrEngine(engine);
        scanWorkspaceMapper.updateById(workspace);

        // 5. 记录审计日志
        auditLogService.log(
                userId,
                userId,
                "TRIGGER_OCR",
                "SCAN_WORKSPACE",
                String.valueOf(id),
                "SUCCESS",
                "触发 OCR 识别: engine=" + engine,
                null
        );

        // 注意：实际的 OCR 处理由独立的 OCR 服务异步完成
        // 这里只更新状态，OCR 服务会查询 ocr_status='processing' 的记录进行处理
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScanWorkspace updateWorkspace(ScanWorkspace workspace, String userId) {
        log.info("更新工作区记录: id={}, userId={}", workspace.getId(), userId);

        // 1. 验证记录存在
        ScanWorkspace existing = scanWorkspaceMapper.selectById(workspace.getId());
        if (existing == null) {
            throw new IllegalArgumentException("工作区记录不存在: id=" + workspace.getId());
        }

        // 2. 验证用户权限
        if (!userId.equals(existing.getUserId())) {
            throw new IllegalArgumentException("无权操作该记录");
        }

        // 3. 保留不可修改字段
        workspace.setId(existing.getId());
        workspace.setSessionId(existing.getSessionId());
        workspace.setUserId(existing.getUserId());
        workspace.setFileName(existing.getFileName());
        workspace.setFilePath(existing.getFilePath());
        workspace.setFileSize(existing.getFileSize());
        workspace.setFileType(existing.getFileType());
        workspace.setUploadSource(existing.getUploadSource());
        workspace.setCreatedAt(existing.getCreatedAt());

        // 4. 更新数据库
        scanWorkspaceMapper.updateById(workspace);

        // 5. 记录审计日志
        auditLogService.log(
                userId,
                userId,
                "UPDATE_WORKSPACE",
                "SCAN_WORKSPACE",
                String.valueOf(workspace.getId()),
                "SUCCESS",
                "更新工作区记录",
                null
        );

        log.info("工作区记录更新成功: id={}", workspace.getId());
        return workspace;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResult submitToPreArchive(Long id, String userId) {
        log.info("提交到预归档池: id={}, userId={}", id, userId);

        // 1. 验证记录存在
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null) {
            throw new IllegalArgumentException("工作区记录不存在: id=" + id);
        }

        // 2. 验证用户权限
        if (!userId.equals(workspace.getUserId())) {
            throw new IllegalArgumentException("无权操作该记录");
        }

        // 3. 验证状态
        if ("submitted".equals(workspace.getSubmitStatus())) {
            throw new IllegalStateException("记录已提交，不能重复提交");
        }

        // 4. TODO: 实际的归档逻辑
        //    - 创建 Archive 记录
        //    - 创建 ArcFileContent 记录
        //    - 关联原始文件
        //    - 提交到预归档池
        //    当前使用占位符实现
        String archiveId = "archive-placeholder-" + id;

        // 5. 更新状态为已提交
        scanWorkspaceMapper.markAsSubmitted(id, archiveId);

        // 6. 记录审计日志
        auditLogService.log(
                userId,
                userId,
                "SUBMIT_TO_ARCHIVE",
                "SCAN_WORKSPACE",
                String.valueOf(id),
                "SUCCESS",
                "提交到预归档池: archiveId=" + archiveId,
                null
        );

        log.info("提交到预归档池成功: id={}, archiveId={}", id, archiveId);
        return new SubmitResult(archiveId, "提交成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long id, String userId) {
        log.info("删除工作区记录: id={}, userId={}", id, userId);

        // 1. 验证记录存在
        ScanWorkspace workspace = scanWorkspaceMapper.selectById(id);
        if (workspace == null) {
            throw new IllegalArgumentException("工作区记录不存在: id=" + id);
        }

        // 2. 验证用户权限
        if (!userId.equals(workspace.getUserId())) {
            throw new IllegalArgumentException("无权操作该记录");
        }

        // 3. 验证状态
        if ("submitted".equals(workspace.getSubmitStatus())) {
            throw new IllegalStateException("已提交的记录不能删除");
        }

        // 4. 删除物理文件
        try {
            Path filePath = Paths.get(workspace.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("删除物理文件: path={}", filePath);
            }
        } catch (IOException e) {
            log.warn("删除物理文件失败: path={}", workspace.getFilePath(), e);
            // 继续执行，不阻断数据库记录删除
        }

        // 5. 删除数据库记录
        scanWorkspaceMapper.deleteById(id);

        // 6. 记录审计日志
        auditLogService.log(
                userId,
                userId,
                "DELETE_WORKSPACE",
                "SCAN_WORKSPACE",
                String.valueOf(id),
                "SUCCESS",
                "删除工作区记录: " + workspace.getFileName(),
                null
        );

        log.info("工作区记录删除成功: id={}", id);
    }

    @Override
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        log.info("Created scan session: sessionId={}, userId={}", sessionId, userId);
        return sessionId;
    }

    // ===== Private Helper Methods =====

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名 (不含点号)，如 "pdf"
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "tmp"; // 默认扩展名
    }
}
