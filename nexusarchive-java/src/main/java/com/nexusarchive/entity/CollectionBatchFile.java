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
 * 资料收集批次文件实体
 *
 * 记录批次内每个文件的上传状态和处理结果
 * 支持幂等性控制 (通过文件哈希去重)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("collection_batch_file")
public class CollectionBatchFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属批次ID
     */
    private Long batchId;

    /**
     * 关联的文件ID (上传成功后关联到 arc_file_content.id)
     */
    private String fileId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小(字节)
     */
    private Long fileSizeBytes;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 哈希算法
     */
    private String hashAlgorithm;

    /**
     * 上传状态
     */
    private String uploadStatus;

    /**
     * 处理结果(JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> processingResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 上传顺序
     */
    private Integer uploadOrder;

    /**
     * 开始时间
     */
    private LocalDateTime startedTime;

    /**
     * 完成时间
     */
    private LocalDateTime completedTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // ========== 状态常量 ==========

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_UPLOADED = "UPLOADED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DUPLICATE = "DUPLICATE";
    public static final String STATUS_VALIDATING = "VALIDATING";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_CHECK_FAILED = "CHECK_FAILED";

    // ========== 便捷方法 ==========

    public boolean isPending() {
        return STATUS_PENDING.equals(uploadStatus);
    }

    public boolean isUploaded() {
        return STATUS_UPLOADED.equals(uploadStatus);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(uploadStatus) ||
               STATUS_DUPLICATE.equals(uploadStatus) ||
               STATUS_CHECK_FAILED.equals(uploadStatus);
    }

    public boolean isCompleted() {
        return isUploaded() || isFailed();
    }

    public long getDurationMillis() {
        if (startedTime == null || completedTime == null) return 0;
        return java.time.Duration.between(startedTime, completedTime).toMillis();
    }
}
