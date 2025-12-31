// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: Borrowing 类
// Pos: borrowing/domain
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅申请实体
 * 对应表: biz_borrowing
 */
@Data
@TableName("biz_borrowing")
public class Borrowing {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 申请人ID
     */
    private String userId;

    /**
     * 申请人姓名
     */
    private String userName;

    /**
     * 借阅档案ID
     */
    private String archiveId;

    /**
     * 档案题名 (冗余字段，方便查询)
     */
    private String archiveTitle;

    /**
     * 借阅原因
     */
    private String reason;

    /**
     * 借阅日期
     */
    private LocalDate borrowDate;

    /**
     * 预计归还日期
     */
    private LocalDate expectedReturnDate;

    /**
     * 实际归还日期
     */
    private LocalDate actualReturnDate;

    /**
     * 状态: PENDING(待审批), APPROVED(已通过/借阅中), REJECTED(已拒绝), RETURNED(已归还), CANCELLED(已取消)
     */
    private String status;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 全宗号 (冗余字段，方便查询)
     */
    private String fondsNo;

    /**
     * 档案年度 (冗余字段，方便查询)
     */
    private Integer archiveYear;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    private Integer deleted;
}
