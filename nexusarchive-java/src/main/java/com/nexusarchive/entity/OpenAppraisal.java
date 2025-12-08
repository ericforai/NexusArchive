package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 开放鉴定实体
 * 对应表: biz_open_appraisal
 */
@Data
@TableName("biz_open_appraisal")
public class OpenAppraisal {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 待鉴定档案ID
     */
    private String archiveId;

    /**
     * 档号（冗余字段）
     */
    private String archiveCode;

    /**
     * 档案题名（冗余字段）
     */
    private String archiveTitle;

    /**
     * 保管期限（10Y, 30Y, PERMANENT）
     */
    private String retentionPeriod;

    /**
     * 当前密级
     */
    private String currentSecurityLevel;

    /**
     * 鉴定人ID
     */
    private String appraiserId;

    /**
     * 鉴定人姓名
     */
    private String appraiserName;

    /**
     * 鉴定日期
     */
    private LocalDate appraisalDate;

    /**
     * 鉴定结果: OPEN(开放), CONTROLLED(控制), EXTENDED(延期)
     */
    private String appraisalResult;

    /**
     * 开放等级: PUBLIC(公开), INTERNAL(内部), RESTRICTED(限制)
     */
    private String openLevel;

    /**
     * 鉴定理由
     */
    private String reason;

    /**
     * 状态: PENDING(待鉴定), COMPLETED(已完成)
     */
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
