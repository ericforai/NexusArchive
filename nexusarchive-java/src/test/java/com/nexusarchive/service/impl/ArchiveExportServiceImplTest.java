package com.nexusarchive.service.impl;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveExportServiceImplTest {

    @Mock
    private ArcFileContentMapper arcFileContentMapper;

    @Mock
    private ArchiveMapper archiveMapper;

    @InjectMocks
    private ArchiveExportServiceImpl archiveExportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(archiveExportService, "archiveRootPath", "/tmp/archives");
    }

    @Test
    void exportAipPackage_ShouldGenerateStructuredZip() throws IOException {
        // Arrange
        String archivalCode = "COMP001-2023-10Y-FIN-AC01-V001";
        
        Archive archive = new Archive();
        archive.setArchiveCode(archivalCode);
        archive.setFondsNo("COMP001");
        archive.setFiscalYear("2023");
        archive.setRetentionPeriod("10Y");
        archive.setCategoryCode("AC01");
        archive.setTitle("Test Voucher");
        archive.setCreator("Test User");
        archive.setOrgName("Test Org");
        archive.setSecurityLevel("INTERNAL");
        
        when(archiveMapper.selectOne(any())).thenReturn(archive);

        // Create dummy files
        Path tempDir = Files.createTempDirectory("test_files");
        Path invoicePath = tempDir.resolve("invoice.ofd");
        Files.createFile(invoicePath);
        Path contractPath = tempDir.resolve("contract.pdf");
        Files.createFile(contractPath);

        ArcFileContent invoice = new ArcFileContent();
        invoice.setFileName("invoice.ofd");
        invoice.setStoragePath(invoicePath.toString());
        invoice.setArchivalCode(archivalCode);

        ArcFileContent contract = new ArcFileContent();
        contract.setFileName("contract.pdf");
        contract.setStoragePath(contractPath.toString());
        contract.setArchivalCode(archivalCode);

        when(arcFileContentMapper.selectList(any())).thenReturn(Arrays.asList(invoice, contract));

        // Act
        File zipFile = archiveExportService.exportAipPackage(archivalCode);

        // Assert
        assertNotNull(zipFile);
        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);

        try (ZipFile zip = new ZipFile(zipFile)) {
            assertNotNull(zip.getEntry("index.xml"));
            assertNotNull(zip.getEntry("data/accounting.xml"));
            assertNotNull(zip.getEntry("content/invoice.ofd"));
            assertNotNull(zip.getEntry("attachment/contract.pdf"));
        }
        
        // Cleanup
        Files.deleteIfExists(zipFile.toPath());
        Files.deleteIfExists(invoicePath);
        Files.deleteIfExists(contractPath);
        Files.deleteIfExists(tempDir);
    }
}
