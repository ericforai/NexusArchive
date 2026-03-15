// Input: Spring Framework、JDBC、匹配引擎组件
// Output: CandidateFinder 候选查找器
// Pos: 匹配引擎/核心
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.engine.matching;

import com.nexusarchive.engine.matching.dto.ScoredCandidate;
import com.nexusarchive.engine.matching.dto.VoucherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 候选文档查找器
 * 
 * 采用两阶段策略：
 * 1. 索引粗筛：利用复合索引快速过滤
 * 2. 内存精筛：进行模糊匹配和评分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateFinder {
    
    private final JdbcTemplate jdbcTemplate;
    private final FuzzyMatcher fuzzyMatcher;
    private final MatchingScorer scorer;
    
    // 默认配置
    private static final int DEFAULT_DATE_RANGE = 7;
    private static final double DEFAULT_AMOUNT_TOLERANCE = 0.05;
    private static final int DEFAULT_MAX_CANDIDATES = 50;
    
    /**
     * 查找并评分候选文档
     */
    public List<ScoredCandidate> findAndScore(VoucherData voucher, String evidenceRole, Map<String, Object> rule) {
        // 解析过滤配置
        int dateRange = getIntConfig(rule, "candidateFilter.dateRange", DEFAULT_DATE_RANGE);
        double amountTolerance = getDoubleConfig(rule, "candidateFilter.amountTolerance", DEFAULT_AMOUNT_TOLERANCE);
        int maxCandidates = getIntConfig(rule, "candidateFilter.maxCandidates", DEFAULT_MAX_CANDIDATES);
        
        // Step 1: 索引粗筛
        List<Map<String, Object>> rawCandidates = findByIndexes(
            voucher, evidenceRole, dateRange, amountTolerance, maxCandidates
        );
        
        if (rawCandidates.isEmpty()) {
            log.debug("No candidates found for voucher {} role {}", voucher.getVoucherId(), evidenceRole);
            return Collections.emptyList();
        }
        
        // Step 2: 转换为 ScoredCandidate
        List<ScoredCandidate> candidates = rawCandidates.stream()
            .map(row -> mapToCandidate(row))
            .collect(Collectors.toList());
        
        // Step 3: 内存精筛（模糊匹配）
        candidates = candidates.stream()
            .filter(c -> passMemoryFilter(voucher, c))
            .collect(Collectors.toList());
        
        // Step 4: 评分
        for (ScoredCandidate candidate : candidates) {
            int score = scorer.score(voucher, candidate, rule);
            candidate.setScore(score);
            candidate.setReasons(scorer.getReasons(voucher, candidate, rule));
        }
        
        // Step 5: 排序（分数降序）
        candidates.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        
        return candidates;
    }
    
    /**
     * 索引粗筛
     */
    private List<Map<String, Object>> findByIndexes(
            VoucherData voucher, String evidenceRole,
            int dateRange, double amountTolerance, int maxCandidates) {

        // 防御性空值检查 (java:S1874)
        if (voucher.getDocDate() == null) {
            log.warn("凭证日期为空，无法查找候选: voucherId={}", voucher.getVoucherId());
            return Collections.emptyList();
        }
        if (voucher.getAmount() == null) {
            log.warn("凭证金额为空，无法查找候选: voucherId={}", voucher.getVoucherId());
            return Collections.emptyList();
        }

        LocalDate dateFrom = voucher.getDocDate().minusDays(dateRange);
        LocalDate dateTo = voucher.getDocDate().plusDays(dateRange);
        BigDecimal amountFrom = voucher.getAmount().multiply(BigDecimal.valueOf(1 - amountTolerance));
        BigDecimal amountTo = voucher.getAmount().multiply(BigDecimal.valueOf(1 + amountTolerance));
        
        log.info("DEBUG CandidateFinder: dateFrom={}, dateTo={}, amountFrom={}, amountTo={}, role={}", 
            dateFrom, dateTo, amountFrom, amountTo, evidenceRole);
        
        // 查询时过滤 evidenceRole（通过 cfg_doc_type_mapping 关联）
        String sql = """
            SELECT ov.id, ov.voucher_no, ov.voucher_type, 
                   ov.business_date, ov.amount, ov.counterparty, ov.source_doc_id
            FROM arc_original_voucher ov
            LEFT JOIN cfg_doc_type_mapping dtm 
                ON ov.voucher_type = dtm.customer_doc_type
            WHERE ov.business_date BETWEEN ? AND ?
              AND ov.amount BETWEEN ? AND ?
              AND (dtm.evidence_role = ? OR ? IS NULL)
            ORDER BY ov.business_date DESC
            LIMIT ?
            """;
        
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, 
                Date.valueOf(dateFrom), Date.valueOf(dateTo),
                amountFrom, amountTo,
                evidenceRole, evidenceRole,
                maxCandidates
            );
            log.info("DEBUG CandidateFinder: returned {} candidates", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Failed to query candidates: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * 转换为候选对象
     */
    private ScoredCandidate mapToCandidate(Map<String, Object> row) {
        return ScoredCandidate.builder()
            .docId(getString(row, "id"))
            .docNo(getString(row, "voucher_no"))
            .docType(getString(row, "voucher_type"))
            .docTypeName(getString(row, "voucher_type_name"))
            .docDate(getLocalDate(row, "business_date"))
            .amount(getBigDecimal(row, "amount"))
            .counterparty(getString(row, "counterparty"))
            .isLinked(false)
            .build();
    }
    
    /**
     * 内存精筛
     */
    private boolean passMemoryFilter(VoucherData voucher, ScoredCandidate candidate) {
        // 供应商模糊匹配
        if (voucher.getCounterpartyName() != null && candidate.getCounterparty() != null) {
            return fuzzyMatcher.match(
                voucher.getCounterpartyName(), 
                candidate.getCounterparty(), 
                com.nexusarchive.engine.matching.enums.MatchStrategy.SIMILARITY, 
                0.5
            );
        }
        return true;
    }
    
    // ========== 工具方法 ==========
    
    private int getIntConfig(Map<String, Object> rule, String path, int defaultValue) {
        try {
            String[] parts = path.split("\\.");
            Object current = rule;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                }
            }
            if (current instanceof Number) {
                return ((Number) current).intValue();
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    private double getDoubleConfig(Map<String, Object> rule, String path, double defaultValue) {
        try {
            String[] parts = path.split("\\.");
            Object current = rule;
            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                }
            }
            if (current instanceof Number) {
                return ((Number) current).doubleValue();
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
    
    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }
    
    private LocalDate getLocalDate(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        if (value instanceof java.time.LocalDate) {
            return (LocalDate) value;
        }
        return null;
    }
    
    private BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }
}
