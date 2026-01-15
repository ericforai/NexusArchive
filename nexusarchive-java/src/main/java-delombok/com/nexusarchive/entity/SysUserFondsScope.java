// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: SysUserFondsScope 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-全宗授权范围
 * 对应表: sys_user_fonds_scope
 */
@Data
@TableName("sys_user_fonds_scope")
public class SysUserFondsScope {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 授权来源类型 (DIRECT/ROLE/IMPORT)
     */
    private String scopeType;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;

    // Manual Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getFondsNo() { return fondsNo; }
    public String getScopeType() { return scopeType; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public LocalDateTime getLastModifiedTime() { return lastModifiedTime; }
    public Integer getDeleted() { return deleted; }

    // Manual Setters
    public void setId(String id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setFondsNo(String fondsNo) { this.fondsNo = fondsNo; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public void setLastModifiedTime(LocalDateTime lastModifiedTime) { this.lastModifiedTime = lastModifiedTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
