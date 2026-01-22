// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: CollectionBatch 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 资料收集批次实体
 *
 * 对应表: collection_batch
 * 符合 GB/T 39362-2020 电子会计档案管理规范
 * 管理批量上传会话的完整生命周期
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_batch")
public class CollectionBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次编号，格式: COL-YYYYMMDD-NNN
     */
    private String batchNo;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 全宗ID (VARCHAR(32) in database to match bas_fonds.id)
     */
    private String fondsId;

    /**
     * 全宗代码
     */
    private String fondsCode;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 会计期间
     */
    private String fiscalPeriod;

    /**
     * 档案门类: VOUCHER/LEDGER/REPORT/OTHER
     */
    private String archivalCategory;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 批次状态
     */
    private String status;

    /**
     * 总文件数
     */
    private Integer totalFiles;

    /**
     * 已上传文件数
     */
    private Integer uploadedFiles;

    /**
     * 失败文件数
     */
    private Integer failedFiles;

    /**
     * 总大小(字节)
     */
    private Long totalSizeBytes;

    /**
     * 校验报告(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationReport;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 修改时间
     */
    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    // ========== 状态常量 ==========

    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    public static final String CATEGORY_VOUCHER = "VOUCHER";
    public static final String CATEGORY_LEDGER = "LEDGER";
    public static final String CATEGORY_REPORT = "REPORT";
    public static final String CATEGORY_OTHER = "OTHER";

    // ========== 便捷方法 ==========

    public boolean isUploading() {
        return STATUS_UPLOADING.equals(status);
    }

    public boolean isUploaded() {
        return STATUS_UPLOADED.equals(status);
    }

    public boolean isCompleted() {
        return STATUS_VALIDATED.equals(status) || STATUS_ARCHIVED.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public boolean canUpload() {
        return STATUS_UPLOADING.equals(status);
    }

    public int getProgress() {
        if (totalFiles == null || totalFiles == 0) return 0;
        int uploaded = uploadedFiles != null ? uploadedFiles : 0;
        return (int) ((uploaded * 100L) / totalFiles);
    }
}
