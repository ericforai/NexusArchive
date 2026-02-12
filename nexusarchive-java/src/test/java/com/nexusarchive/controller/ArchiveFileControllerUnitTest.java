package com.nexusarchive.controller;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OriginalVoucherMapper;
import com.nexusarchive.mapper.VoucherRelationMapper;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ArchiveFileControllerUnitTest {

    @Mock
    private ArchiveFileContentService archiveFileContentService;
    @Mock
    private ArchiveMapper archiveMapper;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private DataScopeService dataScopeService;
    @Mock
    private VoucherRelationMapper voucherRelationMapper;
    @Mock
    private OriginalVoucherMapper originalVoucherMapper;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ArchiveFileController archiveFileController;

    @Test
    @DisplayName("downloadByFileId should allow original voucher file when archivalCode matches voucher_no")
    void downloadByFileId_shouldAllowOriginalVoucherByVoucherNo() throws IOException {
        String fileId = "f4653466-b670-a083-acff-19ff6d55be02";
        String archivalCode = "INV-202311-089";

        ArcFileContent content = new ArcFileContent();
        content.setId(fileId);
        content.setItemId("seed-invoice-001");
        content.setArchivalCode(archivalCode);
        content.setStoragePath("uploads/demo/test.pdf");
        content.setFileType("pdf");
        content.setFileName("test.pdf");
        content.setFileSize(123L); // Intentionally mismatched metadata size
        when(archiveFileContentService.getFileContentById(eq(fileId), any())).thenReturn(content);

        when(archiveMapper.selectById(archivalCode)).thenReturn(null);
        when(archiveMapper.selectOne(any())).thenReturn(null);
        when(originalVoucherMapper.selectById(archivalCode)).thenReturn(null);

        OriginalVoucher voucher = new OriginalVoucher();
        voucher.setId("seed-invoice-001");
        voucher.setVoucherNo(archivalCode);
        voucher.setFondsCode("DEMO");
        when(originalVoucherMapper.selectOne(any())).thenReturn(voucher);

        DataScopeService.DataScopeContext scope = new DataScopeService.DataScopeContext(
                com.nexusarchive.common.enums.DataScopeType.ALL,
                "user_admin_001",
                Set.of("DEMO")
        );
        when(dataScopeService.resolve()).thenReturn(scope);
        when(dataScopeService.canAccessOriginalVoucher(voucher, scope)).thenReturn(true);

        Path tempFile = Files.createTempFile("archive-download-", ".pdf");
        byte[] payload = "test-pdf-body".getBytes(StandardCharsets.UTF_8);
        Files.write(tempFile, payload);

        when(fileStorageService.resolvePath(content.getStoragePath())).thenReturn(tempFile);
        when(fileStorageService.exists(content.getStoragePath())).thenReturn(true);

        try {
            ResponseEntity<?> response = archiveFileController.downloadByFileId(fileId, request);

            assertEquals(200, response.getStatusCode().value());
            assertEquals((long) payload.length, response.getHeaders().getContentLength());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
