// Input: Spring Framework, Java 标准库
// Output: MatchStrategy 接口
// Pos: 匹配引擎 - 匹配策略接口

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.VoucherData;
import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import java.util.List;

/**
 * 匹配策略接口
 * <p>
 * 定义候选凭证的评分策略
 * </p>
 */
public interface MatchStrategy {

    /**
     * 计算候选凭证的匹配分数
     *
     * @param voucher 目标凭证
     * @param candidate 候选凭证
     * @param context 匹配上下文
     * @return 匹配分数 (0-100)
     */
    int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context);

    /**
     * 获取策略名称
     */
    String getName();

    /**
     * 获取策略权重 (影响最终评分)
     */
    int getWeight();

    /**
     * 是否适用于当前场景
     */
    boolean isApplicable(MatchContext context);
}
