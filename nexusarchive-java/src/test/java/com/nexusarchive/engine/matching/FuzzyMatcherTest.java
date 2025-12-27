// Input: JUnit 5、匹配引擎组件
// Output: FuzzyMatcherTest 单元测试
// Pos: 匹配引擎测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import com.nexusarchive.engine.matching.enums.MatchStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FuzzyMatcher 单元测试
 * 
 * 覆盖场景：
 * 1. 精确匹配
 * 2. 包含匹配
 * 3. 相似度匹配
 * 4. 数值百分比容差
 * 5. 数值绝对值容差
 * 6. 空值处理
 * 7. 企业名称规范化
 */
class FuzzyMatcherTest {
    
    private FuzzyMatcher matcher;
    
    @BeforeEach
    void setUp() {
        matcher = new FuzzyMatcher();
    }
    
    // ========== 精确匹配测试 ==========
    
    @Test
    @DisplayName("精确匹配 - 相同字符串")
    void testExactMatch_Same() {
        assertTrue(matcher.match("京东", "京东", MatchStrategy.EXACT, null));
    }
    
    @Test
    @DisplayName("精确匹配 - 不同字符串")
    void testExactMatch_Different() {
        assertFalse(matcher.match("京东", "淘宝", MatchStrategy.EXACT, null));
    }
    
    // ========== 包含匹配测试 ==========
    
    @Test
    @DisplayName("包含匹配 - 目标包含源")
    void testContainsMatch_TargetContainsSource() {
        assertTrue(matcher.match("京东", "北京京东世纪贸易", MatchStrategy.CONTAINS, null));
    }
    
    @Test
    @DisplayName("包含匹配 - 源包含目标")
    void testContainsMatch_SourceContainsTarget() {
        assertTrue(matcher.match("北京京东世纪贸易", "京东", MatchStrategy.CONTAINS, null));
    }
    
    @Test
    @DisplayName("包含匹配 - 去除公司后缀后匹配")
    void testContainsMatch_AfterNormalize() {
        assertTrue(matcher.match("京东", "京东有限责任公司", MatchStrategy.CONTAINS, null));
    }
    
    // ========== 相似度匹配测试 ==========
    
    @Test
    @DisplayName("相似度匹配 - 高相似度")
    void testSimilarityMatch_HighSimilarity() {
        // "上海强生" vs "强生上海" 单字符集合相同
        assertTrue(matcher.match("上海强生", "强生上海", MatchStrategy.SIMILARITY, 0.7));
    }
    
    @Test
    @DisplayName("相似度匹配 - 低相似度")
    void testSimilarityMatch_LowSimilarity() {
        assertFalse(matcher.match("阿里巴巴", "京东集团", MatchStrategy.SIMILARITY, 0.7));
    }
    
    @Test
    @DisplayName("相似度计算 - 完全相同")
    void testCalculateSimilarity_Same() {
        assertEquals(1.0, matcher.calculateSimilarity("测试", "测试"), 0.01);
    }
    
    @Test
    @DisplayName("相似度计算 - 完全不同")
    void testCalculateSimilarity_Different() {
        assertEquals(0.0, matcher.calculateSimilarity("甲", "乙"), 0.01);
    }
    
    // ========== 数值匹配测试 ==========
    
    @ParameterizedTest
    @DisplayName("数值百分比容差 - 参数化测试")
    @CsvSource({
        "10000, 10000, 0.05, true",   // 完全相等
        "10000, 10500, 0.05, true",   // 差 500 = 5%
        "10000, 10501, 0.05, false",  // 差 501 > 5%
        "10000, 9500, 0.05, true",    // 差 -500 = 5%
        "10000, 9499, 0.05, false",   // 差 -501 > 5%
    })
    void testMatchNumericPercent(String source, String target, String tolerance, boolean expected) {
        assertEquals(expected, matcher.matchNumericPercent(
            new BigDecimal(source),
            new BigDecimal(target),
            Double.parseDouble(tolerance)
        ));
    }
    
    @ParameterizedTest
    @DisplayName("数值绝对值容差 - 参数化测试")
    @CsvSource({
        "100.00, 100.00, 0.01, true",  // 完全相等
        "100.00, 100.01, 0.01, true",  // 差 0.01
        "100.00, 100.02, 0.01, false", // 差 0.02 > 0.01
        "100.00, 99.99, 0.01, true",   // 差 -0.01
    })
    void testMatchNumericAbsolute(String source, String target, String tolerance, boolean expected) {
        assertEquals(expected, matcher.matchNumericAbsolute(
            new BigDecimal(source),
            new BigDecimal(target),
            new BigDecimal(tolerance)
        ));
    }
    
    // ========== 空值处理测试 ==========
    
    @Test
    @DisplayName("空值处理 - 源为空")
    void testNullHandling_SourceNull() {
        assertFalse(matcher.match(null, "test", MatchStrategy.EXACT, null));
        assertFalse(matcher.match("", "test", MatchStrategy.EXACT, null));
        assertFalse(matcher.match("  ", "test", MatchStrategy.EXACT, null));
    }
    
    @Test
    @DisplayName("空值处理 - 目标为空")
    void testNullHandling_TargetNull() {
        assertFalse(matcher.match("test", null, MatchStrategy.EXACT, null));
        assertFalse(matcher.match("test", "", MatchStrategy.EXACT, null));
    }
    
    @Test
    @DisplayName("空值处理 - 两者都为空返回 false")
    void testNullHandling_BothNull() {
        assertFalse(matcher.match(null, null, MatchStrategy.EXACT, null));
        assertEquals(0.0, matcher.calculateSimilarity(null, null), 0.01);
    }
    
    @Test
    @DisplayName("数值空值处理")
    void testNumericNullHandling() {
        assertFalse(matcher.matchNumericPercent(null, BigDecimal.TEN, 0.05));
        assertFalse(matcher.matchNumericPercent(BigDecimal.TEN, null, 0.05));
        assertFalse(matcher.matchNumericAbsolute(null, BigDecimal.TEN, BigDecimal.ONE));
    }
    
    // ========== 企业名称规范化测试 ==========
    
    @Test
    @DisplayName("规范化 - 去除有限责任公司")
    void testNormalize_RemoveSuffix() {
        // "有限责任公司" 应一次性去除，而非逐个替换
        assertTrue(matcher.match(
            "北京京东世纪贸易有限责任公司",
            "北京京东世纪",
            MatchStrategy.CONTAINS,
            null
        ));
    }
    
    @Test
    @DisplayName("规范化 - 去除股份有限公司")
    void testNormalize_RemoveStockSuffix() {
        assertTrue(matcher.match(
            "阿里巴巴网络技术股份有限公司",
            "阿里巴巴网络",
            MatchStrategy.CONTAINS,
            null
        ));
    }
}
