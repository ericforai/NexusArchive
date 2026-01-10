// Input: cn.hutool、org.junit、org.mockito、Spring Framework、等
// Output: IngestFlowTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

/**
 * IngestService 集成测试
 * 验证 SIP 接收、状态初始化和事件发布
 */
public class IngestFlowTest {

    private IngestServiceImpl ingestService;
    private IngestRequestStatusMapper statusMapper;
    private ApplicationEventPublisher eventPublisher;
    private com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private com.nexusarchive.service.ArchivalPackageService archivalPackageService;
    private com.nexusarchive.service.ArchiveService archiveService;
    private com.nexusarchive.service.ErpFeedbackService erpFeedbackService;
    private com.nexusarchive.service.ArchiveSecurityService archiveSecurityService;
    private PathSecurityUtils pathSecurityUtils;

    @BeforeEach
    void setUp() {
        statusMapper = mock(IngestRequestStatusMapper.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        arcFileContentMapper = mock(com.nexusarchive.mapper.ArcFileContentMapper.class);
        archivalPackageService = mock(com.nexusarchive.service.ArchivalPackageService.class);
        archiveService = mock(com.nexusarchive.service.ArchiveService.class);
        erpFeedbackService = mock(com.nexusarchive.service.ErpFeedbackService.class);
        archiveSecurityService = mock(com.nexusarchive.service.ArchiveSecurityService.class);
        pathSecurityUtils = mock(PathSecurityUtils.class);
        when(pathSecurityUtils.getSafeFileName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        ingestService = new IngestServiceImpl(statusMapper, eventPublisher, arcFileContentMapper, archivalPackageService, archiveService, erpFeedbackService, archiveSecurityService, pathSecurityUtils);
        
        // 注入临时路径配置
        ReflectionTestUtils.setField(ingestService, "tempRootPath", "/tmp/nexus-test");
    }

    @Test
    void testIngestSipPublishesEvent() {
        // 1. 构造 SIP 包
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

        // 2. 执行接收
        // 在新的事件驱动架构下，ingestSip 应该立即返回成功，而不是抛出异常
        // 异常会在异步 Listener 中处理
        var response = ingestService.ingestSip(sip);

        // 3. 验证响应
        assertNotNull(response);
        assertEquals("RECEIVED", response.getStatus());
        assertEquals(sip.getRequestId(), response.getRequestId());

        // 4. 验证事件发布
        ArgumentCaptor<VoucherReceivedEvent> eventCaptor = ArgumentCaptor.forClass(VoucherReceivedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        VoucherReceivedEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(sip.getRequestId(), event.getSipDto().getRequestId());
        
        // 5. 验证状态初始化
        verify(statusMapper).insert(any(com.nexusarchive.entity.IngestRequestStatus.class));
    }
}
