// Input: AipExportService
// Output: Test Results
// Pos: NexusCore preservation/aip/test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.preservation.aip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.aip.impl.DefaultAipExportService;
import com.nexusarchive.core.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AipExportServiceTests {

    private DefaultAipExportService service;
    
    @Mock private ArchiveObjectMapper archiveMapper;
    @Mock private FileContentMapper fileContentMapper;
    @Mock private PreservationAuditMapper auditMapper;
    @Mock private StorageService storageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultAipExportService(archiveMapper, fileContentMapper, auditMapper, storageService);
    }

    @Test
    void testExportAipStructure() throws Exception {
        String archiveId = "test-aip-1";
        
        ArchiveObject archive = new ArchiveObject();
        archive.setId(archiveId);
        archive.setFondsNo("FONDS-001");
        archive.setStatus("ARCHIVED");
        
        FileContent file = new FileContent();
        file.setId("f1");
        file.setFileName("doc.pdf");
        file.setStoragePath("/path/doc.pdf");
        file.setItemId(archiveId);
        
        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        when(fileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
        when(storageService.getInputStream(anyString())).thenReturn(new ByteArrayInputStream("PDF-DATA".getBytes()));
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        service.exportAip(archiveId, bos);
        
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            ZipEntry entry;
            boolean hasMetadata = false;
            boolean hasContent = false;
            boolean hasManifest = false;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("metadata.xml")) {
                    hasMetadata = true;
                }
                if (entry.getName().equals("content/doc.pdf")) {
                    hasContent = true;
                }
                if (entry.getName().equals("manifest.sha256")) {
                    hasManifest = true;
                }
            }
            
            Assertions.assertTrue(hasMetadata, "Should allow metadata.xml");
            Assertions.assertTrue(hasContent, "Should allow content file");
            Assertions.assertTrue(hasManifest, "Should allow manifest");
        }
    }
}
