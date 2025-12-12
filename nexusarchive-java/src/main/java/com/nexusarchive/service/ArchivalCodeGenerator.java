package com.nexusarchive.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 档号生成器服务
 * 
 * 遵循 DA/T 94-2022 档号格式规范:
 * [全宗号]-[年度]-[保管期限]-[分类代码]-[件号]
 * 
 * 示例: QZ01-2025-30Y-AC01-000001
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArchivalCodeGenerator {

    // 序号计数器缓存 (按年度+分类维护)
    private final Map<String, AtomicInteger> sequenceCounters = new ConcurrentHashMap<>();

    /**
     * 保管期限代码
     */
    public enum RetentionPeriod {
        TEN_YEARS("10Y", "10年"),
        THIRTY_YEARS("30Y", "30年"),
        PERMANENT("PERM", "永久");

        private final String code;
        private final String label;

        RetentionPeriod(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public static RetentionPeriod fromCode(String code) {
            for (RetentionPeriod rp : values()) {
                if (rp.code.equalsIgnoreCase(code)) {
                    return rp;
                }
            }
            return THIRTY_YEARS; // 默认30年
        }
    }

    /**
     * 生成符合 DA/T 94-2022 的正式档号
     * 
     * @param fondsCode    全宗号 (必填)
     * @param year         年度 (必填, YYYY格式)
     * @param retention    保管期限 (10Y/30Y/PERM)
     * @param categoryCode 分类代码 (AC01/AC02/AC03/AC04)
     * @return 正式档号
     */
    @Transactional
    public String generate(String fondsCode, String year, String retention, String categoryCode) {
        // 参数校验
        if (fondsCode == null || fondsCode.isEmpty()) {
            fondsCode = "DEFAULT";
            log.warn("全宗号为空，使用默认值: {}", fondsCode);
        }
        if (year == null || year.length() != 4) {
            year = String.valueOf(java.time.LocalDate.now().getYear());
            log.warn("年度格式异常，使用当前年份: {}", year);
        }
        if (retention == null || retention.isEmpty()) {
            retention = RetentionPeriod.THIRTY_YEARS.getCode();
        }
        if (categoryCode == null || categoryCode.isEmpty()) {
            categoryCode = "AC01";
        }

        // 获取或初始化序号计数器
        String counterKey = String.format("%s-%s-%s", fondsCode, year, categoryCode);
        AtomicInteger counter = sequenceCounters.computeIfAbsent(counterKey, 
            k -> new AtomicInteger(0));
        
        int sequence = counter.incrementAndGet();

        // 生成档号: [全宗号]-[年度]-[保管期限]-[分类代码]-[件号]
        String archivalCode = String.format("%s-%s-%s-%s-%06d",
                fondsCode, year, retention, categoryCode, sequence);

        log.info("生成档号: {} (序号: {})", archivalCode, sequence);
        return archivalCode;
    }

    /**
     * 生成预归档临时编号
     * 
     * @param sourceSystem 来源系统
     * @param year         年度
     * @param sequence     序号
     * @return 预归档编号
     */
    public String generatePreArchiveCode(String sourceSystem, String year, int sequence) {
        return String.format("PRE-%s-%s-%06d", 
                sourceSystem != null ? sourceSystem : "UNKNOWN", 
                year != null ? year : String.valueOf(java.time.LocalDate.now().getYear()),
                sequence);
    }

    /**
     * 验证档号格式是否符合规范
     * 
     * @param archivalCode 档号
     * @return 是否合规
     */
    public boolean validate(String archivalCode) {
        if (archivalCode == null || archivalCode.isEmpty()) {
            return false;
        }
        // 格式: XXX-YYYY-XXX-ACXX-NNNNNN
        String pattern = "^[A-Z0-9]+-\\d{4}-(10Y|30Y|PERM)-AC0[1-4]-\\d{6}$";
        return archivalCode.matches(pattern);
    }

    /**
     * 解析档号各组成部分
     * 
     * @param archivalCode 档号
     * @return 解析结果 Map
     */
    public Map<String, String> parse(String archivalCode) {
        Map<String, String> result = new java.util.HashMap<>();
        if (archivalCode == null || !validate(archivalCode)) {
            return result;
        }
        
        String[] parts = archivalCode.split("-");
        if (parts.length >= 5) {
            result.put("fondsCode", parts[0]);
            result.put("year", parts[1]);
            result.put("retention", parts[2]);
            result.put("categoryCode", parts[3]);
            result.put("sequence", parts[4]);
        }
        return result;
    }

    /**
     * 重置序号计数器 (用于测试或新年度初始化)
     */
    public void resetCounters() {
        sequenceCounters.clear();
        log.info("档号序号计数器已重置");
    }
}
