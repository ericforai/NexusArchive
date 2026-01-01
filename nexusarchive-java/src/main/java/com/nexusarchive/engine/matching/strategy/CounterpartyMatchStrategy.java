// Input: Spring Framework, Java 标准库
// Output: CounterpartyMatchStrategy 类
// Pos: 匹配引擎 - 客商匹配策略

package com.nexusarchive.engine.matching.strategy;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 客商匹配策略
 * <p>
 * 验证客商名称是否一致或相似
 * </p>
 */
@Slf4j
@Component
public class CounterpartyMatchStrategy implements MatchStrategy {

    @Override
    public int calculateScore(VoucherData voucher, ScoredCandidate candidate, MatchContext context) {
        String voucherCounterparty = normalize(voucher.getCounterpartyName());
        String candidateCounterparty = normalize(candidate.getCounterparty());

        if (voucherCounterparty == null || candidateCounterparty == null) {
            return 50; // 无客商信息给中等分数
        }

        // 精确匹配
        if (voucherCounterparty.equals(candidateCounterparty)) {
            return 100;
        }

        // 包含关系
        if (voucherCounterparty.contains(candidateCounterparty) ||
            candidateCounterparty.contains(voucherCounterparty)) {
            return 80;
        }

        // 计算相似度 (简单的关键词匹配)
        int similarity = calculateSimilarity(voucherCounterparty, candidateCounterparty);
        return similarity;
    }

    @Override
    public String getName() {
        return "客商匹配";
    }

    @Override
    public int getWeight() {
        return 25; // 客商重要性较高
    }

    @Override
    public boolean isApplicable(MatchContext context) {
        // 客商匹配适用于大多数场景
        return true;
    }

    private String normalize(String name) {
        if (name == null) return null;
        return name.trim()
                .replaceAll("有限公司", "")
                .replaceAll("股份有限公司", "")
                .replaceAll("有限责任公司", "")
                .replaceAll("公司", "")
                .replaceAll("\\s+", "");
    }

    private int calculateSimilarity(String s1, String s2) {
        // 检查关键词匹配
        String[] keywords1 = s1.split("(?=[a-zA-Z0-9]+)|(?=[\\u4e00-\\u9fa5])");
        String[] keywords2 = s2.split("(?=[a-zA-Z0-9]+)|(?=[\\u4e00-\\u9fa5])");

        int matchCount = 0;
        for (String k1 : keywords1) {
            for (String k2 : keywords2) {
                if (k1.length() > 1 && k1.equals(k2)) {
                    matchCount++;
                    break;
                }
            }
        }

        if (matchCount == 0) return 0;

        double ratio = (double) matchCount / Math.max(keywords1.length, keywords2.length);
        return (int) (ratio * 60); // 最高60分
    }
}
