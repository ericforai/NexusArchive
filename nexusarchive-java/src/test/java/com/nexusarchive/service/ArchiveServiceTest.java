// Input: MyBatis-Plus、org.junit、org.mockito、Spring Framework、等
// Output: ArchiveServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import com.nexusarchive.service.strategy.ArchivalCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 档案服务综合测试
 * <p>
 * TDD 测试套件，覆盖 ArchiveService 的核心业务逻辑：
 * - CRUD 操作 (创建、读取、更新、删除)
 * - 状态转换与业务规则
 * - 关联管理 (文件关联、凭证关联)
 * - 查询与过滤 (分页、搜索、权限)
 * - 边界情况与异常处理
 * </p>
 *
 * 测试覆盖率目标: 80%+
 *
 * @see ArchiveService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("档案服务测试套件")
class ArchiveServiceTest {

    // ========== Mock Dependencies ==========
    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArchivalCodeGenerator codeGenerator;

    @Mock
    private DataScopeService dataScopeService;

    @Mock
    private com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private VoucherRelationMapper voucherRelationMapper;

    @InjectMocks
    private ArchiveService archiveService;

    // ========== Test Fixtures ==========
    private Archive sampleArchive;
    private Archive existingArchive;
    private final String TEST_USER_ID = "test-user-001";
    private final String TEST_ARCHIVE_ID = "archive-001";
    private final String TEST_ARCHIVE_CODE = "A-2023-001";
    private final String TEST_FONDS_NO = "F001";

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        sampleArchive = createSampleArchive();

        existingArchive = createExistingArchive();

