package com.nexusarchive.listener;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.AbnormalVoucherService;
import com.nexusarchive.service.FourNatureCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceListenerTest {

    @Mock
    private FourNatureCheckService fourNatureCheckService;
    @Mock
    private IngestRequestStatusMapper statusMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AbnormalVoucherService abnormalVoucherService;

    private ComplianceListener complianceListener;

    @BeforeEach
    void setUp() {
        complianceListener = new ComplianceListener(
                fourNatureCheckService, statusMapper, eventPublisher, abnormalVoucherService
        );
    }

    @Test
    void testHandleVoucherReceived_CheckFailed_SavesAbnormal() {
        // Arrange
        AccountingSipDto sip = new AccountingSipDto();
        sip.setRequestId("REQ-001");
        
        VoucherReceivedEvent event = new VoucherReceivedEvent(this, sip, "/tmp", new HashMap<>());
        
        FourNatureReport failedReport = FourNatureReport.builder()
                .status(OverallStatus.FAIL)
                .build();

        when(fourNatureCheckService.performFullCheck(any(), any())).thenReturn(failedReport);

        // Act
        complianceListener.handleVoucherReceived(event);

        // Assert
        // Verify status updated to FAILED
        verify(statusMapper).updateById(argThat((IngestRequestStatus status) -> 
            status.getRequestId().equals("REQ-001") && "FAILED".equals(status.getStatus())
        ));
        
        // Verify saved to Abnormal Pool
        verify(abnormalVoucherService).saveAbnormal(eq(sip), anyString());
        
        // Verify NO CheckPassedEvent published
        verify(eventPublisher, never()).publishEvent(any());
    }
}
