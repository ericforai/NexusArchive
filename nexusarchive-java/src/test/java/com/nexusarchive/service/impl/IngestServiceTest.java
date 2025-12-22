// Input: org.junit、org.mockito、Spring Framework、Java 标准库、等
// Output: IngestServiceTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherEntryDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.event.VoucherReceivedEvent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import com.nexusarchive.service.ArchivalPackageService;
import com.nexusarchive.service.ArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestServiceTest {

    @Mock
    private IngestRequestStatusMapper ingestRequestStatusMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private ArchivalPackageService archivalPackageService;

    @Mock
    private ArchiveService archiveService;

    @InjectMocks
    private IngestServiceImpl ingestService;

    @BeforeEach
    void setUp() {
        // Set temp path for file operations
        ReflectionTestUtils.setField(ingestService, "tempRootPath", System.getProperty("java.io.tmpdir") + "/nexusarchive_test");
    }

    @Test
    @DisplayName("Ingest SIP - Success")
    void ingestSip_Success() {
        // Arrange
        AccountingSipDto sipDto = createValidSipDto();

        // Act
        IngestResponse response = ingestService.ingestSip(sipDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("RECEIVED");
        assertThat(response.getRequestId()).isEqualTo("REQ-001");

        // Verify Status Init
        verify(ingestRequestStatusMapper).insert(any(IngestRequestStatus.class));

        // Verify Event Published
        verify(eventPublisher).publishEvent(any(VoucherReceivedEvent.class));
    }

    @Test
    @DisplayName("Ingest SIP - Validation Error (Attachment Count Mismatch)")
    void ingestSip_ValidationError_AttachmentCount() {
        // Arrange
        AccountingSipDto sipDto = createValidSipDto();
        sipDto.getHeader().setAttachmentCount(99); // Intentional mismatch

        // Act & Assert
        assertThatThrownBy(() -> ingestService.ingestSip(sipDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("附件数量不匹配");
    }

    @Test
    @DisplayName("Ingest SIP - Validation Error (Balance Error)")
    void ingestSip_ValidationError_Balance() {
        // Arrange
        AccountingSipDto sipDto = createValidSipDto();
        sipDto.getHeader().setTotalAmount(new BigDecimal("99999.00")); // Intentional mismatch

        // Act & Assert
        assertThatThrownBy(() -> ingestService.ingestSip(sipDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("借贷不平衡");
    }
    
    // Helper to create valid SIP DTO
    private AccountingSipDto createValidSipDto() {
        AccountingSipDto sip = new AccountingSipDto();
        sip.setRequestId("REQ-001");
        
        VoucherHeadDto header = new VoucherHeadDto();
        header.setVoucherNumber("V001");
        header.setAttachmentCount(1);
        header.setTotalAmount(new BigDecimal("100.00"));
        sip.setHeader(header);
        
        VoucherEntryDto entry = new VoucherEntryDto();
        entry.setAmount(new BigDecimal("100.00"));
        sip.setEntries(Collections.singletonList(entry));
        
        AttachmentDto attachment = new AttachmentDto();
        attachment.setFileName("test.pdf");
        attachment.setBase64Content("ZHVtbXkgY29udGVudA=="); // "dummy content" base64
        sip.setAttachments(Collections.singletonList(attachment));
        
        return sip;
    }
}
