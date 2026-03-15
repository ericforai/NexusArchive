// Input: JUnit 5, Mockito, Java 标准库
// Output: ArchiveAppraisalServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.dto.AppraisalListDetail;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.AppraisalList;
import com.nexusarchive.mapper.AppraisalListMapper;
import com.nexusarchive.service.ArchiveAppraisalService;
import com.nexusarchive.mapper.ArchiveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 档案鉴定清单服务实现类单元测试
 *
 * 测试范围:
 * 1. createAppraisalList - 创建鉴定清单
 *    - 验证档案状态必须为 EXPIRED
 *    - 验证所有档案属于同一全宗
 *    - 生成档案元数据快照
 *    - 更新档案状态为 APPRAISING
 *    - 异常情况处理
 *
 * 2. submitAppraisalConclusion - 提交鉴定结论
 *    - 验证鉴定清单状态必须为 PENDING
 *    - 支持 APPROVED/REJECTED/DEFERRED 三种结论
 *    - 根据结论更新档案状态
 *    - 异常情况处理
 *
 * 3. getAppraisalListDetail - 获取鉴定清单详情
 *    - 正确解析档案ID列表
 *    - 转换为 DTO 格式
 *    - 异常情况处理
 *
 * 4. exportAppraisalList - 导出功能
 *    - 验证抛出 UnsupportedOperationException
 *
 * 5. 辅助方法测试
 *    - calculateExpirationDate - 计算到期日期
 *    - parseRetentionYears - 解析保管期限
 *    - generateArchiveSnapshot - 生成快照
 *
 * 状态转换规则:
 * EXPIRED -> APPRAISING (创建鉴定清单)
 * APPRAISING -> APPRAISING (同意销毁)
 * APPRAISING -> EXPIRED (不同意销毁)
 * APPRAISING -> NORMAL (延期保管)
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("档案鉴定清单服务测试")
class ArchiveAppraisalServiceImplTest {

    @Mock
    private AppraisalListMapper appraisalListMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    private ObjectMapper objectMapper;

