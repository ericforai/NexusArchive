package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案卷实体 (虚拟装订)
 * 对应表: acc_archive_volume
 */
@Data
@TableName("acc_archive_volume")
public class ArchiveVolume {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 案卷号 (2025-KJ-PZ-01-001)
     */
    private String volumeCode;

    /**
     * 年度
     */
    private Integer archiveYear;

    /**
     * 保管期限: 10Y, 30Y, PERMANENT
     */
    private String retentionPeriod;

    /**
     * 状态: 0:打开(可追加) 1:封卷(不可改)
     */
    private Integer status;

    /**
     * 四性检测报告路径
     */
    private String validationReportPath;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 当前保管部门: ACCOUNTING(会计), ARCHIVES(档案)
     */
    @TableField("custodian_dept")
    private String custodianDept;

    @TableLogic
    private Integer deleted;
}
