// Input: JUnit、Mockito、Archive/ArcFileContent Mapper
// Output: ArchiveFileQueryServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.app;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArchiveFileQueryServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private VoucherRelationMapper voucherRelationMapper;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private ArchiveFileQueryService archiveFileQueryService;

    @Test
    @DisplayName("应聚合直接文件、附件和原始凭证文件")
    void getFilesByArchiveId_ShouldAggregateAllSources() {
        Archive archive = new Archive();
        archive.setId("arc-001");
        archive.setArchiveCode("ARC-001");
        archive.setFondsNo("F001");

        ArcFileContent directFile = new ArcFileContent();
        directFile.setId("file-direct");
        ArcFileContent attachmentFile = new ArcFileContent();
        attachmentFile.setId("file-attachment");
        ArcFileContent voucherFile = new ArcFileContent();
        voucherFile.setId("file-voucher");

        VoucherRelation relation = new VoucherRelation();
        relation.setOriginalVoucherId("ov-001");
        relation.setAccountingVoucherId("arc-001");
        relation.setDeleted(0);

        when(archiveMapper.selectById("arc-001")).thenReturn(archive);
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());
        when(dataScopeService.canAccessArchive(any(), any())).thenReturn(true);
        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(directFile))
                .thenReturn(List.of(voucherFile));
        when(arcFileContentMapper.selectAttachmentsByArchiveId("arc-001")).thenReturn(List.of(attachmentFile));
        when(voucherRelationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(relation));

        List<ArcFileContent> result = archiveFileQueryService.getFilesByArchiveId("arc-001");

        assertThat(result).containsExactly(directFile, attachmentFile, voucherFile);

        ArgumentCaptor<LambdaQueryWrapper<ArcFileContent>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(arcFileContentMapper, org.mockito.Mockito.times(2)).selectList(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
    }

    @Test
    @DisplayName("按 ID 查不到时应回退到 archiveCode 查询")
    void getFilesByArchiveId_ShouldFallbackToArchiveCodeLookup() {
        Archive archive = new Archive();
        archive.setId("arc-001");
        archive.setArchiveCode("ARC-001");
        archive.setFondsNo("F001");

        when(archiveMapper.selectById("ARC-001")).thenReturn(null);
        when(archiveMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(archive);
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());
        when(dataScopeService.canAccessArchive(any(), any())).thenReturn(true);
        when(arcFileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(arcFileContentMapper.selectAttachmentsByArchiveId("arc-001")).thenReturn(List.of());
        when(voucherRelationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<ArcFileContent> result = archiveFileQueryService.getFilesByArchiveId("ARC-001");

        assertThat(result).isEmpty();
        verify(archiveMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("无访问权限时应拒绝查询文件")
    void getFilesByArchiveId_WhenAccessDenied_ShouldThrowException() {
        Archive archive = new Archive();
        archive.setId("arc-001");
        archive.setFondsNo("F001");

        when(archiveMapper.selectById("arc-001")).thenReturn(archive);
        when(dataScopeService.resolve()).thenReturn(new DataScopeContext(com.nexusarchive.common.enums.DataScopeType.SELF, "user-001", java.util.Set.of()));
        when(dataScopeService.canAccessArchive(archive, new DataScopeContext(com.nexusarchive.common.enums.DataScopeType.SELF, "user-001", java.util.Set.of())))
                .thenReturn(false);

        assertThatThrownBy(() -> archiveFileQueryService.getFilesByArchiveId("arc-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权查看该档案");

        verify(arcFileContentMapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(voucherRelationMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }
}
