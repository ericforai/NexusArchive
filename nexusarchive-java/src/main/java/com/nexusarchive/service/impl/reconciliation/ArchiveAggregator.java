// Input: MyBatis-Plus、Jackson、Lombok、Java 标准库
// Output: ArchiveAggregator 类
// Pos: 对账服务 - 档案数据聚合层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.reconciliation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 档案数据聚合器
 * <p>
 * 负责从数据库获取档案数据并进行聚合统计
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAggregator {

    private static final String ARCHIVE_STATUS_ARCHIVED = "archived";

    private final ArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;
    private final SubjectExtractor subjectExtractor;

    /**
     * 获取并聚合档案数据（科目模式）
     *
     * @param period      会计期间
     * @param accbookCode 账簿代码
     * @param subjectCode 科目代码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 档案聚合结果
     */
    public ArchiveAggregation aggregateBySubject(YearMonth period, String accbookCode, String subjectCode,
                                                  LocalDate startDate, LocalDate endDate) {
        List<Archive> archivedItems = fetchArchives(period, accbookCode);
        return aggregateArchives(archivedItems, subjectCode, startDate, endDate);
    }

    /**
     * 获取并聚合档案数据（凭证模式）
     *
     * @param period      会计期间
     * @param accbookCode 账簿代码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 档案聚合结果
     */
    public ArchiveAggregation aggregateByVoucher(YearMonth period, String accbookCode,
                                                  LocalDate startDate, LocalDate endDate) {
        List<Archive> archivedItems = fetchArchives(period, accbookCode);
        return aggregateArchivesForVoucherOnly(archivedItems, startDate, endDate);
    }

    /**
     * 获取档案数据
     */
    private List<Archive> fetchArchives(YearMonth period, String accbookCode) {
        List<String> periodCandidates = buildPeriodCandidates(period);
        QueryWrapper<Archive> archiveQuery = new QueryWrapper<>();
        archiveQuery.select("id", "archive_code", "unique_biz_id", "custom_metadata", "doc_date", "fiscal_year",
                        "fiscal_period", "fonds_no", "status")
                .eq("fiscal_year", String.valueOf(period.getYear()))
                .in("fiscal_period", periodCandidates)
                .eq("status", ARCHIVE_STATUS_ARCHIVED);
        if (hasText(accbookCode)) {
            archiveQuery.eq("fonds_no", accbookCode);
        }
        List<Archive> archivedItems = archiveMapper.selectList(archiveQuery);
        return archivedItems == null ? Collections.emptyList() : archivedItems;
    }

    /**
     * 聚合档案数据（科目模式）
     */
    private ArchiveAggregation aggregateArchives(List<Archive> archivedItems, String subjectCode,
                                                   LocalDate startDate, LocalDate endDate) {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        Set<String> voucherIds = new LinkedHashSet<>();
        Set<String> archiveCodes = new LinkedHashSet<>();
        int missingMetadataCount = 0;
        int metadataParseErrorCount = 0;
        int missingDocDateCount = 0;
        int outOfRangeCount = 0;
        String fondsCode = null;

        for (Archive archive : archivedItems) {
            if (archive == null) {
                continue;
            }
            LocalDate docDate = archive.getDocDate();
            if (docDate == null) {
                missingDocDateCount++;
                continue;
            }
            if (docDate.isBefore(startDate) || docDate.isAfter(endDate)) {
                outOfRangeCount++;
                continue;
            }

            SubjectAggregation subjectAggregation = subjectExtractor.extract(archive.getCustomMetadata(), subjectCode);
            if (subjectAggregation.metadataMissing) {
                missingMetadataCount++;
                continue;
            }
            if (subjectAggregation.parseError) {
                metadataParseErrorCount++;
                continue;
            }
            if (!subjectAggregation.matched) {
                continue;
            }

            debit = debit.add(subjectAggregation.debit);
            credit = credit.add(subjectAggregation.credit);
            String voucherId = resolveVoucherId(archive);
            if (voucherId != null) {
                voucherIds.add(voucherId);
            }
            if (archive.getArchiveCode() != null) {
                archiveCodes.add(archive.getArchiveCode());
            }
            if (fondsCode == null && hasText(archive.getFondsNo())) {
                fondsCode = archive.getFondsNo();
            }
        }

        return new ArchiveAggregation(debit, credit, voucherIds, archiveCodes, missingMetadataCount,
                metadataParseErrorCount, missingDocDateCount, outOfRangeCount, fondsCode);
    }

    /**
     * 聚合档案数据（凭证模式）
     */
    private ArchiveAggregation aggregateArchivesForVoucherOnly(List<Archive> archivedItems, LocalDate startDate,
                                                                 LocalDate endDate) {
        Set<String> voucherIds = new LinkedHashSet<>();
        Set<String> archiveCodes = new LinkedHashSet<>();
        int missingDocDateCount = 0;
        int outOfRangeCount = 0;
        String fondsCode = null;

        for (Archive archive : archivedItems) {
            if (archive == null) {
                continue;
            }
            LocalDate docDate = archive.getDocDate();
            if (docDate == null) {
                missingDocDateCount++;
                continue;
            }
            if (docDate.isBefore(startDate) || docDate.isAfter(endDate)) {
                outOfRangeCount++;
                continue;
            }
            String voucherId = resolveVoucherId(archive);
            if (voucherId != null) {
                voucherIds.add(voucherId);
            }
            if (archive.getArchiveCode() != null) {
                archiveCodes.add(archive.getArchiveCode());
            }
            if (fondsCode == null && hasText(archive.getFondsNo())) {
                fondsCode = archive.getFondsNo();
            }
        }

        return new ArchiveAggregation(BigDecimal.ZERO, BigDecimal.ZERO, voucherIds, archiveCodes, 0, 0,
                missingDocDateCount, outOfRangeCount, fondsCode);
    }

    /**
     * 解析凭证ID
     */
    private String resolveVoucherId(Archive archive) {
        if (archive == null) {
            return null;
        }
        if (hasText(archive.getUniqueBizId())) {
            return archive.getUniqueBizId();
        }
        if (hasText(archive.getArchiveCode())) {
            return archive.getArchiveCode();
        }
        return archive.getId();
    }

    /**
     * 构建期间候选值
     */
    private List<String> buildPeriodCandidates(YearMonth period) {
        String monthOnly = String.format("%02d", period.getMonthValue());
        String yearMonth = String.format("%d-%02d", period.getYear(), period.getMonthValue());
        String yearMonthCompact = String.format("%d%02d", period.getYear(), period.getMonthValue());
        return Arrays.asList(monthOnly, yearMonth, yearMonthCompact);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 档案聚合结果
     */
    public static class ArchiveAggregation {
        public final BigDecimal debitTotal;
        public final BigDecimal creditTotal;
        public final Set<String> voucherIds;
        public final Set<String> archiveCodes;
        public final int missingMetadataCount;
        public final int metadataParseErrorCount;
        public final int missingDocDateCount;
        public final int outOfRangeCount;
        public final String fondsCode;

        public ArchiveAggregation(BigDecimal debitTotal, BigDecimal creditTotal, Set<String> voucherIds,
                                  Set<String> archiveCodes, int missingMetadataCount, int metadataParseErrorCount,
                                  int missingDocDateCount, int outOfRangeCount, String fondsCode) {
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
            this.voucherIds = voucherIds;
            this.archiveCodes = archiveCodes;
            this.missingMetadataCount = missingMetadataCount;
            this.metadataParseErrorCount = metadataParseErrorCount;
            this.missingDocDateCount = missingDocDateCount;
            this.outOfRangeCount = outOfRangeCount;
            this.fondsCode = fondsCode;
        }

        public static ArchiveAggregation empty() {
            return new ArchiveAggregation(BigDecimal.ZERO, BigDecimal.ZERO, new LinkedHashSet<>(), new LinkedHashSet<>(),
                    0, 0, 0, 0, null);
        }
    }

    /**
     * 科目聚合结果
     */
    public static class SubjectAggregation {
        public boolean metadataMissing = false;
        public boolean parseError = false;
        public boolean matched = false;
        public BigDecimal debit = BigDecimal.ZERO;
        public BigDecimal credit = BigDecimal.ZERO;
    }
}
