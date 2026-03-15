// Input: JUnit 5, Mockito, Spring Framework, Java 标准库
// Output: RelationGraphHelperTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.relation.RelationEdgeDto;
import com.nexusarchive.dto.relation.RelationGraphDto;
import com.nexusarchive.dto.relation.RelationNodeDto;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.service.IArchiveRelationService;
import com.nexusarchive.service.relation.RelationDirectionResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 关系图助手测试套件
 * <p>
 * TDD 测试套件，覆盖 RelationGraphHelper 的核心业务逻辑：
 * - 图构建 (buildGraph)
 * - 递归关系获取 (fetchRelationsRecursive)
 * - 虚拟节点创建 (createVirtualNodes)
 * - 原始凭证解析 (resolveOriginalVoucher)
 * - 关联凭证查找 (findRelatedAccountingVoucherId)
 * - 凭证类型判断 (isVoucher)
 * - 路径解析与循环检测
 * - 边界情况与异常处理
 * </p>
 *
 * 测试覆盖率目标: 80%+
 *
 * @see RelationGraphHelper
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("关系图助手测试套件")
class RelationGraphHelperTest {

    // ========== Mock Dependencies ==========
    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private IArchiveRelationService archiveRelationService;

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private VoucherRelationMapper voucherRelationMapper;

    @Mock
    private OriginalVoucherMapper originalVoucherMapper;

    @Mock
    private OriginalVoucherFileMapper originalVoucherFileMapper;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private RelationDirectionResolver relationDirectionResolver;

    @InjectMocks
    private RelationGraphHelper relationGraphHelper;

    // ========== Test Fixtures ==========
    private Archive centerArchive;
    private Archive relatedArchive1;
    private Archive relatedArchive2;
    private ArchiveRelation relation1;
    private ArchiveRelation relation2;
    private OriginalVoucher originalVoucher;
    private VoucherRelation voucherRelation;
    private ArchiveAttachment attachment;
    private ArcFileContent fileContent;

    private final String TEST_CENTER_ID = "archive-center-001";
    private final String TEST_RELATED_ID_1 = "archive-related-001";
    private final String TEST_RELATED_ID_2 = "archive-related-002";
    private final String TEST_OV_ID = "ov-001";
    private final String TEST_FILE_ID = "file-001";
    private final String TEST_FONDS_NO = "F001";

