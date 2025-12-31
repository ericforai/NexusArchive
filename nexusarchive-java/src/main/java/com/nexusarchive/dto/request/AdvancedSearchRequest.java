// Input: Java 标准库
// Output: AdvancedSearchRequest DTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 高级检索请求 DTO
 * 
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
@Data
public class AdvancedSearchRequest {
    
    /**
     * 全宗号（可选，不提供则查询用户授权范围内的所有全宗）
     */
    private String fondsNo;
    
    /**
     * 档案类型
     */
    private String docType;
    
    /**
     * 关键字（搜索标题、摘要等）
     */
    private String keyword;
    
    /**
     * 摘要搜索
     */
    private String summary;
    
    /**
     * 金额范围 - 最小值
     */
    private BigDecimal minAmount;
    
    /**
     * 金额范围 - 最大值
     */
    private BigDecimal maxAmount;
    
    /**
     * 日期范围 - 开始日期
     */
    private LocalDate startDate;
    
    /**
     * 日期范围 - 结束日期
     */
    private LocalDate endDate;
    
    /**
     * 归档年度
     */
    private Integer archiveYear;
    
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
     * 页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}

