// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/mapper/StandardScenario.java
// Input: ApiIntent
// Output: StandardScenario
// Pos: AI 模块 - 业务语义映射器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.mapper;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

/**
 * 系统标准业务场景
 * <p>
 * 定义系统支持的标准业务场景
 * </p>
 */
@Getter
public enum StandardScenario {

    VOUCHER_SYNC("voucherSync", "记账凭证同步",
                 ApiIntent.OperationType.QUERY,
                 ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    INVOICE_SYNC("invoiceSync", "发票同步",
                 ApiIntent.OperationType.QUERY,
                 ApiIntent.BusinessObject.INVOICE),

    RECEIPT_SYNC("receiptSync", "收据同步",
                 ApiIntent.OperationType.QUERY,
                 ApiIntent.BusinessObject.RECEIPT),

    ATTACHMENT_SYNC("attachmentSync", "附件同步",
                     ApiIntent.OperationType.QUERY,
                     ApiIntent.BusinessObject.ATTACHMENT),

    SALES_OUT_SYNC("salesOutSync", "销售出库同步",
                    ApiIntent.OperationType.QUERY,
                    ApiIntent.BusinessObject.SALES_OUT),

    VOUCHER_WEBHOOK("voucherWebhook", "凭证推送",
                     ApiIntent.OperationType.CALLBACK,
                     ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    ARCHIVAL_FEEDBACK("archivalFeedback", "归档状态回写",
                       ApiIntent.OperationType.SUBMIT,
                       ApiIntent.BusinessObject.ACCOUNTING_VOUCHER),

    ACCOUNT_QUERY("accountQuery", "科目查询",
                   ApiIntent.OperationType.QUERY,
                   ApiIntent.BusinessObject.ACCOUNT_BALANCE),

    REFUND_FILE_SYNC("refundFileSync", "付款退款单文件获取",
                      ApiIntent.OperationType.QUERY,
                      ApiIntent.BusinessObject.REFUND),

    UNKNOWN("unknown", "未知场景",
            ApiIntent.OperationType.QUERY,
            ApiIntent.BusinessObject.UNKNOWN);

    private final String code;
    private final String description;
    private final ApiIntent.OperationType operationType;
    private final ApiIntent.BusinessObject businessObject;

    StandardScenario(String code, String description,
                    ApiIntent.OperationType operationType,
                    ApiIntent.BusinessObject businessObject) {
        this.code = code;
        this.description = description;
        this.operationType = operationType;
        this.businessObject = businessObject;
    }

    /**
     * 根据 API 意图匹配标准场景
     * <p>
     * 匹配规则：
     * 1. 业务对象必须匹配
     * 2. 操作类型兼容（QUERY 和 SYNC 互相兼容，因为同步通常也是查询）
     * </p>
     */
    public static StandardScenario fromIntent(ApiIntent intent) {
        // 简单的规则匹配（MVP 版本）

        for (StandardScenario scenario : values()) {
            if (scenario == UNKNOWN) continue;

            // 业务对象必须匹配
            if (scenario.businessObject != intent.getBusinessObject()) {
                continue;
            }

            // 操作类型匹配
            // QUERY 和 SYNC 互相兼容（同步通常也是查询操作）
            boolean operationMatches = scenario.operationType == intent.getOperationType()
                || (scenario.operationType == ApiIntent.OperationType.QUERY && intent.getOperationType() == ApiIntent.OperationType.SYNC)
                || (scenario.operationType == ApiIntent.OperationType.SYNC && intent.getOperationType() == ApiIntent.OperationType.QUERY);

            if (operationMatches) {
                return scenario;
            }
        }

        return UNKNOWN;
    }
}
