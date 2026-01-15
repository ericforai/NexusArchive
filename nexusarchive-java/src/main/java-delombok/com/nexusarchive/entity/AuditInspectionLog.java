// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: AuditInspectionLog 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 四性检测日志实体
 * 对应表: audit_inspection_log
 */
@Data
@TableName("audit_inspection_log")
public class AuditInspectionLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String archiveId;

    /**
     * 检测环节: receive, transfer, patrol, migration
     */
    private String inspectionStage;

    private LocalDateTime inspectionTime;

    private String inspectorId;

    private Boolean isAuthentic;

    private Boolean isComplete;

    private Boolean isAvailable;

    private Boolean isSecure;

    private String hashSnapshot;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object integrityCheck;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object authenticityCheck;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object availabilityCheck;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object securityCheck;

    private String checkResult;

    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object detailReport;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 检测报告文件路径 (XML)
     */
    private String reportFilePath;

    /**
     * 检测报告文件哈希
     */
    private String reportFileHash;
    
    /**
     * 是否符合《会计档案管理办法》
     */
    private Boolean isCompliant;
    
    /**
     * 符合性检查违规项
     */
    private String complianceViolations;
    
    /**
     * 符合性检查警告项
     */
    private String complianceWarnings;

    // Manual Getters
    public Boolean getIsCompliant() { return isCompliant; }
    public String getComplianceWarnings() { return complianceWarnings; }
    public String getComplianceViolations() { return complianceViolations; }
}
