// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: DestructionLog 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 销毁清册实体
 * 对应表: destruction_log
 * 
 * 特性：
 * - 永久只读，禁止修改/删除（数据库触发器保护）
 * - 支持哈希链验真
 */
@Data
@TableName("destruction_log")
public class DestructionLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 归档年度
     */
    private Integer archiveYear;

    /**
     * 档案ID
     */
    private String archiveObjectId;

    /**
     * 保管期限ID
     */
    private String retentionPolicyId;

    /**
     * 审批票据ID（销毁申请ID）
     */
    private String approvalTicketId;

    /**
     * 执行人ID
     */
    private String destroyedBy;

    /**
     * 销毁时间
     */
    private LocalDateTime destroyedAt;

    /**
     * 追踪ID（用于审计）
     */
    private String traceId;

    /**
     * 档案元数据快照（JSON格式）
     */
    private String snapshot;

    /**
     * 前一条记录的哈希值（哈希链）
     */
    private String prevHash;

    /**
     * 当前记录的哈希值（哈希链）
     */
    private String currHash;

    /**
     * 数字签名（可选）
     */
    private String sig;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}



