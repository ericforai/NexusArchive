// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: User 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户表
 * 
 * 对应表: sys_user
 */
@Data
@TableName("sys_user")
public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码哈希
     */
    private String passwordHash;
    
    /**
     * M84 机构人员名称 (DA/T 94)
     */
    private String fullName;
    
    /**
     * M85 组织机构代码 (DA/T 94)
     */
    private String orgCode;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 组织ID（已替换 departmentId）
     */
    private String organizationId;
    
    /**
     * 状态 (active/disabled/locked)
     */
    private String status;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 工号
     */
    private String employeeId;
    
    /**
     * 职位
     */
    private String jobTitle;
    
    /**
     * 入职日期
     */
    private String joinDate;
    
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
}
