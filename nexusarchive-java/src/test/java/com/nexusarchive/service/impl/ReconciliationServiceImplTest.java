// Input: Jackson、org.junit、org.mockito、Java 标准库、等
// Output: ReconciliationServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceImplTest {

    @Mock
    private ErpConfigMapper erpConfigMapper;
    @Mock
    private ErpAdapterFactory erpAdapterFactory;
    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private ArcFileContentMapper arcFileContentMapper;
    @Mock
    private ReconciliationRecordMapper reconciliationRecordMapper;
    @Mock
    private ErpAdapter erpAdapter;

    private ExecutorService executorService;
    private ReconciliationServiceImpl reconciliationService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(1);
        reconciliationService = new ReconciliationServiceImpl(
                erpConfigMapper,
                erpAdapterFactory,
                archiveMapper,
                arcFileContentMapper,
                reconciliationRecordMapper,
                executorService,
                new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldComputeSubjectTotalsAndEvidence() {
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(1L)).thenReturn(config);
        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("60"))
                        .creditTotal(new BigDecimal("50"))
                        .voucherCount(2)
                        .subjectName("Cash")
                        .build()));

        List<Archive> archives = Arrays.asList(
                buildArchive("A1", "ARC1", "biz-1",
                        "[{\"debit_org\":60,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                        LocalDate.of(2024, 1, 5)),
                buildArchive("A2", "ARC2", "biz-2",
                        "[{\"debit_org\":0,\"credit_org\":50,\"accsubject\":{\"code\":\"1001\"}}]",
                        LocalDate.of(2024, 1, 10))
        );
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = Arrays.asList(
                buildAttachment("ARC1"),
                buildAttachment("ARC2")
        );
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.performReconciliation(1L, "1001",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "operator");

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordMapper).insert(recordCaptor.capture());
        ReconciliationRecord record = recordCaptor.getValue();

        assertThat(record.getReconStatus()).isEqualTo("SUCCESS");
        assertThat(record.getErpDebitTotal()).isEqualTo(new BigDecimal("60"));
        assertThat(record.getErpCreditTotal()).isEqualTo(new BigDecimal("50"));
        assertThat(record.getArcDebitTotal()).isEqualTo(new BigDecimal("60"));
        assertThat(record.getArcCreditTotal()).isEqualTo(new BigDecimal("50"));
        assertThat(record.getArcVoucherCount()).isEqualTo(2);
        assertThat(record.getAttachmentMissingCount()).isEqualTo(0);
        assertThat(record.getConfigId()).isEqualTo(1L);
        assertThat(record.getAccbookCode()).isEqualTo("AC01");
        assertThat(record.getSnapshotData()).isNotNull();
    }

    @Test
    void shouldFlagMissingEvidenceAsDiscrepancy() {
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(1L)).thenReturn(config);
        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("60"))
                        .creditTotal(new BigDecimal("50"))
                        .voucherCount(2)
                        .build()));

        List<Archive> archives = Arrays.asList(
                buildArchive("A1", "ARC1", "biz-1",
                        "[{\"debit_org\":60,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                        LocalDate.of(2024, 1, 5)),
                buildArchive("A2", "ARC2", "biz-2",
                        "[{\"debit_org\":0,\"credit_org\":50,\"accsubject\":{\"code\":\"1001\"}}]",
                        LocalDate.of(2024, 1, 10))
        );
        when(archiveMapper.selectList(any())).thenReturn(archives);
        when(arcFileContentMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.performReconciliation(1L, "1001",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "operator");

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordMapper).insert(recordCaptor.capture());
        ReconciliationRecord record = recordCaptor.getValue();

        assertThat(record.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(record.getAttachmentMissingCount()).isEqualTo(2);
    }

    @Test
    void shouldPersistErrorWhenErpFails() {
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(1L)).thenReturn(config);
        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("timeout"));

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.performReconciliation(1L, "1001",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "operator");

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordMapper).insert(recordCaptor.capture());
        ReconciliationRecord record = recordCaptor.getValue();

        assertThat(record.getReconStatus()).isEqualTo("ERROR");
        assertThat(record.getReconMessage()).contains("ERP汇总获取失败");
    }

    @Test
    void shouldUpdateExistingRecordForIdempotency() {
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(1L)).thenReturn(config);
        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("10"))
                        .creditTotal(new BigDecimal("0"))
                        .voucherCount(1)
                        .build()));

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());

        ReconciliationRecord existing = new ReconciliationRecord();
        existing.setId("existing-id");
        when(reconciliationRecordMapper.selectOne(any())).thenReturn(existing);

        reconciliationService.performReconciliation(1L, "1001",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "operator");

        verify(reconciliationRecordMapper, never()).insert(any(ReconciliationRecord.class));
        verify(reconciliationRecordMapper).updateById(any(ReconciliationRecord.class));
    }

    @Test
    void shouldReconcileVoucherOnlyWhenSubjectMissing() {
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(1L)).thenReturn(config);
        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.syncVouchers(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        VoucherDTO.builder().voucherId("v1").build(),
                        VoucherDTO.builder().voucherId("v2").build()));

        List<Archive> archives = Arrays.asList(
                buildArchive("A1", "ARC1", "biz-1", null, LocalDate.of(2024, 1, 5)),
                buildArchive("A2", "ARC2", "biz-2", null, LocalDate.of(2024, 1, 10))
        );
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = Arrays.asList(
                buildAttachment("ARC1"),
                buildAttachment("ARC2")
        );
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        reconciliationService.performReconciliation(1L, null,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31), "operator");

        ArgumentCaptor<ReconciliationRecord> recordCaptor = ArgumentCaptor.forClass(ReconciliationRecord.class);
        verify(reconciliationRecordMapper).insert(recordCaptor.capture());
        ReconciliationRecord record = recordCaptor.getValue();

        assertThat(record.getReconStatus()).isEqualTo("SUCCESS");
        assertThat(record.getErpVoucherCount()).isEqualTo(2);
        assertThat(record.getArcVoucherCount()).isEqualTo(2);
        assertThat(record.getSubjectCode()).isEqualTo("ALL");
        assertThat(record.getAttachmentMissingCount()).isEqualTo(0);
    }

    private ErpConfig buildConfig() {
        ErpConfig config = new ErpConfig();
        config.setId(1L);
        config.setName("ERP-TEST");
        config.setErpType("YONSUITE");
        config.setConfigJson("{\"accbookCode\":\"AC01\"}");
        return config;
    }

    private Archive buildArchive(String id, String archiveCode, String uniqueBizId, String customMetadata,
            LocalDate docDate) {
        Archive archive = new Archive();
        archive.setId(id);
        archive.setArchiveCode(archiveCode);
        archive.setUniqueBizId(uniqueBizId);
        archive.setCustomMetadata(customMetadata);
        archive.setDocDate(docDate);
        archive.setFiscalYear("2024");
        archive.setFiscalPeriod("2024-01");
        archive.setStatus("archived");
        archive.setFondsNo("AC01");
        return archive;
    }

    private ArcFileContent buildAttachment(String archivalCode) {
        ArcFileContent file = new ArcFileContent();
        file.setArchivalCode(archivalCode);
        file.setFileName("voucher.pdf");
        file.setFileType("PDF");
        file.setFileHash("hash");
        file.setHashAlgorithm("SHA256");
        file.setTimestampToken(new byte[] { 1 });
        file.setSignValue(new byte[] { 2 });
        file.setCertificate("cert");
        file.setPreArchiveStatus("ARCHIVED");
        return file;
    }
}
