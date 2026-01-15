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
     * 父法人ID（用于集团层级：母公司-子公司）
     */
    private String parentId;

    /**
     * 排序号
     */
    private Integer orderNum;

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

    // Manual Getters to fix compilation issues if Lombok fails
    public String getId() { return id; }
    public String getName() { return name; }
    public String getTaxId() { return taxId; }
    public String getAddress() { return address; }
    public String getContactPerson() { return contactPerson; }
    public String getContactPhone() { return contactPhone; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getUpdatedTime() { return updatedTime; }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
    public void setStatus(String status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
}





