package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体（历史归档）
 */
@Data
@TableName("acc_borrow_log")
public class BorrowLog {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅单号
     */
    private String requestNo;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 借阅目的
     */
    private String purpose;

    /**
     * 借阅类型
     */
    private String borrowType;

    /**
     * 借阅开始日期
     */
    private LocalDate borrowStartDate;

    /**
     * 借阅结束日期
     */
    private LocalDate borrowEndDate;

    /**
     * 档案数量
     */
    private Integer archiveCount;

    /**
     * 最终状态: COMPLETED, CANCELLED
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;
}
