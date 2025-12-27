// Input: JUnit 5、匹配引擎组件
// Output: MatchingScorerTest 单元测试
// Pos: 匹配引擎测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import com.nexusarchive.engine.matching.enums.AccountRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MatchingScorer 单元测试
 * 
 * 覆盖方案中 11 类测试用例中的评分相关场景
 */
class MatchingScorerTest {
    
    private MatchingScorer scorer;
    private FuzzyMatcher fuzzyMatcher;
    
    @BeforeEach
    void setUp() {
        fuzzyMatcher = new FuzzyMatcher();
        scorer = new MatchingScorer(fuzzyMatcher);
    }
    
    // ========== 用例 1: 金额精确匹配 ==========
    
    @Test
    @DisplayName("金额精确匹配 - 获得最高分")
    void testAmountExactMatch() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-25", "京东");
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        
        // 金额精确 40 + 日期相同 15 + 交易对手 20 = 75+
        assertTrue(score >= 70, "金额精确匹配应获得高分");
    }
    
    // ========== 用例 2: 金额容差匹配 ==========
    
    @Test
    @DisplayName("金额容差匹配 - 差额在 5% 内")
    void testAmountToleranceMatch() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10300.00", "2025-12-25", "京东"); // 差 3%
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        
        // 金额容差 25 + 日期 15 + 交易对手 20 = 60
        assertTrue(score >= 50, "金额容差匹配应获得中等分数");
    }
    
    // ========== 用例 3: 日期接近度 ==========
    
    @Test
    @DisplayName("日期接近 - 1天内最高分")
    void testDateClose_1Day() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-24", "京东"); // 1天
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertTrue(score >= 70, "日期相差1天应获得高分");
    }
    
    @Test
    @DisplayName("日期接近 - 3天内中等分")
    void testDateClose_3Days() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-22", "京东"); // 3天
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertTrue(score >= 60, "日期相差3天应获得中等分数");
    }
    
    @Test
    @DisplayName("日期接近 - 7天内低分")
    void testDateClose_7Days() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-18", "京东"); // 7天
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertTrue(score >= 55, "日期相差7天应仍有基础分");
    }
    
    // ========== 用例 4: 交易对手匹配 ==========
    
    @Test
    @DisplayName("交易对手匹配 - 高相似度")
    void testCounterpartyMatch_High() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "北京京东世纪贸易有限公司");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-25", "京东世纪贸易");
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertTrue(score >= 60, "交易对手高相似度应获得分数");
    }
    
    @Test
    @DisplayName("交易对手匹配 - 完全不同")
    void testCounterpartyMatch_Different() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-25", "阿里巴巴");
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        // 金额 40 + 日期 15 = 55，无交易对手分
        assertTrue(score < 60, "交易对手不匹配应扣分");
    }
    
    // ========== 用例 5: 已关联惩罚 ==========
    
    @Test
    @DisplayName("已关联惩罚 - 分数归零")
    void testLinkedPenalty() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-25", "京东");
        candidate.setIsLinked(true);
        candidate.setLinkedVoucherId("V999");
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertEquals(0, score, "已关联文档应被惩罚至 0 分");
    }
    
    // ========== 用例 6: 匹配理由生成 ==========
    
    @Test
    @DisplayName("匹配理由 - 应包含金额和日期")
    void testGetReasons() {
        VoucherData voucher = buildVoucher("10000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("10000.00", "2025-12-25", "京东");
        
        List<String> reasons = scorer.getReasons(voucher, candidate, Collections.emptyMap());
        
        assertFalse(reasons.isEmpty(), "应生成匹配理由");
        assertTrue(reasons.stream().anyMatch(r -> r.contains("金额")), "应包含金额匹配理由");
        assertTrue(reasons.stream().anyMatch(r -> r.contains("日期")), "应包含日期匹配理由");
    }
    
    // ========== 用例 11: 红字凭证（负数金额）==========
    
    @Test
    @DisplayName("红字凭证 - 负数金额匹配")
    void testNegativeAmount() {
        VoucherData voucher = buildVoucher("-5000.00", "2025-12-25", "京东");
        ScoredCandidate candidate = buildCandidate("-5000.00", "2025-12-25", "京东");
        
        int score = scorer.score(voucher, candidate, Collections.emptyMap());
        assertTrue(score >= 70, "负数金额应正常匹配");
    }
    
    // ========== 辅助方法 ==========
    
    private VoucherData buildVoucher(String amount, String date, String counterparty) {
        return VoucherData.builder()
            .voucherId("V001")
            .voucherNo("JZ-2025-001")
            .amount(new BigDecimal(amount))
            .docDate(LocalDate.parse(date))
            .counterpartyName(counterparty)
            .debitRoles(Set.of(AccountRole.PAYABLE))
            .creditRoles(Set.of(AccountRole.BANK))
            .build();
    }
    
    private ScoredCandidate buildCandidate(String amount, String date, String counterparty) {
        return ScoredCandidate.builder()
            .docId("DOC001")
            .docNo("YH-2025-001")
            .docType("SETTLEMENT")
            .docTypeName("银行回单")
            .amount(new BigDecimal(amount))
            .docDate(LocalDate.parse(date))
            .counterparty(counterparty)
            .isLinked(false)
            .build();
    }
}
