// Input: Lombok、Java 标准库
// Output: OcrResult 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * OCR 识别结果 DTO
 *
 * 用于封装 OCR 引擎识别出的发票/凭证数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {

    /**
     * 识别是否成功
     */
    private boolean success;

    /**
     * 文档类型（invoice/voucher/receipt等）
     */
    private String docType;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * 开票日期
     */
    private LocalDate invoiceDate;

    /**
     * 金额（不含税）
     */
    private BigDecimal amount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    private BigDecimal totalAmount;

    /**
     * 销售方名称
     */
    private String sellerName;

    /**
     * 销售方税号
     */
    private String sellerTaxId;

    /**
     * 购买方名称
     */
    private String buyerName;

    /**
     * 购买方税号
     */
    private String buyerTaxId;

    /**
     * OCR 引擎
     */
    private String engine;

    /**
     * 置信度分数（0-100）
     */
    private Integer confidence;

    /**
     * 原始文本
     */
    private String rawText;

    /**
     * 额外识别的字段（键值对）
     */
    private Map<String, Object> extraFields;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 创建成功结果
     */
    public static OcrResult success(String docType, String invoiceNumber, LocalDate invoiceDate,
                                     BigDecimal amount, BigDecimal taxAmount, BigDecimal totalAmount,
                                     String sellerName, String sellerTaxId,
                                     String buyerName, String buyerTaxId,
                                     String engine, Integer confidence) {
        return OcrResult.builder()
                .success(true)
                .docType(docType)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(invoiceDate)
                .amount(amount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .sellerName(sellerName)
                .sellerTaxId(sellerTaxId)
                .buyerName(buyerName)
                .buyerTaxId(buyerTaxId)
                .engine(engine)
                .confidence(confidence)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static OcrResult failure(String errorMessage) {
        return OcrResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
