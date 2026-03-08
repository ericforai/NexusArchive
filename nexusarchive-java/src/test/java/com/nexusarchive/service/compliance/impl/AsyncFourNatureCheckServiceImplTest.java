package com.nexusarchive.service.compliance.impl;

import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcSignatureLogMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.FourNatureCheckService;
import com.nexusarchive.service.FourNatureCoreService;
import com.nexusarchive.service.compliance.AsyncCheckTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ArcSignatureLogMapper arcSignatureLogMapper;

    private AsyncCheckTaskManager taskManager;
    private AsyncFourNatureCheckServiceImpl service;

    @BeforeEach
    void setUp() {
        taskManager = new AsyncCheckTaskManager();
        service = new AsyncFourNatureCheckServiceImpl(
                taskManager,
                archiveService,
                archiveFileContentService,
                fourNatureCoreService,
                fourNatureCheckService,
                arcFileContentMapper,
                arcSignatureLogMapper
        );
    }

    @Test
    void performParallelCheck_shouldPersistSignatureVerificationLogForPdfFile() throws Exception {
        TestFixture fixture = arrangePdfArchive("Hash verified (SM3); PDF签章校验通过", OverallStatus.PASS);
        String taskId = "task-valid";
        taskManager.createTask(taskId, fixture.archive().getId(), fixture.archive().getArchiveCode());

        FourNatureReport report = service.performParallelCheck(taskId, fixture.archive().getId())
                .get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ArcSignatureLog> logCaptor = ArgumentCaptor.forClass(ArcSignatureLog.class);
        verify(arcSignatureLogMapper).insert(logCaptor.capture());

        ArcSignatureLog persistedLog = logCaptor.getValue();
        assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
        assertThat(persistedLog.getArchiveId()).isEqualTo(fixture.archive().getId());
        assertThat(persistedLog.getFileId()).isEqualTo(fixture.file().getId());
        assertThat(persistedLog.getVerifyResult()).isEqualTo("VALID");
        assertThat(persistedLog.getVerifyMessage()).contains("PDF签章校验通过");
        assertThat(persistedLog.getVerifyTime()).isNotNull();
        assertThat(taskManager.getTaskStatus(taskId).getStatus()).isEqualTo(AsyncCheckTaskStatus.TaskStatus.COMPLETED);
    }

    @Test
    void performParallelCheck_shouldPersistUnknownResultForSignatureWarning() throws Exception {
        TestFixture fixture = arrangePdfArchive("签章服务不可用，跳过校验", OverallStatus.WARNING);
        String taskId = "task-warning";
        taskManager.createTask(taskId, fixture.archive().getId(), fixture.archive().getArchiveCode());

        FourNatureReport report = service.performParallelCheck(taskId, fixture.archive().getId())
                .get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ArcSignatureLog> logCaptor = ArgumentCaptor.forClass(ArcSignatureLog.class);
        verify(arcSignatureLogMapper).insert(logCaptor.capture());

        ArcSignatureLog persistedLog = logCaptor.getValue();
        assertThat(report.getStatus()).isEqualTo(OverallStatus.WARNING);
        assertThat(persistedLog.getVerifyResult()).isEqualTo("UNKNOWN");
        assertThat(persistedLog.getVerifyMessage()).contains("跳过校验");
    }

    @Test
    void performParallelCheck_shouldPersistInvalidResultForSignatureFailure() throws Exception {
        TestFixture fixture = arrangePdfArchive("PDF签章校验失败，证书链无效", OverallStatus.FAIL);
        String taskId = "task-invalid";
        taskManager.createTask(taskId, fixture.archive().getId(), fixture.archive().getArchiveCode());

        FourNatureReport report = service.performParallelCheck(taskId, fixture.archive().getId())
                .get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ArcSignatureLog> logCaptor = ArgumentCaptor.forClass(ArcSignatureLog.class);
        verify(arcSignatureLogMapper).insert(logCaptor.capture());

        ArcSignatureLog persistedLog = logCaptor.getValue();
        assertThat(report.getStatus()).isEqualTo(OverallStatus.FAIL);
        assertThat(persistedLog.getVerifyResult()).isEqualTo("INVALID");
        assertThat(persistedLog.getVerifyMessage()).contains("证书链无效");
    }

    @Test
    void performParallelCheck_shouldPersistInvalidResultForAnyFailureStatus() throws Exception {
        TestFixture fixture = arrangePdfArchive("哈希校验失败", OverallStatus.FAIL);
        String taskId = "task-invalid-generic";
        taskManager.createTask(taskId, fixture.archive().getId(), fixture.archive().getArchiveCode());

        service.performParallelCheck(taskId, fixture.archive().getId())
                .get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ArcSignatureLog> logCaptor = ArgumentCaptor.forClass(ArcSignatureLog.class);
        verify(arcSignatureLogMapper).insert(logCaptor.capture());

        ArcSignatureLog persistedLog = logCaptor.getValue();
        assertThat(persistedLog.getVerifyResult()).isEqualTo("INVALID");
        assertThat(persistedLog.getVerifyMessage()).contains("哈希校验失败");
    }

    @Test
    void performParallelCheck_shouldPreserveMessageAndErrorsForFailure() throws Exception {
        Archive archive = new Archive();
        archive.setId("archive-1");
        archive.setArchiveCode("ARCH-2026-0001");
        archive.setUniqueBizId("biz-1");
        archive.setAmount(java.math.BigDecimal.TEN);
        archive.setDocDate(LocalDate.of(2026, 3, 6));
        archive.setStandardMetadata("{\"ok\":true}");

        Path filePath = tempDir.resolve("invoice.pdf");
        Files.writeString(filePath, "%PDF-1.4\nminimal");

        ArcFileContent file = new ArcFileContent();
        file.setId("file-1");
        file.setItemId(archive.getId());
        file.setArchivalCode(archive.getArchiveCode());
        file.setFileName("invoice.pdf");
        file.setFileType("PDF");
        file.setStoragePath(filePath.toString());
        file.setOriginalHash("hash-1");
        file.setHashAlgorithm("SM3");

        when(archiveService.getArchiveById(archive.getId())).thenReturn(archive);
        when(archiveFileContentService.getFilesByItemId(eq(archive.getId()), eq(null))).thenReturn(List.of(file));

        CheckItem authenticity = CheckItem.pass("真实性检测", "PDF签章校验失败");
        authenticity.setStatus(OverallStatus.FAIL);
        authenticity.addError("证书链无效");

        when(fourNatureCoreService.checkSingleFileAuthenticity(
                any(),
                eq(file.getFileName()),
                eq(file.getOriginalHash()),
                eq(file.getHashAlgorithm()),
                eq(file.getFileType())
        )).thenReturn(authenticity);
        when(fourNatureCoreService.checkSingleFileUsability(any(), eq(file.getFileName()), eq(file.getFileType())))
                .thenReturn(CheckItem.pass("可用性检测", "文件可读"));
        when(fourNatureCoreService.checkSingleFileSafety(any(), eq(file.getFileName())))
                .thenReturn(CheckItem.pass("安全性检测", "未发现威胁"));

        String taskId = "task-invalid-message-errors";
        taskManager.createTask(taskId, archive.getId(), archive.getArchiveCode());

        service.performParallelCheck(taskId, archive.getId()).get(5, TimeUnit.SECONDS);

        ArgumentCaptor<ArcSignatureLog> logCaptor = ArgumentCaptor.forClass(ArcSignatureLog.class);
        verify(arcSignatureLogMapper).insert(logCaptor.capture());

        ArcSignatureLog persistedLog = logCaptor.getValue();
        assertThat(persistedLog.getVerifyResult()).isEqualTo("INVALID");
        assertThat(persistedLog.getVerifyMessage()).contains("PDF签章校验失败");
        assertThat(persistedLog.getVerifyMessage()).contains("证书链无效");
    }

    @Test
    void performParallelCheck_shouldKeepWorkflowRunningWhenSignatureLogPersistenceFails() throws Exception {
        TestFixture fixture = arrangePdfArchive("Hash verified (SM3); PDF签章校验通过", OverallStatus.PASS);
        String taskId = "task-persist-fallback";
        taskManager.createTask(taskId, fixture.archive().getId(), fixture.archive().getArchiveCode());
        doThrow(new RuntimeException("db down")).when(arcSignatureLogMapper).insert(any(ArcSignatureLog.class));

        FourNatureReport report = service.performParallelCheck(taskId, fixture.archive().getId())
                .get(5, TimeUnit.SECONDS);

        assertThat(report.getStatus()).isEqualTo(OverallStatus.PASS);
        assertThat(taskManager.getTaskStatus(taskId).getStatus()).isEqualTo(AsyncCheckTaskStatus.TaskStatus.COMPLETED);
        assertThat(taskManager.getResult(taskId)).isSameAs(report);
    }

    private TestFixture arrangePdfArchive(String authenticityMessage, OverallStatus authenticityStatus) throws Exception {
        Archive archive = new Archive();
        archive.setId("archive-1");
        archive.setArchiveCode("ARCH-2026-0001");
        archive.setUniqueBizId("biz-1");
        archive.setAmount(java.math.BigDecimal.TEN);
        archive.setDocDate(LocalDate.of(2026, 3, 6));
        archive.setStandardMetadata("{\"ok\":true}");

        Path filePath = tempDir.resolve("invoice.pdf");
        Files.writeString(filePath, "%PDF-1.4\nminimal");

        ArcFileContent file = new ArcFileContent();
        file.setId("file-1");
        file.setItemId(archive.getId());
        file.setArchivalCode(archive.getArchiveCode());
        file.setFileName("invoice.pdf");
        file.setFileType("PDF");
        file.setStoragePath(filePath.toString());
        file.setOriginalHash("hash-1");
        file.setHashAlgorithm("SM3");

        when(archiveService.getArchiveById(archive.getId())).thenReturn(archive);
        when(archiveFileContentService.getFilesByItemId(eq(archive.getId()), eq(null))).thenReturn(List.of(file));

        CheckItem authenticity = CheckItem.pass("真实性检测", authenticityMessage);
        authenticity.setStatus(authenticityStatus);

        when(fourNatureCoreService.checkSingleFileAuthenticity(
                any(),
                eq(file.getFileName()),
                eq(file.getOriginalHash()),
                eq(file.getHashAlgorithm()),
                eq(file.getFileType())
        )).thenReturn(authenticity);
        when(fourNatureCoreService.checkSingleFileUsability(any(), eq(file.getFileName()), eq(file.getFileType())))
                .thenReturn(CheckItem.pass("可用性检测", "文件可读"));
        when(fourNatureCoreService.checkSingleFileSafety(any(), eq(file.getFileName())))
                .thenReturn(CheckItem.pass("安全性检测", "未发现威胁"));

        return new TestFixture(archive, file);
    }

    private record TestFixture(Archive archive, ArcFileContent file) {
    }
}
