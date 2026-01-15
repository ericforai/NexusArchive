// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchiveAttachment 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 档案附件关联实体
 * 用于全景视图中凭证与附件的多对多关联
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("acc_archive_attachment")
public class ArchiveAttachment {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 档案ID (acc_archive.id)
     */
    private String archiveId;

    /**
     * 文件ID (arc_file_content.id)
     */
    private String fileId;

    /**
     * 附件类型: invoice, contract, bank_slip, other
     */
    private String attachmentType;

    /**
     * 关联描述
     */
    private String relationDesc;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // Manual Getters
    public String getId() { return id; }
    public String getArchiveId() { return archiveId; }
    public String getFileId() { return fileId; }
    public String getAttachmentType() { return attachmentType; }
    public String getRelationDesc() { return relationDesc; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedTime() { return createdTime; }
}
