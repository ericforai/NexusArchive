package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电子会计档案实体
 */
@Data
@TableName("acc_archive")
public class Archive {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号 (M9)
     */
    private String fondsNo;

    /**
     * 档号 (M13)
     */
    private String archiveCode;

    /**
     * 类别号 (M14)
     */
    private String categoryCode;

    /**
     * 题名 (M22)
     */
    private String title;

    /**
     * 年度 (M11)
     */
    private String fiscalYear;

    /**
     * 会计月份/期间 (M41)
     */
    private String fiscalPeriod;

    /**
     * 保管期限 (M12)
     */
    private String retentionPeriod;

    /**
     * 立档单位名称 (M6)
     */
    private String orgName;

    /**
     * 责任者/制单人 (M32)
     */
    private String creator;

    /**
     * 状态: draft, pending, archived
     */
    private String status;

    /**
     * DA/T 94标准元数据 (JSON)
     */
    @TableField("standard_metadata")
    private String standardMetadata;

    /**
     * 客户自定义元数据 (JSON)
     */
    @TableField("custom_metadata")
    private String customMetadata;

    /**
     * 密级: internal, secret, top_secret
     */
    private String securityLevel;

    /**
     * 存放位置
     */
    private String location;

    /**
     * 所属部门ID
     */
    private String departmentId;

    /**
     * 创建人ID
     */
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
