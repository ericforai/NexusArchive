// Input: Lombok、Java 标准库、Destruction Entity
// Output: DestructionResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销毁申请响应 DTO
 * <p>
 * 用于 Controller 返回销毁申请信息，避免直接暴露 Destruction Entity
 * </p>
 */
@Data
public class DestructionResponse {

    /**
     * 销毁申请ID
     */
    private String id;

    /**
     * 申请标题
     */
    private String title;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 销毁档案数量
     */
    private Integer archiveCount;

    /**
     * 销毁原因
     */
    private String reason;

    /**
     * 状态: PENDING, APPROVED, REJECTED, EXECUTING, COMPLETED, CANCELLED
     */
    private String status;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 审批人ID
     */
    private String approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批时间
     */
    private LocalDateTime approveTime;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 执行人ID
     */
    private String executorId;

    /**
     * 执行人姓名
     */
    private String executorName;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 计划销毁日期
     */
    private LocalDate plannedDestructionDate;

    /**
     * 实际销毁日期
     */
    private LocalDate actualDestructionDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
