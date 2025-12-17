package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 全宗基础信息表
 * 对应表: bas_fonds
 */
@Data
@TableName("bas_fonds")
public class BasFonds {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号 (Unique)
     */
    private String fondsCode;

    /**
     * 全宗名称
     */
    private String fondsName;

    /**
     * 立档单位名称
     */
    private String companyName;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 关联组织ID (Company Level)
     */
    private String orgId;
}
