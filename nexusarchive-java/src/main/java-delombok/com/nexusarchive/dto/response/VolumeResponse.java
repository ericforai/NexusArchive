// Input: Lombok、Java 标准库、Volume Entity
// Output: VolumeResponse DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案卷响应 DTO
 * <p>
 * 用于 Controller 返回案卷信息，避免直接暴露 Volume Entity
 * </p>
 */
@Data
public class VolumeResponse {

    /**
     * 案卷ID
     */
    private String id;

    /**
     * 案卷编号
     */
    private String volumeCode;

    /**
     * 案卷标题
     */
    private String title;

    /**
     * 全宗号
     */
    private String fondsNo;

    /**
     * 年度
     */
    private String fiscalYear;

    /**
     * 期间
     */
    private String fiscalPeriod;

    /**
     * 保管期限
     */
    private String retentionPeriod;

    /**
     * 状态: DRAFT, PENDING_REVIEW, APPROVED, ARCHIVED, REJECTED
     */
    private String status;

    /**
     * 档案数量
     */
    private Integer archiveCount;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 审核人ID
     */
    private String reviewerId;

    /**
     * 审核人姓名
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 归档时间
     */
    private LocalDateTime archivedTime;

    /**
     * 移交时间
     */
    private LocalDateTime handoverTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * 保管期限起算日期
     */
    private LocalDate retentionStartDate;
}
