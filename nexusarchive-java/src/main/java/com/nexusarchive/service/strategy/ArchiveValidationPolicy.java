package com.nexusarchive.service.strategy;

import java.util.Set;

/**
 * Policy class for validating Archive categories and types.
 * Separated from ArchiveService to adhere to Single Responsibility Principle.
 */
public class ArchiveValidationPolicy {

    /**
     * Checks if the given subType is valid for the provided categoryCode.
     *
     * @param subType      The sub-type to validate (e.g., "BANK_SLIP").
     * @param categoryCode The category code (e.g., "AC01").
     * @return true if valid, false otherwise.
     */
    public static boolean isValidSubType(String subType, String categoryCode) {
        if (categoryCode == null || categoryCode.isEmpty()) {
            // 如果类别代码为空，暂不强制校验子类型（或者可以根据业务需要决定是否拒绝）
            return true;
        }

        if ("AC01".equals(categoryCode)) {
            // 会计凭证子类型白名单
            return Set.of(
                    "ACCOUNTING_VOUCHER", "ORIGINAL_VOUCHER",
                    // 原始凭证細分类型
                    "SALES_ORDER", "DELIVERY_ORDER", "PURCHASE_ORDER", "RECEIPT_ORDER",
                    "PAYMENT_REQ", "EXPENSE_REPORT", "GEN_INVOICE", "VAT_INVOICE",
                    "BANK_RECEIPT", "BANK_SLIP", "BANK_STATEMENT", "CONTRACT"
            ).contains(subType);
        } else if ("AC02".equals(categoryCode)) {
            // 账簿类型白名单 (Updated V71+)
            return Set.of("GENERAL_LEDGER", "SUBSIDIARY_LEDGER", "JOURNAL",
                    "CASH_BOOK", "BANK_BOOK",
                    // Added from frontend paths
                    "CASH_JOURNAL", "BANK_JOURNAL", "FIXED_ASSETS_CARD", "OTHER_BOOKS"
            ).contains(subType);
        } else if ("AC03".equals(categoryCode)) {
            // 报表周期白名单
            return Set.of("MONTHLY", "QUARTERLY", "ANNUAL", "SEMI_ANNUAL", "SPECIAL").contains(subType);
        } else if ("AC04".equals(categoryCode)) {
            // 其他类型白名单
            return Set.of("CONTRACT", "INVOICE", "RECEIPT", "OTHER",
                    // Added from frontend paths
                    "BANK_RECONCILIATION", "TAX_RETURN",
                    "HANDOVER_REGISTER", "CUSTODY_REGISTER", "DESTRUCTION_REGISTER",
                    "APPRAISAL_OPINION"
            ).contains(subType);
        }
        // 未知分类，拒绝
        return false;
    }
}
