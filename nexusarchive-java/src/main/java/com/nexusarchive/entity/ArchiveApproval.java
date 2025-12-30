// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchiveApproval 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案审批实体
 * 对应表: biz_archive_approval
 * Reference: DA/T 94-2022
 */
@Data
@TableName("biz_archive_approval")
public class ArchiveApproval {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 待审批档案ID
     */
    private String archiveId;

    /**
     * 档号（冗余字段，方便查询）
     */
    private String archiveCode;

    /**
     * 立档单位
     */
    private String orgName;

    /**
     * 档案题名（冗余字段）
     */
    private String archiveTitle;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请理由
     */
    private String applicationReason;

    /**
     * 审批人ID
     */
    private String approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 状态: PENDING(待审批), APPROVED(已批准), REJECTED(已拒绝)
     */
    private String status;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
