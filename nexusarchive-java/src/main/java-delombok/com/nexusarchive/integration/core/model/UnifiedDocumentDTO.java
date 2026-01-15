// Input: Lombok、Java 标准库
// Output: UnifiedDocumentDTO 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 统一文档模型 DTO
 * 用于规范化不同 ERP 系统返回的单据数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDocumentDTO {

    /**
     * 源系统单据ID
     */
    private String sourceId;

    /**
     * 业务单据号（用户可读）
     */
    private String businessCode;

    /**
     * 会计期间（如 2025-10）
     */
    private String period;

    /**
     * 单据日期
     */
    private LocalDate docDate;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 单据类型
     */
    private DocumentType type;

    /**
     * 原始 JSON 数据
     */
    private String originalJson;

    /**
     * 扩展元数据
     */
    private Map<String, Object> metadata;

    /**
     * 单据类型枚举
     */
    public enum DocumentType {
        VOUCHER,      // 会计凭证
        PAYMENT,      // 付款单
        RECEIPT,      // 收款单
        INVOICE,      // 发票
        OTHER         // 其他
    }
}
