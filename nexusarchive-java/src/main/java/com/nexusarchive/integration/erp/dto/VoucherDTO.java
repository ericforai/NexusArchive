// Input: Lombok、Java 标准库
// Output: VoucherDTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 凭证 DTO
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {

    /**
     * 凭证ID (ERP 系统内)
     */
    private String voucherId;

    /**
     * 凭证编号
     */
    private String voucherNo;

    /**
     * 凭证字
     */
    private String voucherWord;

    /**
     * 凭证日期
     */
    private LocalDate voucherDate;

    /**
     * 会计期间 (例如: 2024-01)
     */
    private String accountPeriod;

    /**
     * 账套代码
     */
    private String accbookCode;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 借方合计
     */
    private BigDecimal debitTotal;

    /**
     * 贷方合计
     */
    private BigDecimal creditTotal;

    /**
     * 附件数量
     */
    private Integer attachmentCount;

    /**
     * 制单人
     */
    private String creator;

    /**
     * 审核人
     */
    private String auditor;

    /**
     * 记账人
     */
    private String poster;

    /**
     * 凭证状态
     */
    private String status;

    /**
     * 凭证分录列表
     */
    private List<VoucherEntryDTO> entries;

    /**
     * 附件列表
     */
    private List<AttachmentDTO> attachments;

    /**
     * 凭证分录 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherEntryDTO {
        private Integer lineNo;
        private String summary;
        private String accountCode;
        private String accountName;
        private BigDecimal debit;
        private BigDecimal credit;
    }
}
