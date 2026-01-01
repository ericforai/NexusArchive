// Input: Spring Framework, Java 标准库
// Output: MatchingEngineFacade 类
// Pos: 匹配引擎 - 匹配引擎门面

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.MatchResult;
import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 匹配引擎门面
 * <p>
 * 协调各个匹配策略，计算综合得分
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEngineFacade {

    private final List<MatchStrategy> strategies;

    /**
     * 计算候选凭证的综合得分
     *
     * @param voucher 目标凭证
     * @param candidate 候选凭证
     * @param context 匹配上下文
     * @return 综合得分 (0-100)
     */
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        int totalWeight = 0;
        int weightedScore = 0;

        for (MatchStrategy strategy : strategies) {
            if (!strategy.isApplicable(context)) {
                continue;
            }

            int weight = strategy.getWeight();
            int score = strategy.calculateScore(voucher, candidate, context);

            weightedScore += score * weight;
            totalWeight += weight;

            log.debug("Strategy {}: score={}, weight={}", strategy.getName(), score, weight);
        }

        if (totalWeight == 0) {
            return 0;
        }

        return weightedScore / totalWeight;
    }

    /**
     * 批量计算候选凭证得分
     *
     * @param voucher 目标凭证
     * @param candidates 候选凭证列表
     * @param context 匹配上下文
     */
    public void scoreCandidates(VoucherData voucher, List<ScoredCandidate> candidates, MatchContext context) {
        for (ScoredCandidate candidate : candidates) {
            int score = calculateScore(voucher, candidate, context);
            candidate.setScore(score);
        }

        // 按得分降序排序
        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
    }

    /**
     * 获取所有策略
     */
    public List<MatchStrategy> getStrategies() {
        return List.copyOf(strategies);
    }
}
