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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
