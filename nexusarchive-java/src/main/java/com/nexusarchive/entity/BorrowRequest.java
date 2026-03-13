package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅申请实体
 */
@Data
@TableName("acc_borrow_request")
public class BorrowRequest {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅单号 (BL-YYYYMMDD-序号)
     */
    @NotBlank(message = "借阅单号不能为空")
    private String requestNo;

    /**
     * 申请人ID
     */
    private String applicantId;

    /**
     * 申请人姓名
     */
    @NotBlank(message = "申请人姓名不能为空")
    @Size(max = 100, message = "申请人姓名长度不能超过100")
    private String applicantName;

    /**
     * 申请部门ID
     */
    private String deptId;

    /**
     * 申请部门名称
     */
    @Size(max = 200, message = "部门名称长度不能超过200")
    private String deptName;

    /**
     * 借阅目的
     */
    @NotBlank(message = "借阅目的不能为空")
    @Size(max = 500, message = "借阅目的长度不能超过500")
    private String purpose;

    /**
     * 借阅类型: READING(阅览), COPY(复制), LOAN(外借)
     */
    @NotBlank(message = "借阅类型不能为空")
    private BorrowType borrowType;

    /**
     * 预期开始日期
     */
    @NotNull(message = "预期开始日期不能为空")
    private LocalDate expectedStartDate;

    /**
     * 预期结束日期
     */
    @NotNull(message = "预期结束日期不能为空")
    private LocalDate expectedEndDate;

    /**
     * 状态: PENDING, APPROVED, REJECTED, BORROWING, RETURNED, OVERDUE
     */
    @NotBlank(message = "状态不能为空")
    private BorrowStatus status = BorrowStatus.PENDING;

    /**
     * 借阅档案ID列表 (JSON数组字符串)
     */
    @NotBlank(message = "借阅档案不能为空")
    private String archiveIds;

    /**
     * 借阅档案数量
     */
    @NotNull(message = "借阅档案数量不能为空")
    private Integer archiveCount;

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
    private LocalDateTime approvalTime;

    /**
     * 审批意见
     */
    @Size(max = 500, message = "审批意见长度不能超过500")
    private String approvalComment;

    /**
     * 实际开始日期
     */
    private LocalDate actualStartDate;

    /**
     * 实际结束日期
     */
    private LocalDate actualEndDate;

    /**
     * 归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 归还操作人ID
     */
    private String returnOperatorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Boolean deleted;

    /**
     * 借阅类型枚举
     */
    public enum BorrowType {
        READING("阅览", "在档案室阅览，不可带出"),
        COPY("复制", "复制或扫描档案内容"),
        LOAN("外借", "经批准后可借出档案室");

        private final String label;
        private final String description;

        BorrowType(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 借阅状态枚举
     */
    public enum BorrowStatus {
        PENDING("待审批", "等待审批"),
        APPROVED("已批准", "审批通过，待借出"),
        REJECTED("已拒绝", "审批未通过"),
        BORROWING("借阅中", "档案已借出"),
        RETURNED("已归还", "档案已归还"),
        OVERDUE("逾期未还", "超过归还期限"),
        CANCELLED("已取消", "申请人取消");

        private final String label;
        private final String description;

        BorrowStatus(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }
}
