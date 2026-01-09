// Input: Lombok、Java 标准库、ArcFileContent Entity
// Output: ArchiveFileResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案文件响应 DTO
 * <p>
 * 用于 Controller 返回档案文件内容信息，避免直接暴露 ArcFileContent Entity
 * 不包含敏感字段如 timestampToken, signValue 等
 * </p>
 */
@Data
public class ArchiveFileResponse {

    /**
     * 文件ID
     */
    private String id;

    /**
     * 关联的档案号
     */
    private String archivalCode;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型 (PDF, OFD, JPG, etc.)
     */
    private String fileType;

    /**
     * 文件大小 (字节)
     */
    private Long fileSize;

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 哈希算法
     */
    private String hashAlgorithm;

    /**
     * 关联单据ID
     */
    private String itemId;

    /**
     * 原始哈希值
     */
    private String originalHash;

    /**
     * 当前哈希值
     */
    private String currentHash;

    /**
     * 预归档状态
     */
    private String preArchiveStatus;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 凭证类型
     */
    private String voucherType;

    /**
     * 创建人（已脱敏）
     */
    private String creator;

    /**
     * 全宗号
     */
    private String fondsCode;

    /**
     * 来源系统
     */
    private String sourceSystem;

    /**
     * 来源唯一标识
     */
    private String businessDocNo;

    /**
     * ERP原始凭证号
     */
    private String erpVoucherNo;

    /**
     * 归档批次ID
     */
    private Long batchId;

    /**
     * 批次内序号
     */
    private Integer sequenceInBatch;

    /**
     * 摘要（已脱敏）
     */
    private String summary;

    /**
     * 凭证字
     */
    private String voucherWord;

    /**
     * 单据日期
     */
    private String docDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 检测时间
     */
    private LocalDateTime checkedTime;

    /**
     * 归档时间
     */
    private LocalDateTime archivedTime;
}
