package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VolumeService 单元测试
 * 验证组卷和归档审核功能符合 DA/T 104-2024 规范
 */
@ExtendWith(MockitoExtension.class)
class VolumeServiceTest {

    @Mock
    private VolumeMapper volumeMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private VolumeService volumeService;

    private Archive testArchive1;
    private Archive testArchive2;
    private Volume testVolume;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testArchive1 = new Archive();
        testArchive1.setId("archive-001");
        testArchive1.setArchiveCode("YS-2025-08-记-1");
        testArchive1.setTitle("会计凭证-记-1");
        testArchive1.setFondsNo("BR01");
        testArchive1.setFiscalYear("2025");
        testArchive1.setFiscalPeriod("2025-08");
        testArchive1.setCategoryCode("AC01");
        testArchive1.setOrgName("泊冉演示集团");
        testArchive1.setCreator("王心尹");
        testArchive1.setRetentionPeriod("10Y");
        testArchive1.setAmount(new BigDecimal("150.00"));
        testArchive1.setDocDate(LocalDate.of(2025, 8, 1));
        testArchive1.setStatus("draft");
        testArchive1.setDeleted(0);

        testArchive2 = new Archive();
        testArchive2.setId("archive-002");
        testArchive2.setArchiveCode("YS-2025-08-记-2");
        testArchive2.setTitle("会计凭证-记-2");
        testArchive2.setFondsNo("BR01");
        testArchive2.setFiscalYear("2025");
        testArchive2.setFiscalPeriod("2025-08");
        testArchive2.setCategoryCode("AC01");
        testArchive2.setOrgName("泊冉演示集团");
        testArchive2.setCreator("李珍珍");
        testArchive2.setRetentionPeriod("30Y");  // 不同保管期限
        testArchive2.setAmount(new BigDecimal("200.00"));
        testArchive2.setDocDate(LocalDate.of(2025, 8, 5));
        testArchive2.setStatus("draft");
        testArchive2.setDeleted(0);

