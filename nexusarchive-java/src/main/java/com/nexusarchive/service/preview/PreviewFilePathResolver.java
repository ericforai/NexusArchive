// Input: MyBatis-Plus, Spring Framework
// Output: PreviewFilePathResolver
// Pos: Service Layer

package com.nexusarchive.service.preview;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveAttachmentMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 预览文件路径解析器
 *
 * 负责解析档案文件的存储路径
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PreviewFilePathResolver {

    private final ArchiveAttachmentMapper archiveAttachmentMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final OriginalVoucherFileMapper originalVoucherFileMapper;

    public record ResolvedPreviewFile(
        String resourceType,
        String resourceId,
        String fileId,
        String storagePath,
        String fileName,
        String fileType
    ) {
    }

    /**
     * 获取档案的文件路径
     *
     * <p>查找顺序：</p>
     * <ol>
     *   <li>优先从 ArchiveAttachment 表查询</li>
     *   <li>其次从 ArcFileContent 表查询（通过 item_id 关联）</li>
     * </ol>
     *
     * @param archiveId 档案ID
     * @return 文件存储路径，如果未找到则返回null
     */
    public String resolveArchiveFilePath(String archiveId) {
        ResolvedPreviewFile resolved = resolveArchiveMainFile(archiveId);
        return resolved != null ? resolved.storagePath() : null;
    }

    public ResolvedPreviewFile resolveArchiveMainFile(String archiveId) {
        ResolvedPreviewFile pathFromAttachment = resolveFromAttachment(archiveId);
        if (pathFromAttachment != null) {
            return pathFromAttachment;
        }

        ResolvedPreviewFile pathFromContent = resolveFromArcFileContent(archiveId);
        if (pathFromContent != null) {
            return pathFromContent;
        }

        log.debug("未找到档案文件路径: archiveId={}", archiveId);
        return null;
    }

    public ResolvedPreviewFile resolveFileById(String fileId) {
        ArcFileContent archiveFile = arcFileContentMapper.selectById(fileId);
        if (archiveFile != null && archiveFile.getStoragePath() != null) {
            return toResolvedFile("file", fileId, archiveFile);
        }

        OriginalVoucherFile originalVoucherFile = originalVoucherFileMapper.selectById(fileId);
        if (originalVoucherFile != null && originalVoucherFile.getDeleted() == 0
            && originalVoucherFile.getStoragePath() != null) {
            return new ResolvedPreviewFile(
                "file",
                fileId,
                originalVoucherFile.getId(),
                originalVoucherFile.getStoragePath(),
                originalVoucherFile.getFileName(),
                originalVoucherFile.getFileType()
            );
        }

        log.debug("未找到文件预览路径: fileId={}", fileId);
        return null;
    }

    /**
     * 从附件表解析文件路径
     */
    private ResolvedPreviewFile resolveFromAttachment(String archiveId) {
        List<ArchiveAttachment> attachments = archiveAttachmentMapper.selectByArchiveId(archiveId);
        if (attachments.isEmpty()) {
            return null;
        }

        // 获取第一个附件（通常是主文件）
        String fileId = attachments.get(0).getFileId();
        ArcFileContent fileContent = arcFileContentMapper.selectById(fileId);
        if (fileContent != null && fileContent.getStoragePath() != null) {
            return new ResolvedPreviewFile(
                "archiveMain",
                archiveId,
                fileContent.getId(),
                fileContent.getStoragePath(),
                fileContent.getFileName(),
                fileContent.getFileType()
            );
        }

        return null;
    }

    /**
     * 从档案内容表解析文件路径
     */
    private ResolvedPreviewFile resolveFromArcFileContent(String archiveId) {
        // 先尝试通过 item_id 查找
        List<ArcFileContent> files = arcFileContentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, archiveId)
                .orderByAsc(ArcFileContent::getCreatedTime)
                .last("LIMIT 1")
        );

        if (files.isEmpty() || files.get(0).getStoragePath() == null) {
            // 如果 item_id 查找失败，尝试通过 archival_code 查找
            log.debug("item_id 查找失败，尝试通过 archival_code 查找: archiveId={}", archiveId);
            files = arcFileContentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                    .eq(ArcFileContent::getArchivalCode, archiveId)
                    .orderByAsc(ArcFileContent::getCreatedTime)
                    .last("LIMIT 1")
            );
        }

        if (!files.isEmpty() && files.get(0).getStoragePath() != null) {
            return toResolvedFile("archiveMain", archiveId, files.get(0));
        }

        return null;
    }

    private ResolvedPreviewFile toResolvedFile(String resourceType, String resourceId, ArcFileContent fileContent) {
        return new ResolvedPreviewFile(
            resourceType,
            resourceId,
            fileContent.getId(),
            fileContent.getStoragePath(),
            fileContent.getFileName(),
            fileContent.getFileType()
        );
    }
}
