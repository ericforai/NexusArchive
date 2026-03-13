package com.nexusarchive.controller;

import cn.hutool.core.codec.Base64;
import com.nexusarchive.common.enums.DirectionType;
import com.nexusarchive.common.enums.VoucherType;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.impl.IngestServiceImpl;
import com.nexusarchive.util.PathSecurityUtils;
import com.nexusarchive.service.helper.IngestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IngestFlowTest {

    private IngestServiceImpl ingestService;
    private IngestRequestStatusMapper statusMapper;
    private ApplicationEventPublisher eventPublisher;
    private com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private com.nexusarchive.service.ArchivalPackageService archivalPackageService;
    private com.nexusarchive.service.ArchiveService archiveService;
    private com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper;
    private com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory;
    private com.nexusarchive.service.ArchiveSecurityService archiveSecurityService;
    private PathSecurityUtils pathSecurityUtils;
    private IngestHelper ingestHelper;

    @BeforeEach
    void setUp() {
        statusMapper = mock(IngestRequestStatusMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        arcFileContentMapper = mock(com.nexusarchive.mapper.ArcFileContentMapper.class);
        archivalPackageService = mock(com.nexusarchive.service.ArchivalPackageService.class);
        archiveService = mock(com.nexusarchive.service.ArchiveService.class);
        erpConfigMapper = mock(com.nexusarchive.mapper.ErpConfigMapper.class);
        erpAdapterFactory = mock(com.nexusarchive.integration.erp.adapter.ErpAdapterFactory.class);
        archiveSecurityService = mock(com.nexusarchive.service.ArchiveSecurityService.class);
        pathSecurityUtils = mock(PathSecurityUtils.class);
        ingestHelper = mock(IngestHelper.class);
        when(pathSecurityUtils.getSafeFileName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        ingestService = new IngestServiceImpl(statusMapper, eventPublisher, arcFileContentMapper, archivalPackageService, archiveService, erpConfigMapper, erpAdapterFactory, archiveSecurityService, pathSecurityUtils, ingestHelper);
        ReflectionTestUtils.setField(ingestService, "tempRootPath", "/tmp/nexus-test");
    }

    @Test
    void testIngestSipPublishesEvent() {
        String corruptedContent = "%PDF-1.4\nThis is a corrupted file content...";
        String base64Content = Base64.encode(corruptedContent);
        
        AccountingSipDto sip = AccountingSipDto.builder()
                .requestId(UUID.randomUUID().toString())
                .sourceSystem("TEST_ERP")
                .header(VoucherHeadDto.builder()
                        .fondsCode("TEST001")
                        .accountPeriod("2025-11")
                        .voucherType(VoucherType.PAYMENT)
                        .voucherNumber("V001")
                        .voucherDate(LocalDate.now())
                        .totalAmount(new BigDecimal("100.00"))
                        .currencyCode("CNY")
                        .attachmentCount(1)
                        .issuer("Tester")
                        .build())
                .entries(List.of(VoucherEntryDto.builder()
                        .lineNo(1)
                        .summary("Test Entry")
                        .subjectCode("1001")
                        .direction(DirectionType.DEBIT)
                        .amount(new BigDecimal("100.00"))
                        .build()))
                .attachments(List.of(AttachmentDto.builder()
                        .fileName("corrupted.pdf")
                        .fileType("PDF")
                        .fileSize((long) corruptedContent.length())
                        .base64Content(base64Content)
                        .build()))
                .build();

        var response = ingestService.ingestSip(sip);

        assertNotNull(response);
        assertEquals("RECEIVED", response.getStatus());
        ArgumentCaptor<VoucherReceivedEvent> eventCaptor = ArgumentCaptor.forClass(VoucherReceivedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        verify(statusMapper).insert(any(com.nexusarchive.entity.IngestRequestStatus.class));
    }
}
