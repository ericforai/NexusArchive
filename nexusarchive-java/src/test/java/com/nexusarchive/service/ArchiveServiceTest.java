package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.DataScopeService.DataScopeContext;
import com.nexusarchive.service.strategy.ArchivalCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 档案服务健壮性测试
 * <p>
 * Tests robustness fixes: Race conditions, Validation errors.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ArchiveServiceTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArchivalCodeGenerator codeGenerator;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private ArchiveService archiveService;

    private Archive sampleArchive;

    @BeforeEach
    void setUp() {
        sampleArchive = new Archive();
        sampleArchive.setTitle("Test Archive");
        sampleArchive.setFondsNo("F001");
        sampleArchive.setFiscalYear("2023");
        sampleArchive.setOrgName("Test Org");
        sampleArchive.setRetentionPeriod("10Y");
    }

    @Test
    @DisplayName("Create Archive - Success Flow")
    void createArchive_Success() {
        // Arrange
        when(codeGenerator.generateNextCode(any())).thenReturn("A-2023-001");
        when(archiveMapper.selectCount(any())).thenReturn(0L); // Unique check passes
        when(archiveMapper.insert(any(Archive.class))).thenReturn(1);

        // Act
        Archive result = archiveService.createArchive(sampleArchive, "user-1");

        // Assert
        assertNotNull(result.getId());
        assertEquals("draft", result.getStatus());
        assertEquals("A-2023-001", result.getArchiveCode());
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    @DisplayName("Create Archive - Robustness: Handle Race Condition (DuplicateKeyException)")
    void createArchive_RaceCondition() {
        // Arrange
        sampleArchive.setArchiveCode("EXISTING-CODE");

        // Simulate race condition: selectCount returns 0 (looks unique), but insert fails
        when(archiveMapper.selectCount(any())).thenReturn(0L);
        doThrow(new DuplicateKeyException("Duplicate entry 'EXISTING-CODE'")).when(archiveMapper).insert(any(Archive.class));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.createArchive(sampleArchive, "user-1");
        });

        assertTrue(exception.getMessage().contains("档号或唯一标识已存在"));
        verify(archiveMapper).insert(any(Archive.class));
    }

    @Test
    @DisplayName("Update Archive - Integrity: Prevent Duplicate Code")
    void updateArchive_DuplicateCheck() {
        // Arrange
        String archiveId = "123";
        Archive existing = new Archive();
        existing.setId(archiveId);
        existing.setArchiveCode("OLD-CODE");

        Archive update = new Archive();
        update.setId(archiveId);
        update.setArchiveCode("NEW-CODE"); // Trying to change to this

        when(archiveMapper.selectById(archiveId)).thenReturn(existing);

        // Use DataScopeContext.all() or mock it properly
        when(dataScopeService.resolve()).thenReturn(DataScopeContext.all());
        when(dataScopeService.canAccessArchive(any(), any())).thenReturn(true);

        // Simulate that NEW-CODE already exists
        when(archiveMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            archiveService.updateArchive(archiveId, update);
        });

        assertEquals("档号已存在: NEW-CODE", exception.getMessage());

        // Fix ambiguous reference by specifying the class
        verify(archiveMapper, never()).updateById(any(Archive.class));
    }
}
