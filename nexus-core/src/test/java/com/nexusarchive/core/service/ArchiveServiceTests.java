// Input: ArchiveService
// Output: Test Results
// Pos: NexusCore service/test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.domain.FileContent;
import com.nexusarchive.core.domain.PreservationAudit;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import com.nexusarchive.core.mapper.FileContentMapper;
import com.nexusarchive.core.mapper.PreservationAuditMapper;
import com.nexusarchive.core.preservation.CheckResult;
import com.nexusarchive.core.preservation.FourNaturesService;
import com.nexusarchive.core.service.impl.DefaultArchiveService;
import com.nexusarchive.core.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveServiceTests {

    private DefaultArchiveService archiveService;

    @Mock private ArchiveObjectMapper archiveMapper;
    @Mock private FileContentMapper fileContentMapper;
    @Mock private PreservationAuditMapper auditMapper;
    @Mock private FourNaturesService fourNaturesService;
    @Mock private StorageService storageService;
    @Mock private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        archiveService = new DefaultArchiveService(
                archiveMapper, fileContentMapper, auditMapper, 
                fourNaturesService, storageService, objectMapper
        );
    }

    @Test
    void testArchiveSuccess() throws Exception {
        String archiveId = "test-archive-1";
        
        ArchiveObject archive = new ArchiveObject();
        archive.setId(archiveId);
        archive.setStatus("DRAFT");
        
        FileContent file = new FileContent();
        file.setId("file-1");
        file.setItemId(archiveId);
        file.setStoragePath("/path/to/file");
        file.setFileName("test.pdf");

        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        when(fileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
        when(storageService.exists(anyString())).thenReturn(true);
        when(storageService.getInputStream(anyString())).thenReturn(new ByteArrayInputStream("content".getBytes()));
        
        when(fourNaturesService.validate(any(), any(Path.class)))
                .thenReturn(List.of(CheckResult.pass("MOCK", "Passed")));

        archiveService.archive(archiveId);

        verify(archiveMapper).updateById(any(ArchiveObject.class));
        verify(auditMapper).insert(any(PreservationAudit.class));
        Assertions.assertEquals("ARCHIVED", archive.getStatus());
    }

    @Test
    void testArchiveCorrupted() throws Exception {
        String archiveId = "test-archive-2";
        
        ArchiveObject archive = new ArchiveObject();
        archive.setId(archiveId);
        
        FileContent file = new FileContent();
        file.setId("file-2");
        file.setItemId(archiveId);
        file.setStoragePath("/path/to/bad-file");

        when(archiveMapper.selectById(archiveId)).thenReturn(archive);
        when(fileContentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(file));
        when(storageService.exists(anyString())).thenReturn(true);
        when(storageService.getInputStream(anyString())).thenReturn(new ByteArrayInputStream("content".getBytes()));
        
        when(fourNaturesService.validate(any(), any(Path.class)))
                .thenReturn(List.of(CheckResult.fail("INTEGRITY", "Hash check failed", null)));

        archiveService.archive(archiveId);

        verify(archiveMapper).updateById(any(ArchiveObject.class));
        verify(auditMapper).insert(any(PreservationAudit.class));
        Assertions.assertEquals("CORRUPTED", archive.getStatus());
    }
}
