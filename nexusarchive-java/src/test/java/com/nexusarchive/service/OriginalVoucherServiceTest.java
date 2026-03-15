// Input: JUnit 5、Mockito、MyBatis-Plus、Spring Framework
// Output: OriginalVoucherServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.mapper.OriginalVoucherTypeMapper;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import com.nexusarchive.service.helper.OriginalVoucherHelper;
import com.nexusarchive.service.parser.OfdInvoiceParser;
import com.nexusarchive.service.parser.PdfInvoiceParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 原始凭证服务综合测试
 * <p>
 * TDD 测试套件，覆盖 OriginalVoucherService 的核心业务逻辑：
 * - CRUD 操作 (创建、读取、更新、删除)
 * - 查询与过滤 (分页、搜索、类型过滤、状态过滤)
 * - 关联管理 (文件关联、凭证关联)
 * - 状态转换 (草稿→待归档→已归档)
 * - 版本控制 (创建新版本、版本历史)
 * - 文件操作 (上传、下载、OCR 解析)
 * - 边界情况与异常处理
 * </p>
 *
 * 测试覆盖率目标: 80%+
 *
 * @see OriginalVoucherService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("原始凭证服务测试套件")
class OriginalVoucherServiceTest {

    // ========== Mock Dependencies ==========
    @Mock
    private OriginalVoucherMapper voucherMapper;

    @Mock
    private OriginalVoucherFileMapper fileMapper;

    @Mock
    private VoucherRelationMapper relationMapper;

    @Mock
    private OriginalVoucherTypeMapper typeMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private PdfInvoiceParser pdfInvoiceParser;

    @Mock
    private OfdInvoiceParser ofdInvoiceParser;

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private OriginalVoucherHelper helper;

    @InjectMocks
    private OriginalVoucherService voucherService;

