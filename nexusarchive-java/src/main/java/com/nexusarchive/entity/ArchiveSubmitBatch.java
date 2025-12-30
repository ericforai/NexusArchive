// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchiveSubmitBatch 归档提交批次实体
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 归档提交批次实体
 *
 * 管理从预归档库到正式档案库的批量归档流程。
 * 这是归档工作流的核心控制单元。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "archive_batch", autoResultMap = true)
public class ArchiveSubmitBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次编号，如 AB-2024-001
     */
    private String batchNo;

    /**
     * 全宗 ID（公司/组织）
     */
    private Long fondsId;

    /**
     * 期间起始日期
     */
    private LocalDate periodStart;

    /**
     * 期间结束日期
     */
    private LocalDate periodEnd;

    /**
     * 范围类型
     * PERIOD: 按期间
     * CUSTOM: 自定义范围
     */
    private String scopeType;

    /**
     * 批次状态
     * PENDING: 待提交
     * VALIDATING: 校验中
     * APPROVED: 已审批
     * ARCHIVED: 已归档
     * REJECTED: 已驳回
     * FAILED: 失败
     */
    private String status;

    /**
     * 凭证数量
     */
    private Integer voucherCount;

    /**
     * 单据数量
     */
    private Integer docCount;

    /**
     * 文件数量
     */
    private Integer fileCount;

    /**
     * 总大小（字节）
     */
    private Long totalSizeBytes;

    /**
     * 归档前校验报告（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationReport;

    /**
     * 四性检测报告（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> integrityReport;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 提交人 ID
     */
    private Long submittedBy;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 审批人 ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 归档执行时间
     */
    private LocalDateTime archivedAt;

    /**
     * 归档执行人 ID
     */
    private Long archivedBy;

    /**
     * 创建人 ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    // ========== 状态常量 ==========

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String SCOPE_PERIOD = "PERIOD";
    public static final String SCOPE_CUSTOM = "CUSTOM";

    // ========== 便捷方法 ==========

    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    public boolean isArchived() {
        return STATUS_ARCHIVED.equals(status);
    }

    public boolean canSubmit() {
        return STATUS_PENDING.equals(status);
    }

    public boolean canApprove() {
        return STATUS_VALIDATING.equals(status);
    }

    public boolean canArchive() {
        return STATUS_APPROVED.equals(status);
    }
}
