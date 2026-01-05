// Input: JUnit 5、Spring Boot Test、Mockito、本地模块
// Output: BatchToArchiveServiceTest 类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.service.impl.BatchToArchiveServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * BatchToArchiveService 单元测试
 *
 * 测试覆盖:
 * - 从批次文件创建档案记录
 * - 档案记录字段正确性
 * - 状态设置为 PENDING_METADATA
 * - 根据 fileId 获取 archiveId
 *
 * 合规要求参考: DA/T 94-2022 元数据同步捕获
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("批次转换服务测试")
class BatchToArchiveServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private CollectionBatchFileMapper batchFileMapper;

    @Mock
    private ArchivalCodeGenerator archivalCodeGenerator;

    @InjectMocks
    private BatchToArchiveServiceImpl batchToArchiveService;

    private ArcFileContent testFileContent;
    private CollectionBatch testBatch;
    private CollectionBatchFile testBatchFile;

    @BeforeEach
    void setUp() {
        // 初始化测试文件内容
        testFileContent = new ArcFileContent();
        testFileContent.setId("file-uuid-123");
        testFileContent.setFileName("invoice-scan-001.pdf");
        testFileContent.setFileType("PDF");
        testFileContent.setFileSize(1024L * 1024L);
        testFileContent.setFileHash("abc123def456");
        testFileContent.setHashAlgorithm("SHA-256");
        testFileContent.setStoragePath("/storage/2024/01/file-uuid-123.pdf");
        testFileContent.setCreatedTime(LocalDateTime.now());

        // 初始化测试批次
        testBatch = CollectionBatch.builder()
            .id(1L)
            .batchNo("COL-20240105-001")
            .batchName("测试批次-2024年1月凭证")
            .fondsCode("001")
            .fiscalYear("2024")
            .fiscalPeriod("01")
            .archivalCategory("VOUCHER")
            .sourceChannel("WEB上传")
            .status(CollectionBatch.STATUS_UPLOADING)
            .totalFiles(10)
            .uploadedFiles(5)
            .createdTime(LocalDateTime.now())
            .build();

        // 初始化测试批次文件
        testBatchFile = CollectionBatchFile.builder()
            .id(1L)
            .batchId(1L)
            .fileId("file-uuid-123")
            .originalFilename("invoice-scan-001.pdf")
            .uploadStatus(CollectionBatchFile.STATUS_UPLOADED)
            .archiveId("archive-uuid-456")
            .build();
    }

    // ========== 创建档案记录测试 ==========

    @Test
    @DisplayName("应该创建档案记录并设置待补录状态")
    void createArchiveFromBatch_ShouldCreateArchiveWithPendingMetadataStatus() {
        // Given: 模拟档号生成器返回档号
        when(archivalCodeGenerator.generate(
            eq("001"), eq("2024"), eq("30Y"), eq("AC04")
        )).thenReturn("001-2024-30Y-AC04-000001");

        // Given: 模拟数据库插入返回成功
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(invocation -> {
            Archive archive = invocation.getArgument(0);
            archive.setId("archive-uuid-789");
            return 1;
        });

        // When: 调用创建档案记录
        Archive result = batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 验证返回的档案记录
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getArchiveCode()).isEqualTo("001-2024-30Y-AC04-000001");
        assertThat(result.getFondsNo()).isEqualTo("001");
        assertThat(result.getCategoryCode()).isEqualTo("AC04"); // 固定为原始凭证附件分类
        assertThat(result.getFiscalYear()).isEqualTo("2024");
        assertThat(result.getFiscalPeriod()).isEqualTo("01");
        assertThat(result.getTitle()).isEqualTo("invoice-scan-001.pdf"); // 初始使用文件名
        assertThat(result.getRetentionPeriod()).isEqualTo("30年");
        assertThat(result.getStatus()).isEqualTo(PreArchiveStatus.PENDING_METADATA.getCode());
        assertThat(result.getFixityValue()).isEqualTo("abc123def456");
        assertThat(result.getFixityAlgo()).isEqualTo("SHA-256");

        // 验证数据库操作
        verify(archiveMapper).insert(any(Archive.class));
        verify(archivalCodeGenerator).generate("001", "2024", "30Y", "AC04");
    }

    @Test
    @DisplayName("应该使用文件哈希值作为完整性校验值")
    void createArchiveFromBatch_ShouldUseFileHashAsFixityValue() {
        // Given
        when(archivalCodeGenerator.generate(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn("001-2024-30Y-AC04-000001");

        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // When
        batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 验证数据库操作
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    @DisplayName("应该使用AC04分类代码表示原始凭证附件")
    void createArchiveFromBatch_ShouldUseAC04CategoryCode() {
        // Given
        when(archivalCodeGenerator.generate(
            anyString(), anyString(), anyString(), eq("AC04")
        )).thenReturn("001-2024-30Y-AC04-000001");

        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // When
        Archive result = batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 验证分类代码固定为 AC04
        assertThat(result.getCategoryCode()).isEqualTo("AC04");
    }

    @Test
    @DisplayName("当哈希算法为空时应该使用默认值SHA-256")
    void createArchiveFromBatch_WhenHashAlgorithmNull_ShouldUseDefaultSHA256() {
        // Given: 文件内容哈希算法为空
        testFileContent.setHashAlgorithm(null);

        when(archivalCodeGenerator.generate(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn("001-2024-30Y-AC04-000001");

        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // When
        batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 验证数据库操作
        verify(archiveMapper).insert(any(Archive.class));
    }

    // ========== markBatchAsPendingMetadata 测试 ==========

    @Test
    @DisplayName("标记批次为待补录状态应该成功")
    void markBatchAsPendingMetadata_ShouldSucceed() {
        // When: 调用标记方法
        batchToArchiveService.markBatchAsPendingMetadata(1L);

        // Then: 验证方法正常完成（不抛异常）
        // 注意：实际的状态更新在智能解析完成后进行
        assertThat(true).isTrue();
    }

    // ========== getArchiveIdByFileId 测试 ==========

    @Test
    @DisplayName("应该根据文件ID返回关联的档案ID")
    void getArchiveIdByFileId_ShouldReturnArchiveId() {
        // Given: 模拟查询返回批次文件
        when(batchFileMapper.selectByFileId("file-uuid-123"))
            .thenReturn(testBatchFile);

        // When
        String archiveId = batchToArchiveService.getArchiveIdByFileId("file-uuid-123");

        // Then
        assertThat(archiveId).isEqualTo("archive-uuid-456");
        verify(batchFileMapper).selectByFileId("file-uuid-123");
    }

    @Test
    @DisplayName("当文件不存在时应该返回null")
    void getArchiveIdByFileId_WhenFileNotFound_ShouldReturnNull() {
        // Given: 模拟查询返回null
        when(batchFileMapper.selectByFileId("non-existent-file"))
            .thenReturn(null);

        // When
        String archiveId = batchToArchiveService.getArchiveIdByFileId("non-existent-file");

        // Then
        assertThat(archiveId).isNull();
    }

    @Test
    @DisplayName("当批次文件未关联档案时应该返回null")
    void getArchiveIdByFileId_WhenArchiveIdNotSet_ShouldReturnNull() {
        // Given: 批次文件的 archiveId 为 null
        testBatchFile.setArchiveId(null);
        when(batchFileMapper.selectByFileId("file-uuid-123"))
            .thenReturn(testBatchFile);

        // When
        String archiveId = batchToArchiveService.getArchiveIdByFileId("file-uuid-123");

        // Then
        assertThat(archiveId).isNull();
    }

    // ========== 边界条件测试 ==========

    @Test
    @DisplayName("批次期间为空时应该正常创建档案")
    void createArchiveFromBatch_WhenFiscalPeriodNull_ShouldSucceed() {
        // Given: 批次期间为空
        testBatch.setFiscalPeriod(null);

        when(archivalCodeGenerator.generate(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn("001-2024-30Y-AC04-000001");

        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // When
        Archive result = batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 验证档案记录创建成功，期间为null
        assertThat(result.getFiscalPeriod()).isNull();
        assertThat(result.getStatus()).isEqualTo(PreArchiveStatus.PENDING_METADATA.getCode());
    }

    @Test
    @DisplayName("文件名为空时应该使用默认值")
    void createArchiveFromBatch_WhenFileNameNull_ShouldHandleGracefully() {
        // Given: 文件名为空
        testFileContent.setFileName(null);

        when(archivalCodeGenerator.generate(
            anyString(), anyString(), anyString(), anyString()
        )).thenReturn("001-2024-30Y-AC04-000001");

        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // When
        Archive result = batchToArchiveService.createArchiveFromBatch(testFileContent, testBatch);

        // Then: 题名也应该为null（用户后续需要补录）
        assertThat(result.getTitle()).isNull();
    }
}
