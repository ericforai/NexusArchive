// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ArchiveBatchItem 归档批次条目实体
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
 * 归档批次条目实体
 *
 * 记录批次中包含的凭证和单据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "archive_batch_item", autoResultMap = true)
public class ArchiveBatchItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 批次 ID
     */
    private Long batchId;

    /**
     * 条目类型
     * VOUCHER: 记账凭证
     * SOURCE_DOC: 原始单据
     */
    private String itemType;

    /**
     * 引用 ID
     * itemType=VOUCHER 时指向 arc_file_content.id
     * itemType=SOURCE_DOC 时指向 original_voucher.id
     */
    private Long refId;

    /**
     * 引用编号（凭证号/单据号）
     */
    private String refNo;

    /**
     * 条目状态
     * PENDING: 待校验
     * VALIDATED: 已校验
     * ARCHIVED: 已归档
     * FAILED: 校验失败
     */
    private String status;

    /**
     * 校验结果（JSON）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> validationResult;

    /**
     * 归档时计算的 SM3 哈希
     */
    private String hashSm3;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    // ========== 常量 ==========

    public static final String TYPE_VOUCHER = "VOUCHER";
    public static final String TYPE_SOURCE_DOC = "SOURCE_DOC";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    public static final String STATUS_FAILED = "FAILED";
}
