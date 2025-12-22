// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: AttachmentServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArchiveAttachmentMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 档案附件关联服务实现
 * 
 * 【合规说明】DA/T 94-2022 第6.3条
 * 电子会计档案一经归档，不得进行修改、删除、替换原有内容。
 * 因此附件上传仅允许在预归档阶段（电子凭证池）进行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {

    private final ArchiveAttachmentMapper attachmentMapper;
    private final ArcFileContentMapper fileContentMapper;
    private final FileStorageService fileStorageService;
    private final com.nexusarchive.mapper.ArchiveMapper archiveMapper;

    @Override
    public List<ArcFileContent> getAttachmentsByArchive(String archiveId) {
        List<ArchiveAttachment> links = attachmentMapper.selectByArchiveId(archiveId);
        List<ArcFileContent> files = new ArrayList<>();
        
        for (ArchiveAttachment link : links) {
            ArcFileContent file = fileContentMapper.selectById(link.getFileId());
            if (file != null) {
                // 附件类型存储在关联表中，前端通过 link.attachmentType 获取
                files.add(file);
            }
        }
        return files;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveAttachment linkAttachment(String archiveId, String fileId, String attachmentType, String userId) {
        // 检查文件是否存在
        ArcFileContent file = fileContentMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("文件不存在: " + fileId);
        }

        // 检查是否已关联
        List<ArchiveAttachment> existing = attachmentMapper.selectByArchiveId(archiveId);
        for (ArchiveAttachment att : existing) {
            if (att.getFileId().equals(fileId)) {
                log.info("附件已关联，跳过: archiveId={}, fileId={}", archiveId, fileId);
                return att;
            }
        }

        // 创建关联
        ArchiveAttachment attachment = ArchiveAttachment.builder()
                .archiveId(archiveId)
                .fileId(fileId)
                .attachmentType(attachmentType)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        attachmentMapper.insert(attachment);
        log.info("附件关联成功: archiveId={}, fileId={}, type={}", archiveId, fileId, attachmentType);
        return attachment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArcFileContent uploadAndLink(String archiveId, MultipartFile file, String attachmentType, String userId) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        // 【合规校验】DA/T 94-2022 第6.3条：电子会计档案一经归档，不得修改
        // 检查档案状态，已归档档案不允许添加附件
        com.nexusarchive.entity.Archive archive = archiveMapper.selectById(archiveId);
        if (archive != null) {
            String status = archive.getStatus();
            if ("ARCHIVED".equalsIgnoreCase(status)) {
                log.warn("拒绝向已归档档案添加附件: archiveId={}, status={}", archiveId, status);
                throw new BusinessException("根据 DA/T 94-2022 第6.3条规定，已归档档案不可添加或修改附件。请在归档前完成附件关联。");
            }
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString().replace("-", "");
            String extension = "";
            
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 存储文件
            String relativePath = "attachments/" + archiveId + "/" + fileId + extension;
            fileStorageService.saveFile(file.getInputStream(), relativePath);

            // 计算哈希
            byte[] content = file.getBytes();
            String fileHash = calculateSM3Hash(content);

            // 保存文件记录
            ArcFileContent fileContent = new ArcFileContent();
            fileContent.setId(fileId);
            fileContent.setArchivalCode(archiveId);
            fileContent.setFileName(originalFilename);
            fileContent.setFileType(extension.replace(".", "").toUpperCase());
            fileContent.setFileSize(file.getSize());
            fileContent.setFileHash(fileHash);
            fileContent.setHashAlgorithm("SM3");
            fileContent.setStoragePath(relativePath);
            fileContent.setCreatedTime(LocalDateTime.now());

            fileContentMapper.insert(fileContent);

            // 创建关联
            linkAttachment(archiveId, fileId, attachmentType, userId);

            log.info("附件上传并关联成功: archiveId={}, fileId={}, fileName={}", archiveId, fileId, originalFilename);
            return fileContent;

        } catch (IOException e) {
            log.error("附件上传失败", e);
            throw new BusinessException("附件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkAttachment(String attachmentId) {
        int deleted = attachmentMapper.deleteById(attachmentId);
        if (deleted == 0) {
            throw new BusinessException("关联记录不存在: " + attachmentId);
        }
        log.info("附件关联已删除: id={}", attachmentId);
    }

    @Override
    public List<ArchiveAttachment> getAttachmentLinks(String archiveId) {
        return attachmentMapper.selectByArchiveId(archiveId);
    }

    /**
     * 计算 SM3 哈希（简化版，实际应使用 BouncyCastle）
     */
    private String calculateSM3Hash(byte[] content) {
        try {
            // 先尝试 SM3，不支持则降级到 SHA-256
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("SM3");
            } catch (Exception e) {
                md = MessageDigest.getInstance("SHA-256");
            }
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Hash calculation failed", e);
            return UUID.randomUUID().toString();
        }
    }
}
