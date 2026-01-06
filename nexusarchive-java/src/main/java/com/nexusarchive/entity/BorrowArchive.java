package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 借阅档案明细实体
 */
@Data
@TableName("acc_borrow_archive")
public class BorrowArchive {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 借阅申请ID
     */
    private String borrowRequestId;

    /**
     * 档案ID
     */
    private String archiveId;

    /**
     * 档号
     */
    private String archiveCode;

    /**
     * 题名
     */
    private String archiveTitle;

    /**
     * 归还状态: BORROWED, RETURNED
     */
    private ReturnStatus returnStatus = ReturnStatus.BORROWED;

    /**
     * 归还时间
     */
    private LocalDateTime returnTime;

    /**
     * 归还操作人ID
     */
    private String returnOperatorId;

    /**
     * 是否损坏
     */
    private Boolean damaged = false;

    /**
     * 损坏描述
     */
    private String damageDesc;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 归还状态枚举
     */
    public enum ReturnStatus {
        BORROWED("借阅中"),
        RETURNED("已归还");

        private final String label;

        ReturnStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
