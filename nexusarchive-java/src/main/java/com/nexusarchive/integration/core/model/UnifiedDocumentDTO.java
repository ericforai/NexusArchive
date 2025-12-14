package com.nexusarchive.integration.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统一归档文档模型 (Normalized Document Model)
 * 用于屏蔽不同来源系统(ERP/OA)的数据结构差异
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDocumentDTO {

    /**
     * 来源系统原始ID
     */
    private String sourceId;

    /**
     * 业务单据号 (用户可见, 如 记-001)
     */
    private String businessCode;

    /**
     * 单据日期
     */
    private LocalDate docDate;

    /**
     * 会计期间 (如 2024-01)
     */
    private String period;

    /**
     * 涉及总金额
     */
    private BigDecimal amount;

    /**
     * 文档类型
     * VOUCHER: 凭证
     * INVOICE: 发票
     * BANK_SLIP: 银行回单
     * OTHER: 其他
     */
    private DocumentType type;

    /**
     * 原始数据JSON (用于生成版式文件或留存审计)
     */
    private String originalJson;

    /**
     * 扩展元数据 (Map<String, Object>)
     * 用于存储各系统特有的字段，如制单人、科目信息等
     */
    private Map<String, Object> metadata;

    /**
     * 附件列表
     */
    private List<FileAttachmentDTO> attachments;

    public enum DocumentType {
        VOUCHER,
        INVOICE,
        BANK_SLIP,
        OTHER
    }
}
