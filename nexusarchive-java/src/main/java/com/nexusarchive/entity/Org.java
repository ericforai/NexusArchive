package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 组织/部门实体
 */
@Data
@TableName("sys_org")
public class Org implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 机构/部门编码
     */
    private String code;

    /**
     * 上级ID
     */
    private String parentId;

    /**
     * 类型: COMPANY/DEPARTMENT
     */
    private String type;

    /**
     * 排序
     */
    private Integer orderNum;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
