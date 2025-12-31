// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: AccessReview 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问权限复核记录实体
 * 对应表: access_review
 */
@Data
@TableName("access_review")
public class AccessReview {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 复核类型: PERIODIC(定期), AD_HOC(临时), ON_DEMAND(按需)
     */
    private String reviewType;

    /**
     * 复核日期
     */
    private LocalDate reviewDate;

    /**
     * 复核人ID
     */
    private String reviewerId;

    /**
     * 状态: PENDING, APPROVED, REJECTED
     */
    private String status;

    /**
     * 当前角色列表（JSON格式）
     */
    private String currentRoles;

    /**
     * 当前权限列表（JSON格式）
     */
    private String currentPermissions;

    /**
     * 复核结果说明
     */
    private String reviewResult;

    /**
     * 采取的行动
     */
    private String actionTaken;

    /**
     * 下次复核日期
     */
    private LocalDate nextReviewDate;

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

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}

