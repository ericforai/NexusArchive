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
     * 客户自定义元数据 (JSON) - PostgreSQL JSONB
     */
    @TableField(value = "custom_metadata", typeHandler = com.nexusarchive.config.PostgresJsonTypeHandler.class)
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

    /**
     * 文件哈希值 (用于完整性校验)
     */
    private String fixityValue;

    /**
     * 哈希算法: SM3, SHA256
     */
    private String fixityAlgo;

    /**
     * 唯一单据号 (关键！用于关联ERP/OA)
     */
    private String uniqueBizId;

    /**
     * 金额
     */
    private java.math.BigDecimal amount;

    /**
     * 业务日期
     */
    private java.time.LocalDate docDate;

    /**
     * 所属案卷ID
     */
    private String volumeId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
