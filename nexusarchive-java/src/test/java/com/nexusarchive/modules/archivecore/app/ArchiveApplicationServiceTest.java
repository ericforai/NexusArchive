// Input: JUnit、Mockito、Archive DTO、Archive Entity
// Output: ArchiveApplicationServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.archivecore.app;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveCreateRequest;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveUpdateRequest;
import com.nexusarchive.service.ArchiveReadService;
import com.nexusarchive.service.ArchiveWriteService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArchiveApplicationServiceTest {

    @Mock
    private ArchiveReadService archiveReadService;

    @Mock
    private ArchiveWriteService archiveWriteService;

    @Mock
    private ArchiveFileQueryService archiveFileQueryService;

    @InjectMocks
    private ArchiveApplicationService archiveApplicationService;

    @Test
    @DisplayName("创建档案时应将请求 DTO 映射为 Archive 实体并委托写服务")
    void createArchive_ShouldMapRequestAndDelegateToWriteService() {
        ArchiveCreateRequest request = new ArchiveCreateRequest();
        request.setFondsNo("F001");
        request.setArchiveCode("ARC-001");
        request.setCategoryCode("AC01");
        request.setTitle("测试档案");
        request.setFiscalYear("2024");
        request.setRetentionPeriod("10Y");
        request.setOrgName("测试单位");

        Archive created = new Archive();
        created.setId("arc-001");
        created.setTitle("测试档案");

        when(archiveWriteService.createArchive(any(Archive.class), any())).thenReturn(created);

        Archive result = archiveApplicationService.createArchive(request, "user-001");

        ArgumentCaptor<Archive> captor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).createArchive(captor.capture(), org.mockito.ArgumentMatchers.eq("user-001"));
        Archive delegated = captor.getValue();

        assertThat(result).isSameAs(created);
        assertThat(delegated.getFondsNo()).isEqualTo("F001");
        assertThat(delegated.getArchiveCode()).isEqualTo("ARC-001");
        assertThat(delegated.getTitle()).isEqualTo("测试档案");
        assertThat(delegated.getOrgName()).isEqualTo("测试单位");
    }

    @Test
    @DisplayName("更新档案时应将更新 DTO 映射为 Archive 实体并委托写服务")
    void updateArchive_ShouldMapRequestAndDelegateToWriteService() {
        ArchiveUpdateRequest request = new ArchiveUpdateRequest();
        request.setTitle("更新后的档案");
        request.setFondsNo("F001");
        request.setArchiveCode("ARC-001");
        request.setFiscalYear("2024");
        request.setRetentionPeriod("30Y");
        request.setOrgName("测试单位");

        archiveApplicationService.updateArchive("arc-001", request);

        ArgumentCaptor<Archive> captor = ArgumentCaptor.forClass(Archive.class);
        verify(archiveWriteService).updateArchive(org.mockito.ArgumentMatchers.eq("arc-001"), captor.capture());
        Archive delegated = captor.getValue();

        assertThat(delegated.getTitle()).isEqualTo("更新后的档案");
        assertThat(delegated.getRetentionPeriod()).isEqualTo("30Y");
        assertThat(delegated.getArchiveCode()).isEqualTo("ARC-001");
    }

    @Test
    @DisplayName("查询档案文件时应委托 ArchiveFileQueryService")
    void getArchiveFiles_ShouldDelegateToArchiveFileQueryService() {
        ArcFileContent file = new ArcFileContent();
        file.setId("file-001");
        when(archiveFileQueryService.getFilesByArchiveId("arc-001")).thenReturn(List.of(file));

        List<ArcFileContent> result = archiveApplicationService.getArchiveFiles("arc-001");

        assertThat(result).containsExactly(file);
        verify(archiveFileQueryService).getFilesByArchiveId("arc-001");
    }

    @Test
    @DisplayName("分页查询应直接透传给读服务")
    void getArchives_ShouldDelegateToReadService() {
        Page<Archive> page = new Page<>(1, 10);
        when(archiveReadService.getArchives(1, 10, "kw", "archived", "AC01", "dept-1", "biz-1", "sub", "F001"))
                .thenReturn(page);

        Page<Archive> result = archiveApplicationService.getArchives(1, 10, "kw", "archived", "AC01", "dept-1", "biz-1", "sub", "F001");

        assertThat(result).isSameAs(page);
        verify(archiveReadService).getArchives(1, 10, "kw", "archived", "AC01", "dept-1", "biz-1", "sub", "F001");
    }
}
