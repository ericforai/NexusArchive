// Input: Lombok、Java 标准库
// Output: ErpVoucherDto 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 标准化 ERP 凭证模型
 */
@Data
public class ErpVoucherDto {
    private String externalId;     // 外部系统唯一ID
    private String voucherCode;    // 凭证号 (如: 记-001)
    private LocalDate voucherDate; // 凭证日期
    private String orgCode;        // 组织编码
    private String orgName;        // 组织名称
    private String preparedBy;     // 制单人
    private String checkedBy;      // 审核人
    private String postedBy;       // 记账人
    private BigDecimal totalAmount;// 总金额
    private String voucherType;    // 凭证类型 (记账凭证/银收/银付等)
    private int attachmentCount;   // 附件张数
    
    private List<ErpEntryDto> entries; // 分录列表

    @Data
    public static class ErpEntryDto {
        private int lineNo;             // 行号
        private String summary;         // 摘要
        private String accountCode;     // 科目编码
        private String accountName;     // 科目名称
        private String currency;        // 币种
        private BigDecimal debitAmount; // 借方金额
        private BigDecimal creditAmount;// 贷方金额
        private String direction;       // 余额方向 (DEBIT/CREDIT)
    }
}
