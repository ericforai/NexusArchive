package com.nexusarchive.core.preservation.fixity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.core.compliance.FileHashService;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.domain.PreservationAudit;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.fixity.impl.DefaultFixityCheckService;
import com.nexusarchive.core.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FixityCheckIntegrationTests {

    private DefaultFixityCheckService service;

    @Mock private FileContentMapper fileContentMapper;
    @Mock private PreservationAuditMapper auditMapper;
    @Mock private StorageService storageService;
    @Mock private FileHashService fileHashService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultFixityCheckService(fileContentMapper, auditMapper, storageService, fileHashService);
    }

    @Test
    void testPerformBatchCheck_Valid() throws Exception {
        // Arrange
        FileContent file = new FileContent();
        file.setId("f1");
        file.setItemId("a1");
        file.setFileHash("hash-123");
        file.setStoragePath("/doc.pdf");

        Page<FileContent> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(file));

        when(fileContentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);
            
        when(storageService.getInputStream("/doc.pdf")).thenReturn(new ByteArrayInputStream("content".getBytes()));
        when(fileHashService.hashFile(any(), any())).thenReturn("hash-123");

        // Act
        int processed = service.performBatchCheck(100);

        // Assert
        Assertions.assertEquals(1, processed);
        
        ArgumentCaptor<PreservationAudit> captor = ArgumentCaptor.forClass(PreservationAudit.class);
        verify(auditMapper).insert(captor.capture());
        
        PreservationAudit audit = captor.getValue();
        Assertions.assertEquals("FIXITY_CHECK", audit.getActionType());
        Assertions.assertEquals("VALID", audit.getOverallStatus());
        Assertions.assertTrue(audit.getCheckResultJson().contains("Hash match"));
    }

    @Test
    void testPerformBatchCheck_Corrupted() throws Exception {
        // Arrange
        FileContent file = new FileContent();
        file.setId("f2");
        file.setItemId("a2");
        file.setFileHash("original-hash");
        file.setStoragePath("/corrupt.pdf");

        Page<FileContent> mockPage = new Page<>();
        mockPage.setRecords(Collections.singletonList(file));

        when(fileContentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(mockPage);
            
        when(storageService.getInputStream("/corrupt.pdf")).thenReturn(new ByteArrayInputStream("tampered".getBytes()));
        when(fileHashService.hashFile(any(), any())).thenReturn("TAMPERED-HASH");

        // Act
        service.performBatchCheck(100);

        // Assert
        ArgumentCaptor<PreservationAudit> captor = ArgumentCaptor.forClass(PreservationAudit.class);
        verify(auditMapper).insert(captor.capture());
        
        PreservationAudit audit = captor.getValue();
        Assertions.assertEquals("CORRUPTED", audit.getOverallStatus());
        Assertions.assertTrue(audit.getCheckResultJson().contains("Hash mismatch"));
    }
}
