// Input: JUnit 5, Spring Boot Test, Mockito, Local Types
// Output: CollectionBatchServiceTest
// Pos: 后端测试用例

package com.nexusarchive.service;

import com.nexusarchive.dto.BatchUploadRequest;
import com.nexusarchive.dto.BatchUploadResponse;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.CollectionBatchMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.mapper.BasFondsMapper;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.collection.BatchFileStorageService;
import com.nexusarchive.service.collection.BatchFileValidator;
import com.nexusarchive.service.collection.BatchNumberGenerator;
import com.nexusarchive.service.collection.CollectionMetadataInheritor;
import com.nexusarchive.service.collection.FourNatureCheckHelper;
import com.nexusarchive.service.impl.CollectionBatchServiceImpl;
import com.nexusarchive.service.helper.CollectionBatchHelper;
import com.nexusarchive.util.FileHashUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CollectionBatchService 单元测试
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("资料收集批次服务测试")
class CollectionBatchServiceTest {

    @Mock
    private CollectionBatchMapper batchMapper;
    @Mock
    private CollectionBatchFileMapper batchFileMapper;
    @Mock
    private BasFondsMapper fondsMapper;
    @Mock
    private ArcFileContentMapper arcFileContentMapper;
    @Mock
    private PreArchiveCheckService preArchiveCheckService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private BatchToArchiveService batchToArchiveService;
    @Mock
    private FileHashUtil fileHashUtil;
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

    @InjectMocks
    private CollectionBatchServiceImpl collectionBatchService;

    private BatchUploadRequest testRequest;
    private CollectionBatch testBatch;

    @BeforeEach
    void setUp() {
        FondsContext.setCurrentFondsNo("001");

        testRequest = new BatchUploadRequest();
        testRequest.setBatchName("测试批次-2024年1月凭证");
        testRequest.setFondsCode("001");
        testRequest.setFiscalYear("2024");
        testRequest.setTotalFiles(10);

        testBatch = CollectionBatch.builder()
            .id(1L).batchNo("COL-20240105-001").batchName("测试批次-2024年1月凭证")
            .fondsCode("001").fiscalYear("2024").archivalCategory("VOUCHER")
            .status(CollectionBatch.STATUS_UPLOADING).totalFiles(10).uploadedFiles(0)
            .failedFiles(0).totalSizeBytes(0L).createdBy("1")
            .createdTime(LocalDateTime.now()).lastModifiedTime(LocalDateTime.now()).build();
    }

    @AfterEach
    void tearDown() {
        FondsContext.clear();
    }

    @Nested
    @DisplayName("批次创建")
    class CreateBatchTests {
        @Test
        @DisplayName("应该成功创建批次")
        void createBatch_ShouldReturnBatchResponse() {
            when(batchNumberGenerator.generateBatchNo()).thenReturn("COL-20240105-001");
            when(batchNumberGenerator.generateUploadToken(any(), anyString())).thenReturn("token");
            when(helper.createInitialBatch(any(), anyString(), anyString(), anyString(), anyString())).thenReturn(testBatch);

            BatchUploadResponse response = collectionBatchService.createBatch(testRequest, "1");

            assertThat(response).isNotNull();
            verify(batchMapper).insert(any(CollectionBatch.class));
        }
    }

    @Nested
    @DisplayName("批次详情查询")
    class GetBatchDetailTests {
        @Test
        @DisplayName("应该返回批次详情")
        void getBatchDetail_ShouldReturnBatchInfo() {
            when(batchMapper.selectById(1L)).thenReturn(testBatch);
            CollectionBatchService.BatchDetailResponse mockRes = new CollectionBatchService.BatchDetailResponse(
                1L, "COL-001", "Name", "001", "2024", "VOUCHER", "UPLOADING", 10, 0, 0, 0L, 0
            );
            when(helper.mapToDetail(any())).thenReturn(mockRes);

            var detail = collectionBatchService.getBatchDetail(1L);

            assertThat(detail).isNotNull();
            assertThat(detail.batchNo()).isEqualTo("COL-001");
        }
    }

    @Nested
    @DisplayName("批次完成")
    class CompleteBatchTests {
        @Test
        @DisplayName("应该成功完成批次")
        void completeBatch_ShouldUpdateStatus() {
            when(batchMapper.selectById(1L)).thenReturn(testBatch);
            FourNatureCheckHelper.BatchCheckStatistics stats = new FourNatureCheckHelper.BatchCheckStatistics();
            when(helper.executeBatchCheck(any(), any())).thenReturn(stats);

            var result = collectionBatchService.completeBatch(1L, "1");

            assertThat(result).isNotNull();
            verify(batchMapper).updateById(any(CollectionBatch.class));
        }
    }

    @Nested
    @DisplayName("批次取消")
    class CancelBatchTests {
        @Test
        @DisplayName("应该成功取消批次")
        void cancelBatch_ShouldUpdateStatusToFailed() {
            when(batchMapper.selectById(1L)).thenReturn(testBatch);
            collectionBatchService.cancelBatch(1L, "1");
            verify(batchMapper).updateById(any(CollectionBatch.class));
        }
    }

    @Nested
    @DisplayName("批次文件列表")
    class GetBatchFilesTests {
        @Test
        @DisplayName("应该返回批次文件列表")
        void getBatchFiles_ShouldReturnFileList() {
            when(helper.mapToFiles(any())).thenReturn(List.of());
            var files = collectionBatchService.getBatchFiles(1L);
            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("四性检测")
    class RunFourNatureCheckTests {
        @Test
        @DisplayName("应该执行批次四性检测")
        void runFourNatureCheck_ShouldCheckAllFiles() {
            when(batchMapper.selectById(1L)).thenReturn(testBatch);
            FourNatureCheckHelper.BatchCheckStatistics stats = new FourNatureCheckHelper.BatchCheckStatistics();
            when(helper.executeBatchCheck(any(), any())).thenReturn(stats);

            var result = collectionBatchService.runFourNatureCheck(1L, "1");

            assertThat(result).isNotNull();
            verify(batchMapper, atLeastOnce()).updateById(any(CollectionBatch.class));
        }
    }

    @Nested
    @DisplayName("边界条件")
    class EdgeCaseTests {
        @Test
        @DisplayName("批次不存在时抛异常")
        void getBatchDetail_WhenBatchNotFound_ShouldThrowException() {
            when(batchMapper.selectById(999L)).thenReturn(null);
            assertThatThrownBy(() -> collectionBatchService.getBatchDetail(999L))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
