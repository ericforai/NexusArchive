// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: LegacyImportTask 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历史数据导入任务表
 * 对应表: legacy_import_task
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Data
@TableName("legacy_import_task")
public class LegacyImportTask {

    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 操作人ID
     */
    private String operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 总行数
     */
    private Integer totalRows;

    /**
     * 成功行数
     */
    private Integer successRows;

    /**
     * 失败行数
     */
    private Integer failedRows;

    /**
     * 状态: PENDING, PROCESSING, SUCCESS, FAILED, PARTIAL_SUCCESS
     */
    private String status;

    /**
     * 错误报告文件路径
     */
    private String errorReportPath;

    /**
     * 自动创建的全宗号列表（JSON 数组）
     */
    private String createdFondsNos;

    /**
     * 自动创建的实体ID列表（JSON 数组）
     */
    private String createdEntityIds;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}


