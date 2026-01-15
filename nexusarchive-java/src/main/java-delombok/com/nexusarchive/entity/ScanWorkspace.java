// Input: MyBatis-Plus、Lombok、Java 标准库
// Output: ScanWorkspace 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 扫描工作区实体
 *
 * 对应表: scan_workspace
 * 用于存储扫描文档的临时工作区数据，支持OCR识别、人工审核、提交归档等流程
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("scan_workspace")
public class ScanWorkspace implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID（用于批量操作）
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 上传来源（scan/upload/folder）
     */
    private String uploadSource;

    /**
     * OCR识别状态
     * pending: 待识别
     * processing: 识别中
     * review: 待审核
     * completed: 已完成
     * failed: 识别失败
     */
    private String ocrStatus = "pending";

    /**
     * OCR引擎类型（tesseract/paddle/百度）
     */
    private String ocrEngine;

    /**
     * OCR识别结果（JSON字符串）
     */
    private String ocrResult;

    /**
     * 综合评分（0-100）
     */
    private Integer overallScore;

    /**
     * 文档类型（凭证/发票/合同等）
     */
    private String docType;

    /**
     * 提交状态
     * draft: 草稿
     * submitted: 已提交
     */
    private String submitStatus = "draft";

    /**
     * 关联的档案ID（提交后生成）
     */
    private String archiveId;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

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
