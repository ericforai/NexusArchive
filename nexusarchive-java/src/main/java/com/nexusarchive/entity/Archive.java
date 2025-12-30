// Input: MyBatis-Plus、Jakarta EE、Lombok、Java 标准库
// Output: Archive 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
     * 所属全宗ID (Ref: BasFonds)
     * Note: This column does not exist in DB yet.
     */
    @TableField(exist = false)
    private String fondsId;

    /**
     * 全宗号 (M9) - 冗余存储，便于查询
     */
    @NotBlank(message = "全宗号不能为空")
    @Size(max = 50, message = "全宗号长度不能超过50")
    private String fondsNo;

    /**
     * 档号 (M13)
     */
    @NotBlank(message = "档号不能为空")
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String archiveCode;

    /**
     * 类别号 (M14)
     */
    private String categoryCode;

    /**
     * 题名 (M22) - Encrypted with SM4
     */
    @NotBlank(message = "题名不能为空")
    @TableField(typeHandler = com.nexusarchive.config.mybatis.EncryptTypeHandler.class)
    private String title;

    /**
     * 年度 (M11)
     */
    @NotBlank(message = "年度不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "年度格式必须为4位数字")
    private String fiscalYear;

    /**
     * 会计月份/期间 (M41)
     */
    private String fiscalPeriod;

    /**
     * 保管期限 (M12)
     */
    @NotBlank(message = "保管期限不能为空")
    private String retentionPeriod;

    /**
     * 立档单位名称 (M6)
     */
    @NotBlank(message = "立档单位名称不能为空")
    private String orgName;

    /**
     * 责任者/制单人 (M32) - SM4 加密存储
     * 
     * 合规要求：个人信息加密保护
     */
    @TableField(typeHandler = com.nexusarchive.config.mybatis.EncryptTypeHandler.class)
    private String creator;

    /**
     * 摘要/说明 - SM4 加密存储
     * 
     * 合规要求：档案内容摘要可能包含敏感信息
     */
    @TableField(typeHandler = com.nexusarchive.config.mybatis.EncryptTypeHandler.class)
    private String summary;

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

    // DB uses created_time (not created_at)
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /**
     * 纸质档案关联号 (物理存放位置)
     */
    private String paperRefLink;

    /**
     * 销毁留置 (冻结状态)
     */
    private Boolean destructionHold;

    /**
     * 留置/冻结原因
     */
    private String holdReason;

    /**
     * 智能匹配得分 (0-100)
     */
    private Integer matchScore;

    /**
     * 关联方式
     */
    private String matchMethod;

    @TableLogic
    private Integer deleted;
}
