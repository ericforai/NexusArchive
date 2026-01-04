// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: FondsHistory 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 全宗沿革实体
 * 对应表: fonds_history
 * 
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
@Data
@TableName("fonds_history")
public class FondsHistory {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号（当前全宗）
     */
    private String fondsNo;

    /**
     * 事件类型: MERGE(合并), SPLIT(分立), MIGRATE(迁移), RENAME(重命名)
     */
    private String eventType;

    /**
     * 源全宗号（用于合并/迁移场景）
     */
    private String fromFondsNo;

    /**
     * 目标全宗号（用于迁移场景）
     */
    private String toFondsNo;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 审批票据ID（关联审批流程）
     */
    private String approvalTicketId;

    /**
     * 变更时的快照信息（JSON格式）
     */
    private String snapshotJson;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}



