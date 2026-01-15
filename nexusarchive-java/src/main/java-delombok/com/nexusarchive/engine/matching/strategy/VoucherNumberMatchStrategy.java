// Input: Spring Framework, Java 标准库
// Output: VoucherNumberMatchStrategy 类
// Pos: 匹配引擎 - 凭证号匹配策略

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 凭证号匹配策略
 * <p>
 * 验证摘要中是否包含对方凭证号
 * </p>
 */
@Slf4j
@Component
public class VoucherNumberMatchStrategy implements MatchStrategy {

    @Override
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        String summary = voucher.getSummary();
        String candidateDocNo = candidate.getDocNo();

        if (summary == null || candidateDocNo == null) {
            return 0;
        }

        // 检查摘要中是否包含凭证号
        if (summary.contains(candidateDocNo)) {
            return 100;
        }

        // 检查凭证号的部分匹配 (后6位)
        if (candidateDocNo.length() >= 6) {
            String shortNo = candidateDocNo.substring(candidateDocNo.length() - 6);
            if (summary.contains(shortNo)) {
                return 70;
            }
        }

        return 0;
    }

    @Override
    public String getName() {
        return "凭证号匹配";
    }

    @Override
    public int getWeight() {
        return 15; // 凭证号匹配是辅助因素
    }

    @Override
    public boolean isApplicable(MatchContext context) {
        // 凭证号匹配适用于发票等场景
        String evidenceRole = context.getEvidenceRole();
        return "INVOICE".equals(evidenceRole) || "CONTRACT".equals(evidenceRole);
    }
}
