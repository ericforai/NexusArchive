// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: GlobalSearchControllerIntegrationTest 测试用例类
// Pos: 后端集成测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.GlobalSearchRequest;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试：GlobalSearchController 全局搜索控制器
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArchiveMapper archiveMapper;

    private String token;

    @BeforeEach
    void loginAndGetToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        token = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void testSearchEndpointWithoutAuth() throws Exception {
        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createSearchRequest("test"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testSearchEndpointWithValidQuery() throws Exception {
        // Given - Create test archive
        Archive archive = createTestArchive("TEST001", "Test Archive Title");
        archiveMapper.insert(archive);

        GlobalSearchRequest request = createSearchRequest("TEST001");

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));
    }

    @Test
    void testSearchEndpointWithEmptyQuery() throws Exception {
        // Given
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("");
        request.setPage(1);
        request.setPageSize(20);

        // When & Then
        MvcResult result = mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("\"total\":0");
        assertThat(response).contains("\"totalPages\":0");
    }

    @Test
    void testSearchEndpointWithValidationErrors() throws Exception {
        // Given - Request without query (validation should fail)
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setPage(1);
        request.setPageSize(20);

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchEndpointWithPagination() throws Exception {
        // Given - Create multiple test archives
        for (int i = 0; i < 25; i++) {
            Archive archive = createTestArchive("PAGE" + i, "Pagination Test " + i);
            archiveMapper.insert(archive);
        }

        // First page
        GlobalSearchRequest request1 = new GlobalSearchRequest();
        request1.setQuery("Pagination");
        request1.setPage(1);
        request1.setPageSize(10);

        MvcResult result1 = mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(25))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.isFirst").value(true))
                .andExpect(jsonPath("$.data.isLast").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false))
                .andReturn();

        String response1 = result1.getResponse().getContentAsString();
        int items1 = objectMapper.readTree(response1).path("data").path("items").size();
        assertThat(items1).isEqualTo(10);

        // Second page
        GlobalSearchRequest request2 = new GlobalSearchRequest();
        request2.setQuery("Pagination");
        request2.setPage(2);
        request2.setPageSize(10);

        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.isFirst").value(false))
                .andExpect(jsonPath("$.data.isLast").value(false));

        // Last page (third page with 5 items)
        GlobalSearchRequest request3 = new GlobalSearchRequest();
        request3.setQuery("Pagination");
        request3.setPage(3);
        request3.setPageSize(10);

        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(3))
                .andExpect(jsonPath("$.data.isLast").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void testSearchEndpointWithDateRange() throws Exception {
        // Given - Create archives with different dates
        Archive archive1 = createTestArchiveWithDate("DATE001", "Archive 2024", LocalDate.of(2024, 6, 15));
        Archive archive2 = createTestArchiveWithDate("DATE002", "Archive 2023", LocalDate.of(2023, 6, 15));
        archiveMapper.insert(archive1);
        archiveMapper.insert(archive2);

        // Search with date range filter
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("Archive");
        request.setStartDate(LocalDate.of(2024, 1, 1));
        request.setEndDate(LocalDate.of(2024, 12, 31));
        request.setPage(1);
        request.setPageSize(20);

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void testSearchEndpointWithMatchTypeFilter() throws Exception {
        // Given
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("TEST");
        request.setMatchType("ARCHIVE");
        request.setPage(1);
        request.setPageSize(20);

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void testLegacyGetSearchEndpoint() throws Exception {
        // Given - Create test archive
        Archive archive = createTestArchive("LEGACY001", "Legacy Test");
        archiveMapper.insert(archive);

        // When & Then - Old GET endpoint should still work
        mockMvc.perform(get("/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "LEGACY001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testPageSizeValidation() throws Exception {
        // Given - Page size exceeds maximum (100)
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("test");
        request.setPage(1);
        request.setPageSize(150); // Exceeds max of 100

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPageNumberValidation() throws Exception {
        // Given - Page number less than 1
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery("test");
        request.setPage(0); // Invalid
        request.setPageSize(20);

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSearchWithNonExistentKeyword() throws Exception {
        // Given
        GlobalSearchRequest request = createSearchRequest(UUID.randomUUID().toString());

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void testResponseStructure() throws Exception {
        // Given
        Archive archive = createTestArchive("STRUCT001", "Structure Test");
        archiveMapper.insert(archive);

        GlobalSearchRequest request = createSearchRequest("STRUCT001");

        // When & Then
        mockMvc.perform(post("/search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.page").isNumber())
                .andExpect(jsonPath("$.data.pageSize").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber())
                .andExpect(jsonPath("$.data.hasNext").isBoolean())
                .andExpect(jsonPath("$.data.hasPrevious").isBoolean())
                .andExpect(jsonPath("$.data.isFirst").isBoolean())
                .andExpect(jsonPath("$.data.isLast").isBoolean());
    }

    // Helper methods

    private GlobalSearchRequest createSearchRequest(String query) {
        GlobalSearchRequest request = new GlobalSearchRequest();
        request.setQuery(query);
        request.setPage(1);
        request.setPageSize(20);
        return request;
    }

    private Archive createTestArchive(String code, String title) {
        Archive archive = new Archive();
        archive.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        archive.setArchiveCode(code);
        archive.setTitle(title);
        archive.setFondsNo("TEST_FONDS");
        archive.setCategoryCode("Voucher");
        archive.setAmount(BigDecimal.ZERO);
        archive.setFiscalYear("2024");
        archive.setStatus("archived");
        archive.setCreatedTime(java.time.LocalDateTime.now());
        return archive;
    }

    private Archive createTestArchiveWithDate(String code, String title, LocalDate docDate) {
        Archive archive = createTestArchive(code, title);
        archive.setDocDate(docDate);
        return archive;
    }
}
