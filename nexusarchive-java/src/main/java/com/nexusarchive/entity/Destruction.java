// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Destruction 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 销毁申请实体
 * 对应表: biz_destruction
 */
@Data
@TableName("biz_destruction")
public class Destruction {

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
     * 销毁原因
     */
    private String reason;

    /**
     * 待销毁档案数量
     */
    private Integer archiveCount;

    /**
     * 待销毁档案ID列表 (JSON数组)
     */
    private String archiveIds;

    /**
     * 状态: PENDING(待审批), APPROVED(已批准), REJECTED(已拒绝), EXECUTED(已执行)
     */
    private String status;

    /**
     * 审批人ID
     */
    private String approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 审批链快照 (JSON)
     * 存储审批流程中的所有审批记录
     */
    private String approvalSnapshot;

    /**
     * 执行时间
     */
    private LocalDateTime executionTime;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
