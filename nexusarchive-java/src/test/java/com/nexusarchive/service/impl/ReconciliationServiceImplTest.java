// Input: JUnit 5、Mockito、Java 标准库
// Output: ReconciliationServiceImplTest 单元测试类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.OperationResult;
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
import com.nexusarchive.service.impl.reconciliation.ArchiveAggregator;
import com.nexusarchive.service.impl.reconciliation.ErpDataFetcher;
import com.nexusarchive.service.impl.reconciliation.EvidenceVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReconciliationServiceImpl 单元测试
 * <p>
 * 使用 TDD 方法测试对账服务的核心功能，包括：
 * <ul>
 *   <li>对账逻辑（科目级、凭证级）</li>
 *   <li>差异检测（金额、笔数、证据链）</li>
 *   <li>状态变更（SUCCESS, DISCREPANCY, ERROR）</li>
 *   <li>异常处理（输入验证、并发控制）</li>
 * </ul>
 * </p>
 *
 * @see ReconciliationServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("对账服务单元测试")
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
    @Mock
    private ErpDataFetcher erpDataFetcher;
    @Mock
    private ArchiveAggregator archiveAggregator;
    @Mock
    private EvidenceVerifier evidenceVerifier;

    private ExecutorService executorService;
    private ReconciliationServiceImpl reconciliationService;

    private static final Long TEST_CONFIG_ID = 1L;
    private static final String TEST_SUBJECT_CODE = "1001";
    private static final String TEST_OPERATOR_ID = "admin";
    private static final String TEST_ACCBOOK_CODE = "AC01";

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4);
        reconciliationService = new ReconciliationServiceImpl(
                erpConfigMapper,
                erpAdapterFactory,
                reconciliationRecordMapper,
                executorService,
                new ObjectMapper(),
                erpDataFetcher,
                archiveAggregator,
                evidenceVerifier);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    // ========== 输入验证测试 ==========

    @Test
    @DisplayName("对账时 configId 为空应抛出异常")
    void performReconciliation_WhenConfigIdIsNull_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() ->
                reconciliationService.performReconciliation(
                        null, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("configId不能为空");

        verify(erpConfigMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("对账时 startDate 为空应抛出异常")
    void performReconciliation_WhenStartDateIsNull_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() ->
                reconciliationService.performReconciliation(
                        TEST_CONFIG_ID, TEST_SUBJECT_CODE, null,
                        LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate/endDate不能为空");
    }

    @Test
    @DisplayName("对账时 endDate 为空应抛出异常")
    void performReconciliation_WhenEndDateIsNull_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() ->
                reconciliationService.performReconciliation(
                        TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                        null, TEST_OPERATOR_ID)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate/endDate不能为空");
    }

    @Test
    @DisplayName("对账时 startDate 晚于 endDate 应抛出异常")
    void performReconciliation_WhenStartDateIsAfterEndDate_ShouldThrowException() {
        // When & Then
        assertThatThrownBy(() ->
                reconciliationService.performReconciliation(
                        TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 2, 1),
                        LocalDate.of(2024, 1, 1), TEST_OPERATOR_ID)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startDate不能晚于endDate");
    }

    // ========== ERP 配置不存在测试 ==========

    @Test
    @DisplayName("对账时 ERP 配置不存在应抛出异常")
    void performReconciliation_WhenConfigNotFound_ShouldThrowException() {
        // Given
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() ->
                reconciliationService.performReconciliation(
                        TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID)
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ERP 配置不存在");

        verify(erpConfigMapper).selectById(TEST_CONFIG_ID);
    }

    // ========== 科目级对账成功测试 ==========

    @Test
    @DisplayName("科目级对账成功时应返回 SUCCESS 状态")
    void performReconciliation_SubjectMode_Success_ShouldReturnSuccessStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("1000.00"))
                        .creditTotal(new BigDecimal("1000.00"))
                        .voucherCount(10)
                        .subjectName("测试科目")
                        .build()));

        List<Archive> archives = buildArchiveList(10);
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = buildAttachmentList(10);
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo(OperationResult.SUCCESS);
        assertThat(result.getConfigId()).isEqualTo(TEST_CONFIG_ID);
        assertThat(result.getAccbookCode()).isEqualTo(TEST_ACCBOOK_CODE);
        assertThat(result.getSubjectCode()).isEqualTo(TEST_SUBJECT_CODE);
        assertThat(result.getErpDebitTotal()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getErpCreditTotal()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getErpVoucherCount()).isEqualTo(10);
        assertThat(result.getArcVoucherCount()).isEqualTo(10);
        assertThat(result.getOperatorId()).isEqualTo(TEST_OPERATOR_ID);

        verify(erpConfigMapper).selectById(TEST_CONFIG_ID);
        verify(erpAdapter).fetchAccountSummary(any(), any(), any(), any());
    }

    // ========== 科目级对账差异测试 ==========

    @Test
    @DisplayName("科目级对账借方金额不一致应返回 DISCREPANCY 状态")
    void performReconciliation_SubjectMode_DebitDiscrepancy_ShouldReturnDiscrepancyStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("1000.00"))  // ERP 借方
                        .creditTotal(new BigDecimal("1000.00"))
                        .voucherCount(10)
                        .subjectName("测试科目")
                        .build()));

        List<Archive> archives = buildArchiveListWithDebitCredit(
                10, new BigDecimal("900.00"), new BigDecimal("1000.00"));  // 档案借方不一致
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = buildAttachmentList(10);
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getReconMessage()).contains("总借方不一致");
        assertThat(result.getReconMessage()).contains("ERP=1000.00");
        assertThat(result.getReconMessage()).contains("档案=900.00");
    }

    @Test
    @DisplayName("科目级对账贷方金额不一致应返回 DISCREPANCY 状态")
    void performReconciliation_SubjectMode_CreditDiscrepancy_ShouldReturnDiscrepancyStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("1000.00"))
                        .creditTotal(new BigDecimal("1000.00"))  // ERP 贷方
                        .voucherCount(10)
                        .subjectName("测试科目")
                        .build()));

        List<Archive> archives = buildArchiveListWithDebitCredit(
                10, new BigDecimal("1000.00"), new BigDecimal("1100.00"));  // 档案贷方不一致
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = buildAttachmentList(10);
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getReconMessage()).contains("总贷方不一致");
        assertThat(result.getReconMessage()).contains("ERP=1000.00");
        assertThat(result.getReconMessage()).contains("档案=1100.00");
    }

    // ========== 凭证级对账测试 ==========

    @Test
    @DisplayName("凭证级对账成功时应返回 SUCCESS 状态")
    void performReconciliation_VoucherMode_Success_ShouldReturnSuccessStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.syncVouchers(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        VoucherDTO.builder().voucherId("v1").build(),
                        VoucherDTO.builder().voucherId("v2").build()
                ));

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

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, null, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo(OperationResult.SUCCESS);
        assertThat(result.getSubjectCode()).isEqualTo("ALL");
        assertThat(result.getReconMessage()).contains("按凭证级一致性核对");
        assertThat(result.getErpVoucherCount()).isEqualTo(2);
        assertThat(result.getArcVoucherCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("凭证级对账凭证数不一致应返回 DISCREPANCY 状态")
    void performReconciliation_VoucherMode_VoucherCountDiscrepancy_ShouldReturnDiscrepancyStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.syncVouchers(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        VoucherDTO.builder().voucherId("v1").build(),
                        VoucherDTO.builder().voucherId("v2").build(),
                        VoucherDTO.builder().voucherId("v3").build()
                ));  // ERP 有 3 笔

        List<Archive> archives = Arrays.asList(
                buildArchive("A1", "ARC1", "biz-1", null, LocalDate.of(2024, 1, 5)),
                buildArchive("A2", "ARC2", "biz-2", null, LocalDate.of(2024, 1, 10))
        );  // 档案只有 2 笔
        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = Arrays.asList(
                buildAttachment("ARC1"),
                buildAttachment("ARC2")
        );
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, null, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getReconMessage()).contains("总笔数不一致");
        assertThat(result.getReconMessage()).contains("ERP=3");
        assertThat(result.getReconMessage()).contains("档案=2");
    }

    // ========== 证据链验证测试 ==========

    @Test
    @DisplayName("对账时发现缺失证据应返回 DISCREPANCY 状态")
    void performReconciliation_MissingEvidence_ShouldReturnDiscrepancyStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("1000.00"))
                        .creditTotal(new BigDecimal("1000.00"))
                        .voucherCount(10)
                        .subjectName("测试科目")
                        .build()));

        List<Archive> archives = buildArchiveList(10);
        when(archiveMapper.selectList(any())).thenReturn(archives);

        when(arcFileContentMapper.selectList(any())).thenReturn(Collections.emptyList());  // 无证据

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getReconMessage()).contains("发现");
        assertThat(result.getReconMessage()).contains("笔凭证无原始证据");
        assertThat(result.getAttachmentMissingCount()).isGreaterThan(0);
    }

    // ========== 元数据问题测试 ==========

    @Test
    @DisplayName("对账时发现缺少科目分录元数据应返回 DISCREPANCY 状态")
    void performReconciliation_MissingMetadata_ShouldReturnDiscrepancyStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("1000.00"))
                        .creditTotal(new BigDecimal("1000.00"))
                        .voucherCount(10)
                        .subjectName("测试科目")
                        .build()));

        // 创建部分档案缺少元数据的情况
        List<Archive> archives = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            archives.add(buildArchive(
                    "A" + i, "ARC" + i, "biz-" + i,
                    "[{\"debit_org\":125,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                    LocalDate.of(2024, 1, 5 + i)
            ));
        }
        // 添加2个没有元数据的档案
        archives.add(buildArchive("A8", "ARC8", "biz-8", null, LocalDate.of(2024, 1, 13)));
        archives.add(buildArchive("A9", "ARC9", "biz-9", null, LocalDate.of(2024, 1, 14)));

        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = buildAttachmentList(10);
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getReconMessage()).contains("发现");
        assertThat(result.getReconMessage()).contains("笔凭证缺少科目分录元数据");
    }

    // ========== 历史记录查询测试 ==========

    @Test
    @DisplayName("获取历史记录时配置不存在应返回空列表")
    void getHistory_WhenConfigNotFound_ShouldReturnEmptyList() {
        // Given
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(null);

        // When
        List<ReconciliationRecord> result = reconciliationService.getHistory(TEST_CONFIG_ID);

        // Then
        assertThat(result).isNotNull().isEmpty();
        verify(erpConfigMapper).selectById(TEST_CONFIG_ID);
        verify(reconciliationRecordMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("获取历史记录时应返回记录列表")
    void getHistory_WhenRecordsExist_ShouldReturnRecords() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        List<ReconciliationRecord> mockRecords = Arrays.asList(
                createMockReconciliationRecord("record-1"),
                createMockReconciliationRecord("record-2")
        );
        when(reconciliationRecordMapper.selectList(any())).thenReturn(mockRecords);

        // When
        List<ReconciliationRecord> result = reconciliationService.getHistory(TEST_CONFIG_ID);

        // Then
        assertThat(result).isNotNull().hasSize(2);
        verify(erpConfigMapper).selectById(TEST_CONFIG_ID);
        verify(reconciliationRecordMapper).selectList(any());
    }

    // ========== 跨月对账测试 ==========

    @Test
    @DisplayName("跨月对账应正确聚合各月数据")
    void performReconciliation_CrossMonth_ShouldAggregateAllPeriods() {
        // Given
        LocalDate crossMonthStart = LocalDate.of(2024, 1, 15);
        LocalDate crossMonthEnd = LocalDate.of(2024, 2, 15);

        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("500.00"))
                        .creditTotal(new BigDecimal("500.00"))
                        .voucherCount(5)
                        .subjectName("测试科目")
                        .build()));

        // 创建跨月的档案数据
        List<Archive> archives = new ArrayList<>();
        // 1月的档案
        for (int i = 0; i < 5; i++) {
            archives.add(buildArchive(
                    "A" + i, "ARC" + i, "biz-" + i,
                    "[{\"debit_org\":100,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                    LocalDate.of(2024, 1, 16 + i)
            ));
        }
        // 2月的档案
        for (int i = 5; i < 10; i++) {
            archives.add(buildArchive(
                    "A" + i, "ARC" + i, "biz-" + i,
                    "[{\"debit_org\":100,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                    LocalDate.of(2024, 2, 1 + (i - 5))
            ));
        }

        when(archiveMapper.selectList(any())).thenReturn(archives);

        List<ArcFileContent> attachments = buildAttachmentList(10);
        when(arcFileContentMapper.selectList(any())).thenReturn(attachments);

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, crossMonthStart, crossMonthEnd, TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo(OperationResult.SUCCESS);
        assertThat(result.getErpDebitTotal()).isEqualTo(new BigDecimal("1000.00"));  // 500 + 500
        assertThat(result.getErpCreditTotal()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(result.getErpVoucherCount()).isEqualTo(10);  // 5 + 5
        assertThat(result.getArcVoucherCount()).isEqualTo(10);

        // 验证调用了两次（1月和2月）
        verify(erpAdapter, times(2)).fetchAccountSummary(
                any(), any(), any(), any()
        );
    }

    // ========== 更新已有记录测试 ==========

    @Test
    @DisplayName("对账记录已存在时应更新而非插入")
    void saveReconciliationResult_WhenRecordExists_ShouldUpdate() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("100.00"))
                        .creditTotal(new BigDecimal("100.00"))
                        .voucherCount(1)
                        .subjectName("测试科目")
                        .build()));

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());

        ReconciliationRecord existingRecord = new ReconciliationRecord();
        existingRecord.setId("existing-id");
        when(reconciliationRecordMapper.selectOne(any())).thenReturn(existingRecord);

        // When
        reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        verify(reconciliationRecordMapper).selectOne(any());
        verify(reconciliationRecordMapper).updateById(any(ReconciliationRecord.class));
        verify(reconciliationRecordMapper, never()).insert(any(ReconciliationRecord.class));
    }

    @Test
    @DisplayName("对账记录不存在时应插入新记录")
    void saveReconciliationResult_WhenRecordNotExists_ShouldInsert() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(AccountSummaryDTO.builder()
                        .debitTotal(new BigDecimal("100.00"))
                        .creditTotal(new BigDecimal("100.00"))
                        .voucherCount(1)
                        .subjectName("测试科目")
                        .build()));

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        verify(reconciliationRecordMapper).selectOne(any());
        verify(reconciliationRecordMapper).insert(any(ReconciliationRecord.class));
        verify(reconciliationRecordMapper, never()).updateById(any(ReconciliationRecord.class));
    }

    // ========== 空科目代码测试 ==========

    @Test
    @DisplayName("科目代码为空字符串时应执行凭证级对账")
    void performReconciliation_EmptySubjectCode_ShouldPerformVoucherModeReconciliation() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.syncVouchers(any(), any(), any()))
                .thenReturn(Arrays.asList(
                        VoucherDTO.builder().voucherId("v1").build(),
                        VoucherDTO.builder().voucherId("v2").build()
                ));

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

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, "", LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getSubjectCode()).isEqualTo("ALL");
        assertThat(result.getReconMessage()).contains("按凭证级一致性核对");

        verify(erpAdapter).syncVouchers(any(), any(), any());
        verify(erpAdapter, never()).fetchAccountSummary(any(), any(), any(), any());
    }

    // ========== ERP 错误处理测试 ==========

    @Test
    @DisplayName("ERP 调用失败时应返回 ERROR 状态")
    void performReconciliation_ErpFails_ShouldReturnErrorStatus() {
        // Given
        ErpConfig config = buildConfig();
        when(erpConfigMapper.selectById(TEST_CONFIG_ID)).thenReturn(config);

        when(erpAdapterFactory.getAdapter(anyString())).thenReturn(erpAdapter);
        when(erpAdapter.fetchAccountSummary(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("ERP connection timeout"));

        when(reconciliationRecordMapper.selectOne(any())).thenReturn(null);
        when(reconciliationRecordMapper.insert(any(ReconciliationRecord.class))).thenReturn(1);

        // When
        ReconciliationRecord result = reconciliationService.performReconciliation(
                TEST_CONFIG_ID, TEST_SUBJECT_CODE, LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31), TEST_OPERATOR_ID
        );

        // Then
        assertThat(result.getReconStatus()).isEqualTo("ERROR");
        assertThat(result.getReconMessage()).contains("ERP汇总获取失败");
    }

    // ========== 辅助方法 ==========

    private ErpConfig buildConfig() {
        ErpConfig config = new ErpConfig();
        config.setId(TEST_CONFIG_ID);
        config.setName("ERP-TEST");
        config.setErpType("YONSUITE");
        config.setConfigJson("{\"accbookCode\":\"" + TEST_ACCBOOK_CODE + "\"}");
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
        archive.setFondsNo(TEST_ACCBOOK_CODE);
        return archive;
    }

    private List<Archive> buildArchiveList(int count) {
        List<Archive> archives = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            archives.add(buildArchive(
                    "A" + i, "ARC" + i, "biz-" + i,
                    "[{\"debit_org\":100,\"credit_org\":0,\"accsubject\":{\"code\":\"1001\"}}]",
                    LocalDate.of(2024, 1, 5 + i)
            ));
        }
        return archives;
    }

    private List<Archive> buildArchiveListWithDebitCredit(int count, BigDecimal debit, BigDecimal credit) {
        List<Archive> archives = new ArrayList<>();
        BigDecimal debitPerArchive = debit.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal creditPerArchive = credit.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);

        for (int i = 0; i < count; i++) {
            String metadata = String.format(
                    "[{\"debit_org\":%s,\"credit_org\":%s,\"accsubject\":{\"code\":\"1001\"}}]",
                    debitPerArchive, creditPerArchive
            );
            archives.add(buildArchive(
                    "A" + i, "ARC" + i, "biz-" + i,
                    metadata,
                    LocalDate.of(2024, 1, 5 + i)
            ));
        }
        return archives;
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

    private List<ArcFileContent> buildAttachmentList(int count) {
        List<ArcFileContent> attachments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            attachments.add(buildAttachment("ARC" + i));
        }
        return attachments;
    }

    private ReconciliationRecord createMockReconciliationRecord(String id) {
        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(id);
        record.setConfigId(TEST_CONFIG_ID);
        record.setAccbookCode(TEST_ACCBOOK_CODE);
        record.setReconStartDate(LocalDate.of(2024, 1, 1));
        record.setReconEndDate(LocalDate.of(2024, 1, 31));
        record.setFondsCode(TEST_ACCBOOK_CODE);
        record.setFiscalYear("2024");
        record.setFiscalPeriod("01");
        record.setSubjectCode(TEST_SUBJECT_CODE);
        record.setSubjectName("测试科目");
        record.setErpDebitTotal(new BigDecimal("1000.00"));
        record.setErpCreditTotal(new BigDecimal("1000.00"));
        record.setErpVoucherCount(10);
        record.setArcDebitTotal(new BigDecimal("1000.00"));
        record.setArcCreditTotal(new BigDecimal("1000.00"));
        record.setArcVoucherCount(10);
        record.setAttachmentCount(10);
        record.setAttachmentMissingCount(0);
        record.setReconStatus(OperationResult.SUCCESS);
        record.setReconMessage("核对完成。");
        record.setReconTime(java.time.LocalDateTime.now());
        record.setOperatorId(TEST_OPERATOR_ID);
        record.setSourceSystem("ERP-TEST");
        return record;
    }
}
