// Input: VoucherData, BusinessScene enum
// Output: BusinessSceneIdentifier
// Pos: Matching Engine
// 负责识别凭证的业务场景

package com.nexusarchive.engine.matching.identifier;

import com.nexusarchive.engine.matching.dto.BusinessAttributes;
import com.nexusarchive.engine.matching.dto.VoucherData;
import com.nexusarchive.engine.matching.enums.AccountRole;
import com.nexusarchive.engine.matching.enums.BusinessScene;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 业务场景识别器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>基于科目角色组合识别场景</li>
 *   <li>基于摘要关键词增强识别</li>
 *   <li>计算识别置信度</li>
 * </ul>
 */
@Component
@Slf4j
public class BusinessSceneIdentifier {

    private static final BigDecimal CONFIDENCE_HIGH = new BigDecimal("0.95");
    private static final BigDecimal CONFIDENCE_MEDIUM_HIGH = new BigDecimal("0.9");
    private static final BigDecimal CONFIDENCE_MEDIUM = new BigDecimal("0.85");
    private static final BigDecimal CONFIDENCE_LOW = new BigDecimal("0.7");

    /**
     * 识别业务场景
     *
     * @param voucher 凭证数据
     * @return 业务属性（包含场景、置信度和原因）
     */
    public BusinessAttributes identify(VoucherData voucher) {
        List<String> reasons = new ArrayList<>();
        BusinessScene scene = BusinessScene.UNKNOWN;
        BigDecimal confidence = BigDecimal.ZERO;

        Set<AccountRole> debitRoles = voucher.getDebitRoles();
        Set<AccountRole> creditRoles = voucher.getCreditRoles();
        String summary = voucher.getSummary() != null ? voucher.getSummary() : "";

        // 付款识别：贷方银行/现金，借方应付/费用
        if (creditRoles.contains(AccountRole.BANK) || creditRoles.contains(AccountRole.CASH)) {
            if (debitRoles.contains(AccountRole.PAYABLE)) {
                scene = BusinessScene.PAYMENT;
                confidence = CONFIDENCE_MEDIUM_HIGH;
                reasons.add("贷方银行存款 + 借方应付账款");
            } else if (debitRoles.contains(AccountRole.EXPENSE)) {
                if (summary.contains("报销")) {
                    scene = BusinessScene.EXPENSE;
                    confidence = CONFIDENCE_HIGH;
                    reasons.add("贷方银行存款 + 借方费用 + 摘要含'报销'");
                } else {
                    scene = BusinessScene.PAYMENT;
                    confidence = CONFIDENCE_MEDIUM;
                    reasons.add("贷方银行存款 + 借方费用");
                }
            } else if (debitRoles.contains(AccountRole.SALARY)) {
                scene = BusinessScene.SALARY_PAYMENT;
                confidence = CONFIDENCE_HIGH;
                reasons.add("贷方银行存款 + 借方应付职工薪酬");
            } else if (debitRoles.contains(AccountRole.TAX)) {
                scene = BusinessScene.TAX_PAYMENT;
                confidence = CONFIDENCE_HIGH;
                reasons.add("贷方银行存款 + 借方应交税费");
            }
        }

        // 收款识别：借方银行/现金，贷方应收
        if (debitRoles.contains(AccountRole.BANK) || debitRoles.contains(AccountRole.CASH)) {
            if (creditRoles.contains(AccountRole.RECEIVABLE)) {
                scene = BusinessScene.RECEIPT;
                confidence = CONFIDENCE_MEDIUM_HIGH;
                reasons.add("借方银行存款 + 贷方应收账款");
            } else if (creditRoles.contains(AccountRole.REVENUE)) {
                scene = BusinessScene.SALES_OUT;
                confidence = CONFIDENCE_MEDIUM;
                reasons.add("借方银行存款 + 贷方收入");
            }
        }

        // 关键词增强
        if (summary.contains("付款") || summary.contains("支付")) {
            if (scene == BusinessScene.UNKNOWN) {
                scene = BusinessScene.PAYMENT;
                confidence = CONFIDENCE_LOW;
            }
            reasons.add("摘要含'付款/支付'");
        }

        return BusinessAttributes.builder()
            .scene(scene)
            .templateId(scene.getDefaultTemplateId())
            .confidence(confidence)
            .reasons(reasons)
            .build();
    }
}
