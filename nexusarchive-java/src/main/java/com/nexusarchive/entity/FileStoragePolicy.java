// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: FileStoragePolicy 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件存储策略实体
 * 对应表: file_storage_policy
 */
@Data
@TableName("file_storage_policy")
public class FileStoragePolicy {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 策略类型: IMMUTABLE(不可变), RETENTION(保留策略)
     */
    private String policyType;

    /**
     * 保留天数（NULL表示永久保留）
     */
    private Integer retentionDays;

    /**
     * 不可变截止日期
     */
    private LocalDate immutableUntil;

    /**
     * 是否启用
     */
    private Boolean enabled;

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



