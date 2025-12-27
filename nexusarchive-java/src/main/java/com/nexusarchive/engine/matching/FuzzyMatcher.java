// Input: Spring Framework、Java 标准库、匹配引擎枚举
// Output: FuzzyMatcher 模糊匹配器
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import cn.hutool.core.util.StrUtil;
import com.nexusarchive.engine.matching.enums.MatchStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 模糊匹配器
 * 
 * 支持多种匹配策略：精确、包含、相似度、数值容差
 */
@Component
public class FuzzyMatcher {
    
    // 企业名称后缀正则（一次性替换，避免顺序问题）
    private static final Pattern SUFFIX_PATTERN = Pattern.compile(
        "(有限责任|有限|责任|股份有限|股份|公司|集团|企业|贸易|科技|技术)+$"
    );
    
    /**
     * 字符串匹配
     */
    public boolean match(String source, String target, MatchStrategy strategy, Double threshold) {
        // 统一空白处理：两个都为空视为不匹配
        if (StrUtil.isBlank(source) || StrUtil.isBlank(target)) {
            return false;
        }
        
        return switch (strategy) {
            case EXACT -> source.equals(target);
            case CONTAINS -> containsMatch(source, target);
            case SIMILARITY -> calculateSimilarity(source, target) >= (threshold != null ? threshold : 0.7);
            default -> false;
        };
    }
    
    /**
     * 数值匹配（百分比容差）
     * 
     * @param source 原始金额
     * @param target 目标金额
     * @param tolerancePercent 容差百分比，如 0.05 表示 5%
     * @return 是否在容差范围内
     */
    public boolean matchNumericPercent(BigDecimal source, BigDecimal target, Double tolerancePercent) {
        if (source == null || target == null || tolerancePercent == null) {
            return false;
        }
        // 计算容差绝对值 = source * tolerancePercent
        BigDecimal tolerance = source.abs().multiply(BigDecimal.valueOf(tolerancePercent));
        BigDecimal diff = source.subtract(target).abs();
        return diff.compareTo(tolerance) <= 0;
    }
    
    /**
     * 数值匹配（绝对值容差）
     * 
     * @param source 原始金额
     * @param target 目标金额
     * @param tolerance 容差绝对值，如 0.01 表示 1 分钱
     * @return 是否在容差范围内
     */
    public boolean matchNumericAbsolute(BigDecimal source, BigDecimal target, BigDecimal tolerance) {
        if (source == null || target == null || tolerance == null) {
            return false;
        }
        BigDecimal diff = source.subtract(target).abs();
        return diff.compareTo(tolerance) <= 0;
    }
    
    /**
     * 包含匹配
     */
    private boolean containsMatch(String s1, String s2) {
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        if (n1.isEmpty() || n2.isEmpty()) {
            return false;
        }
        return n1.contains(n2) || n2.contains(n1);
    }
    
    /**
     * 计算 Jaccard 相似度
     */
    public double calculateSimilarity(String s1, String s2) {
        // 统一空白处理
        if (StrUtil.isBlank(s1) && StrUtil.isBlank(s2)) {
            return 0.0;  // 两个都为空返回 0，与 match 方法一致
        }
        if (StrUtil.isBlank(s1) || StrUtil.isBlank(s2)) {
            return 0.0;
        }
        
        Set<String> set1 = tokenize(normalize(s1), 1);
        Set<String> set2 = tokenize(normalize(s2), 1);
        
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }
    
    /**
     * 标准化企业名称（使用正则一次性去除后缀，避免顺序问题）
     */
    private String normalize(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return SUFFIX_PATTERN.matcher(text.trim()).replaceAll("");
    }
    
    /**
     * 分词（支持 N-gram）
     * 
     * @param text 文本
     * @param ngramSize N-gram 大小，1 表示单字符，2 表示双字符
     * @return 分词集合
     */
    private Set<String> tokenize(String text, int ngramSize) {
        if (StrUtil.isBlank(text)) {
            return new HashSet<>();
        }
        
        if (ngramSize <= 1) {
            // 单字符分割
            return Arrays.stream(text.split(""))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        }
        
        // N-gram 分割
        Set<String> result = new HashSet<>();
        for (int i = 0; i <= text.length() - ngramSize; i++) {
            result.add(text.substring(i, i + ngramSize));
        }
        return result;
    }
}
