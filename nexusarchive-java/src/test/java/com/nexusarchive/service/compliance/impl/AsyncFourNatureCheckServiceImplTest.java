package com.nexusarchive.service.compliance.impl;

import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.service.compliance.AsyncCheckTaskManager;
import com.nexusarchive.service.helper.FourNatureAsyncHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AsyncFourNatureCheckServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private ArchiveService archiveService;
    @Mock
    private ArchiveFileContentService archiveFileContentService;
    @Mock
    private FourNatureCoreService fourNatureCoreService;
    @Mock
    private FourNatureCheckService fourNatureCheckService;
    @Mock
    private ArcFileContentMapper arcFileContentMapper;
    @Mock
    private FourNatureAsyncHelper fourNatureAsyncHelper;

    private AsyncCheckTaskManager taskManager;
    private AsyncFourNatureCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        taskManager = new AsyncCheckTaskManager();
        service = new AsyncFourNatureCheckServiceImpl(
                taskManager, archiveService, archiveFileContentService,
                fourNatureCoreService, fourNatureCheckService, arcFileContentMapper, fourNatureAsyncHelper
        );
    }

    @Test
    void performParallelCheck_shouldCompleteSuccessfully() throws Exception {
        Archive archive = new Archive();
        archive.setId("archive-1");
        archive.setArchiveCode("ARCH-001");
        
        ArcFileContent file = new ArcFileContent();
        file.setId("file-1");
        file.setStoragePath(tempDir.resolve("test.pdf").toString());
        Files.writeString(Path.of(file.getStoragePath()), "test");

        when(archiveService.getArchiveById(any())).thenReturn(archive);
        when(archiveFileContentService.getFilesByItemId(any(), any())).thenReturn(List.of(file));
        
        FourNatureReport mockReport = FourNatureReport.builder().status(OverallStatus.PASS).build();
        when(fourNatureAsyncHelper.buildReport(any(), any(), any(), any(), any())).thenReturn(mockReport);

        String taskId = "task-1";
        taskManager.createTask(taskId, archive.getId(), archive.getArchiveCode());

        FourNatureReport report = service.performParallelCheck(taskId, archive.getId()).get(5, TimeUnit.SECONDS);

        assertThat(report).isNotNull();
        assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
        verify(fourNatureAsyncHelper).buildReport(any(), any(), any(), any(), any());
    }
}
