// Input: Jackson、org.junit、Spring Framework、static org.assertj、等
// Output: AdminRoleControllerTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 自动化测试：AdminRoleController 角色管理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AdminRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    public void loginAndGetToken() throws Exception {
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
    public void testRoleCrud() throws Exception {
        // 1. Create Role
        Role role = new Role();
        role.setName("Test Role");
        role.setCode("role_test_" + System.currentTimeMillis());
        role.setRoleCategory("BUSINESS_USER");
        role.setDescription("Test Description");
        
        MvcResult createResult = mockMvc.perform(post("/admin/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andReturn();
        
        String createResponse = createResult.getResponse().getContentAsString();
        String roleId = objectMapper.readTree(createResponse).path("data").path("id").asText();
        assertThat(roleId).isNotBlank();

        // 2. Get Role
        mockMvc.perform(get("/admin/roles/" + roleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Update Role
        role.setName("Updated Role Name");
        mockMvc.perform(put("/admin/roles/" + roleId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk());

        // 4. List Roles
        mockMvc.perform(get("/admin/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 5. Delete Role
        mockMvc.perform(delete("/admin/roles/" + roleId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
