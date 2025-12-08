package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案关联关系实体
 */
@Data
@TableName("acc_archive_relation")
public class ArchiveRelation {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 源档案ID
     */
    private String sourceId;

    /**
     * 目标档案ID
     */
    private String targetId;

    /**
     * 关系类型 (M93)
     */
    private String relationType;

    /**
     * 关系描述 (M95)
     */
    private String relationDesc;

    /**
     * 创建人ID
     */
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableLogic
    private Integer deleted;
}
