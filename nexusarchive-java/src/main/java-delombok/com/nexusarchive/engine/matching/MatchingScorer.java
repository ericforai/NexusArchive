// Input: Spring Framework、匹配引擎组件
// Output: MatchingScorer 评分器
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import com.nexusarchive.engine.matching.enums.MatchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 匹配评分器
 * 
 * 基于多维度对候选文档进行评分：
 * - 金额精确匹配/容差匹配
 * - 日期接近度
 * - 交易对手匹配度
 * - 关键字段命中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingScorer {
    
    private final FuzzyMatcher fuzzyMatcher;
    
    // 默认权重
    private static final int SCORE_AMOUNT_EXACT = 40;
    private static final int SCORE_AMOUNT_TOLERANCE = 25;
    private static final int SCORE_DATE_1D = 15;
    private static final int SCORE_DATE_3D = 10;
    private static final int SCORE_DATE_7D = 5;
    private static final int SCORE_COUNTERPARTY = 20;
    private static final int PENALTY_LINKED = -100;
    
    /**
     * 计算候选评分
     */
    public int score(VoucherData voucher, ScoredCandidate candidate, Map<String, Object> rule) {
        int totalScore = 0;
        
        // 1. 金额评分（防止除零）
        if (voucher.getAmount() != null && candidate.getAmount() != null
                && voucher.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal diff = voucher.getAmount().subtract(candidate.getAmount()).abs();
            if (diff.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                totalScore += SCORE_AMOUNT_EXACT;
            } else {
                BigDecimal tolerance = diff.divide(voucher.getAmount().abs(), 4, BigDecimal.ROUND_HALF_UP);
                if (tolerance.compareTo(BigDecimal.valueOf(0.05)) <= 0) {
                    totalScore += SCORE_AMOUNT_TOLERANCE;
                }
            }
        }
        
        // 2. 日期评分
        if (voucher.getDocDate() != null && candidate.getDocDate() != null) {
            long daysDiff = Math.abs(ChronoUnit.DAYS.between(voucher.getDocDate(), candidate.getDocDate()));
            if (daysDiff <= 1) {
                totalScore += SCORE_DATE_1D;
            } else if (daysDiff <= 3) {
                totalScore += SCORE_DATE_3D;
            } else if (daysDiff <= 7) {
                totalScore += SCORE_DATE_7D;
            }
        }
        
        // 3. 交易对手评分
        if (voucher.getCounterpartyName() != null && candidate.getCounterparty() != null) {
            double similarity = fuzzyMatcher.calculateSimilarity(
                voucher.getCounterpartyName(), candidate.getCounterparty()
            );
            if (similarity >= 0.9) {
                totalScore += SCORE_COUNTERPARTY;
            } else if (similarity >= 0.7) {
                totalScore += (int) (SCORE_COUNTERPARTY * similarity);
            }
        }
        
        // 4. 已关联惩罚
        if (Boolean.TRUE.equals(candidate.getIsLinked())) {
            totalScore += PENALTY_LINKED;
        }
        
        return Math.max(0, Math.min(100, totalScore));
    }
    
    /**
     * 获取匹配理由
     */
    public List<String> getReasons(VoucherData voucher, ScoredCandidate candidate, Map<String, Object> rule) {
        List<String> reasons = new ArrayList<>();
        
        // 金额（防止除零）
        if (voucher.getAmount() != null && candidate.getAmount() != null
                && voucher.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal diff = voucher.getAmount().subtract(candidate.getAmount()).abs();
            if (diff.compareTo(BigDecimal.valueOf(0.01)) <= 0) {
                reasons.add("金额精确匹配: " + candidate.getAmount());
            } else {
                BigDecimal tolerance = diff.divide(voucher.getAmount().abs(), 4, BigDecimal.ROUND_HALF_UP);
                if (tolerance.compareTo(BigDecimal.valueOf(0.05)) <= 0) {
                    reasons.add("金额容差匹配: 差额 " + diff);
                }
            }
        }
        
        // 日期
        if (voucher.getDocDate() != null && candidate.getDocDate() != null) {
            long daysDiff = Math.abs(ChronoUnit.DAYS.between(voucher.getDocDate(), candidate.getDocDate()));
            if (daysDiff == 0) {
                reasons.add("日期相同");
            } else if (daysDiff <= 3) {
                reasons.add("日期接近: 相差 " + daysDiff + " 天");
            }
        }
        
        // 交易对手
        if (voucher.getCounterpartyName() != null && candidate.getCounterparty() != null) {
            double similarity = fuzzyMatcher.calculateSimilarity(
                voucher.getCounterpartyName(), candidate.getCounterparty()
            );
            if (similarity >= 0.7) {
                reasons.add("交易对手匹配: " + candidate.getCounterparty() + 
                    " (相似度 " + String.format("%.0f%%", similarity * 100) + ")");
            }
        }
        
        return reasons;
    }
}