        testVolume = new Volume();
        testVolume.setId("volume-001");
        testVolume.setVolumeCode("BR01-AC01-202508");
        testVolume.setTitle("泊冉演示集团2025年08月会计凭证");
        testVolume.setFondsNo("BR01");
        testVolume.setFiscalYear("2025");
        testVolume.setFiscalPeriod("2025-08");
        testVolume.setCategoryCode("AC01");
        testVolume.setFileCount(2);
        testVolume.setRetentionPeriod("30Y");
        testVolume.setStatus("draft");
    }

    @Test
    @DisplayName("组卷成功 - 应按月组卷并关联凭证")
    void assembleByMonth_Success() {
        // Arrange
        List<Archive> archives = Arrays.asList(testArchive1, testArchive2);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(archives);
        when(volumeMapper.insert(any(Volume.class))).thenReturn(1);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        // Act
        Volume result = volumeService.assembleByMonth("2025-08");

        // Assert
        assertNotNull(result);
        assertEquals("BR01-AC01-202508", result.getVolumeCode());
        assertEquals("泊冉演示集团2025年08月会计凭证", result.getTitle());
        assertEquals("BR01", result.getFondsNo());
        assertEquals("2025", result.getFiscalYear());
        assertEquals("2025-08", result.getFiscalPeriod());
        assertEquals("AC01", result.getCategoryCode());
        assertEquals(2, result.getFileCount());
        assertEquals("30Y", result.getRetentionPeriod()); // 取最长保管期限
        assertEquals("draft", result.getStatus());

        // 验证凭证关联更新
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
        verify(volumeMapper).insert(any(Volume.class));
    }

    @Test
    @DisplayName("组卷失败 - 无待组卷凭证时应抛出异常")
    void assembleByMonth_NoArchives_ThrowsException() {
        // Arrange
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(Exception.class, () -> volumeService.assembleByMonth("2025-08"));
        verify(volumeMapper, never()).insert(any(Volume.class));
    }

    @Test
    @DisplayName("保管期限计算 - 应取卷内最长保管期限")
    void assembleByMonth_RetentionPeriod_TakesMax() {
        // Arrange: archive1=10Y, archive2=30Y
        List<Archive> archives = Arrays.asList(testArchive1, testArchive2);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(archives);
        when(volumeMapper.insert(any(Volume.class))).thenReturn(1);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        // Act
        Volume result = volumeService.assembleByMonth("2025-08");

        // Assert: 应该取 30Y (最长)
        assertEquals("30Y", result.getRetentionPeriod());
    }

    @Test
    @DisplayName("案卷标题格式 - 应符合规范格式")
    void assembleByMonth_TitleFormat() {
        // Arrange
        List<Archive> archives = Collections.singletonList(testArchive1);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(archives);
        when(volumeMapper.insert(any(Volume.class))).thenReturn(1);
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        // Act
        Volume result = volumeService.assembleByMonth("2025-08");

        // Assert: 标题格式 "责任者+年度+月度+业务单据名称"
        assertTrue(result.getTitle().contains("泊冉演示集团"));
        assertTrue(result.getTitle().contains("2025"));
        assertTrue(result.getTitle().contains("08"));
        assertTrue(result.getTitle().contains("会计凭证"));
    }

    @Test
    @DisplayName("提交审核 - 草稿状态案卷可提交")
    void submitForReview_DraftVolume_Success() {
        // Arrange
        testVolume.setStatus("draft");
        when(volumeMapper.selectById("volume-001")).thenReturn(testVolume);
        when(volumeMapper.updateById(any(Volume.class))).thenReturn(1);

        // Act
        volumeService.submitForReview("volume-001");

        // Assert
        ArgumentCaptor<Volume> captor = ArgumentCaptor.forClass(Volume.class);
        verify(volumeMapper).updateById(captor.capture());
        assertEquals("pending", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("提交审核 - 非草稿状态案卷应抛出异常")
    void submitForReview_NonDraftVolume_ThrowsException() {
        // Arrange
        testVolume.setStatus("archived");
        when(volumeMapper.selectById("volume-001")).thenReturn(testVolume);

        // Act & Assert
        assertThrows(Exception.class, () -> volumeService.submitForReview("volume-001"));
        verify(volumeMapper, never()).updateById(any(Volume.class));
    }

    @Test
    @DisplayName("审核归档 - 待审核案卷可归档")
    void approveArchival_PendingVolume_Success() {
        // Arrange
        testVolume.setStatus("pending");
        when(volumeMapper.selectById("volume-001")).thenReturn(testVolume);
        when(volumeMapper.updateById(any(Volume.class))).thenReturn(1);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(testArchive1, testArchive2));
        when(archiveMapper.updateById(any(Archive.class))).thenReturn(1);

        // Act
        volumeService.approveArchival("volume-001", "admin");

        // Assert
        ArgumentCaptor<Volume> volumeCaptor = ArgumentCaptor.forClass(Volume.class);
        verify(volumeMapper).updateById(volumeCaptor.capture());
        Volume updatedVolume = volumeCaptor.getValue();
        assertEquals("archived", updatedVolume.getStatus());
        assertEquals("admin", updatedVolume.getReviewedBy());
        assertNotNull(updatedVolume.getArchivedAt());

        // 验证卷内凭证也被归档
        verify(archiveMapper, times(2)).updateById(any(Archive.class));
    }

    @Test
    @DisplayName("审核归档 - 非待审核状态应抛出异常")
    void approveArchival_NonPendingVolume_ThrowsException() {
        // Arrange
        testVolume.setStatus("draft");
        when(volumeMapper.selectById("volume-001")).thenReturn(testVolume);

        // Act & Assert
        assertThrows(Exception.class, () -> volumeService.approveArchival("volume-001", "admin"));
    }

    @Test
    @DisplayName("生成归档登记表 - 应包含完整信息")
    void generateRegistrationForm_ContainsRequiredFields() {
        // Arrange
        testVolume.setStatus("archived");
        when(volumeMapper.selectById("volume-001")).thenReturn(testVolume);
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(testArchive1, testArchive2));

        // Act
        Map<String, Object> form = volumeService.generateRegistrationForm("volume-001");

        // Assert: 验证包含 GB/T 18894 要求的字段
        assertNotNull(form.get("registrationNo"));
        assertEquals("BR01-AC01-202508", form.get("volumeCode"));
        assertEquals("泊冉演示集团2025年08月会计凭证", form.get("volumeTitle"));
        assertEquals("BR01", form.get("fondsNo"));
        assertEquals("2025", form.get("fiscalYear"));
        assertEquals("2025-08", form.get("fiscalPeriod"));
        assertEquals("AC01", form.get("categoryCode"));
        assertEquals("会计凭证", form.get("categoryName"));
        assertEquals(2, form.get("fileCount"));
        assertEquals("30Y", form.get("retentionPeriod"));
        assertEquals("archived", form.get("status"));

        // 验证卷内文件清单
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fileList = (List<Map<String, Object>>) form.get("fileList");
        assertNotNull(fileList);
        assertEquals(2, fileList.size());
    }

    @Test
    @DisplayName("卷内文件排序 - 应按日期和档号排序")
    void getVolumeFiles_SortedByDateAndCode() {
        // Arrange
        when(archiveMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(testArchive1, testArchive2));

        // Act
        List<Archive> files = volumeService.getVolumeFiles("volume-001");

        // Assert
        assertNotNull(files);
        assertEquals(2, files.size());
        // 验证已排序 (日期较早的在前)
        assertTrue(files.get(0).getDocDate().isBefore(files.get(1).getDocDate()) 
            || files.get(0).getDocDate().equals(files.get(1).getDocDate()));
    }
}
