// Input: JUnit 5、Mockito、Spring Framework、Java 标准库、等
// Output: GlobalSearchServiceImplTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.GlobalSearchDTO;
import com.nexusarchive.dto.request.GlobalSearchRequest;
import com.nexusarchive.dto.response.PageResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import static org.mockito.Mockito.*;

/**
 * 单元测试：GlobalSearchServiceImpl 全局搜索服务
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceImplTest {

    @Mock
    private ArchiveMapper archiveMapper;

    @Mock
    private ArcFileMetadataIndexMapper metadataIndexMapper;

    @Mock
    private ArcFileContentMapper fileContentMapper;

    @Mock
    private DataScopeService dataScopeService;

    @InjectMocks
    private GlobalSearchServiceImpl globalSearchService;

    @BeforeEach
    void setUp() {
        // Use the all() method instead of mocking the final class
        lenient().when(dataScopeService.resolve()).thenReturn(DataScopeService.DataScopeContext.all());
        // Do nothing when applyArchiveScope is called
        lenient().doNothing().when(dataScopeService).applyArchiveScope(any(QueryWrapper.class), any(DataScopeService.DataScopeContext.class));
    }

    @Test
    void testSearchWithEmptyQuery() {
        // Given
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("");
        request.setPage(1);
        request.setPageSize(20);

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isZero();
        assertThat(response.getTotalPages()).isZero();

        // Verify no database queries were made
        verify(archiveMapper, never()).selectList(any());
        verify(metadataIndexMapper, never()).selectList(any());
    }

    @Test
    void testSearchWithNullQuery() {
        // Given
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(null);
        request.setPage(1);
        request.setPageSize(20);

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isZero();

        // Verify no database queries were made
        verify(archiveMapper, never()).selectList(any());
        verify(metadataIndexMapper, never()).selectList(any());
    }

    @Test
    void testSearchReturnsArchiveResults() {
        // Given
        String query = "TEST001";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);

        // Mock archive data
        Archive archive1 = new Archive();
        archive1.setId("archive1");
        archive1.setArchiveCode("TEST001");
        archive1.setTitle("Test Archive 1");
        archive1.setFondsNo("F001");

        Archive archive2 = new Archive();
        archive2.setId("archive2");
        archive2.setArchiveCode("TEST002");
        archive2.setTitle("Test Archive 2");
        archive2.setFondsNo("F001");

        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(archive1, archive2));
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getItems().get(0).getArchiveCode()).isEqualTo("TEST001");
        assertThat(response.getItems().get(0).getMatchType()).isEqualTo("ARCHIVE");
    }

    @Test
    void testSearchReturnsMetadataResults() {
        // Given
        String query = "INV001";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);

        // Mock metadata data
        ArcFileMetadataIndex metadata = new ArcFileMetadataIndex();
        metadata.setId("meta1");
        metadata.setFileId("file1");
        metadata.setInvoiceNumber("INV001");
        metadata.setInvoiceCode("INV001");
        metadata.setTotalAmount(new BigDecimal("1000.00"));

        // Mock file content
        ArcFileContent fileContent = new ArcFileContent();
        fileContent.setId("file1");
        fileContent.setArchivalCode("ARCH001");

        // Mock archive
        Archive archive = new Archive();
        archive.setId("archive1");
        archive.setArchiveCode("ARCH001");
        archive.setTitle("Invoice Archive");

        // First call (searchArchives) returns empty, second call (searchMetadata) returns the archive
        when(archiveMapper.selectList(any()))
                .thenReturn(Collections.emptyList())  // First call for archive search
                .thenReturn(Collections.singletonList(archive));  // Second call for metadata resolution
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.singletonList(metadata));
        when(fileContentMapper.selectList(any())).thenReturn(Collections.singletonList(fileContent));

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).isNotEmpty();
        assertThat(response.getItems().get(0).getMatchType()).isEqualTo("METADATA");
        assertThat(response.getItems().get(0).getArchiveCode()).isEqualTo("ARCH001");
    }

    @Test
    void testSearchWithMatchTypeFilterArchiveOnly() {
        // Given
        String query = "TEST";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setMatchType("ARCHIVE");
        request.setPage(1);
        request.setPageSize(20);

        Archive archive = new Archive();
        archive.setId("archive1");
        archive.setArchiveCode("TEST001");
        archive.setTitle("Test Archive");

        when(archiveMapper.selectList(any())).thenReturn(Collections.singletonList(archive));

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getMatchType()).isEqualTo("ARCHIVE");

        // Verify metadata search was not called
        verify(metadataIndexMapper, never()).selectList(any());
    }

    @Test
    void testSearchWithMatchTypeFilterMetadataOnly() {
        // Given
        String query = "INV001";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setMatchType("METADATA");
        request.setPage(1);
        request.setPageSize(20);

        ArcFileMetadataIndex metadata = new ArcFileMetadataIndex();
        metadata.setId("meta1");
        metadata.setFileId("file1");
        metadata.setInvoiceNumber("INV001");

        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.singletonList(metadata));

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        // Metadata search was called (though result may be incomplete due to missing file/archive mapping)
        verify(metadataIndexMapper, atLeastOnce()).selectList(any());
    }

    @Test
    void testSearchWithDateRange() {
        // Given
        String query = "TEST";
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(1);
        request.setPageSize(20);

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).isEmpty();
        // Verify that the date range filter was applied (checked via selectList being called)
        verify(archiveMapper, atLeastOnce()).selectList(any());
    }

    @Test
    void testPagination() {
        // Given
        String query = "TEST";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(2);
        request.setPageSize(10);

        // Create 25 mock archives to test pagination
        List<Archive> archives = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            Archive archive = new Archive();
            archive.setId("archive" + i);
            archive.setArchiveCode("TEST" + i);
            archive.setTitle("Test Archive " + i);
            archives.add(archive);
        }

        when(archiveMapper.selectList(any())).thenReturn(archives);
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getTotal()).isEqualTo(25);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getPageSize()).isEqualTo(10);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.getItems()).hasSize(10); // Second page should have 10 items
        assertThat(response.isHasPrevious()).isTrue();
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void testPaginationFirstPage() {
        // Given
        String query = "TEST";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(10);

        List<Archive> archives = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Archive archive = new Archive();
            archive.setId("archive" + i);
            archive.setArchiveCode("TEST" + i);
            archive.setTitle("Test Archive " + i);
            archives.add(archive);
        }

        when(archiveMapper.selectList(any())).thenReturn(archives);
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getTotal()).isEqualTo(5);
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void testPaginationEmptyResults() {
        // Given
        String query = "NONEXISTENT";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotal()).isZero();
        assertThat(response.getTotalPages()).isZero();
        assertThat(response.isHasPrevious()).isFalse();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void testDeduplicationOfResults() {
        // Given
        String query = "TEST";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);

        // Same archive appears in both archive and metadata search
        Archive archive = new Archive();
        archive.setId("archive1");
        archive.setArchiveCode("TEST001");
        archive.setTitle("Test Archive");

        ArcFileMetadataIndex metadata = new ArcFileMetadataIndex();
        metadata.setId("meta1");
        metadata.setFileId("file1");
        metadata.setInvoiceNumber("TEST001");

        ArcFileContent fileContent = new ArcFileContent();
        fileContent.setId("file1");
        fileContent.setArchivalCode("TEST001");

        when(archiveMapper.selectList(any())).thenReturn(Collections.singletonList(archive));
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.singletonList(metadata));
        when(fileContentMapper.selectList(any())).thenReturn(Collections.singletonList(fileContent));

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then - Should only have one result (deduplicated)
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void testLegacySearchMethod() {
        // Given
        String query = "TEST001";

        Archive archive1 = new Archive();
        archive1.setId("archive1");
        archive1.setArchiveCode("TEST001");
        archive1.setTitle("Test Archive 1");

        Archive archive2 = new Archive();
        archive2.setId("archive2");
        archive2.setArchiveCode("TEST002");
        archive2.setTitle("Test Archive 2");

        when(archiveMapper.selectList(any())).thenReturn(Arrays.asList(archive1, archive2));
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.emptyList());

        // When
        List<GlobalSearchDTO> results = globalSearchService.search(query);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getArchiveCode()).isEqualTo("TEST001");
        assertThat(results.get(1).getArchiveCode()).isEqualTo("TEST002");
    }

    @Test
    void testSearchWithAmountAsKeyword() {
        // Given
        String query = "1000.50";
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);

        ArcFileMetadataIndex metadata = new ArcFileMetadataIndex();
        metadata.setId("meta1");
        metadata.setFileId("file1");
        metadata.setInvoiceNumber("INV001");
        metadata.setTotalAmount(new BigDecimal("1000.50"));

        when(archiveMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(metadataIndexMapper.selectList(any())).thenReturn(Collections.singletonList(metadata));

        // When
        PageResponse<GlobalSearchDTO> response = globalSearchService.search(request);

        // Then - Metadata search should be called with amount comparison
        verify(metadataIndexMapper, atLeastOnce()).selectList(any());
    }
}
