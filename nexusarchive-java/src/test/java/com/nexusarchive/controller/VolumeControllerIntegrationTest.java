package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VolumeController 集成测试
 * 测试完整 API 流程，包括数据库交互
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional  // 每个测试后自动回滚
class VolumeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArchiveMapper archiveMapper;

    @Autowired
    private VolumeMapper volumeMapper;

    private static String testVolumeId;
    private static final String TEST_PERIOD = "2025-10";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        cleanupTestData();
        
        // 插入测试凭证
        insertTestArchives();
    }

    private void cleanupTestData() {
        // 删除测试期间的案卷和凭证
        volumeMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Volume>()
                .like(Volume::getVolumeCode, "TEST"));
    }

    private void insertTestArchives() {
        for (int i = 1; i <= 3; i++) {
            Archive archive = new Archive();
            archive.setId(UUID.randomUUID().toString().replace("-", ""));
            archive.setArchiveCode("TEST-" + TEST_PERIOD + "-记-" + i);
            archive.setTitle("测试凭证-" + i);
            archive.setFondsNo("TEST01");
            archive.setFiscalYear("2025");
            archive.setFiscalPeriod(TEST_PERIOD);
            archive.setCategoryCode("AC01");
            archive.setOrgName("集成测试公司");
            archive.setCreator("测试员" + i);
            archive.setRetentionPeriod(i == 1 ? "30Y" : "10Y");
            archive.setAmount(new BigDecimal(i * 100));
            archive.setDocDate(LocalDate.of(2025, 10, i));
            archive.setStatus("draft");
            archive.setSecurityLevel("internal");
            archive.setCreatedBy("system");
            archive.setDeleted(0);
            archive.setCreatedTime(LocalDateTime.now());
            archive.setLastModifiedTime(LocalDateTime.now());
            archiveMapper.insert(archive);
        }
    }

    @Test
    @Order(1)
    @DisplayName("集成测试: 按月组卷 API")
    void testAssembleByMonth() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("fiscalPeriod", TEST_PERIOD);

        MvcResult result = mockMvc.perform(post("/volumes/assemble")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("组卷成功"))
                .andExpect(jsonPath("$.data.volumeCode").value("TEST01-AC01-202510"))
                .andExpect(jsonPath("$.data.title").value("集成测试公司2025年10月会计凭证"))
                .andExpect(jsonPath("$.data.fileCount").value(3))
                .andExpect(jsonPath("$.data.retentionPeriod").value("30Y"))  // 取最长
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn();

        // 保存案卷ID供后续测试使用
        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        testVolumeId = (String) data.get("id");
    }

    @Test
    @Order(2)
    @DisplayName("集成测试: 获取案卷列表 API")
    void testGetVolumeList() throws Exception {
        // 先创建一个案卷
        createTestVolume();

        mockMvc.perform(get("/volumes")
                        .param("page", "1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @Order(3)
    @DisplayName("集成测试: 获取案卷详情 API")
    void testGetVolumeDetail() throws Exception {
        String volumeId = createTestVolume();

        mockMvc.perform(get("/volumes/" + volumeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(volumeId))
                .andExpect(jsonPath("$.data.volumeCode").isNotEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("集成测试: 获取卷内文件列表 API")
    void testGetVolumeFiles() throws Exception {
        String volumeId = createTestVolume();

        mockMvc.perform(get("/volumes/" + volumeId + "/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(3)));  // 3条测试凭证
    }

    @Test
    @Order(5)
    @DisplayName("集成测试: 提交审核 API")
    void testSubmitForReview() throws Exception {
        String volumeId = createTestVolume();

        mockMvc.perform(post("/volumes/" + volumeId + "/submit-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已提交审核"));

        // 验证状态变更
        Volume volume = volumeMapper.selectById(volumeId);
        Assertions.assertEquals("pending", volume.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("集成测试: 审核归档 API")
    void testApproveArchival() throws Exception {
        String volumeId = createTestVolume();
        
        // 先提交审核
        Volume volume = volumeMapper.selectById(volumeId);
        volume.setStatus("pending");
        volumeMapper.updateById(volume);

        mockMvc.perform(post("/volumes/" + volumeId + "/approve")
                        .param("reviewerId", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("归档成功"));

        // 验证案卷状态
        Volume updatedVolume = volumeMapper.selectById(volumeId);
        Assertions.assertEquals("archived", updatedVolume.getStatus());
        Assertions.assertNotNull(updatedVolume.getArchivedAt());
        Assertions.assertEquals("admin", updatedVolume.getReviewedBy());
    }

    @Test
    @Order(7)
    @DisplayName("集成测试: 审核驳回 API")
    void testRejectArchival() throws Exception {
        String volumeId = createTestVolume();
        
        // 先提交审核
        Volume volume = volumeMapper.selectById(volumeId);
        volume.setStatus("pending");
        volumeMapper.updateById(volume);

        Map<String, String> request = new HashMap<>();
        request.put("reviewerId", "admin");
        request.put("reason", "凭证信息不完整");

        mockMvc.perform(post("/volumes/" + volumeId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已驳回"));

        // 验证状态回退
        Volume updatedVolume = volumeMapper.selectById(volumeId);
        Assertions.assertEquals("draft", updatedVolume.getStatus());
    }

    @Test
    @Order(8)
    @DisplayName("集成测试: 获取归档登记表 API")
    void testGetRegistrationForm() throws Exception {
        String volumeId = createTestVolume();

        mockMvc.perform(get("/volumes/" + volumeId + "/registration-form"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.volumeCode").isNotEmpty())
                .andExpect(jsonPath("$.data.volumeTitle").isNotEmpty())
                .andExpect(jsonPath("$.data.fondsNo").value("TEST01"))
                .andExpect(jsonPath("$.data.categoryCode").value("AC01"))
                .andExpect(jsonPath("$.data.categoryName").value("会计凭证"))
                .andExpect(jsonPath("$.data.fileCount").value(3))
                .andExpect(jsonPath("$.data.fileList").isArray())
                .andExpect(jsonPath("$.data.fileList", hasSize(3)));
    }

    @Test
    @Order(9)
    @DisplayName("集成测试: 重复组卷应失败")
    void testAssembleByMonth_AlreadyAssembled() throws Exception {
        // 先组卷一次
        createTestVolume();

        // 再次组卷应该失败（无待组卷凭证）
        Map<String, String> request = new HashMap<>();
        request.put("fiscalPeriod", TEST_PERIOD);

        mockMvc.perform(post("/volumes/assemble")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());  // BusinessException returns 400
    }

    @Test
    @Order(10)
    @DisplayName("集成测试: 完整工作流 - 同步到归档")
    void testFullWorkflow() throws Exception {
        // Step 1: 组卷
        Map<String, String> assembleRequest = new HashMap<>();
        assembleRequest.put("fiscalPeriod", TEST_PERIOD);

        MvcResult assembleResult = mockMvc.perform(post("/volumes/assemble")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assembleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andReturn();

        String response = assembleResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        String volumeId = (String) data.get("id");

        // Step 2: 提交审核
        mockMvc.perform(post("/volumes/" + volumeId + "/submit-review"))
                .andExpect(status().isOk());

        // 验证状态
        mockMvc.perform(get("/volumes/" + volumeId))
                .andExpect(jsonPath("$.data.status").value("pending"));

        // Step 3: 审核归档
        mockMvc.perform(post("/volumes/" + volumeId + "/approve")
                        .param("reviewerId", "admin"))
                .andExpect(status().isOk());

        // Step 4: 验证最终状态
        mockMvc.perform(get("/volumes/" + volumeId))
                .andExpect(jsonPath("$.data.status").value("archived"))
                .andExpect(jsonPath("$.data.reviewedBy").value("admin"))
                .andExpect(jsonPath("$.data.archivedAt").isNotEmpty());

        // Step 5: 验证卷内凭证也已归档
        mockMvc.perform(get("/volumes/" + volumeId + "/files"))
                .andExpect(jsonPath("$.data[0].status").value("archived"))
                .andExpect(jsonPath("$.data[1].status").value("archived"))
                .andExpect(jsonPath("$.data[2].status").value("archived"));

        // Step 6: 生成归档登记表
        mockMvc.perform(get("/volumes/" + volumeId + "/registration-form"))
                .andExpect(jsonPath("$.data.status").value("archived"))
                .andExpect(jsonPath("$.data.fileList", hasSize(3)));
    }

    /**
     * 辅助方法: 创建测试案卷
     */
    private String createTestVolume() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("fiscalPeriod", TEST_PERIOD);

        MvcResult result = mockMvc.perform(post("/volumes/assemble")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
        return (String) data.get("id");
    }
}
