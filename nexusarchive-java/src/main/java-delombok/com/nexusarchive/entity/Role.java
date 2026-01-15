// Input: MyBatis-Plus、Lombok、Java 标准库、本地模块
// Output: Role 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nexusarchive.common.enums.RoleCategory;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色表 (三员管理)
 * 
 * 对应表: sys_role
 */
@Data
@TableName("sys_role")
public class Role implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 角色名称
     */
    private String name;
    
    /**
     * 角色编码
     */
    private String code;
    
    /**
     * 角色类别 (三员管理)
     */
    private String roleCategory;
    
    /**
     * 是否互斥 (三员角色)
     */
    private Boolean isExclusive;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 权限列表 (JSON)
     */
    private String permissions;
    
    /**
     * 数据权限范围
     */
    private String dataScope;
    
    /**
     * 角色类型 (system/custom)
     */
    private String type;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;
    
    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // Manual Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getRoleCategory() { return roleCategory; }
    public Boolean getIsExclusive() { return isExclusive; }
    public String getDescription() { return description; }
    public String getPermissions() { return permissions; }
    public String getDataScope() { return dataScope; }
    public String getType() { return type; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    public Integer getDeleted() { return deleted; }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setRoleCategory(String roleCategory) { this.roleCategory = roleCategory; }
    public void setIsExclusive(Boolean isExclusive) { this.isExclusive = isExclusive; }
    public void setDescription(String description) { this.description = description; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public void setType(String type) { this.type = type; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
