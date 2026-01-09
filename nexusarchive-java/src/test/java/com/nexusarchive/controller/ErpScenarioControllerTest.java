// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: ErpScenarioControllerTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.service.ErpScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErpScenarioController 测试
 * <p>
 * 测试分页查询功能
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ERP场景控制器测试")
@Tag("integration")
class ErpScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ErpScenarioService erpScenarioService;

    private String token;

    @BeforeEach
    public void loginAndGetToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        token = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    @DisplayName("分页查询场景列表 - 默认参数")
    void listByConfig_DefaultParameters() throws Exception {
        // Given
        Long configId = 1L;
        List<ErpScenario> scenarios = createTestScenarios(20);
        Page<ErpScenario> mockPage = new Page<>(1, 20, 20);
        mockPage.setRecords(scenarios);

        when(erpScenarioService.listScenariosByConfigIdPage(eq(configId), any()))
                .thenReturn(mockPage);

        // When
        MvcResult result = mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("\"current\":1");
        assertThat(response).contains("\"size\":20");
        assertThat(response).contains("\"total\":20");
    }

    @Test
    @DisplayName("分页查询场景列表 - 自定义页码和页大小")
    void listByConfig_CustomPageParameters() throws Exception {
        // Given
        Long configId = 1L;
        List<ErpScenario> scenarios = createTestScenarios(5);
        Page<ErpScenario> mockPage = new Page<>(2, 5, 25);
        mockPage.setRecords(scenarios);

        when(erpScenarioService.listScenariosByConfigIdPage(eq(configId), any()))
                .thenReturn(mockPage);

        // When
        MvcResult result = mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("\"current\":2");
        assertThat(response).contains("\"size\":5");
        assertThat(response).contains("\"total\":25");
    }

    @Test
    @DisplayName("分页查询场景列表 - 最小页码")
    void listByConfig_MinPageNum() throws Exception {
        // Given
        Long configId = 1L;
        Page<ErpScenario> mockPage = new Page<>(1, 10, 10);
        mockPage.setRecords(createTestScenarios(10));

        when(erpScenarioService.listScenariosByConfigIdPage(eq(configId), any()))
                .thenReturn(mockPage);

        // When & Then
        mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("分页查询场景列表 - 无效页码（小于1）")
    void listByConfig_InvalidPageNum_TooSmall() throws Exception {
        // Given
        Long configId = 1L;

        // When & Then
        mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("分页查询场景列表 - 无效页大小（超过最大值）")
    void listByConfig_InvalidPageSize_TooLarge() throws Exception {
        // Given
        Long configId = 1L;

        // When & Then
        mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("分页查询场景列表 - 无效页大小（小于1）")
    void listByConfig_InvalidPageSize_TooSmall() throws Exception {
        // Given
        Long configId = 1L;

        // When & Then
        mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .header("Authorization", "Bearer " + token)
                        .param("pageNum", "1")
                        .param("pageSize", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("分页查询场景列表 - 未授权访问")
    void listByConfig_Unauthorized() throws Exception {
        // Given
        Long configId = 1L;

        // When & Then
        mockMvc.perform(get("/erp/scenario/list/" + configId)
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 创建测试场景数据
     */
    private List<ErpScenario> createTestScenarios(int count) {
        List<ErpScenario> scenarios = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ErpScenario scenario = new ErpScenario();
            scenario.setId((long) i);
            scenario.setConfigId(1L);
            scenario.setScenarioKey("SCENARIO_" + i);
            scenario.setName("场景" + i);
            scenario.setDescription("测试场景描述" + i);
            scenario.setIsActive(true);
            scenario.setSyncStrategy("MANUAL");
            scenario.setLastSyncStatus("SUCCESS");
            scenario.setLastSyncTime(LocalDateTime.now());
            scenario.setCreatedTime(LocalDateTime.now());
            scenario.setLastModifiedTime(LocalDateTime.now());
            scenarios.add(scenario);
        }
        return scenarios;
    }
}
