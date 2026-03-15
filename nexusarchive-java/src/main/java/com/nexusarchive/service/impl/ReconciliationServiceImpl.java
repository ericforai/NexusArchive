// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework
// Output: ReconciliationServiceImpl 类（对账协调层）
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ReconciliationRecordMapper;
import com.nexusarchive.service.ReconciliationService;
import com.nexusarchive.service.impl.reconciliation.ArchiveAggregator;
import com.nexusarchive.service.impl.reconciliation.ErpDataFetcher;
import com.nexusarchive.service.impl.reconciliation.EvidenceVerifier;
import com.nexusarchive.service.impl.reconciliation.ReconciliationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对账服务实现（协调层）
 * <p>
 * 负责 ERP 与档案数据的核对工作。
 * 具体数据获取和验证逻辑已委托给专用模块：
 * <ul>
 * <li>{@link com.nexusarchive.service.impl.reconciliation.ErpDataFetcher} - ERP数据获取</li>
 * <li>{@link com.nexusarchive.service.impl.reconciliation.ArchiveAggregator} - 档案数据聚合</li>
 * <li>{@link com.nexusarchive.service.impl.reconciliation.EvidenceVerifier} - 证据链验证</li>
 * <li>{@link com.nexusarchive.service.impl.reconciliation.SubjectExtractor} - 科目分录提取</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final int PERIOD_TIMEOUT_SECONDS = 60;
    private static final int MAX_CONCURRENT_PERIODS = 4;
    private static final String SUBJECT_CODE_ALL = "ALL";
    private final java.util.concurrent.Semaphore concurrencyLimiter = new java.util.concurrent.Semaphore(
            MAX_CONCURRENT_PERIODS);

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ReconciliationRecordMapper reconciliationRecordMapper;
    private final ExecutorService reconciliationExecutor;
    private final ObjectMapper objectMapper;

    // 委托的专用模块
    private final com.nexusarchive.service.impl.reconciliation.ErpDataFetcher erpDataFetcher;
    private final com.nexusarchive.service.impl.reconciliation.ArchiveAggregator archiveAggregator;
    private final com.nexusarchive.service.impl.reconciliation.EvidenceVerifier evidenceVerifier;

    @Override
    public ReconciliationRecord performReconciliation(Long configId, String subjectCode,
            LocalDate startDate, LocalDate endDate, String operatorId) {
        validateInputs(configId, subjectCode, startDate, endDate);

        String normalizedSubjectCode = ReconciliationUtils.normalizeOptionalSubject(subjectCode);
        boolean subjectMode = ReconciliationUtils.hasText(normalizedSubjectCode);
        String subjectCodeForRecord = subjectMode ? normalizedSubjectCode : SUBJECT_CODE_ALL;

        log.info("Starting Parallel Three-in-One Reconciliation: configId={}, subject={}, range={} to {}, mode={}",
                configId, subjectCodeForRecord, startDate, endDate, subjectMode ? "SUBJECT" : "VOUCHER_ONLY");

        ErpConfig configEntity = erpConfigMapper.selectById(configId);
        if (configEntity == null) {
            throw new RuntimeException("ERP 配置不存在");
        }

        String accbookCode = extractAccbookCode(configEntity);

        // 按月切分时间范围并并行执行
        List<YearMonth> periods = ReconciliationUtils.buildPeriods(startDate, endDate);
        List<PeriodResult> results = executePeriodTasks(periods, startDate, endDate, normalizedSubjectCode,
                subjectMode, accbookCode, configId, subjectCode);

        // 聚合结果
        return aggregateResults(results, configId, accbookCode, subjectCodeForRecord, startDate, endDate,
                subjectMode, operatorId, configEntity);
    }

    /**
     * 提取账簿代码
     */
    private String extractAccbookCode(ErpConfig configEntity) {
        if (configEntity.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configEntity.getConfigJson());
            return json.getStr("accbookCode");
        }
        return null;
    }

    /**
     * 执行各月份的核对任务
     */
    private List<PeriodResult> executePeriodTasks(List<YearMonth> periods, LocalDate startDate, LocalDate endDate,
                                                    String normalizedSubjectCode, boolean subjectMode, String accbookCode,
                                                    Long configId, String subjectCode) {
        List<CompletableFuture<PeriodResult>> futures = periods.stream()
                .map(period -> CompletableFuture.supplyAsync(() -> {
                    try {
                        concurrencyLimiter.acquire();
                        return computePeriodResult(period, startDate, endDate, normalizedSubjectCode, subjectMode,
                                accbookCode, configId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return PeriodResult.error(period, "任务被中断");
                    } finally {
                        concurrencyLimiter.release();
                    }
                }, reconciliationExecutor)
                        .orTimeout(PERIOD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> PeriodResult.error(period,
                                "Period execution failed: " + (ex.getMessage() == null ? "timeout" : ex.getMessage()))))
                .collect(Collectors.toList());

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    /**
     * 计算单个期间的结果
     */
    private PeriodResult computePeriodResult(YearMonth period, LocalDate startDate, LocalDate endDate,
                                              String subjectCode, boolean subjectMode, String accbookCode,
                                              Long configId) {
        LocalDate pStart = period.atDay(1).isBefore(startDate) ? startDate : period.atDay(1);
        LocalDate pEnd = period.atEndOfMonth().isAfter(endDate) ? endDate : period.atEndOfMonth();

        com.nexusarchive.service.impl.reconciliation.ErpDataFetcher.ErpSummaryResult erpSummary;
        com.nexusarchive.service.impl.reconciliation.ArchiveAggregator.ArchiveAggregation archiveAggregation;

        if (subjectMode) {
            erpSummary = erpDataFetcher.fetchAccountSummary(configId, subjectCode, pStart, pEnd);
            archiveAggregation = archiveAggregator.aggregateBySubject(period, accbookCode, subjectCode, pStart,
                    pEnd);
        } else {
            erpSummary = erpDataFetcher.fetchVoucherCount(configId, pStart, pEnd);
            archiveAggregation = archiveAggregator.aggregateByVoucher(period, accbookCode, pStart, pEnd);
        }

        log.info("Reconciliation period summary: period={}, erpDebit={}, erpCredit={}, erpVouchers={}, arcDebit={}, arcCredit={}, arcVouchers={}",
                period, erpSummary.debitTotal, erpSummary.creditTotal, erpSummary.voucherCount,
                archiveAggregation.debitTotal, archiveAggregation.creditTotal, archiveAggregation.voucherIds.size());

        return new PeriodResult(period, erpSummary, archiveAggregation, null);
    }

    /**
     * 聚合所有期间的结果
     */
    private ReconciliationRecord aggregateResults(List<PeriodResult> results, Long configId, String accbookCode,
                                                   String subjectCodeForRecord, LocalDate startDate, LocalDate endDate,
                                                   boolean subjectMode, String operatorId, ErpConfig configEntity) {
        // 汇总 ERP 数据
        BigDecimal totalErpDebit = results.stream()
                .map(r -> r.erpSummary.debitTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalErpCredit = results.stream()
                .map(r -> r.erpSummary.creditTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalErpVouchers = results.stream().mapToInt(r -> r.erpSummary.voucherCount).sum();

        // 汇总档案数据
        BigDecimal totalArcDebit = results.stream()
                .map(r -> r.archiveAggregation.debitTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalArcCredit = results.stream()
                .map(r -> r.archiveAggregation.creditTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<String> allVoucherIds = new LinkedHashSet<>();
        Set<String> allArchiveCodes = new LinkedHashSet<>();
        for (PeriodResult result : results) {
            allVoucherIds.addAll(result.archiveAggregation.voucherIds);
            allArchiveCodes.addAll(result.archiveAggregation.archiveCodes);
        }
        int totalArcVouchers = allVoucherIds.size();

        int totalMissingMetadata = results.stream().mapToInt(r -> r.archiveAggregation.missingMetadataCount).sum();
        int totalMetadataParseErrors = results.stream().mapToInt(r -> r.archiveAggregation.metadataParseErrorCount).sum();
        int totalMissingDocDate = results.stream().mapToInt(r -> r.archiveAggregation.missingDocDateCount).sum();
        int totalOutOfRange = results.stream().mapToInt(r -> r.archiveAggregation.outOfRangeCount).sum();

        List<String> errorMessages = results.stream()
                .filter(r -> ReconciliationUtils.hasText(r.errorMessage))
                .map(r -> r.period + ": " + r.errorMessage)
                .collect(Collectors.toList());

        // 证据链校验
        com.nexusarchive.service.impl.reconciliation.EvidenceVerifier.EvidenceSummary evidenceSummary = evidenceVerifier.verifyEvidence(new ArrayList<>(allArchiveCodes));

        // 构建状态和消息
        String status = buildStatus(results, errorMessages, evidenceSummary, subjectMode, totalErpDebit, totalArcDebit,
                totalErpCredit, totalArcCredit, totalErpVouchers, totalArcVouchers, totalMissingMetadata,
                totalMetadataParseErrors, totalMissingDocDate, totalOutOfRange);
        StringBuilder message = buildMessage(results, errorMessages, evidenceSummary, subjectMode, totalErpDebit,
                totalArcDebit, totalErpCredit, totalArcCredit, totalErpVouchers, totalArcVouchers, totalMissingMetadata,
                totalMetadataParseErrors, totalMissingDocDate, totalOutOfRange);

        String subjectName = subjectMode
                ? results.stream()
                        .map(r -> r.erpSummary.subjectName)
                        .filter(ReconciliationUtils::hasText)
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
                .fiscalPeriod(String.format("%02d", startDate.getMonthValue()))
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

        return saveReconciliationResult(record);
    }

    /**
     * 构建核对状态
     */
    private String buildStatus(List<PeriodResult> results, List<String> errorMessages,
                              com.nexusarchive.service.impl.reconciliation.EvidenceVerifier.EvidenceSummary evidenceSummary,
                              boolean subjectMode, BigDecimal totalErpDebit, BigDecimal totalArcDebit,
                              BigDecimal totalErpCredit, BigDecimal totalArcCredit, int totalErpVouchers,
                              int totalArcVouchers, int totalMissingMetadata, int totalMetadataParseErrors,
                              int totalMissingDocDate, int totalOutOfRange) {
        if (!errorMessages.isEmpty()) {
            return "ERROR";
        }
        boolean discrepancy = false;
        if (subjectMode) {
            if (totalErpDebit.compareTo(totalArcDebit) != 0 || totalErpCredit.compareTo(totalArcCredit) != 0) {
                discrepancy = true;
            }
        }
        if (totalErpVouchers != totalArcVouchers) {
            discrepancy = true;
        }
        if (evidenceSummary.missingEvidenceCount > 0 || evidenceSummary.invalidEvidenceCount > 0) {
            discrepancy = true;
        }
        if (totalMissingMetadata > 0 || totalMetadataParseErrors > 0 || totalMissingDocDate > 0 || totalOutOfRange > 0) {
            discrepancy = true;
        }
        return discrepancy ? "DISCREPANCY" : OperationResult.SUCCESS;
    }

    /**
     * 构建核对消息
     */
    private StringBuilder buildMessage(List<PeriodResult> results, List<String> errorMessages,
                                      com.nexusarchive.service.impl.reconciliation.EvidenceVerifier.EvidenceSummary evidenceSummary, boolean subjectMode,
                                      BigDecimal totalErpDebit, BigDecimal totalArcDebit, BigDecimal totalErpCredit,
                                      BigDecimal totalArcCredit, int totalErpVouchers, int totalArcVouchers,
                                      int totalMissingMetadata, int totalMetadataParseErrors, int totalMissingDocDate,
                                      int totalOutOfRange) {
        StringBuilder message = new StringBuilder("核对完成。");

        if (!errorMessages.isEmpty()) {
            message.append(" ERP汇总获取失败: ").append(String.join("; ", errorMessages));
        } else {
            if (subjectMode) {
                if (totalErpDebit.compareTo(totalArcDebit) != 0) {
                    message.append(String.format("总借方不一致: ERP=%s, 档案=%s。 ", totalErpDebit, totalArcDebit));
                }
                if (totalErpCredit.compareTo(totalArcCredit) != 0) {
                    message.append(String.format("总贷方不一致: ERP=%s, 档案=%s。 ", totalErpCredit, totalArcCredit));
                }
            } else {
                message.append("按凭证级一致性核对(不含科目分录)。 ");
            }
            if (totalErpVouchers != totalArcVouchers) {
                message.append(String.format("总笔数不一致: ERP=%d, 档案=%d。 ", totalErpVouchers, totalArcVouchers));
            }
            if (evidenceSummary.missingEvidenceCount > 0) {
                message.append(String.format("发现 %d 笔凭证无原始证据。 ", evidenceSummary.missingEvidenceCount));
            }
            if (evidenceSummary.invalidEvidenceCount > 0) {
                message.append(String.format("发现 %d 笔凭证证据链不完整。 ", evidenceSummary.invalidEvidenceCount));
            }
            if (subjectMode && totalMissingMetadata > 0) {
                message.append(String.format("发现 %d 笔凭证缺少科目分录元数据。 ", totalMissingMetadata));
            }
            if (subjectMode && totalMetadataParseErrors > 0) {
                message.append(String.format("发现 %d 笔凭证元数据解析失败。 ", totalMetadataParseErrors));
            }
            if (totalMissingDocDate > 0) {
                message.append(String.format("发现 %d 笔凭证缺少业务日期。 ", totalMissingDocDate));
            }
            if (totalOutOfRange > 0) {
                message.append(String.format("发现 %d 笔凭证日期超出核对范围。 ", totalOutOfRange));
            }
        }
        return message;
    }

    /**
     * 解析全宗代码
     */
    private String resolveFondsCode(String accbookCode, List<PeriodResult> results) {
        if (ReconciliationUtils.hasText(accbookCode)) {
            return accbookCode;
        }
        return results.stream()
                .map(r -> r.archiveAggregation.fondsCode)
                .filter(ReconciliationUtils::hasText)
                .findFirst()
                .orElse("DEFAULT");
    }

    /**
     * 构建快照数据
     */
    private Map<String, Object> buildSnapshot(Long configId, String accbookCode, String subjectCode,
                                               LocalDate startDate, LocalDate endDate, boolean subjectMode,
                                               List<PeriodResult> results,
                                               com.nexusarchive.service.impl.reconciliation.EvidenceVerifier.EvidenceSummary evidenceSummary,
                                               List<String> errorMessages) {
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
            periodSnapshot.put("erpDebitTotal", result.erpSummary.debitTotal);
            periodSnapshot.put("erpCreditTotal", result.erpSummary.creditTotal);
            periodSnapshot.put("erpVoucherCount", result.erpSummary.voucherCount);
            periodSnapshot.put("arcDebitTotal", result.archiveAggregation.debitTotal);
            periodSnapshot.put("arcCreditTotal", result.archiveAggregation.creditTotal);
            periodSnapshot.put("arcVoucherCount", result.archiveAggregation.voucherIds.size());
            periodSnapshot.put("missingMetadataCount", result.archiveAggregation.missingMetadataCount);
            periodSnapshot.put("metadataParseErrorCount", result.archiveAggregation.metadataParseErrorCount);
            periodSnapshot.put("missingDocDateCount", result.archiveAggregation.missingDocDateCount);
            periodSnapshot.put("outOfRangeCount", result.archiveAggregation.outOfRangeCount);
            if (ReconciliationUtils.hasText(result.erpSummary.errorMessage)) {
                periodSnapshot.put("erpError", result.erpSummary.errorMessage);
            }
            if (ReconciliationUtils.hasText(result.errorMessage)) {
                periodSnapshot.put("error", result.errorMessage);
            }
            periodSnapshots.add(periodSnapshot);
        }
        snapshot.put("periods", periodSnapshots);

        return snapshot;
    }

    @Override
    public List<ReconciliationRecord> getHistory(Long configId) {
        ErpConfig config = erpConfigMapper.selectById(configId);
        if (config == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<ReconciliationRecord> historyByConfig = new LambdaQueryWrapper<>();
        historyByConfig.eq(ReconciliationRecord::getConfigId, configId).orderByDesc(ReconciliationRecord::getReconTime);
        List<ReconciliationRecord> records = reconciliationRecordMapper.selectList(historyByConfig);
        if (!records.isEmpty()) {
            return records;
        }

        LambdaQueryWrapper<ReconciliationRecord> historyBySource = new LambdaQueryWrapper<>();
        historyBySource.eq(ReconciliationRecord::getSourceSystem, config.getName()).orderByDesc(ReconciliationRecord::getReconTime);
        return reconciliationRecordMapper.selectList(historyBySource);
    }

    /**
     * 保存对账结果
     * <p>
     * 使用 REQUIRES_NEW 传播属性的原因：
     * 1. 对账过程是长时间运行的只读操作（从 ERP 和档案系统聚合数据）
     * 2. 对账记录的保存应该独立于对账过程本身
     * 3. 即使对账计算过程中出现异常，已经成功计算的结果也应该被保存
     * 4. 允许在外部事务失败时，对账记录仍然能保存（用于故障恢复和审计追踪）
     * </p>
     *
     * @param record 对账记录
     * @return 保存后的对账记录
     */
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

    private ReconciliationRecord findExistingRecord(ReconciliationRecord record) {
        if (record == null || record.getConfigId() == null || record.getReconStartDate() == null
                || record.getReconEndDate() == null || !ReconciliationUtils.hasText(record.getSubjectCode())) {
            return null;
        }
        LambdaQueryWrapper<ReconciliationRecord> query = new LambdaQueryWrapper<>();
        query.eq(ReconciliationRecord::getConfigId, record.getConfigId())
                .eq(ReconciliationRecord::getSubjectCode, record.getSubjectCode())
                .eq(ReconciliationRecord::getReconStartDate, record.getReconStartDate())
                .eq(ReconciliationRecord::getReconEndDate, record.getReconEndDate());
        return reconciliationRecordMapper.selectOne(query);
    }

    /**
     * 期间结果（内部类）
     */
    private static class PeriodResult {
        final YearMonth period;
        final com.nexusarchive.service.impl.reconciliation.ErpDataFetcher.ErpSummaryResult erpSummary;
        final com.nexusarchive.service.impl.reconciliation.ArchiveAggregator.ArchiveAggregation archiveAggregation;
        final String errorMessage;

        private PeriodResult(YearMonth period,
                             com.nexusarchive.service.impl.reconciliation.ErpDataFetcher.ErpSummaryResult erpSummary,
                             com.nexusarchive.service.impl.reconciliation.ArchiveAggregator.ArchiveAggregation archiveAggregation,
                             String errorMessage) {
            this.period = period;
            this.erpSummary = erpSummary;
            this.archiveAggregation = archiveAggregation;
            this.errorMessage = errorMessage;
        }

        static PeriodResult error(YearMonth period, String errorMessage) {
            return new PeriodResult(period,
                    com.nexusarchive.service.impl.reconciliation.ErpDataFetcher.ErpSummaryResult.error(errorMessage),
                    com.nexusarchive.service.impl.reconciliation.ArchiveAggregator.ArchiveAggregation.empty(),
                    errorMessage);
        }
    }
}
