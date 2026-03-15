// Input: JUnit 5, Mockito, Java 标准库
// Output: CollectionBatchServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.collection.BatchFileStorageService;
import com.nexusarchive.service.collection.BatchFileValidator;
import com.nexusarchive.service.collection.BatchNumberGenerator;
import com.nexusarchive.service.collection.CollectionMetadataInheritor;
import com.nexusarchive.service.helper.CollectionBatchHelper;
import com.nexusarchive.util.FileHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 资料收集批次服务实现类单元测试
 *
 * 测试范围:
 * 1. 批次状态查询
 * 2. 批次取消操作
 * 3. 批次权限校验
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("资料收集批次服务测试")
class CollectionBatchServiceImplTest {

    @Mock
    private CollectionBatchMapper batchMapper;

    @Mock
    private CollectionBatchFileMapper batchFileMapper;

    @Mock
    private BasFondsMapper fondsMapper;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private FileHashUtil fileHashUtil;

    @Mock
    private PreArchiveCheckService preArchiveCheckService;

    @Mock
    private BatchNumberGenerator batchNumberGenerator;

    @Mock
    private BatchFileValidator fileValidator;

    @Mock
    private BatchFileStorageService storageService;

    @Mock
    private CollectionMetadataInheritor metadataInheritor;

    @Mock
    private CollectionBatchHelper helper;

    private CollectionBatchServiceImpl collectionBatchService;

    @BeforeEach
    void setUp() {
        collectionBatchService = new CollectionBatchServiceImpl(
                batchMapper,
                batchFileMapper,
                fondsMapper,
                arcFileContentMapper,
                fileHashUtil,
                preArchiveCheckService,
                null, // auditLogService
                null, // batchToArchiveService
                batchNumberGenerator,
                fileValidator,
                storageService,
                metadataInheritor,
                helper
        );

        // 默认 mock 行为
        lenient().when(fileValidator.checkDuplicate(anyString(), anyString(), anyString())).thenReturn(null);
        lenient().when(fileValidator.detectFileType(anyString())).thenReturn("PDF");
        lenient().when(fileValidator.getExtension(anyString())).thenReturn("pdf");
        lenient().when(batchFileMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    }

    private CollectionBatch createTestBatch(Long id, String batchNo, String status, String createdBy) {
        CollectionBatch batch = new CollectionBatch();
        batch.setId(id);
        batch.setBatchNo(batchNo);
        batch.setFondsCode("QZ01");
        batch.setFiscalYear("2025");
        batch.setFiscalPeriod("2025-01");
        batch.setArchivalCategory("VOUCHER");
        batch.setStatus(status);
        batch.setTotalFiles(5);
        batch.setUploadedFiles(0);
        batch.setFailedFiles(0);
        batch.setCreatedBy(createdBy);
        return batch;
    }

    @Nested
    @DisplayName("批次取消测试")
    class CancelBatchTests {

        @Test
        @DisplayName("应该成功取消上传中的批次")
        void shouldSuccessfullyCancelUploadingBatch() {
            // Given
            Long batchId = 1L;
            CollectionBatch batch = createTestBatch(batchId, "COL-001", "UPLOADING", "user-1");
            when(batchMapper.selectById(batchId)).thenReturn(batch);
            when(batchMapper.updateById(any(CollectionBatch.class))).thenReturn(1);

            // When
            collectionBatchService.cancelBatch(batchId, "user-1");

            // Then
            assertThat(batch.getStatus()).isEqualTo("CANCELLED");
            verify(batchMapper).updateById(batch);
        }

        @Test
        @DisplayName("不应该取消已完成的批次")
        void shouldNotCancelCompletedBatch() {
            // Given
            Long batchId = 1L;
            CollectionBatch batch = createTestBatch(batchId, "COL-001", "COMPLETED", "user-1");
            when(batchMapper.selectById(batchId)).thenReturn(batch);

            // When & Then
            assertThatThrownBy(() -> collectionBatchService.cancelBatch(batchId, "user-1"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只能取消上传中");

            verify(batchMapper, never()).updateById(any(CollectionBatch.class));
        }

        @Test
        @DisplayName("非创建者不能取消批次")
        void shouldNotAllowNonCreatorToCancelBatch() {
            // Given
            Long batchId = 1L;
            CollectionBatch batch = createTestBatch(batchId, "COL-001", "UPLOADING", "creator-1");
            when(batchMapper.selectById(batchId)).thenReturn(batch);

            // When & Then
            assertThatThrownBy(() -> collectionBatchService.cancelBatch(batchId, "other-user"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("只能取消自己创建的批次");

            verify(batchMapper, never()).updateById(any(CollectionBatch.class));
        }
    }

    @Nested
    @DisplayName("批次文件查询测试")
    class GetBatchFilesTests {

        @Test
        @DisplayName("应该成功获取批次文件列表")
        void shouldSuccessfullyGetBatchFiles() {
            // Given
            Long batchId = 1L;
            CollectionBatchFile file1 = new CollectionBatchFile();
            file1.setId(1L);
            file1.setOriginalFilename("file1.pdf");

            CollectionBatchFile file2 = new CollectionBatchFile();
            file2.setId(2L);
            file2.setOriginalFilename("file2.pdf");

            when(batchFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(file1, file2));

            // When
            List<?> result = collectionBatchService.getBatchFiles(batchId);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("空批次应该返回空列表")
        void shouldReturnEmptyListForEmptyBatch() {
            // Given
            Long batchId = 1L;
            when(batchFileMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

            // When
            List<?> result = collectionBatchService.getBatchFiles(batchId);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
