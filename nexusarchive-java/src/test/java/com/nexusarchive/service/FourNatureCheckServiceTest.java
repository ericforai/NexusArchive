package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.service.adapter.VirusScanAdapter;
import com.nexusarchive.service.impl.FourNatureCheckServiceImpl;
import com.nexusarchive.util.FileHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FourNatureCheckServiceTest {

    @Mock
    private FileHashUtil fileHashUtil;
    @Mock
    private VirusScanAdapter virusScanAdapter;
    @Mock
    private ArcFileContentMapper arcFileContentMapper;
    @Mock
    private ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    @Mock
    private com.nexusarchive.util.AmountValidator amountValidator;

    private FourNatureCheckServiceImpl fourNatureCheckService;

    @BeforeEach
    void setUp() {
        fourNatureCheckService = new FourNatureCheckServiceImpl(
                fileHashUtil, virusScanAdapter, arcFileContentMapper, arcFileMetadataIndexMapper, amountValidator
        );
        
        // Mock AmountValidator logic to always pass validation for general tests
        // Use lenient() because not all tests might trigger this check call
        org.mockito.Mockito.lenient().when(amountValidator.validateAmount(any())).thenReturn(com.nexusarchive.util.AmountValidator.ValidationResult.success());
    }

    @Test
    void testDeduplication_DuplicateFound() throws Exception {
        // Arrange
        String fileName = "test.pdf";
        byte[] content = "test content".getBytes();
        String hash = "dummy_hash";

        AttachmentDto attachment = AttachmentDto.builder()
                .fileName(fileName)
                .fileHash(hash)
                .hashAlgorithm("SM3")
                .fileType("PDF")
                .build();

        AccountingSipDto sip = AccountingSipDto.builder()
                .header(VoucherHeadDto.builder().voucherNumber("V001").build())
                .attachments(List.of(attachment))
                .build();

        Map<String, byte[]> fileStreams = new HashMap<>();
        fileStreams.put(fileName, content);

        when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
        // Mock DB returning count > 0 (Duplicate exists)
        when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // Act
        FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

        // Assert
        assertEquals(OverallStatus.FAIL, report.getStatus());
        assertEquals(OverallStatus.FAIL, report.getAuthenticity().getStatus());
        assert(report.getAuthenticity().getErrors().get(0).contains("Duplicate file detected"));
    }

    @Test
    void testDeduplication_NoDuplicate() throws Exception {
        // Arrange
        String fileName = "test.pdf";
        byte[] content = "test content".getBytes();
        String hash = "dummy_hash";

        AttachmentDto attachment = AttachmentDto.builder()
                .fileName(fileName)
                .fileHash(hash)
                .hashAlgorithm("SM3")
                .fileType("PDF")
                .build();

        AccountingSipDto sip = AccountingSipDto.builder()
                .header(VoucherHeadDto.builder()
                        .voucherNumber("V001")
                        .fondsCode("F001")
                        .accountPeriod("2023-01")
                        .attachmentCount(1)
                        .totalAmount(BigDecimal.ZERO)
                        .build())
                .attachments(List.of(attachment))
                .build();

        Map<String, byte[]> fileStreams = new HashMap<>();
        fileStreams.put(fileName, content);

        when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
        when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(virusScanAdapter.scan(any(), any())).thenReturn(true);
        when(amountValidator.validateAmount(any(BigDecimal.class))).thenReturn(com.nexusarchive.util.AmountValidator.ValidationResult.success());

        // Act
        FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

        // Assert
        // Should pass deduplication, but might fail other checks if not fully mocked.
        // Here we just want to ensure it didn't fail at deduplication.
        // Since we mocked virusScan=true and provided minimal metadata, it might pass or fail others.
        // But Authenticity should be PASS if hash matches. 
        // Wait, checkAuthenticity also calls calculateSM3 and compares with provided.
        // We provided hash in attachment and mocked calculateSM3 to return same hash.
        assertEquals(OverallStatus.PASS, report.getAuthenticity().getStatus());
    }
    @Test
    void testAuthenticity_HashMismatch() throws Exception {
        // Arrange
        String fileName = "test.pdf";
        byte[] content = "test content".getBytes();
        String providedHash = "original_hash";
        String calculatedHash = "different_hash";

        AttachmentDto attachment = AttachmentDto.builder()
                .fileName(fileName)
                .fileHash(providedHash)
                .hashAlgorithm("SM3")
                .fileType("PDF")
                .build();

        AccountingSipDto sip = AccountingSipDto.builder()
                .header(VoucherHeadDto.builder().voucherNumber("V001").attachmentCount(1).build())
                .attachments(List.of(attachment))
                .build();

        Map<String, byte[]> fileStreams = new HashMap<>();
        fileStreams.put(fileName, content);

        when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(calculatedHash);
        // Assuming no duplicate for this test
        when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // Act
        FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

        // Assert
        assertEquals(OverallStatus.FAIL, report.getStatus());
        assertEquals(OverallStatus.FAIL, report.getAuthenticity().getStatus());
        assert(report.getAuthenticity().getErrors().get(0).contains("Hash mismatch"));
    }

    @Test
    void testIntegrity_MissingMetadata() throws Exception {
        // Arrange
        AccountingSipDto sip = AccountingSipDto.builder()
                .header(VoucherHeadDto.builder()
                        .voucherNumber("V001")
                        .attachmentCount(0)
                        // Missing FondsCode, AccountPeriod etc.
                        .build())
                .attachments(Collections.emptyList())
                .build();

        Map<String, byte[]> fileStreams = new HashMap<>();

        // Act
        FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

        // Assert
        assertEquals(OverallStatus.FAIL, report.getStatus());
        assertEquals(OverallStatus.FAIL, report.getIntegrity().getStatus());
        // Exact error message depends on implementation details, checking if fails is good enough for now
    }

    @Test
    void testSafety_VirusDetected() throws Exception {
        // Arrange
        String fileName = "virus.exe";
        byte[] content = "malware".getBytes();
        String hash = "virus_hash";

        AttachmentDto attachment = AttachmentDto.builder()
                .fileName(fileName)
                .fileHash(hash)
                .hashAlgorithm("SM3")
                .fileType("EXE")
                .build();

        AccountingSipDto sip = AccountingSipDto.builder()
                .header(VoucherHeadDto.builder().voucherNumber("V001").attachmentCount(1).build())
                .attachments(List.of(attachment))
                .build();

        Map<String, byte[]> fileStreams = new HashMap<>();
        fileStreams.put(fileName, content);

        when(fileHashUtil.calculateSM3(any(ByteArrayInputStream.class))).thenReturn(hash);
        when(arcFileContentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(virusScanAdapter.scan(any(), any())).thenReturn(false); // Virus detected

        // Act
        FourNatureReport report = fourNatureCheckService.performFullCheck(sip, fileStreams);

        // Assert
        assertEquals(OverallStatus.FAIL, report.getStatus());
        assertEquals(OverallStatus.FAIL, report.getSafety().getStatus());
        assert(report.getSafety().getErrors().get(0).contains("Security Threat detected"));
    }
}
