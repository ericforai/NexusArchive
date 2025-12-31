// Input: Java 标准库
// Output: ArchiveSearchResult DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 档案检索结果 DTO
 */
@Data
public class ArchiveSearchResult {
    
    /**
     * 档案ID
     */
    private String id;
    
    /**
     * 档号
     */
    private String archiveCode;
    
    /**
     * 题名
     */
    private String title;
    
    /**
     * 摘要（脱敏后）
     */
    private String summary;
    
    /**
     * 金额
     */
    private BigDecimal amount;
    
    /**
     * 日期
     */
    private LocalDate docDate;
    
    /**
     * 对方单位
     */
    private String counterparty;
    
    /**
     * 凭证号
     */
    private String voucherNo;
    
    /**
     * 发票号
     */
    private String invoiceNo;
    
    /**
     * 归档年度
     */
    private Integer archiveYear;
    
    /**
     * 档案类型
     */
    private String docType;
    
    /**
     * 全宗号
     */
    private String fondsNo;
    
    /**
     * 是否脱敏
     */
    private Boolean isMasked;
}

