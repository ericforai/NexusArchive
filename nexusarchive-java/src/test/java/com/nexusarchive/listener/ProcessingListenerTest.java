package com.nexusarchive.listener;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.CheckPassedEvent;
import com.nexusarchive.service.ArchivalPackageService;
import com.nexusarchive.service.IAutoAssociationService;
import com.nexusarchive.service.SmartParserService;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessingListenerTest {

    @Mock
    private ArchivalPackageService archivalPackageService;

    @Mock
    private SmartParserService smartParserService;

    @Mock
    private IAutoAssociationService autoAssociationService;

    @Mock
    private IngestRequestStatusMapper statusMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ProcessingListener processingListener;

    private AccountingSipDto sipDto;
    private String tempPath;
    private String archivalCode = "F001-2025-10Y-FIN-AC01-123456";
    private FourNatureReport report;
    private Map<String, byte[]> fileStreams;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        processingListener = new ProcessingListener(
            archivalPackageService,
            smartParserService,
            autoAssociationService,
            archiveMapper,
            statusMapper,
            objectMapper,
            eventPublisher
        );

        sipDto = new AccountingSipDto();
        sipDto.setRequestId("test-req-id");
        
        com.nexusarchive.dto.sip.VoucherHeadDto header = new com.nexusarchive.dto.sip.VoucherHeadDto();
        header.setFondsCode("F001");
        header.setAccountPeriod("2025");
        header.setVoucherType(com.nexusarchive.common.enums.VoucherType.PAYMENT);
        header.setVoucherNumber("123456");
        sipDto.setHeader(header);
        
        tempPath = "/tmp/test";
        report = new FourNatureReport();
        report.setStatus(OverallStatus.PASS);
        fileStreams = new HashMap<>();
    }

    @Test
    void testSuccessPath() throws Exception {
        // Mock archival package service to return a non-empty list
        List mockedFiles = new ArrayList();
        com.nexusarchive.entity.ArcFileContent content = new com.nexusarchive.entity.ArcFileContent();
        content.setStoragePath("/storage/path/file.pdf");
        content.setArchivalCode("F001-2025-10Y-FIN-AC01-123456");
        content.setFileHash("dummy-hash");
        content.setHashAlgorithm("SHA-256");
        mockedFiles.add(content);
        
        when(archivalPackageService.archivePackage(any(), anyString())).thenReturn(mockedFiles);

        // Mock archiveMapper to set ID
        doAnswer(invocation -> {
            Archive arg = invocation.getArgument(0);
            arg.setId("mock-archive-id");
            return 1;
        }).when(archiveMapper).insert(any(Archive.class));

        CheckPassedEvent event = new CheckPassedEvent(this, sipDto, tempPath, report, fileStreams);
        processingListener.handleCheckPassed(event);

        // Verify status updates to PROCESSING then COMPLETED
        ArgumentCaptor<IngestRequestStatus> statusCaptor = ArgumentCaptor.forClass(IngestRequestStatus.class);
        verify(statusMapper, atLeastOnce()).updateById(statusCaptor.capture());
        assertTrue(statusCaptor.getAllValues().stream()
                .anyMatch(s -> "PROCESSING".equals(s.getStatus())));
        assertTrue(statusCaptor.getAllValues().stream()
                .anyMatch(s -> "COMPLETED".equals(s.getStatus())));

        // Verify downstream services called
        verify(archivalPackageService, times(1)).archivePackage(any(), anyString());
        verify(smartParserService, times(1)).parseAndIndex(mockedFiles);
        verify(autoAssociationService, times(1)).triggerAssociation(anyString());
    }

    @Test
    void testFailurePath() throws Exception {
        // Simulate exception from archival package service
        when(archivalPackageService.archivePackage(any(), anyString()))
                .thenThrow(new RuntimeException("archive error"));

        CheckPassedEvent event = new CheckPassedEvent(this, sipDto, tempPath, report, fileStreams);
        processingListener.handleCheckPassed(event);

        // Verify status set to FAILED
        ArgumentCaptor<IngestRequestStatus> statusCaptor = ArgumentCaptor.forClass(IngestRequestStatus.class);
        verify(statusMapper, atLeastOnce()).updateById(statusCaptor.capture());
        assertTrue(statusCaptor.getAllValues().stream()
                .anyMatch(s -> "FAILED".equals(s.getStatus())));

        // Verify downstream services not called
        verify(smartParserService, never()).parseAndIndex(any());
        verify(autoAssociationService, never()).triggerAssociation(anyString());
    }
}
