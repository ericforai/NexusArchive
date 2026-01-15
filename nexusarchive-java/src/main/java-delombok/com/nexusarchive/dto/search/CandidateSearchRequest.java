// Input: Lombok
// Output: CandidateSearchRequest DTO
// Pos: 数据传输对象层 (搜索请求)
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.search;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CandidateSearchRequest {
    /**
     * Fuzzy search keyword (Filename, Invoice No, Summary, Seller)
     */
    private String keyword;

    /**
     * Minimum Amount (inclusive)
     */
    private BigDecimal minAmount;

    /**
     * Maximum Amount (inclusive)
     */
    private BigDecimal maxAmount;

    /**
     * Start Date (inclusive)
     */
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * Exact Invoice Code
     */
    private String invoiceCode;

    /**
     * Exact Invoice Number
     */
    private String invoiceNumber;

    /**
     * Whether to exclude already linked files (default: true)
     */
    private Boolean excludeLinked = true;
}
