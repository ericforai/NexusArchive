// Input: MyBatis-Plus, Spring Framework
// Output: PreviewFilePathResolver
// Pos: Service Layer

package com.nexusarchive.service.preview;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveAttachmentMapper;
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
        // 1. 优先从 ArchiveAttachment 表查询
        String pathFromAttachment = resolveFromAttachment(archiveId);
        if (pathFromAttachment != null) {
            return pathFromAttachment;
        }

        // 2. 从 ArcFileContent 表查询（通过 item_id 关联）
        String pathFromContent = resolveFromArcFileContent(archiveId);
        if (pathFromContent != null) {
            return pathFromContent;
        }

        log.debug("未找到档案文件路径: archiveId={}", archiveId);
        return null;
    }

    /**
     * 从附件表解析文件路径
     */
    private String resolveFromAttachment(String archiveId) {
        List<ArchiveAttachment> attachments = archiveAttachmentMapper.selectByArchiveId(archiveId);
        if (attachments.isEmpty()) {
            return null;
        }

        // 获取第一个附件（通常是主文件）
        String fileId = attachments.get(0).getFileId();
        ArcFileContent fileContent = arcFileContentMapper.selectById(fileId);
        if (fileContent != null && fileContent.getStoragePath() != null) {
            return fileContent.getStoragePath();
        }

        return null;
    }

    /**
     * 从档案内容表解析文件路径
     */
    private String resolveFromArcFileContent(String archiveId) {
        List<ArcFileContent> files = arcFileContentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, archiveId)
                .orderByAsc(ArcFileContent::getCreatedTime)
                .last("LIMIT 1")
        );

        if (!files.isEmpty() && files.get(0).getStoragePath() != null) {
            return files.get(0).getStoragePath();
        }

        return null;
    }
}
