// Input: JUnit 5, Mockito, Java 标准库
// Output: LegacyImportOrchestratorTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportError;
import com.nexusarchive.dto.request.ImportPreviewResult;
import com.nexusarchive.dto.request.ImportResult;
import com.nexusarchive.dto.request.ImportRow;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.mapper.LegacyImportTaskMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FondsAutoCreationService;
import com.nexusarchive.service.ImportValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 历史数据导入流程编排器单元测试
 *
 * 测试范围:
 * 1. executeImport - 完整导入流程编排
 * 2. previewImport - 预览导入数据
 * 3. 错误处理和边界条件
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("历史数据导入流程编排器测试")
class LegacyImportOrchestratorTest {

    @Mock
    private LegacyImportTaskMapper importTaskMapper;

    @Mock
    private ImportValidationService validationService;

    @Mock
    private FondsAutoCreationService fondsAutoCreationService;

    @Mock
    private LegacyDataConverter dataConverter;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MultipartFile mockFile;

    private ObjectMapper objectMapper;
    private LegacyImportOrchestrator orchestrator;
    private FieldMappingConfig mappingConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orchestrator = new LegacyImportOrchestrator(
                importTaskMapper,
                validationService,
                fondsAutoCreationService,
                dataConverter,
                auditLogService,
                objectMapper
        );
        mappingConfig = new FieldMappingConfig();
    }

    // ========== Test Data Builders ==========

    private ImportRow createValidImportRow(int rowNumber, String fondsNo) {
        return ImportRow.builder()
                .rowNumber(rowNumber)
                .fondsNo(fondsNo)
                .fondsName("测试全宗")
                .archiveYear(2024)
                .docType("VOUCHER")
                .title("测试凭证")
                .retentionPolicyName("永久")
                .entityName("测试法人")
                .entityTaxCode("91110000MA000001XX")
                .docDate(LocalDate.now())
                .amount(new BigDecimal("100.00"))
                .counterparty("测试单位")
                .voucherNo("V001")
                .invoiceNo("I001")
                .build();
    }

    private ImportError createImportError(int rowNumber, String fieldName, String errorCode) {
        return ImportError.builder()
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .errorCode(errorCode)
                .errorMessage("测试错误消息")
                .build();
    }

    // ========== ExecuteImport Tests ==========

    @Nested
    @DisplayName("executeImport - 完整导入流程测试")
    class ExecuteImportTests {

        @Test
        @DisplayName("应该成功执行完整的导入流程 - 全部成功")
        void shouldSuccessfullyExecuteImportWithAllSuccess() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));
            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(fondsAutoCreationService.ensureEntityExists(any(), any()))
                    .thenReturn("entity-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(2);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalRows()).isGreaterThan(0);
            assertThat(result.getSuccessRows()).isEqualTo(2);
            assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.SUCCESS);

            // 验证任务创建
            ArgumentCaptor<LegacyImportTask> taskCaptor = ArgumentCaptor.forClass(LegacyImportTask.class);
            verify(importTaskMapper).insert(taskCaptor.capture());

            LegacyImportTask capturedTask = taskCaptor.getValue();
            assertThat(capturedTask.getOperatorId()).isEqualTo(operatorId);
            assertThat(capturedTask.getFondsNo()).isEqualTo(fondsNo);
            assertThat(capturedTask.getStatus()).isEqualTo("PROCESSING");

            // 验证审计日志被记录
            verify(auditLogService).log(
                    anyString(),
                    anyString(),
                    eq("LEGACY_IMPORT"),
                    eq("IMPORT_TASK"),
                    anyString(),
                    eq(OperationResult.SUCCESS),
                    anyString(),
                    eq(OperationResult.UNKNOWN)
            );
        }

        @Test
        @DisplayName("应该处理部分成功的导入场景")
        void shouldHandlePartialSuccessImport() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);

            // 模拟部分行验证失败
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(false,
                            List.of(createImportError(2, "fonds_no", "REQUIRED"))));

            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(1);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(ImportResult.ImportStatus.PARTIAL_SUCCESS);
            assertThat(result.getSuccessRows()).isLessThan(result.getTotalRows());
            assertThat(result.getErrors()).isNotEmpty();
        }

        @Test
        @DisplayName("应该在导入失败时处理异常并更新任务状态")
        void shouldHandleImportFailureAndUpdateTaskStatus() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);

            // 模拟验证失败
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenThrow(new RuntimeException("验证失败"));

            // When & Then
            assertThatThrownBy(() -> {
                orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);
            }).isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("验证失败");

            // 验证任务状态被更新为 FAILED
            ArgumentCaptor<LegacyImportTask> taskCaptor = ArgumentCaptor.forClass(LegacyImportTask.class);
            verify(importTaskMapper, atLeastOnce()).updateById(taskCaptor.capture());

            List<LegacyImportTask> allTasks = taskCaptor.getAllValues();
            if (!allTasks.isEmpty()) {
                LegacyImportTask finalTask = allTasks.get(allTasks.size() - 1);
                assertThat(finalTask.getStatus()).isEqualTo("FAILED");
                assertThat(finalTask.getCompletedAt()).isNotNull();
            }
        }
    }

    // ========== ValidateRows Tests ==========

    @Nested
    @DisplayName("数据验证逻辑测试")
    class ValidationLogicTests {

        @Test
        @DisplayName("应该正确分离有效和无效行")
        void shouldSeparateValidAndInvalidRows() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);

            // 模拟不同行的验证结果
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()))
                    .thenReturn(new ImportValidationService.ValidationResult(false,
                            List.of(createImportError(2, "fonds_no", "REQUIRED"))))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));

            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(2);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result).isNotNull();
            verify(validationService, times(3)).validateRow(any(), anyInt(), any());
            assertThat(result.getErrors()).hasSize(1);
        }

        @Test
        @DisplayName("应该收集所有验证错误")
        void shouldCollectAllValidationErrors() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);

            ImportError error1 = createImportError(1, "fonds_no", "REQUIRED");
            ImportError error2 = createImportError(2, "title", "REQUIRED");

            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(false, List.of(error1)))
                    .thenReturn(new ImportValidationService.ValidationResult(false, List.of(error2)));

            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(0);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result.getErrors()).hasSize(2);
            assertThat(result.getSuccessRows()).isEqualTo(0);
        }
    }

    // ========== EnsureFondsAndEntities Tests ==========

    @Nested
    @DisplayName("全宗/实体创建测试")
    class FondsAndEntityCreationTests {

        @Test
        @DisplayName("应该成功创建全宗和实体")
        void shouldSuccessfullyCreateFondsAndEntities() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));
            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(fondsAutoCreationService.ensureEntityExists(any(), any()))
                    .thenReturn("entity-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(2);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result.getCreatedFondsNos()).isNotEmpty();
            assertThat(result.getCreatedEntityIds()).isNotEmpty();
            verify(fondsAutoCreationService, atLeastOnce()).ensureFondsExists(any(), any(), any(), any(), any());
            verify(fondsAutoCreationService, atLeastOnce()).ensureEntityExists(any(), any());
        }

        @Test
        @DisplayName("应该处理全宗创建失败的情况")
        void shouldHandleFondsCreationFailure() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));
            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("全宗创建失败"));
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(2);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrors().get(0).getErrorCode()).isEqualTo("FONDS_CREATION_FAILED");
        }
    }

    // ========== BatchImportArchives Tests ==========

    @Nested
    @DisplayName("批量导入测试")
    class BatchImportTests {

        @Test
        @DisplayName("应该成功批量导入档案")
        void shouldSuccessfullyImportArchives() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));
            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(2);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result.getSuccessRows()).isEqualTo(2);
            verify(dataConverter, times(1)).batchImportArchives(any(), anyString());
        }

        @Test
        @DisplayName("应该分批导入档案 - 大于批次大小")
        void shouldImportArchivesInBatches() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("large.csv");
            when(mockFile.getSize()).thenReturn(102400L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));
            when(fondsAutoCreationService.ensureFondsExists(any(), any(), any(), any(), any()))
                    .thenReturn("fonds-id-001");
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(1000);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            // 验证批量导入被调用（假设文件有超过1000行）
            verify(dataConverter, atLeastOnce()).batchImportArchives(any(), anyString());
        }
    }

    // ========== PreviewImport Tests ==========

    @Nested
    @DisplayName("previewImport - 预览导入测试")
    class PreviewImportTests {

        @Test
        @DisplayName("应该成功生成预览结果")
        void shouldSuccessfullyGeneratePreview() {
            // Given
            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(true, List.of()));

            // When
            ImportPreviewResult result = orchestrator.previewImport(mockFile, mappingConfig);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalRows()).isGreaterThan(0);
            assertThat(result.getPreviewData()).isNotNull();
            assertThat(result.getStatistics()).isNotNull();
            assertThat(result.getStatistics().getFondsCount()).isNotNull();
            assertThat(result.getStatistics().getEntityCount()).isNotNull();
        }

        @Test
        @DisplayName("应该统计预览中的错误")
        void shouldCountErrorsInPreview() {
            // Given
            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(false,
                            List.of(createImportError(1, "fonds_no", "REQUIRED"))));

            // When
            ImportPreviewResult result = orchestrator.previewImport(mockFile, mappingConfig);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getInvalidRows()).isGreaterThan(0);
        }

        @Test
        @DisplayName("应该处理预览失败的情况")
        void shouldHandlePreviewFailure() {
            // Given
            when(mockFile.getOriginalFilename()).thenReturn("invalid.csv");
            when(mockFile.isEmpty()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> {
                orchestrator.previewImport(mockFile, mappingConfig);
            }).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("文件不能为空");
        }
    }

    // ========== Edge Cases Tests ==========

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCasesTests {

        @Test
        @DisplayName("应该处理空文件")
        void shouldHandleEmptyFile() {
            // Given
            when(mockFile.getOriginalFilename()).thenReturn("empty.csv");
            when(mockFile.isEmpty()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> {
                orchestrator.previewImport(mockFile, mappingConfig);
            }).isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("应该处理不支持的文件格式")
        void shouldHandleUnsupportedFileFormat() {
            // Given
            when(mockFile.getOriginalFilename()).thenReturn("document.txt");
            when(mockFile.getSize()).thenReturn(1024L);

            // When & Then
            assertThatThrownBy(() -> {
                orchestrator.previewImport(mockFile, mappingConfig);
            }).isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不支持的文件格式");
        }

        @Test
        @DisplayName("应该生成错误报告")
        void shouldGenerateErrorReport() {
            // Given
            String operatorId = "user001";
            String fondsNo = "QZ01";

            when(mockFile.getOriginalFilename()).thenReturn("test.csv");
            when(mockFile.getSize()).thenReturn(1024L);
            when(importTaskMapper.insert(any(LegacyImportTask.class))).thenReturn(1);
            when(importTaskMapper.updateById(any(LegacyImportTask.class))).thenReturn(1);
            when(validationService.validateRow(any(), anyInt(), any()))
                    .thenReturn(new ImportValidationService.ValidationResult(false,
                            List.of(createImportError(1, "fonds_no", "REQUIRED"))));
            when(dataConverter.batchImportArchives(any(), anyString())).thenReturn(0);

            // When
            ImportResult result = orchestrator.executeImport(mockFile, mappingConfig, operatorId, fondsNo);

            // Then
            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrorReportUrl()).isNotNull();
            assertThat(result.getErrorReportUrl()).contains("/error-report");
        }
    }
}
