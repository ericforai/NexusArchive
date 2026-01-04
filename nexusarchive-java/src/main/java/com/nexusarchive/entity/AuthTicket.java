// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: AuthTicket 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跨全宗访问授权票据实体
 * 对应表: auth_ticket
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
@Data
@TableName("auth_ticket")
public class AuthTicket {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 源全宗号（申请人所属全宗）
     */
    private String sourceFonds;

    /**
     * 目标全宗号
     */
    private String targetFonds;

    /**
     * 访问范围（JSON格式）
     * 格式: { "archiveYears": [2020, 2021], "docTypes": ["凭证"], "keywords": [], "accessType": "READ_ONLY" }
     */
    private String scope;

    /**
     * 有效期
     */
    private LocalDateTime expiresAt;

    /**
     * 状态: PENDING(待审批), FIRST_APPROVED(第一审批通过), APPROVED(已批准), 
     * REJECTED(已拒绝), REVOKED(已撤销), EXPIRED(已过期)
     */
    private String status;

    /**
     * 审批链快照（JSON格式）
     */
    private String approvalSnapshot;

    /**
     * 申请原因
     */
    private String reason;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 最后修改时间
     */
    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}



