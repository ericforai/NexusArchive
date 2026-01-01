// Input: Spring Framework, Java 标准库
// Output: AmountMatchStrategy 类
// Pos: 匹配引擎 - 金额匹配策略

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 金额匹配策略
 * <p>
 * 验证金额是否精确匹配或在容差范围内
 * </p>
 */
@Slf4j
@Component
public class AmountMatchStrategy implements MatchStrategy {

    @Override
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        BigDecimal voucherAmount = voucher.getAmount();
        BigDecimal candidateAmount = candidate.getAmount();

        if (voucherAmount == null || candidateAmount == null) {
            return 0;
        }

        // 精确匹配
        if (voucherAmount.compareTo(candidateAmount) == 0) {
            return 100;
        }

        // 计算差异率
        BigDecimal diff = voucherAmount.subtract(candidateAmount).abs();
        BigDecimal ratio = diff.divide(candidateAmount, 4, RoundingMode.HALF_UP);

        // 获取容差配置
        BigDecimal tolerance = getTolerance(context);

        // 在容差范围内
        if (ratio.compareTo(tolerance) <= 0) {
            // 分数随差异线性下降
            BigDecimal score = BigDecimal.ONE.subtract(ratio.divide(tolerance, 4, RoundingMode.HALF_UP));
            return score.multiply(BigDecimal.valueOf(80)).intValue(); // 最高80分
        }

        return 0;
    }

    @Override
    public String getName() {
        return "金额匹配";
    }

    @Override
    public int getWeight() {
        return 40; // 金额是最重要的因素
    }

    @Override
    public boolean isApplicable(MatchContext context) {
        // 金额匹配适用于所有场景
        return true;
    }

    private BigDecimal getTolerance(MatchContext context) {
        if (context.getToleranceConfig() != null) {
            Object tolerance = context.getToleranceConfig().get("amount");
            if (tolerance instanceof BigDecimal) {
                return (BigDecimal) tolerance;
            }
            if (tolerance instanceof Number) {
                return BigDecimal.valueOf(((Number) tolerance).doubleValue());
            }
        }
        return new BigDecimal("0.01"); // 默认 1% 容差
    }
}
