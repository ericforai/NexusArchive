package com.nexusarchive.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.LoginRequest;
import com.nexusarchive.dto.request.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 自动化测试：AdminUserController 用户管理
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdminUserControllerTest {

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
    public void testUserCrud() throws Exception {
        // 1. Create User
        CreateUserRequest createRequest = new CreateUserRequest();
        String username = "testuser_" + System.currentTimeMillis();
        createRequest.setUsername(username);
        createRequest.setPassword("Password123!");
        createRequest.setFullName("Test User");
        createRequest.setRoleIds(Collections.singletonList("role_system_admin")); // Assuming this role exists

        MvcResult createResult = mockMvc.perform(post("/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        // Note: AdminUserController returns ResponseEntity<UserResponse> directly, not wrapped in Result
        // But wait, AdminRoleController returns Result<T>. AdminUserController returns ResponseEntity<UserResponse>.
        // Let's check AdminUserController again. It returns ResponseEntity<UserResponse>.
        // So the JSON is the UserResponse object directly.
        String userId = objectMapper.readTree(createResponse).path("id").asText();
        assertThat(userId).isNotBlank();

        // 2. List Users
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Update User
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setId(userId);
        updateRequest.setFullName("Updated Name");
        updateRequest.setRoleIds(Collections.singletonList("role_system_admin"));

        mockMvc.perform(put("/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk());

        // 4. Delete User
        mockMvc.perform(delete("/admin/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent()); // 204 No Content
    }
}