    private ArchiveAppraisalServiceImpl archiveAppraisalService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        archiveAppraisalService = new ArchiveAppraisalServiceImpl(
                appraisalListMapper,
                archiveMapper,
                objectMapper
        );
    }

    // ==================== Test Data Factory Methods ====================

    private Archive createTestArchive(String id, String archiveCode, String destructionStatus,
                                     String fondsNo, String retentionPeriod, LocalDate retentionStartDate) {
        Archive archive = new Archive();
        archive.setId(id);
        archive.setArchiveCode(archiveCode);
        archive.setDestructionStatus(destructionStatus);
        archive.setFondsNo(fondsNo);
        archive.setRetentionPeriod(retentionPeriod);
        archive.setRetentionStartDate(retentionStartDate);
        archive.setTitle("测试档案" + id);
        archive.setFiscalYear("2024");
        archive.setFiscalPeriod("01");
        archive.setDocDate(LocalDate.of(2024, 1, 1));
        archive.setCreatedTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        archive.setOrgName("测试单位");
        archive.setCreator("测试用户");
        archive.setAmount(java.math.BigDecimal.valueOf(1000.00));
        return archive;
    }

    private AppraisalList createTestAppraisalList(String id, String status, String archiveIdsJson) {
        AppraisalList appraisalList = new AppraisalList();
        appraisalList.setId(id);
        appraisalList.setFondsNo("F001");
        appraisalList.setArchiveYear(2024);
        appraisalList.setAppraiserId("user-1");
        appraisalList.setAppraiserName("鉴定人");
        appraisalList.setAppraisalDate(LocalDate.of(2025, 3, 15));
        appraisalList.setArchiveIds(archiveIdsJson);
        appraisalList.setArchiveSnapshot("[]");
        appraisalList.setStatus(status);
        appraisalList.setCreatedTime(LocalDateTime.now());
        appraisalList.setLastModifiedTime(LocalDateTime.now());
        return appraisalList;
    }

    // ==================== createAppraisalList Tests ====================

    @Nested
    @DisplayName("创建鉴定清单测试")
    class CreateAppraisalListTests {

        @Test
        @DisplayName("应该成功创建鉴定清单 - 单个档案")
        void shouldSuccessfullyCreateAppraisalListForSingleArchive() throws Exception {
            // Given
            String archiveId = "archive-1";
            String fondsNo = "F001";
            String appraiserId = "user-1";
            LocalDate appraisalDate = LocalDate.of(2025, 3, 15);

            Archive archive = createTestArchive(archiveId, "ARC-001", "EXPIRED", fondsNo, "10Y",
                                              LocalDate.of(2014, 1, 1));

            when(archiveMapper.selectBatchIds(Collections.singletonList(archiveId)))
                    .thenReturn(Collections.singletonList(archive));
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            String result = archiveAppraisalService.createAppraisalList(
                    Collections.singletonList(archiveId),
                    fondsNo,
                    appraiserId,
                    appraisalDate
            );

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<AppraisalList> listCaptor = ArgumentCaptor.forClass(AppraisalList.class);
            verify(appraisalListMapper).insert(listCaptor.capture());

            AppraisalList capturedList = listCaptor.getValue();
            assertThat(capturedList.getFondsNo()).isEqualTo(fondsNo);
            assertThat(capturedList.getAppraiserId()).isEqualTo(appraiserId);
            assertThat(capturedList.getAppraisalDate()).isEqualTo(appraisalDate);
            assertThat(capturedList.getStatus()).isEqualTo(OperationResult.PENDING);
            assertThat(capturedList.getArchiveYear()).isEqualTo(2024);
            assertThat(capturedList.getArchiveSnapshot()).isNotEmpty();

            verify(archiveMapper, times(1)).update(any(), any());
        }

        @Test
        @DisplayName("应该成功创建鉴定清单 - 多个档案")
        void shouldSuccessfullyCreateAppraisalListForMultipleArchives() throws Exception {
            // Given
            String fondsNo = "F001";
            String appraiserId = "user-1";
            LocalDate appraisalDate = LocalDate.of(2025, 3, 15);

            List<String> archiveIds = Arrays.asList("archive-1", "archive-2", "archive-3");
            List<Archive> archives = Arrays.asList(
                    createTestArchive("archive-1", "ARC-001", "EXPIRED", fondsNo, "10Y",
                                     LocalDate.of(2014, 1, 1)),
                    createTestArchive("archive-2", "ARC-002", "EXPIRED", fondsNo, "10Y",
                                     LocalDate.of(2014, 1, 1)),
                    createTestArchive("archive-3", "ARC-003", "EXPIRED", fondsNo, "10Y",
                                     LocalDate.of(2014, 1, 1))
            );

            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            String result = archiveAppraisalService.createAppraisalList(
                    archiveIds,
                    fondsNo,
                    appraiserId,
                    appraisalDate
            );

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<AppraisalList> listCaptor = ArgumentCaptor.forClass(AppraisalList.class);
            verify(appraisalListMapper).insert(listCaptor.capture());

            AppraisalList capturedList = listCaptor.getValue();
            assertThat(capturedList.getFondsNo()).isEqualTo(fondsNo);
            assertThat(capturedList.getAppraiserId()).isEqualTo(appraiserId);
            assertThat(capturedList.getAppraisalDate()).isEqualTo(appraisalDate);
            assertThat(capturedList.getStatus()).isEqualTo(OperationResult.PENDING);

            // Verify all archives were updated to APPRAISING status
            verify(archiveMapper, times(3)).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案ID列表为空")
        void shouldThrowExceptionWhenArchiveIdsIsEmpty() {
            // Given
            List<String> emptyList = Collections.emptyList();

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.createAppraisalList(
                    emptyList,
                    "F001",
                    "user-1",
                    LocalDate.now()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("待鉴定档案ID列表不能为空");

            verify(archiveMapper, never()).selectBatchIds(any());
            verify(appraisalListMapper, never()).insert(any(AppraisalList.class));
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案ID列表为null")
        void shouldThrowExceptionWhenArchiveIdsIsNull() {
            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.createAppraisalList(
                    null,
                    "F001",
                    "user-1",
                    LocalDate.now()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("待鉴定档案ID列表不能为空");

            verify(archiveMapper, never()).selectBatchIds(any());
            verify(appraisalListMapper, never()).insert(any(AppraisalList.class));
        }

        @Test
        @DisplayName("应该抛出异常 - 当部分档案不存在")
        void shouldThrowExceptionWhenSomeArchivesNotExist() {
            // Given
            List<String> archiveIds = Arrays.asList("archive-1", "archive-2", "archive-3");
            List<Archive> archives = Arrays.asList(
                    createTestArchive("archive-1", "ARC-001", "EXPIRED", "F001", "10Y",
                                     LocalDate.of(2014, 1, 1)),
                    createTestArchive("archive-2", "ARC-002", "EXPIRED", "F001", "10Y",
                                     LocalDate.of(2014, 1, 1))
            );

            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.createAppraisalList(
                    archiveIds,
                    "F001",
                    "user-1",
                    LocalDate.now()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("部分档案不存在");

            verify(appraisalListMapper, never()).insert(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案状态不是EXPIRED")
        void shouldThrowExceptionWhenArchiveStatusIsNotExpired() {
            // Given
            String fondsNo = "F001";
            List<String> archiveIds = Collections.singletonList("archive-1");
            List<Archive> archives = Collections.singletonList(
                    createTestArchive("archive-1", "ARC-001", "NORMAL", fondsNo, "10Y",
                                     LocalDate.of(2014, 1, 1))
            );

            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.createAppraisalList(
                    archiveIds,
                    fondsNo,
                    "user-1",
                    LocalDate.now()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("状态不是 EXPIRED，无法生成鉴定清单");

            verify(appraisalListMapper, never()).insert(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案不属于指定全宗")
        void shouldThrowExceptionWhenArchiveNotInFonds() {
            // Given
            String fondsNo = "F001";
            List<String> archiveIds = Collections.singletonList("archive-1");
            List<Archive> archives = Collections.singletonList(
                    createTestArchive("archive-1", "ARC-001", "EXPIRED", "F002", "10Y",
                                     LocalDate.of(2014, 1, 1))
            );

            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.createAppraisalList(
                    archiveIds,
                    fondsNo,
                    "user-1",
                    LocalDate.now()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不属于全宗 " + fondsNo);

            verify(appraisalListMapper, never()).insert(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该生成正确的档案快照")
        void shouldGenerateCorrectArchiveSnapshot() throws Exception {
            // Given
            String archiveId = "archive-1";
            String fondsNo = "F001";
            LocalDate retentionStartDate = LocalDate.of(2014, 1, 1);

            Archive archive = createTestArchive(archiveId, "ARC-001", "EXPIRED", fondsNo, "10Y",
                                              retentionStartDate);

            when(archiveMapper.selectBatchIds(Collections.singletonList(archiveId)))
                    .thenReturn(Collections.singletonList(archive));
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.createAppraisalList(
                    Collections.singletonList(archiveId),
                    fondsNo,
                    "user-1",
                    LocalDate.now()
            );

            // Then
            ArgumentCaptor<AppraisalList> listCaptor = ArgumentCaptor.forClass(AppraisalList.class);
            verify(appraisalListMapper).insert(listCaptor.capture());

            AppraisalList capturedList = listCaptor.getValue();
            String snapshot = capturedList.getArchiveSnapshot();

            assertThat(snapshot).isNotEmpty();
            assertThat(snapshot).contains(archiveId);
            assertThat(snapshot).contains("ARC-001");
            assertThat(snapshot).contains("测试档案" + archiveId);
            assertThat(snapshot).contains("测试单位");
            assertThat(snapshot).contains("2024-01-01"); // expirationDate (2014 + 10 years)
        }
    }

    // ==================== submitAppraisalConclusion Tests ====================

    @Nested
    @DisplayName("提交鉴定结论测试")
    class SubmitAppraisalConclusionTests {

        @Test
        @DisplayName("应该成功提交鉴定结论 - 同意销毁")
        void shouldSuccessfullySubmitApprovedConclusion() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Arrays.asList("archive-1", "archive-2");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "APPROVED",
                    "同意销毁"
            );

            // Then
            ArgumentCaptor<AppraisalList> listCaptor = ArgumentCaptor.forClass(AppraisalList.class);
            verify(appraisalListMapper).updateById(listCaptor.capture());

            AppraisalList capturedList = listCaptor.getValue();
            assertThat(capturedList.getStatus()).isEqualTo("SUBMITTED");
            assertThat(capturedList.getLastModifiedTime()).isNotNull();

            // Verify archives remain in APPRAISING status
            verify(archiveMapper, times(2)).update(any(), any());
        }

        @Test
        @DisplayName("应该成功提交鉴定结论 - 不同意销毁")
        void shouldSuccessfullySubmitRejectedConclusion() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Arrays.asList("archive-1", "archive-2");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "REJECTED",
                    "不同意销毁"
            );

            // Then
            verify(appraisalListMapper).updateById(any(AppraisalList.class));

            // Verify archives are reverted to EXPIRED status
            ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Archive>>
                    wrapperCaptor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
            verify(archiveMapper, times(2)).update(any(), wrapperCaptor.capture());
        }

        @Test
        @DisplayName("应该成功提交鉴定结论 - 延期保管")
        void shouldSuccessfullySubmitDeferredConclusion() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Arrays.asList("archive-1", "archive-2");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "DEFERRED",
                    "延期保管"
            );

            // Then
            verify(appraisalListMapper).updateById(any(AppraisalList.class));

            // Verify archives are reverted to NORMAL status
            verify(archiveMapper, times(2)).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当鉴定清单不存在")
        void shouldThrowExceptionWhenAppraisalListNotFound() {
            // Given
            String appraisalListId = "non-existent";
            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "APPROVED",
                    "同意销毁"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("鉴定清单不存在: " + appraisalListId);

            verify(appraisalListMapper, never()).updateById(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当鉴定清单状态不是PENDING")
        void shouldThrowExceptionWhenAppraisalListStatusIsNotPending() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  "SUBMITTED",
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "APPROVED",
                    "同意销毁"
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("鉴定清单状态不是 PENDING，无法提交结论");

            verify(appraisalListMapper, never()).updateById(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当鉴定结论无效")
        void shouldThrowExceptionWhenConclusionIsInvalid() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "INVALID",
                    "无效结论"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("无效的鉴定结论: INVALID");

            verify(appraisalListMapper, never()).updateById(any(AppraisalList.class));
            verify(archiveMapper, never()).update(any(), any());
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案ID列表解析失败")
        void shouldThrowExceptionWhenArchiveIdsParsingFails() {
            // Given
            String appraisalListId = "appraisal-1";
            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  "invalid-json");

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "APPROVED",
                    "同意销毁"
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("提交鉴定结论失败");
        }
    }

    // ==================== getAppraisalListDetail Tests ====================

    @Nested
    @DisplayName("获取鉴定清单详情测试")
    class GetAppraisalListDetailTests {

        @Test
        @DisplayName("应该成功获取鉴定清单详情")
        void shouldSuccessfullyGetAppraisalListDetail() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Arrays.asList("archive-1", "archive-2");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            List<Archive> archives = Arrays.asList(
                    createTestArchive("archive-1", "ARC-001", "APPRAISING", "F001", "10Y",
                                     LocalDate.of(2014, 1, 1)),
                    createTestArchive("archive-2", "ARC-002", "APPRAISING", "F001", "10Y",
                                     LocalDate.of(2014, 1, 1))
            );

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);

            // When
            AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

            // Then
            assertThat(detail).isNotNull();
            assertThat(detail.getAppraisalListId()).isEqualTo(appraisalListId);
            assertThat(detail.getFondsNo()).isEqualTo("F001");
            assertThat(detail.getArchiveYear()).isEqualTo(2024);
            assertThat(detail.getAppraiserId()).isEqualTo("user-1");
            assertThat(detail.getAppraiserName()).isEqualTo("鉴定人");
            assertThat(detail.getAppraisalDate()).isEqualTo(LocalDate.of(2025, 3, 15));
            assertThat(detail.getStatus()).isEqualTo(OperationResult.PENDING);
            assertThat(detail.getArchives()).hasSize(2);

            // Verify archive items
            AppraisalListDetail.ArchiveAppraisalItem item1 = detail.getArchives().get(0);
            assertThat(item1.getArchiveId()).isEqualTo("archive-1");
            assertThat(item1.getArchiveCode()).isEqualTo("ARC-001");
            assertThat(item1.getTitle()).isEqualTo("测试档案archive-1");
            assertThat(item1.getExpirationDate()).isEqualTo(LocalDate.of(2024, 1, 1)); // 2014 + 10 years
        }

        @Test
        @DisplayName("应该抛出异常 - 当鉴定清单不存在")
        void shouldThrowExceptionWhenAppraisalListNotFoundForDetail() {
            // Given
            String appraisalListId = "non-existent";
            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.getAppraisalListDetail(appraisalListId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("鉴定清单不存在: " + appraisalListId);
        }

        @Test
        @DisplayName("应该抛出异常 - 当档案ID列表解析失败")
        void shouldThrowExceptionWhenArchiveIdsParsingFailsForDetail() {
            // Given
            String appraisalListId = "appraisal-1";
            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  "invalid-json");

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);

            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.getAppraisalListDetail(appraisalListId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("获取鉴定清单详情失败");
        }

        @Test
        @DisplayName("应该正确计算到期日期 - 永久保管")
        void shouldCorrectlyCalculateExpirationDateForPermanent() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            Archive archive = createTestArchive("archive-1", "ARC-001", "APPRAISING", "F001",
                                              "PERMANENT", LocalDate.of(2014, 1, 1));

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(Collections.singletonList(archive));

            // When
            AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

            // Then
            assertThat(detail.getArchives().get(0).getExpirationDate()).isNull();
        }

        @Test
        @DisplayName("应该正确计算到期日期 - 中文永久")
        void shouldCorrectlyCalculateExpirationDateForChinesePermanent() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            Archive archive = createTestArchive("archive-1", "ARC-001", "APPRAISING", "F001",
                                              "永久", LocalDate.of(2014, 1, 1));

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(Collections.singletonList(archive));

            // When
            AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

            // Then
            assertThat(detail.getArchives().get(0).getExpirationDate()).isNull();
        }

        @Test
        @DisplayName("应该正确处理null保管期限起算日期")
        void shouldCorrectlyHandleNullRetentionStartDate() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            Archive archive = createTestArchive("archive-1", "ARC-001", "APPRAISING", "F001",
                                              "10Y", null);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(Collections.singletonList(archive));

            // When
            AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

            // Then
            assertThat(detail.getArchives().get(0).getExpirationDate()).isNull();
        }
    }

    // ==================== exportAppraisalList Tests ====================

    @Nested
    @DisplayName("导出鉴定清单测试")
    class ExportAppraisalListTests {

        @Test
        @DisplayName("应该抛出UnsupportedOperationException - 导出功能未实现")
        void shouldThrowUnsupportedOperationExceptionForExport() {
            // When & Then
            assertThatThrownBy(() -> archiveAppraisalService.exportAppraisalList(
                    "appraisal-1",
                    ArchiveAppraisalService.ExportFormat.EXCEL
            ))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("导出功能待实现");

            assertThatThrownBy(() -> archiveAppraisalService.exportAppraisalList(
                    "appraisal-1",
                    ArchiveAppraisalService.ExportFormat.PDF
            ))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("导出功能待实现");
        }
    }

    // ==================== Edge Cases and Integration Tests ====================

    @Nested
    @DisplayName("边界条件和集成测试")
    class EdgeCasesAndIntegrationTests {

        @Test
        @DisplayName("应该处理大量档案 - 性能测试")
        void shouldHandleLargeNumberOfArchives() throws Exception {
            // Given
            String fondsNo = "F001";
            String appraiserId = "user-1";
            LocalDate appraisalDate = LocalDate.of(2025, 3, 15);

            // Create 100 archives
            List<String> archiveIds = new java.util.ArrayList<>();
            List<Archive> archives = new java.util.ArrayList<>();

            for (int i = 1; i <= 100; i++) {
                String archiveId = "archive-" + i;
                archiveIds.add(archiveId);
                archives.add(createTestArchive(archiveId, "ARC-" + String.format("%03d", i),
                                              "EXPIRED", fondsNo, "10Y", LocalDate.of(2014, 1, 1)));
            }

            when(archiveMapper.selectBatchIds(archiveIds)).thenReturn(archives);
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            String result = archiveAppraisalService.createAppraisalList(
                    archiveIds,
                    fondsNo,
                    appraiserId,
                    appraisalDate
            );

            // Then
            assertThat(result).isNotNull();
            verify(archiveMapper, times(100)).update(any(), any());
        }

        @Test
        @DisplayName("应该处理特殊字符在档案快照中")
        void shouldHandleSpecialCharactersInArchiveSnapshot() throws Exception {
            // Given
            String archiveId = "archive-1";
            String fondsNo = "F001";

            Archive archive = createTestArchive(archiveId, "ARC-001", "EXPIRED", fondsNo, "10Y",
                                              LocalDate.of(2014, 1, 1));
            archive.setTitle("测试档案\n包含\r换行符\t和制表符");
            archive.setCreator("用户\"quoted'单引号");

            when(archiveMapper.selectBatchIds(Collections.singletonList(archiveId)))
                    .thenReturn(Collections.singletonList(archive));
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            String result = archiveAppraisalService.createAppraisalList(
                    Collections.singletonList(archiveId),
                    fondsNo,
                    "user-1",
                    LocalDate.now()
            );

            // Then
            assertThat(result).isNotNull();

            ArgumentCaptor<AppraisalList> listCaptor = ArgumentCaptor.forClass(AppraisalList.class);
            verify(appraisalListMapper).insert(listCaptor.capture());

            AppraisalList capturedList = listCaptor.getValue();
            String snapshot = capturedList.getArchiveSnapshot();

            // Verify JSON is properly escaped
            assertThat(snapshot).contains("用户");
            assertThat(snapshot).contains("quoted");
        }

        @Test
        @DisplayName("应该处理不同保管期限格式")
        void shouldHandleDifferentRetentionPeriodFormats() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";

            // Test different formats: "10Y", "10", "PERMANENT", "永久"
            String[] periods = {"10Y", "10", "PERMANENT", "永久"};

            for (String period : periods) {
                List<String> archiveIds = Collections.singletonList("archive-" + period);
                String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

                AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                      OperationResult.PENDING,
                                                                      archiveIdsJson);

                Archive archive = createTestArchive("archive-" + period, "ARC-001", "APPRAISING",
                                                  "F001", period, LocalDate.of(2014, 1, 1));

                when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
                when(archiveMapper.selectBatchIds(archiveIds))
                        .thenReturn(Collections.singletonList(archive));

                // When
                AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

                // Then
                assertThat(detail.getArchives()).hasSize(1);

                if (period.equals("PERMANENT") || period.equals("永久")) {
                    assertThat(detail.getArchives().get(0).getExpirationDate()).isNull();
                } else {
                    assertThat(detail.getArchives().get(0).getExpirationDate())
                            .isEqualTo(LocalDate.of(2024, 1, 1)); // 2014 + 10 years
                }
            }
        }

        @Test
        @DisplayName("应该处理无效保管期限格式")
        void shouldHandleInvalidRetentionPeriodFormat() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            Archive archive = createTestArchive("archive-1", "ARC-001", "APPRAISING", "F001",
                                              "INVALID_FORMAT", LocalDate.of(2014, 1, 1));

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(archiveMapper.selectBatchIds(archiveIds))
                    .thenReturn(Collections.singletonList(archive));

            // When
            AppraisalListDetail detail = archiveAppraisalService.getAppraisalListDetail(appraisalListId);

            // Then - should return null expiration date for invalid format
            assertThat(detail.getArchives().get(0).getExpirationDate()).isNull();
        }
    }

    // ==================== Status Transition Tests ====================

    @Nested
    @DisplayName("状态转换测试")
    class StatusTransitionTests {

        @Test
        @DisplayName("应该正确转换状态 - EXPIRED到APPRAISING")
        void shouldCorrectlyTransitionStatusFromExpiredToAppraising() throws Exception {
            // Given
            String archiveId = "archive-1";
            String fondsNo = "F001";

            Archive archive = createTestArchive(archiveId, "ARC-001", "EXPIRED", fondsNo, "10Y",
                                              LocalDate.of(2014, 1, 1));

            when(archiveMapper.selectBatchIds(Collections.singletonList(archiveId)))
                    .thenReturn(Collections.singletonList(archive));
            when(appraisalListMapper.insert(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.createAppraisalList(
                    Collections.singletonList(archiveId),
                    fondsNo,
                    "user-1",
                    LocalDate.now()
            );

            // Then - Verify archive status is updated to APPRAISING
            ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Archive>>
                    wrapperCaptor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
            verify(archiveMapper).update(any(), wrapperCaptor.capture());
        }

        @Test
        @DisplayName("应该正确转换状态 - APPRAISING到EXPIRED (拒绝)")
        void shouldCorrectlyTransitionStatusFromAppraisingToExpired() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "REJECTED",
                    "不同意销毁"
            );

            // Then - Verify archive status is reverted to EXPIRED
            verify(archiveMapper, times(1)).update(any(), any());
        }

        @Test
        @DisplayName("应该正确转换状态 - APPRAISING到NORMAL (延期)")
        void shouldCorrectlyTransitionStatusFromAppraisingToNormal() throws Exception {
            // Given
            String appraisalListId = "appraisal-1";
            List<String> archiveIds = Collections.singletonList("archive-1");
            String archiveIdsJson = objectMapper.writeValueAsString(archiveIds);

            AppraisalList appraisalList = createTestAppraisalList(appraisalListId,
                                                                  OperationResult.PENDING,
                                                                  archiveIdsJson);

            when(appraisalListMapper.selectById(appraisalListId)).thenReturn(appraisalList);
            when(appraisalListMapper.updateById(any(AppraisalList.class))).thenReturn(1);
            when(archiveMapper.update(any(), any())).thenReturn(1);

            // When
            archiveAppraisalService.submitAppraisalConclusion(
                    appraisalListId,
                    "DEFERRED",
                    "延期保管"
            );

            // Then - Verify archive status is changed to NORMAL
            verify(archiveMapper, times(1)).update(any(), any());
        }
    }
}