        // 设置默认的数据权限上下文
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());
        when(dataScopeService.canAccessArchive(any(), any())).thenReturn(true);
    }

    // ========== Helper Methods ==========

    /**
     * 创建示例档案实体
     */
    private Archive createSampleArchive() {
        Archive archive = new Archive();
        archive.setTitle("Test Archive");
        archive.setFondsNo(TEST_FONDS_NO);
        archive.setFiscalYear("2023");
        archive.setOrgName("Test Org");
        archive.setRetentionPeriod("10Y");
        archive.setCategoryCode("AC01");
        return archive;
    }

    /**
     * 创建已存在的档案实体
     */
    private Archive createExistingArchive() {
        Archive archive = new Archive();
        archive.setId(TEST_ARCHIVE_ID);
        archive.setArchiveCode(TEST_ARCHIVE_CODE);
        archive.setTitle("Existing Archive");
        archive.setFondsNo(TEST_FONDS_NO);
        archive.setFiscalYear("2023");
        archive.setOrgName("Test Org");
        archive.setRetentionPeriod("10Y");
        archive.setStatus("archived");
        archive.setCreatedBy(TEST_USER_ID);
        archive.setCreatedTime(LocalDateTime.now());
        archive.setLastModifiedTime(LocalDateTime.now());
        return archive;
    }

    /**
     * 创建文件内容实体
     */
    private ArcFileContent createFileContent(String id, String itemId) {
        ArcFileContent file = new ArcFileContent();
        file.setId(id);
        file.setItemId(itemId);
        file.setOriginalHash("hash-" + id);
        file.setCurrentHash("hash-" + id);
        file.setFileName("file-" + id + ".pdf");
        file.setCreatedTime(LocalDateTime.now());
        return file;
    }

    // ========== CREATE ARCHIVE TESTS ==========

    @Test
    @DisplayName("创建档案 - 成功流程 (RED → GREEN)")
    void createArchive_Success() {
        // RED: 测试尚未实现时的预期行为
        // Arrange
        when(codeGenerator.generateNextCode(any())).thenReturn(TEST_ARCHIVE_CODE);
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // Act
        Archive result = archiveService.createArchive(sampleArchive, TEST_USER_ID);

        // Assert - GREEN: 实现后应该通过
        assertNotNull(result.getId(), "档案ID应该自动生成");
        assertEquals("draft", result.getStatus(), "默认状态应该是草稿");
        assertEquals(TEST_ARCHIVE_CODE, result.getArchiveCode(), "档号应该自动生成");
        assertEquals(TEST_USER_ID, result.getCreatedBy(), "创建人应该被设置");
        assertNotNull(result.getCreatedTime(), "创建时间应该被设置");
        assertNotNull(result.getLastModifiedTime(), "修改时间应该被设置");

        verify(archiveMapper).insert(any(Archive.class));
        verify(codeGenerator).generateNextCode(any());
    }

    @Test
    @DisplayName("创建档案 - 健壮性: 处理竞态条件 (DuplicateKeyException)")
    void createArchive_RaceCondition() {
        // Arrange - 模拟竞态条件
        sampleArchive.setArchiveCode("EXISTING-CODE");
        when(archiveMapper.selectCount(any())).thenReturn(0L); // 检查时唯一
        doThrow(new DuplicateKeyException("Duplicate entry 'EXISTING-CODE'"))
                .when(archiveMapper).insert(any(Archive.class)); // 插入时冲突

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.createArchive(sampleArchive, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("档号或唯一标识已存在"));
        assertEquals(409, exception.getCode());
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    @DisplayName("创建档案 - 新字段验证 (destructionStatus, retentionStartDate)")
    void createArchive_WithNewFields() {
        // Arrange
        sampleArchive.setDestructionStatus("NORMAL");
        sampleArchive.setRetentionStartDate(LocalDate.now());

        when(codeGenerator.generateNextCode(any())).thenReturn("A-2023-002");
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(archiveMapper.insert(any(Archive.class))).thenAnswer(invocation -> {
            Archive arg = invocation.getArgument(0);
            assertEquals("NORMAL", arg.getDestructionStatus());
            assertNotNull(arg.getRetentionStartDate());
            arg.setId("generated-id");
            return 1;
        });

        // Act
        Archive result = archiveService.createArchive(sampleArchive, TEST_USER_ID);

        // Assert
        assertEquals("NORMAL", result.getDestructionStatus());
        assertNotNull(result.getRetentionStartDate());
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    @DisplayName("创建档案 - 边界: 档号已存在时抛出异常")
    void createArchive_ArchiveCodeAlreadyExists() {
        // Arrange
        sampleArchive.setArchiveCode("EXISTING-CODE");
        when(archiveMapper.selectCount(any())).thenReturn(1L); // 档号已存在

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.createArchive(sampleArchive, TEST_USER_ID);
        });

        assertEquals(ErrorCode.ARCHIVE_CODE_EXISTS.getCode(), exception.getCode());
        verify(archiveMapper, never()).insert(any(Archive.class));
    }

    @Test
    @DisplayName("创建档案 - 边界: 唯一业务ID已存在时抛出异常")
    void createArchive_UniqueBizIdAlreadyExists() {
        // Arrange
        sampleArchive.setUniqueBizId("UNIQUE-BIZ-001");
        when(archiveMapper.selectCount(any())).thenReturn(0L).thenReturn(1L); // 第二次调用返回1

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.createArchive(sampleArchive, TEST_USER_ID);
        });

        assertTrue(exception.getMessage().contains("唯一业务ID已存在"));
        verify(archiveMapper, never()).insert(any(Archive.class));
    }

    @Test
    @DisplayName("创建档案 - 边界: null 用户ID")
    void createArchive_NullUserId() {
        // Arrange
        when(codeGenerator.generateNextCode(any())).thenReturn(TEST_ARCHIVE_CODE);
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // Act
        Archive result = archiveService.createArchive(sampleArchive, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getCreatedBy());
    }

    // ========== UPDATE ARCHIVE TESTS ==========

    @Test
    @DisplayName("更新档案 - 成功流程")
    void updateArchive_Success() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        Archive update = new Archive();
        update.setTitle("Updated Title");

        // Act
        archiveService.updateArchive(archiveId, update);

        // Assert
        verify(archiveMapper).updateById(any(Archive.class));
        ArgumentCaptor<Archive> captor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveMapper).updateById(captor.capture());
        assertEquals("Updated Title", captor.getValue().getTitle());
        assertEquals(archiveId, captor.getValue().getId());
    }

    @Test
    @DisplayName("更新档案 - 完整性: 防止档号重复")
    void updateArchive_DuplicateCodeCheck() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        Archive update = new Archive();
        update.setArchiveCode("NEW-CODE"); // 尝试更改为已存在的档号

        when(archiveMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L); // 档号已存在

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.updateArchive(archiveId, update);
        });

        assertTrue(exception.getMessage().contains("档号已存在"));
        verify(archiveMapper, never()).updateById(any(Archive.class));
    }

    @Test
    @DisplayName("更新档案 - 边界: 档案不存在")
    void updateArchive_ArchiveNotFound() {
        // Arrange
        String nonExistentId = "non-existent-id";
        when(archiveMapper.selectById(nonExistentId)).thenReturn(null);

        Archive update = new Archive();

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            archiveService.updateArchive(nonExistentId, update);
        });

        verify(archiveMapper, never()).updateById(any(Archive.class));
    }

    @Test
    @DisplayName("更新档案 - 权限: 用户无权限访问")
    void updateArchive_AccessDenied() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(false);

        Archive update = new Archive();

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.updateArchive(archiveId, update);
        });

        assertEquals(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE.getCode(), exception.getCode());
        verify(archiveMapper, never()).updateById(any(Archive.class));
    }

    @Test
    @DisplayName("更新档案 - 唯一业务ID冲突")
    void updateArchive_UniqueBizIdConflict() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        Archive update = new Archive();
        update.setUniqueBizId("NEW-UNIQUE-BIZ-ID");

        when(archiveMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.updateArchive(archiveId, update);
        });

        assertTrue(exception.getMessage().contains("唯一业务ID已存在"));
        verify(archiveMapper, never()).updateById(any(Archive.class));
    }

    // ========== DELETE ARCHIVE TESTS ==========

    @Test
    @DisplayName("删除档案 - 成功流程")
    void deleteArchive_Success() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);
        when(archiveMapper.deleteById(archiveId)).thenReturn(1);

        // Act
        archiveService.deleteArchive(archiveId);

        // Assert
        verify(archiveMapper).deleteById(archiveId);
    }

    @Test
    @DisplayName("删除档案 - 边界: 档案不存在")
    void deleteArchive_ArchiveNotFound() {
        // Arrange
        String nonExistentId = "non-existent-id";
        when(archiveMapper.selectById(nonExistentId)).thenReturn(null);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            archiveService.deleteArchive(nonExistentId);
        });

        verify(archiveMapper, never()).deleteById(any(String.class));
    }

    @Test
    @DisplayName("删除档案 - 权限: 用户无权限访问")
    void deleteArchive_AccessDenied() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.deleteArchive(archiveId);
        });

        assertEquals(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE.getCode(), exception.getCode());
        verify(archiveMapper, never()).deleteById(any(String.class));
    }

    // ========== GET ARCHIVE BY ID TESTS ==========

    @Test
    @DisplayName("根据ID获取档案 - 成功流程")
    void getArchiveById_Success() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        // Act
        Archive result = archiveService.getArchiveById(archiveId);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ARCHIVE_ID, result.getId());
        assertEquals(TEST_ARCHIVE_CODE, result.getArchiveCode());
    }

    @Test
    @DisplayName("根据ID获取档案 - 回退到档号查询")
    void getArchiveById_FallbackToArchiveCode() {
        // Arrange
        String archiveCode = TEST_ARCHIVE_CODE;
        when(archiveMapper.selectById(archiveCode)).thenReturn(null); // ID查询失败
        when(archiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        // Act
        Archive result = archiveService.getArchiveById(archiveCode);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ARCHIVE_CODE, result.getArchiveCode());
    }

    @Test
    @DisplayName("根据ID获取档案 - 边界: 档案不存在")
    void getArchiveById_ArchiveNotFound() {
        // Arrange
        String nonExistentId = "non-existent-id";
        when(archiveMapper.selectById(nonExistentId)).thenReturn(null);
        when(archiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.getArchiveById(nonExistentId);
        });

        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("档案不存在"));
    }

    @Test
    @DisplayName("根据ID获取档案 - 权限: 用户无权限访问")
    void getArchiveById_AccessDenied() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.getArchiveById(archiveId);
        });

        assertEquals(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE.getCode(), exception.getCode());
    }

    // ========== GET ARCHIVES (PAGINATED) TESTS ==========

    @Test
    @DisplayName("分页查询档案 - 会计档案默认过滤")
    void getArchives_AccountingCategory_DefaultFilter() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        archiveService.getArchives(1, 10, null, null, "AC01", null, null, null, TEST_FONDS_NO);

        // Assert
        ArgumentCaptor<LambdaQueryWrapper<Archive>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), captor.capture());

        String sqlSegment = captor.getValue().getSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.toLowerCase().contains("lower(status)"),
                "会计档案默认筛选应使用大小写无关状态过滤");
    }

    @Test
    @DisplayName("分页查询档案 - 搜索关键词")
    void getArchives_WithSearchKeyword() {
        // Arrange
        Page<Archive> resultPage = new Page<>(1, 10);
        resultPage.setRecords(Arrays.asList(existingArchive));
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(resultPage);

        // Act
        Page<Archive> result = archiveService.getArchives(1, 10, "Test", null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("分页查询档案 - 状态过滤 (大小写无关)")
    void getArchives_WithStatusFilter() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        archiveService.getArchives(1, 10, null, "ArChIvEd", null, null, null, null, null);

        // Assert
        ArgumentCaptor<LambdaQueryWrapper<Archive>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), captor.capture());

        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.toLowerCase().contains("lower(status)"));
    }

    @Test
    @DisplayName("分页查询档案 - 多状态过滤")
    void getArchives_WithMultipleStatusFilter() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        archiveService.getArchives(1, 10, null, "draft,archived", null, null, null, null, null);

        // Assert
        verify(archiveMapper).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("分页查询档案 - 显式全宗过滤")
    void getArchives_WithExplicitFondsNo() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        archiveService.getArchives(1, 10, null, null, null, null, null, null, TEST_FONDS_NO);

        // Assert
        ArgumentCaptor<LambdaQueryWrapper<Archive>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(archiveMapper).selectPage(any(Page.class), captor.capture());
        // 验证显式全宗过滤被应用
        assertNotNull(captor.getValue());
    }

    @Test
    @DisplayName("分页查询档案 - 数据权限自动应用")
    void getArchives_DataScopeApplied() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // 设置受限的数据权限
        DataScopeContext limitedScope = new DataScopeContext(
                com.nexusarchive.common.enums.DataScopeType.SELF,
                TEST_USER_ID,
                Set.of(TEST_FONDS_NO)
        );
        when(dataScopeService.resolve()).thenReturn(limitedScope);

        // Act
        archiveService.getArchives(1, 10, null, null, null, null, null, null, null);

        // Assert
        verify(dataScopeService).resolve();
        verify(dataScopeService).applyArchiveScope(any(LambdaQueryWrapper.class), eq(limitedScope));
    }

    // ========== GET BY UNIQUE BIZ ID TESTS ==========

    @Test
    @DisplayName("根据唯一业务ID获取档案 - 成功")
    void getByUniqueBizId_Success() {
        // Arrange
        String uniqueBizId = "UNIQUE-BIZ-001";
        when(archiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingArchive);

        // Act
        Archive result = archiveService.getByUniqueBizId(uniqueBizId);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("根据唯一业务ID获取档案 - 未找到")
    void getByUniqueBizId_NotFound() {
        // Arrange
        String uniqueBizId = "NON-EXISTENT";
        when(archiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act
        Archive result = archiveService.getByUniqueBizId(uniqueBizId);

        // Assert
        assertNull(result);
    }

    // ========== GET RECENT ARCHIVES TESTS ==========

    @Test
    @DisplayName("获取最近创建的档案 - 成功")
    void getRecentArchives_Success() {
        // Arrange
        int limit = 10;
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(existingArchive));

        // Act
        List<Archive> result = archiveService.getRecentArchives(limit);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(archiveMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取最近创建的档案 - 应用数据权限")
    void getRecentArchives_DataScopeApplied() {
        // Arrange
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act
        archiveService.getRecentArchives(5);

        // Assert
        verify(dataScopeService).resolve();
        verify(dataScopeService).applyArchiveScope(any(LambdaQueryWrapper.class), any());
    }

    // ========== GET ARCHIVES BY IDS TESTS ==========

    @Test
    @DisplayName("根据ID列表获取档案 - 成功")
    void getArchivesByIds_Success() {
        // Arrange
        Set<String> ids = Set.of(TEST_ARCHIVE_ID, "archive-002");
        when(archiveMapper.selectBatchIds(ids))
                .thenReturn(Arrays.asList(existingArchive));

        // Act
        List<Archive> result = archiveService.getArchivesByIds(ids);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectBatchIds(ids);
    }

    @Test
    @DisplayName("根据ID列表获取档案 - 边界: 空集合")
    void getArchivesByIds_EmptySet() {
        // Arrange
        Set<String> emptyIds = Collections.emptySet();

        // Act
        List<Archive> result = archiveService.getArchivesByIds(emptyIds);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(archiveMapper, never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("根据ID列表获取档案 - 边界: null")
    void getArchivesByIds_NullSet() {
        // Act
        List<Archive> result = archiveService.getArchivesByIds(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(archiveMapper, never()).selectBatchIds(any());
    }

    @Test
    @DisplayName("根据ID列表获取档案 - 应用数据权限过滤")
    void getArchivesByIds_WithDataScopeFilter() {
        // Arrange
        Set<String> ids = Set.of(TEST_ARCHIVE_ID);
        DataScopeContext limitedScope = new DataScopeContext(
                com.nexusarchive.common.enums.DataScopeType.SELF,
                TEST_USER_ID,
                Set.of(TEST_FONDS_NO)
        );
        when(dataScopeService.resolve()).thenReturn(limitedScope);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(existingArchive));

        // Act
        List<Archive> result = archiveService.getArchivesByIds(ids);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectList(any(LambdaQueryWrapper.class));
        verify(archiveMapper, never()).selectBatchIds(any());
    }

    // ========== GET FILES BY ARCHIVE ID TESTS ==========

    @Test
    @DisplayName("获取档案关联文件 - 成功")
    void getFilesByArchiveId_Success() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        List<ArcFileContent> directFiles = Arrays.asList(
                createFileContent("file-1", archiveId),
                createFileContent("file-2", archiveId)
        );
        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(directFiles);
        when(arcFileContentMapper.selectAttachmentsByArchiveId(archiveId)).thenReturn(Collections.emptyList());

        // Act
        List<ArcFileContent> result = archiveService.getFilesByArchiveId(archiveId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("获取档案关联文件 - 包含附件")
    void getFilesByArchiveId_WithAttachments() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        List<ArcFileContent> directFiles = Arrays.asList(createFileContent("file-1", archiveId));
        List<ArcFileContent> attachments = Arrays.asList(createFileContent("attach-1", archiveId));

        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(directFiles);
        when(arcFileContentMapper.selectAttachmentsByArchiveId(archiveId)).thenReturn(attachments);
        when(voucherRelationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act
        List<ArcFileContent> result = archiveService.getFilesByArchiveId(archiveId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // direct + attachment
    }

    @Test
    @DisplayName("获取档案关联文件 - 包含关联凭证文件")
    void getFilesByArchiveId_WithVoucherRelations() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        String originalVoucherId = "original-voucher-001";

        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(true);

        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(arcFileContentMapper.selectAttachmentsByArchiveId(archiveId)).thenReturn(Collections.emptyList());

        VoucherRelation relation = new VoucherRelation();
        relation.setAccountingVoucherId(archiveId);
        relation.setOriginalVoucherId(originalVoucherId);
        relation.setDeleted(0);
        when(voucherRelationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(relation));

        List<ArcFileContent> voucherFiles = Arrays.asList(createFileContent("voucher-file-1", originalVoucherId));
        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(voucherFiles);

        // Act
        List<ArcFileContent> result = archiveService.getFilesByArchiveId(archiveId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取档案关联文件 - 权限拒绝")
    void getFilesByArchiveId_AccessDenied() {
        // Arrange
        String archiveId = TEST_ARCHIVE_ID;
        when(archiveMapper.selectById(archiveId)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), any())).thenReturn(false);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.getFilesByArchiveId(archiveId);
        });

        assertEquals(ErrorCode.NO_PERMISSION_TO_VIEW_ARCHIVE.getCode(), exception.getCode());
    }

    // ========== GET EXPIRED ARCHIVES TESTS ==========

    @Test
    @DisplayName("获取过期档案 - 成功")
    void getExpiredArchives_Success() {
        // Arrange
        Page<Archive> resultPage = new Page<>(1, 10);
        when(archiveMapper.selectExpired(any(Page.class), eq(TEST_FONDS_NO))).thenReturn(resultPage);

        // Act
        IPage<Archive> result = archiveService.getExpiredArchives(1, 10, TEST_FONDS_NO);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectExpired(any(Page.class), eq(TEST_FONDS_NO));
    }

    // ========== EDGE CASES AND ERROR HANDLING ==========

    @Test
    @DisplayName("边界: 空字符串搜索")
    void getArchives_EmptySearchString() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        Page<Archive> result = archiveService.getArchives(1, 10, "", null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("边界: 空状态字符串")
    void getArchives_EmptyStatusString() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        Page<Archive> result = archiveService.getArchives(1, 10, null, "", null, null, null, null, null);

        // Assert
        assertNotNull(result);
    }

    @Test
    @DisplayName("边界: 特殊字符搜索")
    void getArchives_SpecialCharactersInSearch() {
        // Arrange
        Page<Archive> emptyPage = new Page<>(1, 10);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(emptyPage);

        // Act
        Page<Archive> result = archiveService.getArchives(1, 10, "Test; DROP TABLE--", null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectPage(any(Page.class), any());
    }

    @Test
    @DisplayName("性能: 大量档案查询")
    void getArchives_LargeDataset() {
        // Arrange
        Page<Archive> largePage = new Page<>(1, 100);
        when(archiveMapper.selectPage(any(Page.class), any())).thenReturn(largePage);

        // Act
        Page<Archive> result = archiveService.getArchives(1, 100, null, null, null, null, null, null, null);

        // Assert
        assertNotNull(result);
        verify(archiveMapper).selectPage(any(Page.class), any());
    }

    // ========== DATA SCOPE INTEGRATION TESTS ==========

    @Test
    @DisplayName("数据权限: 全部权限用户")
    void dataScope_AllAccess() {
        // Arrange
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());
        when(archiveMapper.selectById(TEST_ARCHIVE_ID)).thenReturn(existingArchive);

        // Act
        Archive result = archiveService.getArchiveById(TEST_ARCHIVE_ID);

        // Assert
        assertNotNull(result);
        verify(dataScopeService).canAccessArchive(eq(existingArchive), any());
    }

    @Test
    @DisplayName("数据权限: 受限权限用户")
    void dataScope_LimitedAccess() {
        // Arrange
        DataScopeContext limitedScope = new DataScopeContext(
                com.nexusarchive.common.enums.DataScopeType.SELF,
                TEST_USER_ID,
                Set.of(TEST_FONDS_NO)
        );
        when(dataScopeService.resolve()).thenReturn(limitedScope);
        when(archiveMapper.selectById(TEST_ARCHIVE_ID)).thenReturn(existingArchive);
        when(dataScopeService.canAccessArchive(eq(existingArchive), eq(limitedScope))).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            archiveService.getArchiveById(TEST_ARCHIVE_ID);
        });
    }

    // ========== CONCURRENT OPERATIONS TESTS ==========

    @Test
    @DisplayName("并发: 同时创建相同档号的档案")
    void createArchive_ConcurrentSameCode() {
        // Arrange - 模拟并发场景
        sampleArchive.setArchiveCode("CONCURRENT-CODE");
        when(archiveMapper.selectCount(any())).thenReturn(0L); // 第一次检查通过
        when(archiveMapper.insert(any(Archive.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry")); // 插入时冲突

        // Act & Assert
        assertThrows(BusinessException.class, () -> {
            archiveService.createArchive(sampleArchive, TEST_USER_ID);
        });
    }
}
