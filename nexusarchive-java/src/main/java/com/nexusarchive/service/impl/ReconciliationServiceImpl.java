// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework、等
// Output: ReconciliationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.AccountSummaryDTO;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ReconciliationRecordMapper;
import com.nexusarchive.service.ReconciliationService;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final int PERIOD_TIMEOUT_SECONDS = 60;
    private static final int ATTACHMENT_BATCH_SIZE = 500;
    private static final String ARCHIVE_STATUS_ARCHIVED = "archived";
    private static final String PRE_ARCHIVE_STATUS_ARCHIVED = "ARCHIVED";
    private static final String SUBJECT_CODE_ALL = "ALL";
    
    // ✅ P1 修复: 限制最大并发度为 4,避免连接池耗尽
    private static final int MAX_CONCURRENT_PERIODS = 4;
    private final java.util.concurrent.Semaphore concurrencyLimiter = new java.util.concurrent.Semaphore(MAX_CONCURRENT_PERIODS);

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    // 线程池：注入Spring管理的Bean (修复资源泄漏)
    private final ExecutorService reconciliationExecutor;
    private final ObjectMapper objectMapper;

    @Override
    // 移除外层事务，避免并行流中的事务上下文和连接问题 (Critical #4 Fix)
    public ReconciliationRecord performReconciliation(Long configId, String subjectCode,
            LocalDate startDate, LocalDate endDate,
            String operatorId) {
        validateInputs(configId, subjectCode, startDate, endDate);

        String normalizedSubjectCode = normalizeOptionalSubject(subjectCode);
        boolean subjectMode = hasText(normalizedSubjectCode);
        String subjectCodeForRecord = subjectMode ? normalizedSubjectCode : SUBJECT_CODE_ALL;

        log.info("Starting Parallel Three-in-One Reconciliation: configId={}, subject={}, range={} to {}, mode={}",
                configId, subjectCodeForRecord, startDate, endDate, subjectMode ? "SUBJECT" : "VOUCHER_ONLY");

        // 1. 获取 ERP 配置
        ErpConfig configEntity = erpConfigMapper.selectById(configId);
        if (configEntity == null) {
            throw new RuntimeException("ERP 配置不存在");
        }

        ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
        if (adapter == null) {
            return saveReconciliationResult(buildErrorRecord(configEntity, configId, subjectCodeForRecord, startDate, endDate,
                    operatorId, "ERP适配器未注册: " + configEntity.getErpType()));
        }

        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig;
        try {
            dtoConfig = convertToDto(configEntity);
        } catch (RuntimeException e) {
            return saveReconciliationResult(buildErrorRecord(configEntity, configId, subjectCodeForRecord, startDate, endDate,
                    operatorId, "ERP配置解析失败: " + e.getMessage()));
        }

        String accbookCode = dtoConfig.getAccbookCode();

        // 2. 将时间范围按月切分（并行分片策略）
        List<YearMonth> periods = buildPeriods(startDate, endDate);

        // 3. 并行执行各月份的核对任务 (账 - 凭) - ✅ P1 修复: 添加 Semaphore 限流
        List<CompletableFuture<PeriodResult>> futures = periods.stream()
                .map(period -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // ✅ 获取信号量,限制并发度
                        concurrencyLimiter.acquire();
                        return computePeriodResult(period, startDate, endDate, normalizedSubjectCode, subjectMode,
                                accbookCode, adapter, dtoConfig);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return PeriodResult.error(period, "任务被中断");
                    } finally {
                        // ✅ 释放信号量
                        concurrencyLimiter.release();
                    }
                }, reconciliationExecutor)
                        .orTimeout(PERIOD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> PeriodResult.error(period,
                                "Period execution failed: " + (ex.getMessage() == null ? "timeout" : ex.getMessage()))))
                .collect(Collectors.toList());

        // 等待所有分片完成并聚合结果
        List<PeriodResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        // 汇总聚合数据
        BigDecimal totalErpDebit = results.stream()
                .map(r -> safeErpSummary(r).debitTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalErpCredit = results.stream()
                .map(r -> safeErpSummary(r).creditTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalErpVouchers = results.stream().mapToInt(r -> safeErpSummary(r).voucherCount).sum();

        BigDecimal totalArcDebit = results.stream()
                .map(r -> safeArchiveAggregation(r).debitTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalArcCredit = results.stream()
                .map(r -> safeArchiveAggregation(r).creditTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<String> allVoucherIds = new LinkedHashSet<>();
        Set<String> allArchiveCodes = new LinkedHashSet<>();
        for (PeriodResult result : results) {
            ArchiveAggregation agg = safeArchiveAggregation(result);
            allVoucherIds.addAll(agg.voucherIds);
            allArchiveCodes.addAll(agg.archiveCodes);
        }
        int totalArcVouchers = allVoucherIds.size();

        int totalMissingMetadata = results.stream().mapToInt(r -> safeArchiveAggregation(r).missingMetadataCount).sum();
        int totalMetadataParseErrors = results.stream().mapToInt(r -> safeArchiveAggregation(r).metadataParseErrorCount)
                .sum();
        int totalMissingDocDate = results.stream().mapToInt(r -> safeArchiveAggregation(r).missingDocDateCount).sum();
        int totalOutOfRange = results.stream().mapToInt(r -> safeArchiveAggregation(r).outOfRangeCount).sum();

        List<String> errorMessages = results.stream()
                .flatMap(r -> buildErrorMessages(r).stream())
                .collect(Collectors.toList());

        // 证据链校验 (证)
        EvidenceSummary evidenceSummary = verifyEvidence(new ArrayList<>(allArchiveCodes));

        String status;
        StringBuilder message = new StringBuilder("核对完成。");

        if (!errorMessages.isEmpty()) {
            log.error("Reconciliation failed: configId={}, subject={}, range={} to {}, errors={}",
                    configId, subjectCodeForRecord, startDate, endDate, errorMessages);
            status = "ERROR";
            message.append(" ERP汇总获取失败: ").append(String.join("; ", errorMessages));
        } else {
            boolean discrepancy = false;
            if (subjectMode) {
                if (totalErpDebit.compareTo(totalArcDebit) != 0) {
                    discrepancy = true;
                    message.append(String.format("总借方不一致: ERP=%s, 档案=%s。 ", totalErpDebit, totalArcDebit));
                }
                if (totalErpCredit.compareTo(totalArcCredit) != 0) {
                    discrepancy = true;
                    message.append(String.format("总贷方不一致: ERP=%s, 档案=%s。 ", totalErpCredit, totalArcCredit));
                }
            } else {
                message.append("按凭证级一致性核对(不含科目分录)。 ");
            }
            if (totalErpVouchers != totalArcVouchers) {
                discrepancy = true;
                message.append(String.format("总笔数不一致: ERP=%d, 档案=%d。 ", totalErpVouchers, totalArcVouchers));
            }
            if (evidenceSummary.missingEvidenceCount > 0) {
                discrepancy = true;
                message.append(String.format("发现 %d 笔凭证无原始证据。 ", evidenceSummary.missingEvidenceCount));
            }
            if (evidenceSummary.invalidEvidenceCount > 0) {
                discrepancy = true;
                message.append(String.format("发现 %d 笔凭证证据链不完整。 ", evidenceSummary.invalidEvidenceCount));
            }
            if (subjectMode) {
                if (totalMissingMetadata > 0) {
                    discrepancy = true;
                    message.append(String.format("发现 %d 笔凭证缺少科目分录元数据。 ", totalMissingMetadata));
                }
                if (totalMetadataParseErrors > 0) {
                    discrepancy = true;
                    message.append(String.format("发现 %d 笔凭证元数据解析失败。 ", totalMetadataParseErrors));
                }
            }
            if (totalMissingDocDate > 0) {
                discrepancy = true;
                message.append(String.format("发现 %d 笔凭证缺少业务日期。 ", totalMissingDocDate));
            }
            if (totalOutOfRange > 0) {
                discrepancy = true;
                message.append(String.format("发现 %d 笔凭证日期超出核对范围。 ", totalOutOfRange));
            }
            status = discrepancy ? "DISCREPANCY" : "SUCCESS";
        }

        String subjectName = subjectMode
                ? results.stream()
                        .map(r -> safeErpSummary(r).subjectName)
                        .filter(this::hasText)
                        .findFirst()
                        .orElse("")
                : "";
        String fondsCode = resolveFondsCode(accbookCode, results);

        Map<String, Object> snapshot = buildSnapshot(configId, accbookCode, subjectCodeForRecord, startDate, endDate,
                subjectMode, results, evidenceSummary, errorMessages);

        ReconciliationRecord record = ReconciliationRecord.builder()
                .configId(configId)
                .accbookCode(accbookCode)
                .reconStartDate(startDate)
                .reconEndDate(endDate)
                .fondsCode(fondsCode)
                .fiscalYear(String.valueOf(startDate.getYear()))
                .fiscalPeriod(String.format("%02d", startDate.getMonthValue())) // 记录跨度起点
                .subjectCode(subjectCodeForRecord)
                .subjectName(subjectName)
                .erpDebitTotal(totalErpDebit)
                .erpCreditTotal(totalErpCredit)
                .erpVoucherCount(totalErpVouchers)
                .arcDebitTotal(totalArcDebit)
                .arcCreditTotal(totalArcCredit)
                .arcVoucherCount(totalArcVouchers)
                .attachmentCount(evidenceSummary.totalAttachments)
                .attachmentMissingCount(evidenceSummary.missingEvidenceCount)
                .reconStatus(status)
                .reconMessage(message.toString())
                .reconTime(LocalDateTime.now())
                .operatorId(operatorId)
                .sourceSystem(configEntity.getName())
                .snapshotData(snapshot)
                .build();

        // 独立事务保存结果
        return saveReconciliationResult(record);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public ReconciliationRecord saveReconciliationResult(ReconciliationRecord record) {
        ReconciliationRecord existing = findExistingRecord(record);
        if (existing != null) {
            record.setId(existing.getId());
            reconciliationRecordMapper.updateById(record);
            return record;
        }
        reconciliationRecordMapper.insert(record);
        return record;
    }

    @Override
    public List<ReconciliationRecord> getHistory(Long configId) {
        ErpConfig config = erpConfigMapper.selectById(configId);
        if (config == null) {
            return Collections.emptyList();
        }

        QueryWrapper<ReconciliationRecord> historyByConfig = new QueryWrapper<>();
        historyByConfig.eq("config_id", configId).orderByDesc("recon_time");
        List<ReconciliationRecord> records = reconciliationRecordMapper.selectList(historyByConfig);
        if (!records.isEmpty()) {
            return records;
        }

        QueryWrapper<ReconciliationRecord> historyBySource = new QueryWrapper<>();
        historyBySource.eq("source_system", config.getName()).orderByDesc("recon_time");
        return reconciliationRecordMapper.selectList(historyBySource);
    }

    private void validateInputs(Long configId, String subjectCode, LocalDate startDate, LocalDate endDate) {
        if (configId == null) {
            throw new IllegalArgumentException("configId不能为空");
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate/endDate不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate不能晚于endDate");
        }
    }

    private PeriodResult computePeriodResult(YearMonth period, LocalDate startDate, LocalDate endDate,
            String subjectCode, boolean subjectMode, String accbookCode, ErpAdapter adapter,
            com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig) {
        LocalDate pStart = period.atDay(1).isBefore(startDate) ? startDate : period.atDay(1);
        LocalDate pEnd = period.atEndOfMonth().isAfter(endDate) ? endDate : period.atEndOfMonth();

        List<Archive> archivedItems = fetchArchives(period, accbookCode);
        ErpSummaryResult erpSummary;
        ArchiveAggregation archiveAggregation;
        if (subjectMode) {
            erpSummary = fetchErpSummary(adapter, dtoConfig, subjectCode, pStart, pEnd);
            archiveAggregation = aggregateArchives(archivedItems, subjectCode, pStart, pEnd);
        } else {
            erpSummary = fetchErpVoucherCount(adapter, dtoConfig, pStart, pEnd);
            archiveAggregation = aggregateArchivesForVoucherOnly(archivedItems, pStart, pEnd);
        }

        log.info(
                "Reconciliation period summary: period={}, erpDebit={}, erpCredit={}, erpVouchers={}, arcDebit={}, arcCredit={}, arcVouchers={}, missingMetadata={}, missingDocDate={}, outOfRange={}, erpError={}",
                period,
                erpSummary.debitTotal,
                erpSummary.creditTotal,
                erpSummary.voucherCount,
                archiveAggregation.debitTotal,
                archiveAggregation.creditTotal,
                archiveAggregation.voucherIds.size(),
                archiveAggregation.missingMetadataCount,
                archiveAggregation.missingDocDateCount,
                archiveAggregation.outOfRangeCount,
                erpSummary.errorMessage);

        return new PeriodResult(period, erpSummary, archiveAggregation, null);
    }

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

            SubjectAggregation subjectAggregation = extractSubjectAggregation(archive.getCustomMetadata(), subjectCode);
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

    private ErpSummaryResult fetchErpVoucherCount(ErpAdapter adapter,
            com.nexusarchive.integration.erp.dto.ErpConfig config,
            LocalDate startDate, LocalDate endDate) {
        try {
            List<VoucherDTO> vouchers = adapter.syncVouchers(config, startDate, endDate);
            if (vouchers == null) {
                return ErpSummaryResult.error("ERP凭证同步返回空结果");
            }
            return ErpSummaryResult.ok(BigDecimal.ZERO, BigDecimal.ZERO, vouchers.size(), "");
        } catch (Exception e) {
            return ErpSummaryResult.error("ERP凭证同步失败: " + e.getMessage());
        }
    }

    private ErpSummaryResult fetchErpSummary(ErpAdapter adapter,
            com.nexusarchive.integration.erp.dto.ErpConfig config,
            String subjectCode, LocalDate startDate, LocalDate endDate) {
        try {
            List<AccountSummaryDTO> summaries = adapter.fetchAccountSummary(config, subjectCode, startDate, endDate);
            if (summaries == null) {
                return ErpSummaryResult.error("ERP返回空结果(适配器未实现或异常)");
            }
            if (summaries.isEmpty()) {
                return ErpSummaryResult.empty();
            }

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            int voucherCount = 0;
            String subjectName = "";

            for (AccountSummaryDTO summary : summaries) {
                if (summary == null) {
                    continue;
                }
                debit = debit.add(nullToZero(summary.getDebitTotal()));
                credit = credit.add(nullToZero(summary.getCreditTotal()));
                voucherCount += summary.getVoucherCount() == null ? 0 : summary.getVoucherCount();
                if (!hasText(subjectName) && hasText(summary.getSubjectName())) {
                    subjectName = summary.getSubjectName();
                }
            }

            return ErpSummaryResult.ok(debit, credit, voucherCount, subjectName);
        } catch (Exception e) {
            return ErpSummaryResult.error("ERP拉取失败: " + e.getMessage());
        }
    }

    private EvidenceSummary verifyEvidence(List<String> archiveCodes) {
        EvidenceSummary summary = new EvidenceSummary();
        if (archiveCodes == null || archiveCodes.isEmpty()) {
            return summary;
        }

        List<String> uniqueCodes = new ArrayList<>(new LinkedHashSet<>(archiveCodes));
        Map<String, List<ArcFileContent>> filesByCode = new HashMap<>();

        for (int i = 0; i < uniqueCodes.size(); i += ATTACHMENT_BATCH_SIZE) {
            int end = Math.min(i + ATTACHMENT_BATCH_SIZE, uniqueCodes.size());
            List<String> batch = uniqueCodes.subList(i, end);
            QueryWrapper<ArcFileContent> fileQuery = new QueryWrapper<>();
            fileQuery.select("archival_code", "file_name", "file_type", "file_hash", "hash_algorithm", "original_hash",
                    "current_hash", "timestamp_token", "sign_value", "certificate", "pre_archive_status")
                    .in("archival_code", batch);

            List<ArcFileContent> batchFiles = arcFileContentMapper.selectList(fileQuery);
            if (batchFiles == null) {
                continue;
            }
            summary.totalAttachments += batchFiles.size();
            for (ArcFileContent file : batchFiles) {
                filesByCode.computeIfAbsent(file.getArchivalCode(), key -> new ArrayList<>()).add(file);
            }
        }

        for (String code : uniqueCodes) {
            List<ArcFileContent> files = filesByCode.get(code);
            if (files == null || files.isEmpty()) {
                summary.missingEvidenceCount++;
                continue;
            }
            boolean hasArchived = files.stream().anyMatch(file -> !hasText(file.getPreArchiveStatus())
                    || PRE_ARCHIVE_STATUS_ARCHIVED.equalsIgnoreCase(file.getPreArchiveStatus()));
            if (!hasArchived) {
                summary.missingEvidenceCount++;
                continue;
            }
            boolean hasStandardFile = files.stream().anyMatch(this::isStandardFile);
            boolean hasHash = files.stream().anyMatch(this::hasHash);
            boolean hasSignature = files.stream().anyMatch(this::hasSignature);
            if (!hasStandardFile || !hasHash || !hasSignature) {
                summary.invalidEvidenceCount++;
            }
        }

        return summary;
    }

    private SubjectAggregation extractSubjectAggregation(String customMetadata, String subjectCode) {
        SubjectAggregation aggregation = new SubjectAggregation();
        if (!hasText(customMetadata)) {
            aggregation.metadataMissing = true;
            return aggregation;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(customMetadata);
        } catch (Exception e) {
            aggregation.parseError = true;
            return aggregation;
        }

        List<JsonNode> entries = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(entries::add);
        } else if (root.isObject()) {
            JsonNode body = root.get("body");
            if (body != null && body.isArray()) {
                body.forEach(entries::add);
            }
            JsonNode entriesNode = root.get("entries");
            if (entriesNode != null && entriesNode.isArray()) {
                entriesNode.forEach(entries::add);
            }
        }
        if (entries.isEmpty()) {
            aggregation.metadataMissing = true;
            return aggregation;
        }

        String normalizedSubject = normalizeSubjectCode(subjectCode);
        for (JsonNode entry : entries) {
            if (entry == null) {
                continue;
            }
            String entrySubject = normalizeSubjectCode(extractSubjectCode(entry));
            if (entrySubject == null || !entrySubject.equalsIgnoreCase(normalizedSubject)) {
                continue;
            }
            aggregation.matched = true;
            aggregation.debit = aggregation.debit.add(readBigDecimal(entry, "debit_org", "debitOrg", "debit",
                    "debit_original", "debitOriginal"));
            aggregation.credit = aggregation.credit.add(readBigDecimal(entry, "credit_org", "creditOrg", "credit",
                    "credit_original", "creditOriginal"));
        }

        return aggregation;
    }

    private String extractSubjectCode(JsonNode entry) {
        JsonNode accSubject = entry.get("accsubject");
        if (accSubject == null || accSubject.isNull()) {
            accSubject = entry.get("accSubject");
        }
        if (accSubject != null && accSubject.has("code")) {
            return accSubject.path("code").asText(null);
        }
        String direct = textValue(entry, "subjectCode", "accSubjectCode", "accountCode", "accountcode",
                "account_code");
        if (hasText(direct)) {
            return direct;
        }
        JsonNode accountNode = entry.get("account");
        if (accountNode != null && accountNode.has("code")) {
            return accountNode.path("code").asText(null);
        }
        return null;
    }

    /**
     * 从 JSON 节点读取 BigDecimal 值
     * ✅ P0 修复: 添加详细日志,继续尝试其他字段
     */
    private BigDecimal readBigDecimal(JsonNode node, String... fieldNames) {
        for (String field : fieldNames) {
            JsonNode valueNode = node.get(field);
            if (valueNode == null || valueNode.isNull()) {
                continue;
            }
            String text = valueNode.asText();
            if (!hasText(text)) {
                continue;
            }
            try {
                // ✅ 使用 BigDecimal 构造函数,并记录解析失败的情况
                BigDecimal value = new BigDecimal(text);
                return value;
            } catch (NumberFormatException e) {
                // ✅ 记录详细日志,便于排查问题
                log.warn("金额解析失败: field={}, value={}, error={}", 
                    field, text, e.getMessage());
                // ✅ 继续尝试下一个字段,而不是直接返回 ZERO
            }
        }
        // ✅ 所有字段都解析失败时,记录警告日志
        log.warn("所有金额字段均解析失败,返回 ZERO: fields={}", Arrays.toString(fieldNames));
        return BigDecimal.ZERO;
    }

    private List<YearMonth> buildPeriods(LocalDate startDate, LocalDate endDate) {
        List<YearMonth> periods = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        while (!start.isAfter(end)) {
            periods.add(start);
            start = start.plusMonths(1);
        }
        return periods;
    }

    private List<String> buildPeriodCandidates(YearMonth period) {
        String monthOnly = String.format("%02d", period.getMonthValue());
        String yearMonth = String.format("%d-%02d", period.getYear(), period.getMonthValue());
        String yearMonthCompact = String.format("%d%02d", period.getYear(), period.getMonthValue());
        return Arrays.asList(monthOnly, yearMonth, yearMonthCompact);
    }

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

    private String resolveFondsCode(String accbookCode, List<PeriodResult> results) {
        if (hasText(accbookCode)) {
            return accbookCode;
        }
        return results.stream()
                .map(r -> safeArchiveAggregation(r).fondsCode)
                .filter(this::hasText)
                .findFirst()
                .orElse("DEFAULT");
    }

    private Map<String, Object> buildSnapshot(Long configId, String accbookCode, String subjectCode,
            LocalDate startDate, LocalDate endDate, boolean subjectMode, List<PeriodResult> results,
            EvidenceSummary evidenceSummary, List<String> errorMessages) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("configId", configId);
        snapshot.put("accbookCode", accbookCode);
        snapshot.put("subjectCode", subjectCode);
        snapshot.put("mode", subjectMode ? "SUBJECT" : "VOUCHER_ONLY");
        snapshot.put("rangeStart", startDate.toString());
        snapshot.put("rangeEnd", endDate.toString());
        snapshot.put("erpErrors", errorMessages);
        snapshot.put("attachmentMissingCount", evidenceSummary.missingEvidenceCount);
        snapshot.put("attachmentInvalidCount", evidenceSummary.invalidEvidenceCount);

        List<Map<String, Object>> periodSnapshots = new ArrayList<>();
        for (PeriodResult result : results) {
            Map<String, Object> periodSnapshot = new LinkedHashMap<>();
            periodSnapshot.put("period", result.period.toString());
            ErpSummaryResult erpSummary = safeErpSummary(result);
            ArchiveAggregation archiveAggregation = safeArchiveAggregation(result);
            periodSnapshot.put("erpDebitTotal", erpSummary.debitTotal);
            periodSnapshot.put("erpCreditTotal", erpSummary.creditTotal);
            periodSnapshot.put("erpVoucherCount", erpSummary.voucherCount);
            periodSnapshot.put("arcDebitTotal", archiveAggregation.debitTotal);
            periodSnapshot.put("arcCreditTotal", archiveAggregation.creditTotal);
            periodSnapshot.put("arcVoucherCount", archiveAggregation.voucherIds.size());
            periodSnapshot.put("missingMetadataCount", archiveAggregation.missingMetadataCount);
            periodSnapshot.put("metadataParseErrorCount", archiveAggregation.metadataParseErrorCount);
            periodSnapshot.put("missingDocDateCount", archiveAggregation.missingDocDateCount);
            periodSnapshot.put("outOfRangeCount", archiveAggregation.outOfRangeCount);
            if (hasText(erpSummary.errorMessage)) {
                periodSnapshot.put("erpError", erpSummary.errorMessage);
            }
            if (hasText(result.errorMessage)) {
                periodSnapshot.put("error", result.errorMessage);
            }
            periodSnapshots.add(periodSnapshot);
        }
        snapshot.put("periods", periodSnapshots);

        return snapshot;
    }

    private List<String> buildErrorMessages(PeriodResult result) {
        List<String> messages = new ArrayList<>();
        if (result == null) {
            return messages;
        }
        if (hasText(result.errorMessage)) {
            messages.add(result.period + ": " + result.errorMessage);
        }
        if (result.erpSummary != null && hasText(result.erpSummary.errorMessage)
                && !result.erpSummary.errorMessage.equals(result.errorMessage)) {
            messages.add(result.period + ": " + result.erpSummary.errorMessage);
        }
        return messages;
    }

    private boolean isStandardFile(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        String type = file.getFileType();
        String name = file.getFileName();
        if (hasText(type)) {
            String normalized = type.trim().toLowerCase();
            if (normalized.contains("pdf") || normalized.contains("ofd")) {
                return true;
            }
        }
        if (hasText(name)) {
            String normalized = name.trim().toLowerCase();
            return normalized.endsWith(".pdf") || normalized.endsWith(".ofd");
        }
        return false;
    }

    private boolean hasHash(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        boolean hasValue = hasText(file.getFileHash()) || hasText(file.getOriginalHash())
                || hasText(file.getCurrentHash());
        return hasValue && hasText(file.getHashAlgorithm());
    }

    private boolean hasSignature(ArcFileContent file) {
        if (file == null) {
            return false;
        }
        return (file.getTimestampToken() != null && file.getTimestampToken().length > 0)
                || (file.getSignValue() != null && file.getSignValue().length > 0)
                || hasText(file.getCertificate());
    }

    private String textValue(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (hasText(text)) {
                    return text;
                }
            }
        }
        return null;
    }

    private String normalizeOptionalSubject(String subjectCode) {
        String normalized = normalizeSubjectCode(subjectCode);
        return hasText(normalized) ? normalized : null;
    }

    private String normalizeSubjectCode(String subjectCode) {
        return subjectCode == null ? null : subjectCode.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ErpSummaryResult safeErpSummary(PeriodResult result) {
        return result == null || result.erpSummary == null ? ErpSummaryResult.empty() : result.erpSummary;
    }

    private ArchiveAggregation safeArchiveAggregation(PeriodResult result) {
        return result == null || result.archiveAggregation == null ? ArchiveAggregation.empty() : result.archiveAggregation;
    }

    private ReconciliationRecord findExistingRecord(ReconciliationRecord record) {
        if (record == null || record.getConfigId() == null || record.getReconStartDate() == null
                || record.getReconEndDate() == null || !hasText(record.getSubjectCode())) {
            return null;
        }
        QueryWrapper<ReconciliationRecord> query = new QueryWrapper<>();
        query.eq("config_id", record.getConfigId())
                .eq("subject_code", record.getSubjectCode())
                .eq("recon_start_date", record.getReconStartDate())
                .eq("recon_end_date", record.getReconEndDate());
        return reconciliationRecordMapper.selectOne(query);
    }

    private ReconciliationRecord buildErrorRecord(ErpConfig configEntity, Long configId, String subjectCode,
            LocalDate startDate, LocalDate endDate, String operatorId, String errorMessage) {
        String subject = hasText(subjectCode) ? subjectCode : SUBJECT_CODE_ALL;
        return ReconciliationRecord.builder()
                .configId(configId)
                .reconStartDate(startDate)
                .reconEndDate(endDate)
                .fondsCode("DEFAULT")
                .fiscalYear(String.valueOf(startDate.getYear()))
                .fiscalPeriod(String.format("%02d", startDate.getMonthValue()))
                .subjectCode(subject)
                .subjectName("")
                .erpDebitTotal(BigDecimal.ZERO)
                .erpCreditTotal(BigDecimal.ZERO)
                .erpVoucherCount(0)
                .arcDebitTotal(BigDecimal.ZERO)
                .arcCreditTotal(BigDecimal.ZERO)
                .arcVoucherCount(0)
                .attachmentCount(0)
                .attachmentMissingCount(0)
                .reconStatus("ERROR")
                .reconMessage(errorMessage)
                .reconTime(LocalDateTime.now())
                .operatorId(operatorId)
                .sourceSystem(configEntity.getName())
                .snapshotData(Collections.singletonMap("error", errorMessage))
                .build();
    }

    private com.nexusarchive.integration.erp.dto.ErpConfig convertToDto(ErpConfig entity) {
        com.nexusarchive.integration.erp.dto.ErpConfig dto = new com.nexusarchive.integration.erp.dto.ErpConfig();
        dto.setId(String.valueOf(entity.getId()));
        dto.setName(entity.getName());
        dto.setAdapterType(entity.getErpType());

        if (entity.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entity.getConfigJson());
            dto.setBaseUrl(json.getStr("baseUrl"));
            dto.setAppKey(json.getStr("appKey", json.getStr("clientId")));
            String secret = json.getStr("appSecret", json.getStr("clientSecret"));
            try {
                dto.setAppSecret(SM4Utils.decryptStrict(secret));
            } catch (Exception e) {
                log.error("ERP配置 [{}] 密钥解密失败，请检查SM4_KEY配置", entity.getName());
                throw new RuntimeException("ERP密钥解密失败", e);
            }
            dto.setAccbookCode(json.getStr("accbookCode"));
            dto.setExtraConfig(entity.getConfigJson());
        }
        return dto;
    }

    private static class PeriodResult {
        final YearMonth period;
        final ErpSummaryResult erpSummary;
        final ArchiveAggregation archiveAggregation;
        final String errorMessage;

        private PeriodResult(YearMonth period, ErpSummaryResult erpSummary,
                ArchiveAggregation archiveAggregation, String errorMessage) {
            this.period = period;
            this.erpSummary = erpSummary;
            this.archiveAggregation = archiveAggregation;
            this.errorMessage = errorMessage;
        }

        static PeriodResult error(YearMonth period, String errorMessage) {
            return new PeriodResult(period, ErpSummaryResult.error(errorMessage), ArchiveAggregation.empty(),
                    errorMessage);
        }
    }

    private static class ErpSummaryResult {
        final BigDecimal debitTotal;
        final BigDecimal creditTotal;
        final int voucherCount;
        final String subjectName;
        final String errorMessage;

        private ErpSummaryResult(BigDecimal debitTotal, BigDecimal creditTotal, int voucherCount, String subjectName,
                String errorMessage) {
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
            this.voucherCount = voucherCount;
            this.subjectName = subjectName;
            this.errorMessage = errorMessage;
        }

        static ErpSummaryResult ok(BigDecimal debitTotal, BigDecimal creditTotal, int voucherCount, String subjectName) {
            return new ErpSummaryResult(debitTotal, creditTotal, voucherCount, subjectName, null);
        }

        static ErpSummaryResult empty() {
            return new ErpSummaryResult(BigDecimal.ZERO, BigDecimal.ZERO, 0, "", null);
        }

        static ErpSummaryResult error(String message) {
            return new ErpSummaryResult(BigDecimal.ZERO, BigDecimal.ZERO, 0, "", message);
        }
    }

    private static class ArchiveAggregation {
        final BigDecimal debitTotal;
        final BigDecimal creditTotal;
        final Set<String> voucherIds;
        final Set<String> archiveCodes;
        final int missingMetadataCount;
        final int metadataParseErrorCount;
        final int missingDocDateCount;
        final int outOfRangeCount;
        final String fondsCode;

        private ArchiveAggregation(BigDecimal debitTotal, BigDecimal creditTotal, Set<String> voucherIds,
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

        static ArchiveAggregation empty() {
            return new ArchiveAggregation(BigDecimal.ZERO, BigDecimal.ZERO, new LinkedHashSet<>(), new LinkedHashSet<>(),
                    0, 0, 0, 0, null);
        }
    }

    private static class EvidenceSummary {
        int totalAttachments = 0;
        int missingEvidenceCount = 0;
        int invalidEvidenceCount = 0;
    }

    private static class SubjectAggregation {
        boolean metadataMissing = false;
        boolean parseError = false;
        boolean matched = false;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
    }
}
