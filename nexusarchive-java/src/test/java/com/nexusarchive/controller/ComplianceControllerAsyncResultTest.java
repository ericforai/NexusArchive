package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.ComplianceCheckService;
import com.nexusarchive.service.compliance.AsyncFourNatureCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ComplianceControllerAsyncResultTest {

    @Mock
    private ComplianceCheckService complianceCheckService;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private ArchiveFileContentService archiveFileContentService;

    @Mock
    private AsyncFourNatureCheckService asyncFourNatureCheckService;

    private ComplianceController controller;

    @BeforeEach
    void setUp() {
        controller = new ComplianceController(
                complianceCheckService,
                archiveService,
                archiveFileContentService,
                asyncFourNatureCheckService
        );
    }

    @Test
    void getAsyncCheckResult_shouldReturnCompletedReportWithAuthenticityDetails() {
        String taskId = "task-123";

        AsyncCheckTaskStatus statusDto = AsyncCheckTaskStatus.builder()
                .taskId(taskId)
                .archiveId("archive-1")
                .archiveCode("ARCH-2026-0001")
                .status(AsyncCheckTaskStatus.TaskStatus.COMPLETED)
                .build();

        CheckItem authenticity = CheckItem.pass("真实性检测", "Hash verified (SM3); PDF签章校验通过");
        FourNatureReport report = FourNatureReport.builder()
                .checkId("check-1")
                .archivalCode("ARCH-2026-0001")
                .status(OverallStatus.PASS)
                .authenticity(authenticity)
                .build();

        when(asyncFourNatureCheckService.getTaskStatus(taskId)).thenReturn(statusDto);
        when(asyncFourNatureCheckService.getCheckResult(taskId)).thenReturn(report);

        Result<FourNatureReport> response = controller.getAsyncCheckResult(taskId);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getStatus()).isEqualTo(OverallStatus.PASS);
        assertThat(response.getData().getAuthenticity().getMessage()).isEqualTo("Hash verified (SM3); PDF签章校验通过");
    }
}
