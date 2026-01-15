// Input: Lombok、Java 标准库、ArchiveAttachment Entity
// Output: ArchiveAttachmentResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案附件关联响应 DTO
 * <p>
 * 用于 Controller 返回档案附件关联信息，避免直接暴露 ArchiveAttachment Entity
 * </p>
 */
@Data
public class ArchiveAttachmentResponse {

    /**
     * 关联ID
     */
    private String id;

    /**
     * 档案ID
     */
    private String archiveId;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * 附件类型: invoice, contract, voucher, other
     */
    private String attachmentType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 关联顺序
     */
    private Integer displayOrder;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setArchiveId(String archiveId) { this.archiveId = archiveId; }
    public void setFileId(String fileId) { this.fileId = fileId; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
