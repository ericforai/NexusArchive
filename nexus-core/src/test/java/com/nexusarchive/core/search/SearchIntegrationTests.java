// Input: Spring Boot Test, MockMvc, H2
// Output: Search/Masking Integration Test
// Pos: NexusCore test
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.core.domain.ArchiveObject;
import com.nexusarchive.core.masking.DataMaskingProperties;
import com.nexusarchive.core.mapper.ArchiveObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArchiveObjectMapper archiveMapper;

    @Autowired
    private DataMaskingProperties maskingProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Setup data
        ArchiveObject a1 = new ArchiveObject();
        a1.setId("A001");
        a1.setFondsNo("F001");
        a1.setArchiveYear("2025");
        a1.setAmount(new BigDecimal("1000.00"));
        a1.setDocDate(LocalDate.of(2025, 1, 15));
        a1.setCounterparty("Google");
        a1.setVoucherNo("V-1001");
        a1.setInvoiceNo("INV-2025-001");
        a1.setTitle("Server Purchase");
        a1.setCreatedTime(LocalDateTime.now());
        archiveMapper.insert(a1);

        ArchiveObject a2 = new ArchiveObject();
        a2.setId("A002");
        a2.setFondsNo("F001");
        a2.setArchiveYear("2025");
        a2.setAmount(new BigDecimal("500.00"));
        a2.setDocDate(LocalDate.of(2025, 2, 20));
        a2.setCounterparty("Apple");
        a2.setVoucherNo("V-1002");
        a2.setInvoiceNo("INV-2025-002");
        a2.setTitle("Software License");
        a2.setCreatedTime(LocalDateTime.now());
        archiveMapper.insert(a2);
        
        // Setup masking rules in-memory for testing
        DataMaskingProperties.MaskingRule rule = new DataMaskingProperties.MaskingRule();
        rule.setFieldMatch("counterparty");
        rule.setPattern(DataMaskingProperties.MaskPattern.MIDDLE_4);
        maskingProperties.setRules(java.util.Collections.singletonList(rule));
        maskingProperties.setEnabled(true);
    }

    @Test
    void search_shouldReturnFilteredAndMaskedResults() throws Exception {
        // Search request: amount > 800 (should find A001)
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setAmountFrom(new BigDecimal("800.00"));

        mockMvc.perform(post("/api/v1/archives/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].id", is("A001")))
                // Verify Masking: "Google" (6 chars) -> MIDDLE_4 -> G****e 
                // wait, 6 chars len - 4 = 2. start = 1. "G" + "****" + "e" -> "G****e"
                .andExpect(jsonPath("$.records[0].counterparty", is("G****e"))); 
    }
    
    @Test
    void search_byDateRange() throws Exception {
        // Search request: 2025-02-01 to 2025-02-28 (should find A002)
        ArchiveSearchRequest request = new ArchiveSearchRequest();
        request.setDateFrom(LocalDate.of(2025, 2, 1));
        request.setDateTo(LocalDate.of(2025, 2, 28));

        mockMvc.perform(post("/api/v1/archives/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records", hasSize(1)))
                .andExpect(jsonPath("$.records[0].id", is("A002")));
    }
}
