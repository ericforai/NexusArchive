// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SysEntity 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 法人实体表
 * 对应表: sys_entity
 * 
 * PRD 来源: Section 1.1 - 法人仅管理维度
 * 说明: entity_id 仅用于治理、统计与合规台账，不作为数据隔离键
 */
@Data
@TableName("sys_entity")
public class SysEntity {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 法人名称
     */
    private String name;

    /**
     * 统一社会信用代码/税号
     */
    private String taxId;

    /**
     * 注册地址
     */
    private String address;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 状态: ACTIVE, INACTIVE
     */
    private String status;

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
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}


