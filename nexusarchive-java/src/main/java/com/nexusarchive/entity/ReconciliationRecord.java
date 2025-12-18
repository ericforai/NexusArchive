package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 账、凭、证三位一体核对记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "arc_reconciliation_record", autoResultMap = true)
public class ReconciliationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 全宗号 */
    private String fondsCode;

    /** 财务年度 */
    private String fiscalYear;

    /** 财务期间 */
    private String fiscalPeriod;

    /** 科目代码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** ERP 侧借方合计 */
    private BigDecimal erpDebitTotal;

    /** ERP 侧贷方合计 */
    private BigDecimal erpCreditTotal;

    /** ERP 侧凭证笔数 */
    private Integer erpVoucherCount;

    /** 档案系统侧借方合计 */
    private BigDecimal arcDebitTotal;

    /** 档案系统侧贷方合计 */
    private BigDecimal arcCreditTotal;

    /** 档案系统侧凭证笔数 */
    private Integer arcVoucherCount;

    /** 附件总数 (证) */
    private Integer attachmentCount;

    /** 附件缺失笔数 */
    private Integer attachmentMissingCount;

    /** 核对状态: SUCCESS, DISCREPANCY, ERROR */
    private String reconStatus;

    /** 差异说明 */
    private String reconMessage;

    /** 核对执行时间 */
    private LocalDateTime reconTime;

    /** 操作人ID */
    private String operatorId;

    /** 来源系统名称 */
    private String sourceSystem;

    /** 核对快照数据 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> snapshotData;
}
