package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心功能集成测试 - 用户管理
 * 
 * 商业化上线前系统体检 Phase 1
 * 
 * @author 系统体检
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("核心功能测试 - 用户管理")
public class UserManagementIntegrationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static String adminToken;
    private static String createdUserId;

    @BeforeAll
    static void setup() throws Exception {
        // 获取管理员 Token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        
        String body = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            BASE_URL + "/auth/login", request, String.class
        );
        
        JsonNode json = objectMapper.readTree(response.getBody());
        adminToken = json.path("data").path("token").asText();
        System.out.println("✅ 管理员登录成功");
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(adminToken);
        return headers;
    }

    // ==================== 用户列表查询 ====================

    @Test
    @Order(1)
    @DisplayName("查询用户列表")
    void listUsers_shouldReturnUserList() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users?page=1&limit=10",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(200, json.path("code").asInt());
        assertNotNull(json.path("data").path("records"));
        System.out.println("✅ 查询用户列表成功，共 " + json.path("data").path("total").asInt() + " 条");
    }

    // ==================== 创建用户 ====================

    @Test
    @Order(10)
    @DisplayName("创建用户 - 成功")
    void createUser_withValidData_shouldSucceed() throws Exception {
        String uniqueUsername = "test_user_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"测试用户\",\"email\":\"test@example.com\"}",
            uniqueUsername
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            BASE_URL + "/admin/users",
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        createdUserId = json.path("data").path("id").asText();
        assertFalse(createdUserId.isEmpty());
        System.out.println("✅ 创建用户成功: " + uniqueUsername + " (ID: " + createdUserId + ")");
    }

    @Test
    @Order(11)
    @DisplayName("创建用户 - 重复用户名应失败")
    void createUser_withDuplicateUsername_shouldFail() {
        String body = "{\"username\":\"admin\",\"password\":\"Test@123!\",\"fullName\":\"重复用户\"}";
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期返回错误");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode().is4xxClientError());
            System.out.println("✅ 重复用户名被正确拒绝");
        }
    }

    @Test
    @Order(12)
    @DisplayName("创建用户 - 弱密码应失败")
    void createUser_withWeakPassword_shouldFail() {
        String uniqueUsername = "test_weak_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"123\",\"fullName\":\"弱密码用户\"}",
            uniqueUsername
        );
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期返回错误");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode().is4xxClientError());
            System.out.println("✅ 弱密码被正确拒绝");
        }
    }

    // ==================== 查询单个用户 ====================

    @Test
    @Order(20)
    @DisplayName("查询单个用户")
    void getUser_shouldReturnUserDetails() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users/" + createdUserId,
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 查询用户详情成功");
    }

    // ==================== 更新用户 ====================

    @Test
    @Order(30)
    @DisplayName("更新用户信息")
    void updateUser_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        String body = String.format(
            "{\"id\":\"%s\",\"fullName\":\"更新后的姓名\",\"email\":\"updated@example.com\"}",
            createdUserId
        );
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users/" + createdUserId,
            HttpMethod.PUT,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 更新用户成功");
    }

    // ==================== 重置密码 ====================

    @Test
    @Order(40)
    @DisplayName("重置用户密码")
    void resetPassword_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        String body = "{\"newPassword\":\"NewPass@456!\"}";
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users/" + createdUserId + "/reset-password",
            HttpMethod.POST,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 重置密码成功");
    }

    // ==================== 删除用户 ====================

    @Test
    @Order(90)
    @DisplayName("删除用户")
    void deleteUser_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdUserId != null, "需要先创建用户");
        
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users/" + createdUserId,
            HttpMethod.DELETE,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 删除用户成功");
    }

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("用户管理测试完成");
        System.out.println("========================================");
    }
}
