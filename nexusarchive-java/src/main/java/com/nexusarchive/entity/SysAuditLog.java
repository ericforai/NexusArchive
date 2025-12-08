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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 操作者ID (对应 operator_id)
     */
    private String userId;

    /**
     * 操作者姓名 (对应 operator_name)
     */
    private String username;

    private String roleType;

    /**
     * 操作类型 (对应 operation_type): CAPTURE, ARCHIVE, MODIFY_META, DESTROY, PRINT, DOWNLOAD
     */
    private String action;

    private String resourceType;

    private String resourceId;

    private String operationResult;

    private String riskLevel;

    private String details;

    /**
     * 操作前数据快照 (对应 before_value)
     */
    private String dataBefore;

    /**
     * 操作后数据快照 (对应 after_value)
     */
    private String dataAfter;

    private String sessionId;

    /**
     * 客户端IP地址 (对应 client_ip) - 必填
     */
    @TableField("ip_address")
    private String clientIp;

    /**
     * MAC地址 (对应 mac_address) - 必填,无法获取时存储 'UNKNOWN'
     */
    private String macAddress;

    /**
     * 被操作对象的哈希值 (对应 object_digest)
     */
    private String objectDigest;

    private String userAgent;

    /**
     * 前一条日志的 SM3 哈希值
     * 用于构建日志链，确保日志不可篡改
     */
    private String prevLogHash;

    /**
     * 当前日志的 SM3 哈希值
     * 计算方式: SM3(operatorId + operationType + objectDigest + createdTime + prevLogHash)
     */
    private String logHash;

    /**
     * 设备指纹
     * 用于标识客户端设备
     */
    private String deviceFingerprint;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
