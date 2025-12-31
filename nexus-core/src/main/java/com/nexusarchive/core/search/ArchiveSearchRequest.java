// Input: 检索条件
// Output: 搜索请求 DTO
// Pos: NexusCore search
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 档案高级检索请求
 */
@Data
public class ArchiveSearchRequest {
    private String fondsNo;
    private Integer archiveYear;
    
    // 结构化检索字段 (利用 BTree 索引)
    private BigDecimal amountFrom;
    private BigDecimal amountTo;
    
    private LocalDate dateFrom;
    private LocalDate dateTo;
    
    private String counterparty;    // 支持模糊
    private String voucherNo;       // 精确
    private String invoiceNo;       // 精确
    
    // 全文检索 (兼容旧模式)
    private String keyword;
}
