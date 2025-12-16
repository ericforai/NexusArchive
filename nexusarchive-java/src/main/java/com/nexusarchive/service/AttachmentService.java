package com.nexusarchive.service;

import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.ArcFileContent;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 档案附件关联服务
 */
public interface AttachmentService {

    /**
     * 获取档案关联的所有附件
     * @param archiveId 档案ID
     * @return 附件文件列表
     */
    List<ArcFileContent> getAttachmentsByArchive(String archiveId);

    /**
     * 关联附件到档案
     * @param archiveId 档案ID
     * @param fileId 文件ID
     * @param attachmentType 附件类型 (invoice/contract/bank_slip/other)
     * @param userId 操作人ID
     * @return 关联记录
     */
    ArchiveAttachment linkAttachment(String archiveId, String fileId, String attachmentType, String userId);

    /**
     * 上传附件并关联到档案
     * @param archiveId 档案ID
     * @param file 上传的文件
     * @param attachmentType 附件类型
     * @param userId 操作人ID
     * @return 上传的文件记录
     */
    ArcFileContent uploadAndLink(String archiveId, MultipartFile file, String attachmentType, String userId);

    /**
     * 删除附件关联
     * @param attachmentId 关联记录ID
     */
    void unlinkAttachment(String attachmentId);

    /**
     * 获取关联记录详情
     * @param archiveId 档案ID
     * @return 关联记录列表
     */
    List<ArchiveAttachment> getAttachmentLinks(String archiveId);
}
