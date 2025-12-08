package com.nexusarchive.common.constant;

/**
 * 错误码常量
 * Reference: DA/T 104-2024 接口规范
 */
public class ErrorCode {
    
    // ==================== SIP 接收相关错误 (EAA_1xxx) ====================
    
    /**
     * EAA_1001: 附件数量不匹配
     * Rule: attachment_count (in Header) MUST equal attachments.size()
     */
    public static final String EAA_1001_COUNT_MISMATCH = "EAA_1001";
    public static final String EAA_1001_MSG = "附件数量不匹配：header.attachment_count=%d, 实际附件数=%d";
    
    /**
     * EAA_1002: 借贷不平衡
     * Rule: The sum of entry_amount in entries MUST equal total_amount in the header
     */
    public static final String EAA_1002_BALANCE_ERROR = "EAA_1002";
    public static final String EAA_1002_MSG = "借贷不平衡：凭证总额=%.2f, 分录合计=%.2f, 差异=%.2f";
    
    /**
     * EAA_1003: 必填字段缺失
     * Reference: DA/T 94 Appendix A - Mandatory Metadata Fields
     */
    public static final String EAA_1003_MISSING_FIELD = "EAA_1003";
    public static final String EAA_1003_MSG = "必填字段缺失：%s (参考: DA/T 94-2022 附录A)";
    
    /**
     * EAA_1004: 凭证已归档
     * Idempotency Check: If Status = ARCHIVED, reject with 409
     */
    public static final String EAA_1004_ALREADY_ARCHIVED = "EAA_1004";
    public static final String EAA_1004_MSG = "凭证已归档，无法重复接收：archival_code=%s";
    
    /**
     * EAA_1005: 文件格式不支持
     */
    public static final String EAA_1005_INVALID_FILE_TYPE = "EAA_1005";
    public static final String EAA_1005_MSG = "文件格式不支持：%s，允许的格式：OFD, PDF, XML, JPG, PNG";
    
    /**
     * EAA_1006: Base64 解码失败
     */
    public static final String EAA_1006_BASE64_ERROR = "EAA_1006";
    public static final String EAA_1006_MSG = "附件 Base64 内容解码失败：%s";
    
    // ==================== 通用错误 (EAA_4xx / EAA_5xx) ====================
    
    /**
     * EAA_400: 请求参数错误
     */
    public static final String EAA_400 = "EAA_400";
    
    /**
     * EAA_500: 系统内部错误
     */
    public static final String EAA_500 = "EAA_500";
    
    private ErrorCode() {
        // 工具类，禁止实例化
    }
}