    // ========== Test Fixtures ==========
    private OriginalVoucher sampleVoucher;
    private OriginalVoucher existingVoucher;
    private OriginalVoucherFile sampleFile;
    private OriginalVoucherType sampleType;
    private final String TEST_USER_ID = "test-user-001";
    private final String TEST_VOUCHER_ID = "voucher-001";
    private final String TEST_VOUCHER_NO = "OV-2023-INV-000001";
    private final String TEST_FONDS_CODE = "F001";
    private final String TEST_FISCAL_YEAR = "2023";

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        sampleVoucher = createSampleVoucher();
        existingVoucher = createExistingVoucher();
        sampleFile = createSampleFile();
        sampleType = createSampleType();
    }

    // ========== 查询和过滤测试 ==========

    @Test
    @DisplayName("应该成功分页查询原始凭证列表")
    void shouldGetVouchersSuccessfully() {
        // Given
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));
        mockPage.setTotal(1L);

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals(1L, result.getTotal());
        assertTrue(sampleVoucher.getIsLatest());
        verify(voucherMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该支持关键字搜索 (凭证号/对方单位/摘要)")
    void shouldSearchVouchersByKeyword() {
        // Given
        String searchKeyword = "测试单位";
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, searchKeyword, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains("测试单位")
        ));
    }

    @Test
    @DisplayName("应该支持按档案门类过滤")
    void shouldFilterVouchersByCategory() {
        // Given
        String category = "INVOICE";
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, category, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains("INVOICE")
        ));
    }

    @Test
    @DisplayName("应该支持按类型代码过滤 (含别名映射)")
    void shouldFilterVouchersByTypeWithAliases() {
        // Given
        String typeCode = "BANK_RECEIPT";
        List<String> typeAliases = Arrays.asList("BANK_RECEIPT", "BANK_SLIP");

        OriginalVoucher bankSlipVoucher = new OriginalVoucher();
        bankSlipVoucher.setId("voucher-002");
        bankSlipVoucher.setVoucherType("BANK_SLIP");
        bankSlipVoucher.setIsLatest(true);

        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(bankSlipVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(typeCode)).thenReturn(typeAliases);

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, typeCode, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals("BANK_SLIP", result.getRecords().get(0).getVoucherType());
        verify(helper).getTypeAliases(typeCode);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains("BANK_RECEIPT") ||
                wrapper.toString().contains("BANK_SLIP")
        ));
    }

    @Test
    @DisplayName("应该支持按归档状态过滤")
    void shouldFilterVouchersByStatus() {
        // Given
        String status = "ARCHIVED";
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(existingVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, status, null, null, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains("ARCHIVED")
        ));
    }

    @Test
    @DisplayName("应该支持按全宗号过滤")
    void shouldFilterVouchersByFondsCode() {
        // Given
        String fondsCode = "F001";
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, null, fondsCode, null, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains(fondsCode)
        ));
    }

    @Test
    @DisplayName("应该支持按会计年度过滤")
    void shouldFilterVouchersByFiscalYear() {
        // Given
        String fiscalYear = "2023";
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, null, null, fiscalYear, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), argThat(wrapper ->
                wrapper.toString().contains(fiscalYear)
        ));
    }

    @Test
    @DisplayName("应该应用数据权限过滤")
    void shouldApplyDataScopeFilter() {
        // Given
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(sampleVoucher));

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());

        // When
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        verify(dataScopeService).applyOriginalVoucherScope(any(LambdaQueryWrapper.class), any(DataScopeContext.class));
    }

    // ========== CRUD 操作测试 ==========

    @Test
    @DisplayName("应该通过ID成功获取原始凭证")
    void shouldGetVoucherById() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);

        // When
        OriginalVoucher result = voucherService.getById(TEST_VOUCHER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_VOUCHER_ID, result.getId());
        verify(voucherMapper).selectById(TEST_VOUCHER_ID);
    }

    @Test
    @DisplayName("应该通过凭证号成功获取原始凭证 (fallback)")
    void shouldGetVoucherByVoucherNo() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_NO)).thenReturn(null);
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingVoucher);

        // When
        OriginalVoucher result = voucherService.getById(TEST_VOUCHER_NO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_VOUCHER_NO, result.getVoucherNo());
        verify(voucherMapper).selectById(TEST_VOUCHER_NO);
        verify(voucherMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取不存在的凭证应该抛出异常")
    void shouldThrowExceptionWhenVoucherNotFound() {
        // Given
        String nonExistentId = "non-existent-id";
        when(voucherMapper.selectById(nonExistentId)).thenReturn(null);
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.getById(nonExistentId);
        });

        assertTrue(exception.getMessage().contains("原始凭证不存在"));
        verify(voucherMapper).selectById(nonExistentId);
        verify(voucherMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取已删除的凭证应该抛出异常")
    void shouldThrowExceptionWhenVoucherIsDeleted() {
        // Given
        OriginalVoucher deletedVoucher = new OriginalVoucher();
        deletedVoucher.setId(TEST_VOUCHER_ID);
        deletedVoucher.setDeleted(1);

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(deletedVoucher);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.getById(TEST_VOUCHER_ID);
        });

        assertTrue(exception.getMessage().contains("原始凭证不存在"));
    }

    @Test
    @DisplayName("应该成功创建原始凭证")
    void shouldCreateVoucherSuccessfully() {
        // Given
        doNothing().when(helper).validateVoucherType(anyString(), anyString());
        when(helper.generateVoucherNo(anyString(), anyString(), anyString()))
                .thenReturn(TEST_VOUCHER_NO);
        when((Integer) voucherMapper.insert(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.create(sampleVoucher, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_VOUCHER_NO, result.getVoucherNo());
        assertEquals("DRAFT", result.getArchiveStatus());
        assertEquals(1, result.getVersion());
        assertTrue(result.getIsLatest());
        assertEquals(TEST_USER_ID, result.getCreatedBy());
        assertNotNull(result.getCreatedTime());

        verify(helper).validateVoucherType(anyString(), anyString());
        verify(helper).generateVoucherNo(anyString(), anyString(), anyString());
        verify(voucherMapper).insert(any(OriginalVoucher.class));
    }

    @Test
    @DisplayName("创建凭证时应该自动设置默认值")
    void shouldSetDefaultValuesWhenCreatingVoucher() {
        // Given
        sampleVoucher.setId(null);
        sampleVoucher.setBusinessDate(null);

        doNothing().when(helper).validateVoucherType(anyString(), anyString());
        when(helper.generateVoucherNo(anyString(), anyString(), anyString()))
                .thenReturn(TEST_VOUCHER_NO);
        when((Integer) voucherMapper.insert(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.create(sampleVoucher, TEST_USER_ID);

        // Then
        assertNotNull(result.getId());
        assertNotNull(result.getBusinessDate());
        assertEquals(LocalDate.now(), result.getBusinessDate());
        assertEquals("DRAFT", result.getArchiveStatus());
        assertEquals(1, result.getVersion());
        assertTrue(result.getIsLatest());
    }

    @Test
    @DisplayName("创建凭证时应该自动设置保管期限")
    void shouldSetDefaultRetentionPeriod() {
        // Given
        sampleVoucher.setRetentionPeriod(null);

        doNothing().when(helper).validateVoucherType(anyString(), anyString());
        when(helper.generateVoucherNo(anyString(), anyString(), anyString()))
                .thenReturn(TEST_VOUCHER_NO);
        when(typeMapper.findByTypeCode(anyString())).thenReturn(sampleType);
        when((Integer) voucherMapper.insert(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.create(sampleVoucher, TEST_USER_ID);

        // Then
        assertEquals("30Y", result.getRetentionPeriod());
        verify(typeMapper).findByTypeCode(anyString());
    }

    @Test
    @DisplayName("应该成功更新草稿状态的原始凭证")
    void shouldUpdateDraftVoucherSuccessfully() {
        // Given
        OriginalVoucher updates = new OriginalVoucher();
        updates.setAmount(new BigDecimal("2000.00"));
        updates.setCounterparty("新对方单位");

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.update(TEST_VOUCHER_ID, updates, null, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("2000.00"), result.getAmount());
        assertEquals("新对方单位", result.getCounterparty());
        assertEquals(TEST_USER_ID, result.getLastModifiedBy());
        assertNotNull(result.getLastModifiedTime());

        verify(voucherMapper).updateById(any(OriginalVoucher.class));
    }

    @Test
    @DisplayName("更新已归档凭证应该创建新版本")
    void shouldCreateNewVersionWhenUpdatingArchivedVoucher() {
        // Given
        OriginalVoucher archivedVoucher = createArchivedVoucher();
        OriginalVoucher updates = new OriginalVoucher();
        updates.setAmount(new BigDecimal("3000.00"));

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(archivedVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);
        when((Integer) voucherMapper.insert(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.update(TEST_VOUCHER_ID, updates, "变更原因", TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertNotEquals(TEST_VOUCHER_ID, result.getId());
        assertEquals(new BigDecimal("3000.00"), result.getAmount());
        assertEquals(2, result.getVersion());
        assertEquals("DRAFT", result.getArchiveStatus());
        assertTrue(result.getIsLatest());

        verify(voucherMapper).updateById(any(OriginalVoucher.class)); // 设置旧版本为非最新
        verify(voucherMapper).insert(any(OriginalVoucher.class)); // 插入新版本
    }

    @Test
    @DisplayName("应该成功删除草稿状态的原始凭证")
    void shouldDeleteDraftVoucherSuccessfully() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);

        // When
        voucherService.delete(TEST_VOUCHER_ID, TEST_USER_ID);

        // Then
        ArgumentCaptor<OriginalVoucher> captor = ArgumentCaptor.forClass(OriginalVoucher.class);
        verify(voucherMapper).updateById(captor.capture());
        OriginalVoucher deletedVoucher = captor.getValue();
        assertEquals(1, deletedVoucher.getDeleted());
        assertEquals(TEST_USER_ID, deletedVoucher.getLastModifiedBy());
    }

    @Test
    @DisplayName("删除已归档凭证应该抛出异常")
    void shouldThrowExceptionWhenDeletingArchivedVoucher() {
        // Given
        OriginalVoucher archivedVoucher = createArchivedVoucher();
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(archivedVoucher);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.delete(TEST_VOUCHER_ID, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("已归档的原始凭证不允许删除"));
        verify(voucherMapper, never()).updateById((OriginalVoucher) any());
    }

    // ========== 文件管理测试 ==========

    @Test
    @DisplayName("应该成功获取凭证的文件列表")
    void shouldGetVoucherFilesSuccessfully() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(fileMapper.findByVoucherId(TEST_VOUCHER_ID))
                .thenReturn(Arrays.asList(sampleFile));

        // When
        List<OriginalVoucherFile> result = voucherService.getFiles(TEST_VOUCHER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(sampleFile.getId(), result.get(0).getId());
        verify(fileMapper).findByVoucherId(TEST_VOUCHER_ID);
    }

    @Test
    @DisplayName("应该支持通过凭证号获取文件列表")
    void shouldGetFilesByVoucherNo() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_NO)).thenReturn(null);
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingVoucher);
        when(fileMapper.findByVoucherId(TEST_VOUCHER_ID))
                .thenReturn(Arrays.asList(sampleFile));

        // When
        List<OriginalVoucherFile> result = voucherService.getFiles(TEST_VOUCHER_NO);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(voucherMapper).selectById(TEST_VOUCHER_NO);
        verify(voucherMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取不存在凭证的文件应该抛出异常")
    void shouldThrowExceptionWhenGettingFilesForNonExistentVoucher() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(null);
        when(voucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.getFiles(TEST_VOUCHER_ID);
        });

        assertTrue(exception.getMessage().contains("原始凭证不存在"));
    }

    @Test
    @DisplayName("应该成功下载文件")
    void shouldDownloadFileSuccessfully() throws Exception {
        // Given
        String fileId = "file-001";
        Path mockPath = mock(Path.class);
        File mockFile = mock(File.class);

        when(fileMapper.selectById(fileId)).thenReturn(sampleFile);
        when(fileStorageService.resolvePath(sampleFile.getStoragePath())).thenReturn(mockPath);
        when(mockPath.toFile()).thenReturn(mockFile);
        when(fileStorageService.exists(sampleFile.getStoragePath())).thenReturn(true);
        when(helper.determineContentType(anyString(), anyString())).thenReturn("application/pdf");

        // When
        ResponseEntity<Resource> result = voucherService.downloadFile(fileId);

        // Then
        assertNotNull(result);
        assertEquals(200, result.getStatusCode().value());
        assertEquals("application/pdf", result.getHeaders().getContentType().toString());
        assertNotNull(result.getBody());

        verify(fileStorageService).resolvePath(sampleFile.getStoragePath());
        verify(fileStorageService).exists(sampleFile.getStoragePath());
    }

    @Test
    @DisplayName("下载不存在的文件应该抛出异常")
    void shouldThrowExceptionWhenDownloadingNonExistentFile() {
        // Given
        String fileId = "non-existent-file";
        when(fileMapper.selectById(fileId)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.downloadFile(fileId);
        });

        assertTrue(exception.getMessage().contains("FILE_NOT_FOUND") ||
                exception.getMessage().contains("文件不存在"));
    }

    @Test
    @DisplayName("下载物理文件不存在的文件应该抛出异常")
    void shouldThrowExceptionWhenPhysicalFileNotFound() {
        // Given
        String fileId = "file-001";
        when(fileMapper.selectById(fileId)).thenReturn(sampleFile);
        when(fileStorageService.resolvePath(sampleFile.getStoragePath())).thenReturn(mock(Path.class));
        when(fileStorageService.exists(sampleFile.getStoragePath())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.downloadFile(fileId);
        });

        assertTrue(exception.getMessage().contains("PHYSICAL_FILE_NOT_FOUND") ||
                exception.getMessage().contains("物理文件不存在"));
    }

    @Test
    @DisplayName("应该成功上传文件")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        MultipartFile mockFile = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(fileMapper.findByVoucherId(TEST_VOUCHER_ID)).thenReturn(Collections.emptyList());
        doNothing().when(fileStorageService).saveFile(any(), anyString());
        when(helper.calculateHash(any())).thenReturn("abc123");
        when((Integer) fileMapper.insert(any(OriginalVoucherFile.class))).thenReturn(1);
        when(helper.determineContentType(anyString(), anyString())).thenReturn("application/pdf");

        // When
        OriginalVoucherFile result = voucherService.addFile(TEST_VOUCHER_ID, mockFile, "PRIMARY", TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals("test.pdf", result.getFileName());
        assertEquals("PDF", result.getFileType());
        assertEquals("PRIMARY", result.getFileRole());
        assertEquals(1, result.getSequenceNo());
        assertEquals(TEST_USER_ID, result.getCreatedBy());

        verify(fileStorageService).saveFile(any(), anyString());
        verify(fileMapper).insert(any(OriginalVoucherFile.class));
    }

    @Test
    @DisplayName("上传空文件应该抛出异常")
    void shouldThrowExceptionWhenUploadingEmptyFile() throws Exception {
        // Given
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.addFile(TEST_VOUCHER_ID, emptyFile, "PRIMARY", TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("上传文件为空"));
        verify(fileMapper, never()).insert((OriginalVoucherFile) any());
    }

    @Test
    @DisplayName("上传PDF文件时应该自动解析发票信息")
    void shouldParsePdfInvoiceWhenUploading() throws Exception {
        // Given
        MultipartFile pdfFile = new MockMultipartFile(
                "file",
                "invoice.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(fileMapper.findByVoucherId(TEST_VOUCHER_ID)).thenReturn(Collections.emptyList());
        doNothing().when(fileStorageService).saveFile(any(), anyString());
        when(helper.calculateHash(any())).thenReturn("abc123");
        when((Integer) fileMapper.insert(any(OriginalVoucherFile.class))).thenReturn(1);
        when(fileStorageService.resolvePath(anyString())).thenReturn(mock(Path.class));
        when(fileStorageService.exists(anyString())).thenReturn(true);

        // Mock PDF 解析结果
        Map<String, Object> parseResult = Map.of(
                "total_amount_value", "1000.00",
                "invoice_date_value", "2023-01-15"
        );
        when(pdfInvoiceParser.parse(any(File.class))).thenReturn(parseResult);
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);

        // When
        voucherService.addFile(TEST_VOUCHER_ID, pdfFile, "PRIMARY", TEST_USER_ID);

        // Then
        verify(pdfInvoiceParser).parse(any(File.class));
        ArgumentCaptor<OriginalVoucher> captor = ArgumentCaptor.forClass(OriginalVoucher.class);
        verify(voucherMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getAmount());
        assertNotNull(captor.getValue().getBusinessDate());
    }

    // ========== 关联管理测试 ==========

    @Test
    @DisplayName("应该成功创建凭证关联")
    void shouldCreateRelationSuccessfully() {
        // Given
        String accountingVoucherId = "acc-voucher-001";
        String relationDesc = "手工关联";

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(relationMapper.countRelation(TEST_VOUCHER_ID, accountingVoucherId)).thenReturn(0);
        when((Integer) relationMapper.insert(any(VoucherRelation.class))).thenReturn(1);

        // When
        VoucherRelation result = voucherService.createRelation(
                TEST_VOUCHER_ID, accountingVoucherId, relationDesc, TEST_USER_ID
        );

        // Then
        assertNotNull(result);
        assertEquals(TEST_VOUCHER_ID, result.getOriginalVoucherId());
        assertEquals(accountingVoucherId, result.getAccountingVoucherId());
        assertEquals("ORIGINAL_TO_ACCOUNTING", result.getRelationType());
        assertEquals(relationDesc, result.getRelationDesc());
        assertEquals(TEST_USER_ID, result.getCreatedBy());

        verify(relationMapper).insert(any(VoucherRelation.class));
    }

    @Test
    @DisplayName("创建重复关联应该抛出异常")
    void shouldThrowExceptionWhenCreatingDuplicateRelation() {
        // Given
        String accountingVoucherId = "acc-voucher-001";

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(relationMapper.countRelation(TEST_VOUCHER_ID, accountingVoucherId)).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.createRelation(TEST_VOUCHER_ID, accountingVoucherId, null, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("关联关系已存在"));
        verify(relationMapper, never()).insert((VoucherRelation) any());
    }

    @Test
    @DisplayName("应该成功删除关联")
    void shouldDeleteRelationSuccessfully() {
        // Given
        String relationId = "relation-001";
        VoucherRelation relation = VoucherRelation.builder()
                .id(relationId)
                .originalVoucherId(TEST_VOUCHER_ID)
                .accountingVoucherId("acc-voucher-001")
                .deleted(0)
                .build();

        when(relationMapper.selectById(relationId)).thenReturn(relation);
        when((Integer) relationMapper.updateById(any(VoucherRelation.class))).thenReturn(1);

        // When
        voucherService.deleteRelation(relationId);

        // Then
        ArgumentCaptor<VoucherRelation> captor = ArgumentCaptor.forClass(VoucherRelation.class);
        verify(relationMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleted());
    }

    @Test
    @DisplayName("删除不存在的关联不应该报错")
    void shouldNotThrowWhenDeletingNonExistentRelation() {
        // Given
        String relationId = "non-existent-relation";
        when(relationMapper.selectById(relationId)).thenReturn(null);

        // When & Then - 不应该抛出异常
        assertDoesNotThrow(() -> {
            voucherService.deleteRelation(relationId);
        });

        verify(relationMapper).selectById(relationId);
        verify(relationMapper, never()).updateById((VoucherRelation) any());
    }

    // ========== 状态转换测试 ==========

    @Test
    @DisplayName("应该成功提交归档")
    void shouldSubmitForArchiveSuccessfully() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher);
        when(fileMapper.findByVoucherId(TEST_VOUCHER_ID))
                .thenReturn(Arrays.asList(sampleFile));
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);

        // When
        voucherService.submitForArchive(TEST_VOUCHER_ID, TEST_USER_ID);

        // Then
        ArgumentCaptor<OriginalVoucher> captor = ArgumentCaptor.forClass(OriginalVoucher.class);
        verify(voucherMapper).updateById(captor.capture());
        OriginalVoucher pendingVoucher = captor.getValue();
        assertEquals("PENDING", pendingVoucher.getArchiveStatus());
        assertEquals(TEST_USER_ID, pendingVoucher.getLastModifiedBy());
    }

    @Test
    @DisplayName("提交归档时应该验证必需字段")
    void shouldValidateRequiredFieldsWhenSubmittingForArchive() {
        // Given
        OriginalVoucher invalidVoucher = new OriginalVoucher();
        invalidVoucher.setId(TEST_VOUCHER_ID);
        invalidVoucher.setArchiveStatus("DRAFT");
        invalidVoucher.setBusinessDate(null); // 缺少业务日期

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(invalidVoucher);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.submitForArchive(TEST_VOUCHER_ID, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("业务日期不能为空") ||
                exception.getMessage().contains("全宗号不能为空") ||
                exception.getMessage().contains("原始凭证必须至少包含一个文件"));
    }

    @Test
    @DisplayName("只有草稿状态的凭证可以提交归档")
    void shouldOnlyAllowDraftStatusForSubmit() {
        // Given
        OriginalVoucher archivedVoucher = createArchivedVoucher();
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(archivedVoucher);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.submitForArchive(TEST_VOUCHER_ID, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("只有草稿状态的凭证可以提交归档"));
        verify(voucherMapper, never()).updateById((OriginalVoucher) any());
    }

    @Test
    @DisplayName("应该成功确认归档")
    void shouldConfirmArchiveSuccessfully() {
        // Given
        OriginalVoucher pendingVoucher = new OriginalVoucher();
        pendingVoucher.setId(TEST_VOUCHER_ID);
        pendingVoucher.setArchiveStatus("PENDING");

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(pendingVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);

        // When
        voucherService.confirmArchive(TEST_VOUCHER_ID, TEST_USER_ID);

        // Then
        ArgumentCaptor<OriginalVoucher> captor = ArgumentCaptor.forClass(OriginalVoucher.class);
        verify(voucherMapper).updateById(captor.capture());
        OriginalVoucher archivedVoucher = captor.getValue();
        assertEquals("ARCHIVED", archivedVoucher.getArchiveStatus());
        assertNotNull(archivedVoucher.getArchivedTime());
        assertEquals(TEST_USER_ID, archivedVoucher.getLastModifiedBy());
    }

    @Test
    @DisplayName("只有待归档状态的凭证可以确认归档")
    void shouldOnlyAllowPendingStatusForConfirm() {
        // Given
        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(existingVoucher); // DRAFT 状态

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            voucherService.confirmArchive(TEST_VOUCHER_ID, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("只有待归档状态的凭证可以确认归档"));
        verify(voucherMapper, never()).updateById((OriginalVoucher) any());
    }

    // ========== 版本控制测试 ==========

    @Test
    @DisplayName("应该成功获取版本历史")
    void shouldGetVersionHistorySuccessfully() {
        // Given
        List<OriginalVoucher> history = Arrays.asList(
                existingVoucher,
                createNewVersionVoucher()
        );

        when(voucherMapper.findVersionHistory(TEST_VOUCHER_ID)).thenReturn(history);

        // When
        List<OriginalVoucher> result = voucherService.getVersionHistory(TEST_VOUCHER_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(voucherMapper).findVersionHistory(TEST_VOUCHER_ID);
    }

    @Test
    @DisplayName("创建新版本时应该保留关键字段")
    void shouldPreserveKeyFieldsWhenCreatingNewVersion() {
        // Given
        OriginalVoucher archivedVoucher = createArchivedVoucher();
        OriginalVoucher updates = new OriginalVoucher();
        updates.setAmount(new BigDecimal("5000.00"));

        when(voucherMapper.selectById(TEST_VOUCHER_ID)).thenReturn(archivedVoucher);
        when((Integer) voucherMapper.updateById(any(OriginalVoucher.class))).thenReturn(1);
        when((Integer) voucherMapper.insert(any(OriginalVoucher.class))).thenReturn(1);

        // When
        OriginalVoucher result = voucherService.createNewVersion(
                archivedVoucher, updates, "修改金额", TEST_USER_ID
        );

        // Then
        assertNotNull(result);
        assertEquals(archivedVoucher.getVoucherNo(), result.getVoucherNo());
        assertEquals(archivedVoucher.getVoucherCategory(), result.getVoucherCategory());
        assertEquals(archivedVoucher.getFondsCode(), result.getFondsCode());
        assertEquals(archivedVoucher.getFiscalYear(), result.getFiscalYear());
        assertEquals(new BigDecimal("5000.00"), result.getAmount()); // 新值
        assertEquals(archivedVoucher.getCounterparty(), result.getCounterparty()); // 旧值
        assertEquals(2, result.getVersion());
        assertEquals("DRAFT", result.getArchiveStatus());
    }

    // ========== 统计测试 ==========

    @Test
    @DisplayName("应该成功获取统计数据")
    void shouldGetStatsSuccessfully() {
        // Given
        LambdaQueryWrapper<OriginalVoucher> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalVoucher::getIsLatest, true);

        when(voucherMapper.selectCount(any())).thenReturn((Long) 100L, (Long) 60L, (Long) 20L);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When
        OriginalVoucherService.OriginalVoucherStats result =
                voucherService.getStats(TEST_FONDS_CODE, TEST_FISCAL_YEAR);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.total());
        assertEquals(60L, result.archived());
        assertEquals(20L, result.pending());
        assertEquals(20L, result.draft());
    }

    @Test
    @DisplayName("获取统计时应该应用数据权限")
    void shouldApplyDataScopeWhenGettingStats() {
        // Given
        when(voucherMapper.selectCount(any())).thenReturn((Long) 50L, (Long) 30L, (Long) 10L);
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());

        // When
        OriginalVoucherService.OriginalVoucherStats result =
                voucherService.getStats(null, TEST_FISCAL_YEAR);

        // Then
        assertNotNull(result);
        verify(dataScopeService).applyOriginalVoucherScope((LambdaQueryWrapper<OriginalVoucher>) any(), any(DataScopeContext.class));
    }

    // ========== 边界情况测试 ==========

    @Test
    @DisplayName("应该处理空输入参数")
    void shouldHandleNullInputParameters() {
        // Given
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When - 所有参数都为 null
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    @DisplayName("应该处理空字符串参数")
    void shouldHandleEmptyStringParameters() {
        // Given
        Page<OriginalVoucher> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When - 空字符串参数
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, 10, "", "", "", "", "", "", ""
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该处理分页边界")
    void shouldHandlePaginationBoundaries() {
        // Given
        Page<OriginalVoucher> mockPage = new Page<>(1, Integer.MAX_VALUE);
        mockPage.setRecords(Collections.emptyList());

        when(voucherMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);
        when(helper.getTypeAliases(anyString())).thenReturn(Collections.singletonList("INV_PAPER"));

        // When - 超大分页
        Page<OriginalVoucher> result = voucherService.getVouchers(
                1, Integer.MAX_VALUE, null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(result);
        verify(voucherMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ========== Helper Methods ==========

    private OriginalVoucher createSampleVoucher() {
        return OriginalVoucher.builder()
                .id(null) // 将在创建时生成
                .voucherNo(null)
                .voucherCategory("INVOICE")
                .voucherType("INV_PAPER")
                .businessDate(LocalDate.now())
                .amount(new BigDecimal("1000.00"))
                .currency("CNY")
                .counterparty("测试单位")
                .summary("测试摘要")
                .fondsCode(TEST_FONDS_CODE)
                .fiscalYear(TEST_FISCAL_YEAR)
                .archiveStatus("DRAFT")
                .version(1)
                .isLatest(true)
                .deleted(0)
                .build();
    }

    private OriginalVoucher createExistingVoucher() {
        return OriginalVoucher.builder()
                .id(TEST_VOUCHER_ID)
                .voucherNo(TEST_VOUCHER_NO)
                .voucherCategory("INVOICE")
                .voucherType("INV_PAPER")
                .businessDate(LocalDate.now())
                .amount(new BigDecimal("1000.00"))
                .currency("CNY")
                .counterparty("测试单位")
                .summary("测试摘要")
                .fondsCode(TEST_FONDS_CODE)
                .fiscalYear(TEST_FISCAL_YEAR)
                .archiveStatus("DRAFT")
                .version(1)
                .isLatest(true)
                .createdBy(TEST_USER_ID)
                .createdTime(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private OriginalVoucher createArchivedVoucher() {
        return OriginalVoucher.builder()
                .id(TEST_VOUCHER_ID)
                .voucherNo(TEST_VOUCHER_NO)
                .voucherCategory("INVOICE")
                .voucherType("INV_PAPER")
                .businessDate(LocalDate.now())
                .amount(new BigDecimal("1000.00"))
                .currency("CNY")
                .counterparty("测试单位")
                .summary("测试摘要")
                .fondsCode(TEST_FONDS_CODE)
                .fiscalYear(TEST_FISCAL_YEAR)
                .archiveStatus("ARCHIVED")
                .archivedTime(LocalDateTime.now())
                .version(1)
                .isLatest(true)
                .createdBy(TEST_USER_ID)
                .createdTime(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private OriginalVoucher createNewVersionVoucher() {
        return OriginalVoucher.builder()
                .id("voucher-002")
                .voucherNo(TEST_VOUCHER_NO)
                .voucherCategory("INVOICE")
                .voucherType("INV_PAPER")
                .businessDate(LocalDate.now())
                .amount(new BigDecimal("2000.00"))
                .currency("CNY")
                .counterparty("测试单位")
                .summary("修改后的摘要")
                .fondsCode(TEST_FONDS_CODE)
                .fiscalYear(TEST_FISCAL_YEAR)
                .archiveStatus("DRAFT")
                .version(2)
                .parentVersionId(TEST_VOUCHER_ID)
                .versionReason("修改金额")
                .isLatest(false)
                .createdBy(TEST_USER_ID)
                .createdTime(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private OriginalVoucherFile createSampleFile() {
        return OriginalVoucherFile.builder()
                .id("file-001")
                .voucherId(TEST_VOUCHER_ID)
                .fileName("test.pdf")
                .fileType("PDF")
                .fileSize(1024L)
                .storagePath("original-vouchers/" + TEST_VOUCHER_ID + "/file-001.pdf")
                .fileHash("abc123")
                .hashAlgorithm("SM3")
                .fileRole("PRIMARY")
                .sequenceNo(1)
                .createdBy(TEST_USER_ID)
                .createdTime(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private OriginalVoucherType createSampleType() {
        return OriginalVoucherType.builder()
                .id("type-001")
                .typeCode("INV_PAPER")
                .typeName("纸质发票")
                .categoryCode("INVOICE")
                .enabled(true)
                .defaultRetention("30Y")
                .build();
    }
}
