// Input: Spring Framework, Java 标准库
// Output: DateMatchStrategy 类
// Pos: 匹配引擎 - 日期匹配策略

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 日期匹配策略
 * <p>
 * 验证业务日期是否邻近
 * </p>
 */
@Slf4j
@Component
public class DateMatchStrategy implements MatchStrategy {

    @Override
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        LocalDate voucherDate = voucher.getDocDate();
        LocalDate candidateDate = candidate.getDocDate();

        if (voucherDate == null || candidateDate == null) {
            return 50; // 无日期信息给中等分数
        }

        long daysDiff = Math.abs(ChronoUnit.DAYS.between(voucherDate, candidateDate));

        // 精确匹配
        if (daysDiff == 0) {
            return 100;
        }

        // 获取容差天数
        int toleranceDays = getToleranceDays(context);

        // 在容差范围内
        if (daysDiff <= toleranceDays) {
            // 分数随天数差异线性下降
            int score = 100 - (int) ((daysDiff * 40) / toleranceDays); // 最低60分
            return score;
        }

        // 超出容差范围但仍在合理范围内 (30天内)
        if (daysDiff <= 30) {
            return 40 - (int) (daysDiff * 0.5);
        }

        return 0;
    }

    @Override
    public String getName() {
        return "日期匹配";
    }

    @Override
    public int getWeight() {
        return 20; // 日期重要性中等
    }

    @Override
    public boolean isApplicable(MatchContext context) {
        // 日期匹配适用于所有场景
        return true;
    }

    private int getToleranceDays(MatchContext context) {
        if (context.getToleranceConfig() != null) {
            Object tolerance = context.getToleranceConfig().get("date");
            if (tolerance instanceof Number) {
                return ((Number) tolerance).intValue();
            }
        }
        return 3; // 默认容差3天
    }
}
