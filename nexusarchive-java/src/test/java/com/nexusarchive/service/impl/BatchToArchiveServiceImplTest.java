// Input: JUnit 5, Mockito, Java 标准库
// Output: BatchToArchiveServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.service.ArchivalCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次转档案服务实现类单元测试
 *
 * 测试范围:
 * 1. createArchiveFromBatch - 从批次创建档案记录
 * 2. markBatchAsPendingMetadata - 标记批次为待补录元数据
 * 3. getArchiveIdByFileId - 根据文件ID获取档案ID
 * 4. 分类代码映射逻辑
 * 5. 档号生成逻辑
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("批次转档案服务测试")
class BatchToArchiveServiceImplTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private CollectionBatchFileMapper batchFileMapper;

    @Mock
    private ArchivalCodeGenerator archivalCodeGenerator;

    private BatchToArchiveServiceImpl batchToArchiveService;

    @BeforeEach
    void setUp() {
        batchToArchiveService = new BatchToArchiveServiceImpl(
                archiveMapper,
                batchFileMapper,
                archivalCodeGenerator
        );
    }

    private CollectionBatch createTestBatch(String batchNo, String fondsCode, String fiscalYear,
                                            String fiscalPeriod, String archivalCategory) {
        return CollectionBatch.builder()
                .id(1L)
                .batchNo(batchNo)
                .fondsCode(fondsCode)
                .fiscalYear(fiscalYear)
                .fiscalPeriod(fiscalPeriod)
                .archivalCategory(archivalCategory)
                .build();
    }

    private ArcFileContent createTestFileContent(String id, String fileName, String fileHash,
                                                 String hashAlgorithm, String storagePath) {
        ArcFileContent fileContent = new ArcFileContent();
        fileContent.setId(id);
        fileContent.setFileName(fileName);
        fileContent.setFileHash(fileHash);
        fileContent.setHashAlgorithm(hashAlgorithm);
        fileContent.setStoragePath(storagePath);
        fileContent.setFileType("PDF");
        return fileContent;
    }

    @Nested
    @DisplayName("创建档案记录测试")
    class CreateArchiveFromBatchTests {

        @Test
        @DisplayName("应该成功创建档案记录 - VOUCHER 类型")
        void shouldSuccessfullyCreateArchiveForVoucherType() {
            // Given
            String batchNo = "COL-20250115-001";
            String fondsCode = "QZ01";
            String fiscalYear = "2025";
            String fiscalPeriod = "2025-01";
            String archivalCategory = "VOUCHER";

            CollectionBatch batch = createTestBatch(
                    batchNo, fondsCode, fiscalYear, fiscalPeriod, archivalCategory
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "voucher.pdf", "sha256-hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            String expectedArchiveCode = "QZ01-2025-30Y-AC01-000001";
            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(expectedArchiveCode);
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getArchiveCode()).isEqualTo(expectedArchiveCode);
            assertThat(result.getFondsNo()).isEqualTo(fondsCode);
            assertThat(result.getCategoryCode()).isEqualTo("AC01"); // VOUCHER -> AC01
            assertThat(result.getFiscalYear()).isEqualTo(fiscalYear);
            assertThat(result.getFiscalPeriod()).isEqualTo(fiscalPeriod);
            assertThat(result.getTitle()).isEqualTo("voucher.pdf");
            assertThat(result.getRetentionPeriod()).isEqualTo("30Y");
            assertThat(result.getStatus()).isEqualTo(PreArchiveStatus.NEEDS_ACTION.getCode());
            assertThat(result.getFixityValue()).isEqualTo("sha256-hash");
            assertThat(result.getFixityAlgo()).isEqualTo("SHA-256");

            ArgumentCaptor<Archive> archiveCaptor = ArgumentCaptor.forClass(Archive.class);
            verify(archiveMapper).insert(archiveCaptor.capture());
            Archive capturedArchive = archiveCaptor.getValue();
            assertThat(capturedArchive.getArchiveCode()).isEqualTo(expectedArchiveCode);
        }

        @Test
        @DisplayName("应该正确映射 LEDGER 类型到 AC02")
        void shouldCorrectlyMapLedgerToAC02() {
            // Given
            String batchNo = "COL-20250115-002";
            CollectionBatch batch = createTestBatch(
                    batchNo, "QZ01", "2025", "2025-01", "LEDGER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "ledger.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC02-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC02"); // LEDGER -> AC02
        }

        @Test
        @DisplayName("应该正确映射 REPORT 类型到 AC03")
        void shouldCorrectlyMapReportToAC03() {
            // Given
            String batchNo = "COL-20250115-003";
            CollectionBatch batch = createTestBatch(
                    batchNo, "QZ01", "2025", "2025-01", "REPORT"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "report.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC03-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC03"); // REPORT -> AC03
        }

        @Test
        @DisplayName("应该正确映射 OTHER 类型到 AC04")
        void shouldCorrectlyMapOtherToAC04() {
            // Given
            String batchNo = "COL-20250115-004";
            CollectionBatch batch = createTestBatch(
                    batchNo, "QZ01", "2025", "2025-01", "OTHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "other.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC04-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC04"); // OTHER -> AC04
        }

        @Test
        @DisplayName("空门类类型应该默认映射到 AC04")
        void shouldDefaultToAC04WhenCategoryIsNull() {
            // Given
            String batchNo = "COL-20250115-005";
            CollectionBatch batch = createTestBatch(
                    batchNo, "QZ01", "2025", "2025-01", null
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "attachment.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC04-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC04");
        }

        @Test
        @DisplayName("应该使用文件名作为初始标题")
        void shouldUseFileNameAsInitialTitle() {
            // Given
            String fileName = "重要凭证.pdf";
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", fileName, "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getTitle()).isEqualTo(fileName);
        }

        @Test
        @DisplayName("应该保存文件的哈希值和哈希算法")
        void shouldSaveFileHashAndAlgorithm() {
            // Given
            String expectedHash = "abc123def456";
            String expectedAlgorithm = "SM3";

            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", expectedHash, expectedAlgorithm, "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getFixityValue()).isEqualTo(expectedHash);
            assertThat(result.getFixityAlgo()).isEqualTo(expectedAlgorithm);
        }

        @Test
        @DisplayName("哈希算法为空时应该使用默认值 SHA-256")
        void shouldUseDefaultHashAlgorithmWhenNull() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", null, "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getFixityAlgo()).isEqualTo("SHA-256");
        }

        @Test
        @DisplayName("应该设置档案状态为 NEEDS_ACTION")
        void shouldSetArchiveStatusToNeedsAction() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getStatus()).isEqualTo(PreArchiveStatus.NEEDS_ACTION.getCode());
        }

        @Test
        @DisplayName("应该调用档号生成器生成档号")
        void shouldCallArchivalCodeGenerator() {
            // Given
            String fondsCode = "QZ02";
            String fiscalYear = "2024";
            String expectedRetention = "30Y";
            String expectedCategoryCode = "AC01";

            CollectionBatch batch = createTestBatch(
                    "COL-20240115-001", fondsCode, fiscalYear, "2024-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(
                    fondsCode, fiscalYear, expectedRetention, expectedCategoryCode
            )).thenReturn("QZ02-2024-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            verify(archivalCodeGenerator).generate(
                    fondsCode, fiscalYear, expectedRetention, expectedCategoryCode
            );
        }
    }

    @Nested
    @DisplayName("获取档案ID测试")
    class GetArchiveIdByFileIdTests {

        @Test
        @DisplayName("应该根据文件ID获取关联的档案ID")
        void shouldGetArchiveIdByFileId() {
            // Given
            String fileId = "file-123";
            String expectedArchiveId = "archive-456";

            CollectionBatchFile batchFile = new CollectionBatchFile();
            batchFile.setFileId(fileId);
            batchFile.setArchiveId(expectedArchiveId);

            when(batchFileMapper.selectByFileId(fileId)).thenReturn(batchFile);

            // When
            String result = batchToArchiveService.getArchiveIdByFileId(fileId);

            // Then
            assertThat(result).isEqualTo(expectedArchiveId);
            verify(batchFileMapper).selectByFileId(fileId);
        }

        @Test
        @DisplayName("文件不存在时应该返回 null")
        void shouldReturnNullWhenFileNotFound() {
            // Given
            String fileId = "nonexistent-file";
            when(batchFileMapper.selectByFileId(fileId)).thenReturn(null);

            // When
            String result = batchToArchiveService.getArchiveIdByFileId(fileId);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("批次文件没有关联档案时应该返回 null")
        void shouldReturnNullWhenBatchFileHasNoArchiveId() {
            // Given
            String fileId = "file-123";

            CollectionBatchFile batchFile = new CollectionBatchFile();
            batchFile.setFileId(fileId);
            batchFile.setArchiveId(null);

            when(batchFileMapper.selectByFileId(fileId)).thenReturn(batchFile);

            // When
            String result = batchToArchiveService.getArchiveIdByFileId(fileId);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("标记批次测试")
    class MarkBatchAsPendingMetadataTests {

        @Test
        @DisplayName("应该成功标记批次为待补录元数据")
        void shouldSuccessfullyMarkBatchAsPendingMetadata() {
            // Given
            Long batchId = 1L;

            // When
            batchToArchiveService.markBatchAsPendingMetadata(batchId);

            // Then - 方法执行成功，无异常抛出
            // 注意：根据实现，此方法目前只记录日志
            assertThat(batchId).isNotNull();
        }
    }

    @Nested
    @DisplayName("分类代码映射测试")
    class CategoryMappingTests {

        @Test
        @DisplayName("应该支持小写的门类类型")
        void shouldSupportLowerCaseCategory() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "voucher" // 小写
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC01");
        }

        @Test
        @DisplayName("应该支持混合大小写的门类类型")
        void shouldSupportMixedCaseCategory() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VoUcHeR" // 混合大小写
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC01");
        }

        @Test
        @DisplayName("无效的门类类型应该默认映射到 AC04")
        void shouldDefaultToAC04ForInvalidCategory() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "INVALID_TYPE"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC04-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getCategoryCode()).isEqualTo("AC04");
        }
    }

    @Nested
    @DisplayName("档号生成测试")
    class ArchiveIdGenerationTests {

        @Test
        @DisplayName("应该生成有效的档案ID")
        void shouldGenerateValidArchiveId() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then
            assertThat(result.getId()).isNotNull();
            assertThat(result.getId()).isNotEmpty();
            // UUID 格式: 32位十六进制字符（无连字符）
            assertThat(result.getId()).matches("^[a-f0-9]{32}$");
        }

        @Test
        @DisplayName("每次创建应该生成唯一的档案ID")
        void shouldGenerateUniqueArchiveIdForEachCreation() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent1 = createTestFileContent(
                    "file-1", "file1.pdf", "hash1", "SHA-256", "/storage/path/file-1.pdf"
            );
            ArcFileContent fileContent2 = createTestFileContent(
                    "file-2", "file2.pdf", "hash2", "SHA-256", "/storage/path/file-2.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result1 = batchToArchiveService.createArchiveFromBatch(fileContent1, batch);
            Archive result2 = batchToArchiveService.createArchiveFromBatch(fileContent2, batch);

            // Then - 两次生成的ID应该不同（虽然这个测试不是绝对确定，但概率上应该不同）
            assertThat(result1.getId()).isNotEqualTo(result2.getId());
        }
    }

    @Nested
    @DisplayName("完整字段测试")
    class CompleteFieldsTests {

        @Test
        @DisplayName("应该设置所有必填字段")
        void shouldSetAllRequiredFields() {
            // Given
            CollectionBatch batch = createTestBatch(
                    "COL-20250115-001", "QZ01", "2025", "2025-01", "VOUCHER"
            );

            ArcFileContent fileContent = createTestFileContent(
                    "file-1", "file.pdf", "sha256-hash", "SHA-256", "/storage/path/file-1.pdf"
            );

            when(archivalCodeGenerator.generate(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn("QZ01-2025-30Y-AC01-000001");
            when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

            // When
            Archive result = batchToArchiveService.createArchiveFromBatch(fileContent, batch);

            // Then - 验证所有必填字段都已设置
            assertThat(result.getId()).isNotEmpty();
            assertThat(result.getArchiveCode()).isNotEmpty();
            assertThat(result.getFondsNo()).isNotEmpty();
            assertThat(result.getCategoryCode()).isNotEmpty();
            assertThat(result.getFiscalYear()).isNotEmpty();
            assertThat(result.getFiscalPeriod()).isNotEmpty();
            assertThat(result.getTitle()).isNotEmpty();
            assertThat(result.getRetentionPeriod()).isNotEmpty();
            assertThat(result.getStatus()).isNotEmpty();
            assertThat(result.getFixityValue()).isNotEmpty();
            assertThat(result.getFixityAlgo()).isNotEmpty();
        }
    }
}
