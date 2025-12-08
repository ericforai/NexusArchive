package com.nexusarchive.controller;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.AuditInspectionLogMapper;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.ComplianceCheckService;
import com.nexusarchive.service.StandardReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(ComplianceController.class)
@WithMockUser
public class ComplianceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplianceCheckService complianceCheckService;

    @MockBean
    private ArchiveService archiveService;

    @MockBean
    private ArcFileContentMapper arcFileContentMapper;

    @MockBean
    private AuditInspectionLogMapper auditInspectionLogMapper;
    
    @MockBean
    private StandardReportGenerator standardReportGenerator;

    @MockBean
    private com.nexusarchive.util.JwtUtil jwtUtil;

    @MockBean
    private com.nexusarchive.service.TokenBlacklistService tokenBlacklistService;

    @MockBean
    private com.nexusarchive.service.CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        Archive archive = new Archive();
        archive.setId("test-archive-id");
        archive.setArchiveCode("Z001-2024-30-CW-AC01-0001");
        archive.setCategoryCode("AC01");
        archive.setAmount(new BigDecimal("100.00"));
        archive.setDocDate(LocalDate.of(2024, 1, 1));
        
        when(archiveService.getArchiveById("test-archive-id")).thenReturn(archive);
        
        ArcFileContent file = new ArcFileContent();
        file.setId("file-1");
        file.setItemId("test-archive-id");
        List<ArcFileContent> files = Collections.singletonList(file);
        
        when(arcFileContentMapper.selectList(any())).thenReturn(files);
        
        ComplianceCheckService.ComplianceResult result = new ComplianceCheckService.ComplianceResult();
        // Assume compliant for this test
        when(complianceCheckService.checkCompliance(any(), any())).thenReturn(result);
    }

    @Test
    @DisplayName("Test Check Archive Compliance Endpoint")
    void testCheckArchiveCompliance() throws Exception {
        mockMvc.perform(get("/api/compliance/archives/test-archive-id")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Test Get Compliance Report XML")
    void testGetComplianceReportXml() throws Exception {
        mockMvc.perform(get("/api/compliance/archives/test-archive-id/report?format=xml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("<?xml")));
    }
    
    @Test
    @DisplayName("Test Get Compliance Report JSON")
    void testGetComplianceReportJson() throws Exception {
        mockMvc.perform(get("/api/compliance/archives/test-archive-id/report?format=json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("archiveId")));
    }
    
    @Test
    @DisplayName("Test Get Statistics Endpoint")
    void testGetStatistics() throws Exception {
        when(auditInspectionLogMapper.selectCount(any())).thenReturn(10L);
        
        mockMvc.perform(get("/api/compliance/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalArchives").value(10));
    }
}
