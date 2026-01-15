// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Permission 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 权限实体
 * 对应表: sys_permission
 */
@Data
@TableName("sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 权限标识 (如 manage_users)
     */
    private String permKey;

    /**
     * 显示名称 (如 用户管理)
     */
    private String label;

    /**
     * 分组名称 (如 系统管理)
     */
    private String groupName;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    // Manual Getters
    public String getId() { return id; }
    public String getPermKey() { return permKey; }
    public String getLabel() { return label; }
    public String getGroupName() { return groupName; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setPermKey(String permKey) { this.permKey = permKey; }
    public void setLabel(String label) { this.label = label; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
}