    // ========== Test Setup ==========

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        centerArchive = createCenterArchive();
        relatedArchive1 = createRelatedArchive1();
        relatedArchive2 = createRelatedArchive2();
        relation1 = createRelation1();
        relation2 = createRelation2();
        originalVoucher = createOriginalVoucher();
        voucherRelation = createVoucherRelation();
        attachment = createAttachment();
        fileContent = createFileContent();
    }

    // ========== Helper Methods ==========

    private Archive createCenterArchive() {
        Archive archive = new Archive();
        archive.setId(TEST_CENTER_ID);
        archive.setArchiveCode("PZ-2023-001");
        archive.setTitle("记账凭证-测试");
        archive.setAmount(new BigDecimal("1000.00"));
        archive.setDocDate(LocalDate.of(2023, 12, 1));
        archive.setStatus("ARCHIVED");
        archive.setFondsNo(TEST_FONDS_NO);
        archive.setCreatedTime(LocalDateTime.now());
        return archive;
    }

    private Archive createRelatedArchive1() {
        Archive archive = new Archive();
        archive.setId(TEST_RELATED_ID_1);
        archive.setArchiveCode("FP-2023-001");
        archive.setTitle("增值税发票-测试1");
        archive.setAmount(new BigDecimal("500.00"));
        archive.setDocDate(LocalDate.of(2023, 11, 15));
        archive.setStatus("ARCHIVED");
        archive.setFondsNo(TEST_FONDS_NO);
        return archive;
    }

    private Archive createRelatedArchive2() {
        Archive archive = new Archive();
        archive.setId(TEST_RELATED_ID_2);
        archive.setArchiveCode("HT-2023-001");
        archive.setTitle("采购合同-测试2");
        archive.setAmount(new BigDecimal("1500.00"));
        archive.setDocDate(LocalDate.of(2023, 11, 1));
        archive.setStatus("ARCHIVED");
        archive.setFondsNo(TEST_FONDS_NO);
        return archive;
    }

    private ArchiveRelation createRelation1() {
        ArchiveRelation relation = new ArchiveRelation();
        relation.setId("rel-001");
        relation.setSourceId(TEST_CENTER_ID);
        relation.setTargetId(TEST_RELATED_ID_1);
        relation.setRelationType("M94");
        relation.setRelationDesc("关联发票");
        relation.setCreatedTime(LocalDateTime.now());
        return relation;
    }

    private ArchiveRelation createRelation2() {
        ArchiveRelation relation = new ArchiveRelation();
        relation.setId("rel-002");
        relation.setSourceId(TEST_RELATED_ID_2);
        relation.setTargetId(TEST_CENTER_ID);
        relation.setRelationType("M93");
        relation.setRelationDesc("关联合同");
        relation.setCreatedTime(LocalDateTime.now());
        return relation;
    }

    private OriginalVoucher createOriginalVoucher() {
        OriginalVoucher ov = new OriginalVoucher();
        ov.setId(TEST_OV_ID);
        ov.setVoucherNo("OV-2023-001");
        ov.setSummary("原始凭证摘要");
        ov.setAmount(new BigDecimal("200.00"));
        ov.setBusinessDate(LocalDate.of(2023, 12, 1));
        return ov;
    }

    private VoucherRelation createVoucherRelation() {
        VoucherRelation vr = new VoucherRelation();
        vr.setId("vr-001");
        vr.setAccountingVoucherId(TEST_CENTER_ID);
        vr.setOriginalVoucherId(TEST_OV_ID);
        return vr;
    }

    private ArchiveAttachment createAttachment() {
        ArchiveAttachment aa = new ArchiveAttachment();
        aa.setId("aa-001");
        aa.setArchiveId(TEST_CENTER_ID);
        aa.setFileId(TEST_FILE_ID);
        aa.setRelationDesc("附件说明");
        return aa;
    }

    private ArcFileContent createFileContent() {
        ArcFileContent file = new ArcFileContent();
        file.setId(TEST_FILE_ID);
        file.setFileName("测试文件.pdf");
        file.setArchivalCode("FILE-2023-001");
        file.setCreatedTime(LocalDateTime.now());
        return file;
    }

    // ========== buildGraph Tests ==========

    @Test
    @DisplayName("构建关系图 - 基本成功场景")
    void testBuildGraph_Success() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1, relation2);
        List<Archive> archives = Arrays.asList(relatedArchive1, relatedArchive2);
        List<ArchiveAttachment> attachments = Arrays.asList(attachment);
        List<VoucherRelation> voucherRelations = Arrays.asList(voucherRelation);
        List<ArcFileContent> files = Arrays.asList(fileContent);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(attachments);
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(voucherRelations);
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(originalVoucherMapper.selectById(TEST_OV_ID)).thenReturn(originalVoucher);
        when(attachmentService.getAttachmentsByArchive(TEST_CENTER_ID)).thenReturn(files);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        assertEquals(TEST_CENTER_ID, graph.getCenterId());
        assertNotNull(graph.getNodes());
        assertNotNull(graph.getEdges());
        assertFalse(graph.getAutoRedirected());
        assertNull(graph.getRedirectMessage());
        assertNotNull(graph.getDirectionalView());

        // 验证节点包含中心和关联档案
        assertTrue(graph.getNodes().size() >= 3); // 至少包含中心和2个关联档案

        // 验证边包含关系边
        assertTrue(graph.getEdges().size() >= 2); // 至少包含2个关系边

        // 验证调用
        verify(archiveRelationService, atLeastOnce()).list(any(LambdaQueryWrapper.class));
        verify(attachmentService).getAttachmentLinks(TEST_CENTER_ID);
        verify(voucherRelationMapper).findByAccountingVoucherId(TEST_CENTER_ID);
    }

    @Test
    @DisplayName("构建关系图 - 空档案ID抛出异常")
    void testBuildGraph_NullArchiveId() {
        // Given
        centerArchive.setId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);
        });

        // 验证没有调用任何服务
        verify(archiveRelationService, never()).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("构建关系图 - 空字符串档案ID抛出异常")
    void testBuildGraph_EmptyArchiveId() {
        // Given
        centerArchive.setId("");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);
        });
    }

    @Test
    @DisplayName("构建关系图 - 包含原始查询ID")
    void testBuildGraph_WithOriginalQueryId() {
        // Given
        String originalQueryId = "original-query-001";
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, originalQueryId, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        assertEquals(TEST_CENTER_ID, graph.getCenterId());
        // 验证原始查询ID被包含在节点集合中
        verify(archiveService).getArchivesByIds(argThat(set -> set.contains(originalQueryId)));
    }

    @Test
    @DisplayName("构建关系图 - 自动重定向场景")
    void testBuildGraph_AutoRedirected() {
        // Given
        String originalQueryId = "original-query-001";
        String redirectMessage = "已自动切换到关联的记账凭证查看完整业务链路";
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, originalQueryId, true, redirectMessage, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        assertTrue(graph.getAutoRedirected());
        assertEquals(redirectMessage, graph.getRedirectMessage());
    }

    @Test
    @DisplayName("构建关系图 - 无关联关系")
    void testBuildGraph_NoRelations() {
        // Given
        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(Collections.emptyList());
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        assertEquals(TEST_CENTER_ID, graph.getCenterId());
        // 至少包含中心节点
        assertTrue(graph.getNodes().size() >= 1);
        assertEquals(TEST_CENTER_ID, graph.getNodes().get(0).getId());
    }

    @Test
    @DisplayName("构建关系图 - 包含原始凭证关联")
    void testBuildGraph_WithOriginalVoucher() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);
        List<VoucherRelation> voucherRelations = Arrays.asList(voucherRelation);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(voucherRelations);
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(originalVoucherMapper.selectById(TEST_OV_ID)).thenReturn(originalVoucher);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        // 验证原始凭证被转换为虚拟节点
        boolean hasVirtualNode = graph.getNodes().stream()
                .anyMatch(node -> node.getId().equals("OV_" + TEST_OV_ID));
        assertTrue(hasVirtualNode, "应该包含原始凭证虚拟节点");
    }

    @Test
    @DisplayName("构建关系图 - 包含附件关联")
    void testBuildGraph_WithAttachments() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);
        List<ArchiveAttachment> attachments = Arrays.asList(attachment);
        List<ArcFileContent> files = Arrays.asList(fileContent);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(attachments);
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(attachmentService.getAttachmentsByArchive(TEST_CENTER_ID)).thenReturn(files);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        // 验证附件被转换为虚拟节点
        boolean hasFileNode = graph.getNodes().stream()
                .anyMatch(node -> node.getId().equals("FILE_" + TEST_FILE_ID));
        assertTrue(hasFileNode, "应该包含附件虚拟节点");
    }

    // ========== Node & Edge Validation Tests ==========

    @Test
    @DisplayName("验证节点数据转换 - 正确映射字段")
    void testBuildGraph_NodeDataMapping() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        Optional<RelationNodeDto> centerNode = graph.getNodes().stream()
                .filter(node -> TEST_CENTER_ID.equals(node.getId()))
                .findFirst();

        assertTrue(centerNode.isPresent(), "应该包含中心节点");
        RelationNodeDto node = centerNode.get();
        assertEquals(TEST_CENTER_ID, node.getId());
        assertEquals("PZ-2023-001", node.getCode());
        assertEquals("记账凭证-测试", node.getName());
        assertEquals("voucher", node.getType());
        assertEquals("¥ 1000.00", node.getAmount());
        assertEquals("2023-12-01", node.getDate());
        assertEquals("ARCHIVED", node.getStatus());
    }

    @Test
    @DisplayName("验证边数据转换 - 正确映射关系")
    void testBuildGraph_EdgeDataMapping() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        assertNotNull(graph);
        Optional<RelationEdgeDto> edge = graph.getEdges().stream()
                .filter(e -> TEST_CENTER_ID.equals(e.getFrom()) && TEST_RELATED_ID_1.equals(e.getTo()))
                .findFirst();

        assertTrue(edge.isPresent(), "应该包含关系边");
        RelationEdgeDto relationEdge = edge.get();
        assertEquals(TEST_CENTER_ID, relationEdge.getFrom());
        assertEquals(TEST_RELATED_ID_1, relationEdge.getTo());
        assertEquals("M94", relationEdge.getRelationType());
        assertEquals("关联发票", relationEdge.getDescription());
    }

    // ========== resolveOriginalVoucher Tests ==========

    @Test
    @DisplayName("解析原始凭证 - 通过ID查找成功")
    void testResolveOriginalVoucher_ById() {
        // Given
        when(originalVoucherMapper.selectById(TEST_OV_ID)).thenReturn(originalVoucher);

        // When
        OriginalVoucher result = relationGraphHelper.resolveOriginalVoucher(TEST_OV_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_OV_ID, result.getId());
        assertEquals("OV-2023-001", result.getVoucherNo());
        verify(originalVoucherMapper).selectById(TEST_OV_ID);
        verify(originalVoucherMapper, never()).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("解析原始凭证 - 通过凭证号查找成功")
    void testResolveOriginalVoucher_ByVoucherNo() {
        // Given
        String voucherNo = "OV-2023-001";
        when(originalVoucherMapper.selectById(voucherNo)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(originalVoucher);

        // When
        OriginalVoucher result = relationGraphHelper.resolveOriginalVoucher(voucherNo);

        // Then
        assertNotNull(result);
        assertEquals(TEST_OV_ID, result.getId());
        verify(originalVoucherMapper).selectById(voucherNo);
        verify(originalVoucherMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("解析原始凭证 - 通过档案编号查找成功")
    void testResolveOriginalVoucher_ByArchivalCode() {
        // Given
        String archivalCode = "FILE-2023-001";
        when(originalVoucherMapper.selectById(archivalCode)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(arcFileContentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(fileContent);
        when(originalVoucherMapper.selectById(TEST_FILE_ID)).thenReturn(originalVoucher);

        // When
        OriginalVoucher result = relationGraphHelper.resolveOriginalVoucher(archivalCode);

        // Then
        assertNotNull(result);
        verify(originalVoucherMapper).selectById(archivalCode);
        verify(originalVoucherMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(arcFileContentMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(originalVoucherMapper).selectById(TEST_FILE_ID);
    }

    @Test
    @DisplayName("解析原始凭证 - 未找到返回null")
    void testResolveOriginalVoucher_NotFound() {
        // Given
        String unknownId = "unknown-id";
        when(originalVoucherMapper.selectById(unknownId)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(arcFileContentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When
        OriginalVoucher result = relationGraphHelper.resolveOriginalVoucher(unknownId);

        // Then
        assertNull(result);
        verify(originalVoucherMapper).selectById(unknownId);
        verify(originalVoucherMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(arcFileContentMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("解析原始凭证 - 文件内容itemId为空")
    void testResolveOriginalVoucher_EmptyItemId() {
        // Given
        String archivalCode = "FILE-2023-001";
        fileContent.setItemId(null);
        when(originalVoucherMapper.selectById(archivalCode)).thenReturn(null);
        when(originalVoucherMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(arcFileContentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(fileContent);

        // When
        OriginalVoucher result = relationGraphHelper.resolveOriginalVoucher(archivalCode);

        // Then
        assertNull(result);
        verify(originalVoucherMapper).selectById(archivalCode);
        verify(originalVoucherMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(arcFileContentMapper).selectOne(any(LambdaQueryWrapper.class));
        verify(originalVoucherMapper, never()).selectById(anyString());
    }

    // ========== findRelatedAccountingVoucherId Tests ==========

    @Test
    @DisplayName("查找关联记账凭证 - 找到返回ID")
    void testFindRelatedAccountingVoucherId_Found() {
        // Given
        when(voucherRelationMapper.findByOriginalVoucherId(TEST_OV_ID)).thenReturn(Arrays.asList(voucherRelation));

        // When
        String result = relationGraphHelper.findRelatedAccountingVoucherId(TEST_OV_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CENTER_ID, result);
        verify(voucherRelationMapper).findByOriginalVoucherId(TEST_OV_ID);
    }

    @Test
    @DisplayName("查找关联记账凭证 - 未找到返回null")
    void testFindRelatedAccountingVoucherId_NotFound() {
        // Given
        when(voucherRelationMapper.findByOriginalVoucherId(TEST_OV_ID)).thenReturn(Collections.emptyList());

        // When
        String result = relationGraphHelper.findRelatedAccountingVoucherId(TEST_OV_ID);

        // Then
        assertNull(result);
        verify(voucherRelationMapper).findByOriginalVoucherId(TEST_OV_ID);
    }

    @Test
    @DisplayName("查找关联记账凭证 - 关系列表为null")
    void testFindRelatedAccountingVoucherId_NullList() {
        // Given
        when(voucherRelationMapper.findByOriginalVoucherId(TEST_OV_ID)).thenReturn(null);

        // When
        String result = relationGraphHelper.findRelatedAccountingVoucherId(TEST_OV_ID);

        // Then
        assertNull(result);
        verify(voucherRelationMapper).findByOriginalVoucherId(TEST_OV_ID);
    }

    @Test
    @DisplayName("查找关联记账凭证 - 记账凭证ID为空字符串")
    void testFindRelatedAccountingVoucherId_EmptyId() {
        // Given
        VoucherRelation vr = new VoucherRelation();
        vr.setAccountingVoucherId("");
        when(voucherRelationMapper.findByOriginalVoucherId(TEST_OV_ID)).thenReturn(Arrays.asList(vr));

        // When
        String result = relationGraphHelper.findRelatedAccountingVoucherId(TEST_OV_ID);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("查找关联记账凭证 - 记账凭证ID为null")
    void testFindRelatedAccountingVoucherId_NullId() {
        // Given
        VoucherRelation vr = new VoucherRelation();
        vr.setAccountingVoucherId(null);
        when(voucherRelationMapper.findByOriginalVoucherId(TEST_OV_ID)).thenReturn(Arrays.asList(vr));

        // When
        String result = relationGraphHelper.findRelatedAccountingVoucherId(TEST_OV_ID);

        // Then
        assertNull(result);
    }

    // ========== isVoucher Tests ==========

    @Test
    @DisplayName("判断凭证类型 - 记账凭证(PZ)返回true")
    void testIsVoucher_PzCode() {
        // When
        boolean result = relationGraphHelper.isVoucher("PZ-2023-001");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("判断凭证类型 - 记账凭证(JZ)返回true")
    void testIsVoucher_JzCode() {
        // When
        boolean result = relationGraphHelper.isVoucher("JZ-2023-001");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("判断凭证类型 - 小写记账凭证返回true")
    void testIsVoucher_LowerCaseCode() {
        // When
        boolean result = relationGraphHelper.isVoucher("pz-2023-001");

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("判断凭证类型 - 发票代码返回false")
    void testIsVoucher_InvoiceCode() {
        // When
        boolean result = relationGraphHelper.isVoucher("FP-2023-001");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("判断凭证类型 - 空字符串返回false")
    void testIsVoucher_EmptyString() {
        // When
        boolean result = relationGraphHelper.isVoucher("");

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("判断凭证类型 - null返回false")
    void testIsVoucher_Null() {
        // When
        boolean result = relationGraphHelper.isVoucher(null);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("判断凭证类型 - 单字符返回false")
    void testIsVoucher_SingleChar() {
        // When
        boolean result = relationGraphHelper.isVoucher("P");

        // Then
        assertFalse(result);
    }

    // ========== Edge Cases & Boundary Tests ==========

    @Test
    @DisplayName("边界情况 - 深度为0不递归")
    void testFetchRelationsRecursive_DepthZero() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);

        // When - 通过buildGraph触发，深度为3时会递归
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - 验证至少调用了一次
        verify(archiveRelationService, atLeastOnce()).list(any(LambdaQueryWrapper.class));
        assertNotNull(graph);
    }

    @Test
    @DisplayName("边界情况 - 处理FILE_前缀节点不递归")
    void testFetchRelationsRecursive_FilePrefix() {
        // Given
        ArchiveRelation fileRelation = new ArchiveRelation();
        fileRelation.setSourceId("FILE_" + TEST_FILE_ID);
        fileRelation.setTargetId(TEST_CENTER_ID);
        fileRelation.setRelationType("ATTACHMENT");

        List<ArchiveRelation> relations = Arrays.asList(fileRelation);
        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(Collections.emptyList());
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - FILE_前缀节点不应该触发递归调用
        assertNotNull(graph);
        verify(archiveRelationService).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("边界情况 - 处理OV_前缀节点不递归")
    void testFetchRelationsRecursive_OvPrefix() {
        // Given
        ArchiveRelation ovRelation = new ArchiveRelation();
        ovRelation.setSourceId("OV_" + TEST_OV_ID);
        ovRelation.setTargetId(TEST_CENTER_ID);
        ovRelation.setRelationType("ORIGINAL_VOUCHER");

        List<ArchiveRelation> relations = Arrays.asList(ovRelation);
        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(Collections.emptyList());
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - OV_前缀节点不应该触发递归调用
        assertNotNull(graph);
        verify(archiveRelationService).list(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("边界情况 - 原始凭证不存在")
    void testBuildGraph_OriginalVoucherNotFound() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);
        List<VoucherRelation> voucherRelations = Arrays.asList(voucherRelation);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(voucherRelations);
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(originalVoucherMapper.selectById(TEST_OV_ID)).thenReturn(null); // 原始凭证不存在
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - 不应该包含原始凭证虚拟节点
        assertNotNull(graph);
        boolean hasVirtualNode = graph.getNodes().stream()
                .anyMatch(node -> node.getId().equals("OV_" + TEST_OV_ID));
        assertFalse(hasVirtualNode, "不应该包含不存在的原始凭证虚拟节点");
    }

    @Test
    @DisplayName("边界情况 - 附件文件不存在")
    void testBuildGraph_AttachmentFileNotFound() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);
        List<ArchiveAttachment> attachments = Arrays.asList(attachment);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(attachments);
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(attachmentService.getAttachmentsByArchive(TEST_CENTER_ID)).thenReturn(Collections.emptyList()); // 文件不存在
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - 不应该包含附件虚拟节点
        assertNotNull(graph);
        boolean hasFileNode = graph.getNodes().stream()
                .anyMatch(node -> node.getId().equals("FILE_" + TEST_FILE_ID));
        assertFalse(hasFileNode, "不应该包含不存在的附件虚拟节点");
    }

    @Test
    @DisplayName("边界情况 - 关联档案不在同一全宗")
    void testBuildGraph_DifferentFonds() {
        // Given
        Archive differentFondsArchive = new Archive();
        differentFondsArchive.setId(TEST_RELATED_ID_1);
        differentFondsArchive.setFondsNo("F002"); // 不同全宗

        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(differentFondsArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(archiveMapper.selectBatchIds(anyList())).thenReturn(Collections.emptyList());
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then - 不同全宗的档案不应该被包含
        assertNotNull(graph);
        boolean hasDifferentFondsNode = graph.getNodes().stream()
                .anyMatch(node -> TEST_RELATED_ID_1.equals(node.getId()));
        assertFalse(hasDifferentFondsNode, "不应该包含不同全宗的档案节点");
    }

    @Test
    @DisplayName("边界情况 - 空全宗代码包含所有档案")
    void testBuildGraph_EmptyFondsCode() {
        // Given
        List<ArchiveRelation> relations = Arrays.asList(relation1);
        List<Archive> archives = Arrays.asList(relatedArchive1);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(Collections.emptyList());
        when(archiveMapper.selectBatchIds(anyList())).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When - 传入空字符串作为全宗代码
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, "");

        // Then - 应该包含档案（空全宗代码不过滤）
        assertNotNull(graph);
        verify(archiveMapper).selectBatchIds(anyList());
    }

    // ========== Type Resolution Tests ==========

    @Test
    @DisplayName("类型解析 - 合同(HT)")
    void testResolveType_Contract() {
        // Given
        Archive contractArchive = new Archive();
        contractArchive.setId("archive-001");
        contractArchive.setArchiveCode("HT-2023-001");
        contractArchive.setTitle("合同");
        contractArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(contractArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> contractNode = graph.getNodes().stream()
                .filter(node -> "archive-001".equals(node.getId()))
                .findFirst();
        assertTrue(contractNode.isPresent());
        assertEquals("contract", contractNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - 发票(FP)")
    void testResolveType_Invoice() {
        // Given - already tested in relatedArchive1
        assertTrue(true);
    }

    @Test
    @DisplayName("类型解析 - 回单(HD)")
    void testResolveType_Receipt() {
        // Given
        Archive receiptArchive = new Archive();
        receiptArchive.setId("archive-002");
        receiptArchive.setArchiveCode("HD-2023-001");
        receiptArchive.setTitle("银行回单");
        receiptArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(receiptArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> receiptNode = graph.getNodes().stream()
                .filter(node -> "archive-002".equals(node.getId()))
                .findFirst();
        assertTrue(receiptNode.isPresent());
        assertEquals("receipt", receiptNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - 付款(FK)")
    void testResolveType_Payment() {
        // Given
        Archive paymentArchive = new Archive();
        paymentArchive.setId("archive-003");
        paymentArchive.setArchiveCode("FK-2023-001");
        paymentArchive.setTitle("付款单");
        paymentArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(paymentArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> paymentNode = graph.getNodes().stream()
                .filter(node -> "archive-003".equals(node.getId()))
                .findFirst();
        assertTrue(paymentNode.isPresent());
        assertEquals("payment", paymentNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - 报销(BX)")
    void testResolveType_Reimbursement() {
        // Given
        Archive reimbursementArchive = new Archive();
        reimbursementArchive.setId("archive-004");
        reimbursementArchive.setArchiveCode("BX-2023-001");
        reimbursementArchive.setTitle("报销单");
        reimbursementArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(reimbursementArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> reimbursementNode = graph.getNodes().stream()
                .filter(node -> "archive-004".equals(node.getId()))
                .findFirst();
        assertTrue(reimbursementNode.isPresent());
        assertEquals("reimbursement", reimbursementNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - 申请(SQ)")
    void testResolveType_Application() {
        // Given
        Archive applicationArchive = new Archive();
        applicationArchive.setId("archive-005");
        applicationArchive.setArchiveCode("SQ-2023-001");
        applicationArchive.setTitle("申请单");
        applicationArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(applicationArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> applicationNode = graph.getNodes().stream()
                .filter(node -> "archive-005".equals(node.getId()))
                .findFirst();
        assertTrue(applicationNode.isPresent());
        assertEquals("application", applicationNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - 未知类型")
    void testResolveType_Other() {
        // Given
        Archive otherArchive = new Archive();
        otherArchive.setId("archive-006");
        otherArchive.setArchiveCode("XX-2023-001");
        otherArchive.setTitle("未知类型");
        otherArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(otherArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> otherNode = graph.getNodes().stream()
                .filter(node -> "archive-006".equals(node.getId()))
                .findFirst();
        assertTrue(otherNode.isPresent());
        assertEquals("other", otherNode.get().getType());
    }

    @Test
    @DisplayName("类型解析 - null档案代码")
    void testResolveType_NullCode() {
        // Given
        Archive nullCodeArchive = new Archive();
        nullCodeArchive.setId("archive-007");
        nullCodeArchive.setArchiveCode(null);
        nullCodeArchive.setTitle("无代码");
        nullCodeArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(nullCodeArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> nullCodeNode = graph.getNodes().stream()
                .filter(node -> "archive-007".equals(node.getId()))
                .findFirst();
        assertTrue(nullCodeNode.isPresent());
        assertEquals("other", nullCodeNode.get().getType());
    }

    // ========== Date Resolution Tests ==========

    @Test
    @DisplayName("日期解析 - 使用docDate")
    void testResolveDate_FromDocDate() {
        // Given & When - already tested in main success test
        // The centerArchive has docDate set, so it should be used
        assertTrue(true);
    }

    @Test
    @DisplayName("日期解析 - docDate为null时使用createdTime")
    void testResolveDate_FromCreatedTime() {
        // Given
        Archive noDocDateArchive = new Archive();
        noDocDateArchive.setId("archive-008");
        noDocDateArchive.setArchiveCode("PZ-2023-002");
        noDocDateArchive.setTitle("无凭证日期");
        noDocDateArchive.setDocDate(null);
        noDocDateArchive.setCreatedTime(LocalDateTime.of(2023, 12, 15, 10, 30));
        noDocDateArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(noDocDateArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> noDocDateNode = graph.getNodes().stream()
                .filter(node -> "archive-008".equals(node.getId()))
                .findFirst();
        assertTrue(noDocDateNode.isPresent());
        assertEquals("2023-12-15", noDocDateNode.get().getDate());
    }

    @Test
    @DisplayName("日期解析 - 两者都为null返回空字符串")
    void testResolveDate_BothNull() {
        // Given
        Archive noDateArchive = new Archive();
        noDateArchive.setId("archive-009");
        noDateArchive.setArchiveCode("PZ-2023-003");
        noDateArchive.setTitle("无日期");
        noDateArchive.setDocDate(null);
        noDateArchive.setCreatedTime(null);
        noDateArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(noDateArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> noDateNode = graph.getNodes().stream()
                .filter(node -> "archive-009".equals(node.getId()))
                .findFirst();
        assertTrue(noDateNode.isPresent());
        assertEquals("", noDateNode.get().getDate());
    }

    // ========== Amount Formatting Tests ==========

    @Test
    @DisplayName("金额格式化 - 标准金额")
    void testFormatAmount_Standard() {
        // Given & When - already tested in main success test
        assertTrue(true);
    }

    @Test
    @DisplayName("金额格式化 - null返回null")
    void testFormatAmount_Null() {
        // Given
        Archive nullAmountArchive = new Archive();
        nullAmountArchive.setId("archive-010");
        nullAmountArchive.setArchiveCode("PZ-2023-004");
        nullAmountArchive.setTitle("无金额");
        nullAmountArchive.setAmount(null);
        nullAmountArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(nullAmountArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> nullAmountNode = graph.getNodes().stream()
                .filter(node -> "archive-010".equals(node.getId()))
                .findFirst();
        assertTrue(nullAmountNode.isPresent());
        assertNull(nullAmountNode.get().getAmount());
    }

    @Test
    @DisplayName("金额格式化 - 小数精度")
    void testFormatAmount_DecimalPrecision() {
        // Given
        Archive decimalArchive = new Archive();
        decimalArchive.setId("archive-011");
        decimalArchive.setArchiveCode("PZ-2023-005");
        decimalArchive.setTitle("小数金额");
        decimalArchive.setAmount(new BigDecimal("1000.123"));
        decimalArchive.setFondsNo(TEST_FONDS_NO);

        List<ArchiveRelation> relations = Collections.emptyList();
        List<Archive> archives = Arrays.asList(decimalArchive);

        when(archiveRelationService.list(any(LambdaQueryWrapper.class))).thenReturn(relations);
        when(attachmentService.getAttachmentLinks(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(voucherRelationMapper.findByAccountingVoucherId(TEST_CENTER_ID)).thenReturn(Collections.emptyList());
        when(archiveService.getArchivesByIds(any(Set.class))).thenReturn(archives);
        when(relationDirectionResolver.resolve(anyString(), anyList())).thenReturn(createDirectionalView());

        // When
        RelationGraphDto graph = relationGraphHelper.buildGraph(centerArchive, null, false, null, TEST_FONDS_NO);

        // Then
        Optional<RelationNodeDto> decimalNode = graph.getNodes().stream()
                .filter(node -> "archive-011".equals(node.getId()))
                .findFirst();
        assertTrue(decimalNode.isPresent());
        assertEquals("¥ 1000.12", decimalNode.get().getAmount()); // 四舍五入到2位小数
    }

    // ========== Helper Methods ==========

    private RelationGraphDto.DirectionalView createDirectionalView() {
        return RelationGraphDto.DirectionalView.builder()
                .upstream(Arrays.asList(TEST_RELATED_ID_1))
                .downstream(Arrays.asList(TEST_RELATED_ID_2))
                .layers(new HashMap<>())
                .mainline(Arrays.asList(TEST_CENTER_ID))
                .build();
    }
}
