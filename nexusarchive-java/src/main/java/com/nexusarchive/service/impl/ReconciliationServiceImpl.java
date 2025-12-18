package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.AccountSummaryDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    // 线程池：用于并行对账分片处理
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 8));

    @Override
    @Transactional
    public ReconciliationRecord performReconciliation(Long configId, String subjectCode, 
                                                     LocalDate startDate, LocalDate endDate, 
                                                     String operatorId) {
        log.info("Starting Parallel Three-in-One Reconciliation: configId={}, subject={}, range={} to {}", 
                configId, subjectCode, startDate, endDate);

        // 1. 获取 ERP 配置
        ErpConfig configEntity = erpConfigMapper.selectById(configId);
        if (configEntity == null) throw new RuntimeException("ERP 配置不存在");

        ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = convertToDto(configEntity);

        // 2. 将时间范围按月切分（并行分片策略）
        List<YearMonth> periods = new ArrayList<>();
        YearMonth start = YearMonth.from(startDate);
        YearMonth end = YearMonth.from(endDate);
        while (!start.isAfter(end)) {
            periods.add(start);
            start = start.plusMonths(1);
        }

        // 3. 并行执行各月份的核对任务 (账 - 凭)
        List<CompletableFuture<PeriodResult>> futures = periods.stream()
                .map(period -> CompletableFuture.supplyAsync(() -> {
                    LocalDate pStart = period.atDay(1).isBefore(startDate) ? startDate : period.atDay(1);
                    LocalDate pEnd = period.atEndOfMonth().isAfter(endDate) ? endDate : period.atEndOfMonth();
                    
                    // Fetch ERP Summary
                    List<AccountSummaryDTO> erpSummaries = adapter.fetchAccountSummary(dtoConfig, subjectCode, pStart, pEnd);
                    AccountSummaryDTO erpSub = (erpSummaries != null && !erpSummaries.isEmpty()) ? erpSummaries.get(0) : 
                            AccountSummaryDTO.builder().debitTotal(BigDecimal.ZERO).creditTotal(BigDecimal.ZERO).voucherCount(0).build();
                    
                    // Aggregate Archive
                    LambdaQueryWrapper<Archive> archiveQuery = new LambdaQueryWrapper<>();
                    archiveQuery.eq(Archive::getFiscalYear, String.valueOf(period.getYear()))
                                .eq(Archive::getFiscalPeriod, String.format("%02d", period.getMonthValue()))
                                .eq(Archive::getStatus, "archived");
                    List<Archive> archivedItems = archiveMapper.selectList(archiveQuery);
                    
                    BigDecimal arcDebit = archivedItems.stream()
                            .map(a -> a.getAmount() != null ? a.getAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    return new PeriodResult(period, erpSub, arcDebit, archivedItems);
                }, executorService))
                .collect(Collectors.toList());

        // 等待所有分片完成并聚合结果
        List<PeriodResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        // 4. 汇总聚合数据
        BigDecimal totalErpDebit = results.stream().map(r -> r.erpSummary.getDebitTotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalErpVouchers = results.stream().mapToInt(r -> r.erpSummary.getVoucherCount()).sum();
        BigDecimal totalArcDebit = results.stream().map(r -> r.archiveAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalArcVouchers = results.stream().mapToInt(r -> r.archivedItems.size()).sum();
        
        // 5. 批量核对原始证据 (证 - 优化 N+1 查询为批量查询)
        List<String> allArchiveCodes = results.stream()
                .flatMap(r -> r.archivedItems.stream().map(Archive::getArchiveCode))
                .collect(Collectors.toList());
        
        int totalAttachments = 0;
        int missingEvidenceCount = 0;
        
        if (!allArchiveCodes.isEmpty()) {
            // 这里使用更高效的大表分片查询或 Join (PoC 环境暂时使用 In 列表，生产环境应使用存储过程或临时表)
            // 优化：一次性查询所有有附件的档案号
            LambdaQueryWrapper<ArcFileContent> fileQuery = new LambdaQueryWrapper<>();
            fileQuery.select(ArcFileContent::getArchivalCode)
                     .in(ArcFileContent::getArchivalCode, allArchiveCodes);
            
            List<ArcFileContent> allFiles = arcFileContentMapper.selectList(fileQuery);
            totalAttachments = allFiles.size();
            
            Map<String, Long> fileCounts = allFiles.stream()
                    .collect(Collectors.groupingBy(ArcFileContent::getArchivalCode, Collectors.counting()));
            
            for (String code : allArchiveCodes) {
                if (!fileCounts.containsKey(code)) {
                    missingEvidenceCount++;
                }
            }
        }

        // 5. 构建最终记录与状态判断
        String status = "SUCCESS";
        StringBuilder message = new StringBuilder("全链路并行核对完成。");
        
        if (totalErpDebit.compareTo(totalArcDebit) != 0) {
            status = "DISCREPANCY";
            message.append(String.format("总金额不一致: ERP=%s, 档案=%s。 ", totalErpDebit, totalArcDebit));
        }
        if (totalErpVouchers != totalArcVouchers) {
            status = "DISCREPANCY";
            message.append(String.format("总笔数不一致: ERP=%d, 档案=%d。 ", totalErpVouchers, totalArcVouchers));
        }
        if (missingEvidenceCount > 0) {
            status = "DISCREPANCY";
            message.append(String.format("发现 %d 笔凭证缺失原始证据。 ", missingEvidenceCount));
        }

        ReconciliationRecord record = ReconciliationRecord.builder()
                .fondsCode(results.isEmpty() || results.get(0).archivedItems.isEmpty() ? "DEFAULT" : results.get(0).archivedItems.get(0).getFondsNo())
                .fiscalYear(String.valueOf(startDate.getYear()))
                .fiscalPeriod(String.format("%02d", startDate.getMonthValue())) // 记录跨度起点
                .subjectCode(subjectCode)
                .subjectName(results.isEmpty() ? "" : results.get(0).erpSummary.getSubjectName())
                .erpDebitTotal(totalErpDebit)
                .erpCreditTotal(BigDecimal.ZERO) 
                .erpVoucherCount(totalErpVouchers)
                .arcDebitTotal(totalArcDebit)
                .arcCreditTotal(BigDecimal.ZERO)
                .arcVoucherCount(totalArcVouchers)
                .attachmentCount(totalAttachments)
                .attachmentMissingCount(missingEvidenceCount)
                .reconStatus(status)
                .reconMessage(message.toString())
                .reconTime(LocalDateTime.now())
                .operatorId(operatorId)
                .sourceSystem(configEntity.getName())
                .snapshotData(null) // TODO: Fix JSONB type handler for insert
                .build();

        reconciliationRecordMapper.insert(record);
        return record;
    }

    // 内部类：用于并行结果承载
    private static class PeriodResult {
        final YearMonth period;
        final AccountSummaryDTO erpSummary;
        final BigDecimal archiveAmount;
        final List<Archive> archivedItems;

        PeriodResult(YearMonth period, AccountSummaryDTO erpSummary, BigDecimal archiveAmount, List<Archive> archivedItems) {
            this.period = period;
            this.erpSummary = erpSummary;
            this.archiveAmount = archiveAmount;
            this.archivedItems = archivedItems;
        }
    }

    @Override
    public List<ReconciliationRecord> getHistory(Long configId) {
        ErpConfig config = erpConfigMapper.selectById(configId);
        if (config == null) return java.util.Collections.emptyList();
        
        return reconciliationRecordMapper.selectList(
                new LambdaQueryWrapper<ReconciliationRecord>()
                        .eq(ReconciliationRecord::getSourceSystem, config.getName())
                        .orderByDesc(ReconciliationRecord::getReconTime)
        );
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
            dto.setAppSecret(SM4Utils.decrypt(secret));
            dto.setAccbookCode(json.getStr("accbookCode"));
            dto.setExtraConfig(entity.getConfigJson());
        }
        return dto;
    }
}
