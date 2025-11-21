package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ArchiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private String token;

    @BeforeEach
    public void setup() {
        // 生成测试Token
        token = jwtUtil.generateToken("admin", "user_admin");
    }

    @Test
    public void testArchiveCrud() throws Exception {
        String archiveCode = "ARC_" + System.currentTimeMillis();
        
        // 1. 创建档案
        Archive archive = new Archive();
        archive.setFondsNo("F001");
        archive.setArchiveCode(archiveCode);
        archive.setCategoryCode("C001");
        archive.setTitle("Test Archive");
        archive.setFiscalYear("2023");
        archive.setRetentionPeriod("10Y");
        archive.setOrgName("Test Org");
        
        String content = mockMvc.perform(post("/api/archives")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archive)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("Test Archive"))
                .andReturn().getResponse().getContentAsString();
        
        String archiveId = objectMapper.readTree(content).get("data").get("id").asText();

        // 2. 获取档案列表
        mockMvc.perform(get("/api/archives")
                        .header("Authorization", "Bearer " + token)
                        .param("search", archiveCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(archiveId));

        // 3. 更新档案
        archive.setTitle("Updated Archive Title");
        mockMvc.perform(put("/api/archives/" + archiveId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(archive)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 获取单个档案
        mockMvc.perform(get("/api/archives/" + archiveId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Archive Title"));

        // 5. 删除档案
        mockMvc.perform(delete("/api/archives/" + archiveId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
