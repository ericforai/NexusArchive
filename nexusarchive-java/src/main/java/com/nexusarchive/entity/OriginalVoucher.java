// Input: MyBatis-Plus、Jakarta EE、Lombok、Java 标准库
// Output: OriginalVoucher 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 原始凭证实体
 * 对应表: arc_original_voucher
 * 
 * 设计理念：原始凭证独立于记账凭证，是经济业务真实发生的证明
 * Reference: DA/T 94-2022, GB/T 39362-2020
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_original_voucher")
public class OriginalVoucher {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 原始凭证编号 (系统生成)
     * 格式: OV-{年度}-{类型简码}-{序号}
     */
    private String voucherNo;

    @NotBlank(message = "档案门类不能为空")
    @TableField("archival_category")
    private String voucherCategory;

    /**
     * 数据来源类型: API_SYNC / MANUAL_UPLOAD
     */
    @Builder.Default
    private String sourceType = "MANUAL_UPLOAD";

    /**
     * 二级类型: INV_PAPER/INV_VAT_E/BANK_RECEIPT/...
     */
    private String voucherType;

    /**
     * 业务发生日期
     */
    private LocalDate businessDate;

    /**
     * 金额 (使用 BigDecimal 确保精度)
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    @Builder.Default
    private String currency = "CNY";

    /**
     * 对方单位
     */
    private String counterparty;

    /**
     * 摘要/说明
     */
    private String summary;

    // ===== 责任人链 (GB/T 39362 M32-M35) =====

    /**
     * 制单人/经办人
     */
    private String creator;

    /**
     * 审核人
     */
    private String auditor;

    /**
     * 记账人
     */
    private String bookkeeper;

    /**
     * 会计主管/审批人
     */
    private String approver;

    // ===== 来源追溯 =====

    /**
     * 来源系统 (ERP/税控/银行)
     */
    private String sourceSystem;

    /**
     * 来源系统单据ID (幂等控制)
     */
    private String sourceDocId;

    // ===== 归档信息 =====

    /**
     * 全宗号
     */
    private String fondsCode;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 保管期限: 10Y/30Y/PERMANENT
     */
    private String retentionPeriod;

    /**
     * 归档状态: DRAFT/PENDING/ARCHIVED/FROZEN
     */
    @Builder.Default
    private String archiveStatus = "DRAFT";

    /**
     * 归档时间
     */
    private LocalDateTime archivedTime;

    // ===== 版本控制 =====

    /**
     * 版本号
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * 前版本ID (形成版本链)
     */
    private String parentVersionId;

    /**
     * 变更原因
     */
    private String versionReason;

    /**
     * 是否最新版本
     */
    @Builder.Default
    private Boolean isLatest = true;

    // ===== 审计字段 =====

    private String createdBy;

    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private String lastModifiedBy;

    @TableField(value = "last_modified_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastModifiedTime;

    @TableLogic
    @Builder.Default
    private Integer deleted = 0;
}
