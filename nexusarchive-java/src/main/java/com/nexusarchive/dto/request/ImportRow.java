// Input: Lombok、Java 标准库
// Output: ImportRow 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 导入行数据
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRow {
    /**
     * 行号（从1开始，包含表头）
     */
    private int rowNumber;
    
    /**
     * 原始数据（字段名 -> 原始值）
     */
    private Map<String, String> rawData;
    
    // ========== 必需字段 ==========
    
    /**
     * 全宗号
     */
    private String fondsNo;
    
    /**
     * 全宗名称
     */
    private String fondsName;
    
    /**
     * 归档年度
     */
    private Integer archiveYear;
    
    /**
     * 档案类型
     */
    private String docType;
    
    /**
     * 档案标题
     */
    private String title;
    
    /**
     * 保管期限名称
     */
    private String retentionPolicyName;
    
    // ========== 可选字段 ==========
    
    /**
     * 法人实体名称
     */
    private String entityName;
    
    /**
     * 统一社会信用代码
     */
    private String entityTaxCode;
    
    /**
     * 形成日期
     */
    private LocalDate docDate;
    
    /**
     * 金额
     */
    private BigDecimal amount;
    
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
     * 扩展元数据（JSON String）
     */
    private String customMetadata;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件哈希值
     */
    private String fileHash;
    
    /**
     * 获取字段值（支持映射）
     */
    public String getFieldValue(String fieldName) {
        if (rawData == null) {
            return null;
        }
        return rawData.get(fieldName);
    }
}

