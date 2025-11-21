package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全审计日志实体
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String userId;

    private String username;

    private String roleType;

    private String action;

    private String resourceType;

    private String resourceId;

    private String operationResult;

    private String riskLevel;

    private String details;

    private String dataBefore;

    private String dataAfter;

    private String sessionId;

    private String ipAddress;

    private String userAgent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
