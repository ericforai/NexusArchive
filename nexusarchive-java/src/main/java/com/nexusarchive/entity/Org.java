// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Org 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
