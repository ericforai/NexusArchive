package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ERP 财务科目汇总 DTO (用于三位一体核对)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDTO {

    /** 科目代码 */
    private String subjectCode;

    /** 科目名称 */
    private String subjectName;

    /** 借方发生额合计 */
    private BigDecimal debitTotal;

    /** 贷方发生额合计 */
    private BigDecimal creditTotal;

    /** 凭证笔数 */
    private Integer voucherCount;

    /** 币种 */
    private String currency;
}
