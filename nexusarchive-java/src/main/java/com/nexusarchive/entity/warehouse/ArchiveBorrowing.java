// Input: MyBatis-Plus 注解、Lombok、JPA 规范
// Output: 实物借阅实体类
// Pos: src/main/java/com/nexusarchive/entity/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity.warehouse;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实物借阅实体类
 *
 * 核心职责：
 * 1. 管理实物档案的借阅和归还
 * 2. 记录借阅人、部门、时间
 * 3. 支持借阅审批流程
 * 4. 逾期跟踪和提醒
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("archives_borrowing")
public class ArchiveBorrowing implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 借阅单号
     * 格式：BW-YYYY-NNN
     */
    @TableField("borrow_no")
    private String borrowNo;

    /**
     * 档案袋ID
     */
    @TableField("container_id")
    private Long containerId;

    /**
     * 借阅人
     */
    @TableField("borrower")
    private String borrower;

    /**
     * 借阅部门
     */
    @TableField("borrower_dept")
    private String borrowerDept;

    /**
     * 借阅日期
     */
    @TableField("borrow_date")
    private LocalDate borrowDate;

    /**
     * 预计归还日期
     */
    @TableField("expected_return_date")
    private LocalDate expectedReturnDate;

    /**
     * 借阅状态
     * borrowed - 已借出
     * returned - 已归还
     * overdue - 逾期未还
     */
    @TableField("status")
    private String status;

    /**
     * 实际归还日期
     */
    @TableField("actual_return_date")
    private LocalDate actualReturnDate;

    /**
     * 审批人ID
     */
    @TableField("approved_by")
    private Long approvedBy;

    /**
     * 审批时间
     */
    @TableField("approved_at")
    private LocalDateTime approvedAt;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 所属全宗ID
     */
    @TableField("fonds_id")
    private Long fondsId;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
