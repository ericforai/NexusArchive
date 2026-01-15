// Input: Java 标准库
// Output: ReconciliationUtils 类
// Pos: 对账服务 - 工具方法层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.reconciliation;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 对账工具类
 * <p>
 * 提供对账相关的通用工具方法
 * </p>
 */
@UtilityClass
public class ReconciliationUtils {

    /**
     * 构建时间范围内的所有月份
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 月份列表
     */
    public static List<YearMonth> buildPeriods(LocalDate startDate, LocalDate endDate) {
        List<YearMonth> periods = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        while (!start.isAfter(end)) {
            periods.add(start);
            start = start.plusMonths(1);
        }
        return periods;
    }

    /**
     * 检查字符串是否有实际内容
     *
     * @param value 字符串值
     * @return 是否非空且非空白
     */
    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 归一化科目代码
     *
     * @param subjectCode 原始科目代码
     * @return 归一化后的科目代码
     */
    public static String normalizeSubjectCode(String subjectCode) {
        return subjectCode == null ? null : subjectCode.trim();
    }

    /**
     * 归一化可选科目代码
     *
     * @param subjectCode 原始科目代码
     * @return 归一化后的科目代码，如果为空返回 null
     */
    public static String normalizeOptionalSubject(String subjectCode) {
        String normalized = normalizeSubjectCode(subjectCode);
        return hasText(normalized) ? normalized : null;
    }

    /**
     * 将 null 值转为零
     *
     * @param value BigDecimal 值
     * @return 非零值
     */
    public static java.math.BigDecimal nullToZero(java.math.BigDecimal value) {
        return value == null ? java.math.BigDecimal.ZERO : value;
    }

    /**
     * 构建期间候选值列表
     *
     * @param period 年月
     * @return 可能的期间值列表（月、年-月、年月紧凑格式）
     */
    public static List<String> buildPeriodCandidates(YearMonth period) {
        String monthOnly = String.format("%02d", period.getMonthValue());
        String yearMonth = String.format("%d-%02d", period.getYear(), period.getMonthValue());
        String yearMonthCompact = String.format("%d%02d", period.getYear(), period.getMonthValue());
        return Arrays.asList(monthOnly, yearMonth, yearMonthCompact);
    }
}
