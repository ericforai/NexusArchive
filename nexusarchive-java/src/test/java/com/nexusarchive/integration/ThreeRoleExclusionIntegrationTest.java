// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: ThreeRoleExclusionIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三员互斥集成测试
 * 
 * 验证系统管理员、安全管理员、审计管理员三种角色的互斥性
 * 
 * 前置条件：
 * 1. 后端服务必须在 localhost:8080 运行
 * 2. 数据库中必须有三员角色定义
 * 
 * 根据等保2.0三级要求，这三个角色不能同时分配给同一用户
 * 
 * @author 权限系统测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("三员互斥集成测试")
public class ThreeRoleExclusionIntegrationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String adminToken;
    
    // 三员角色编码
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String SECURITY_ADMIN = "SECURITY_ADMIN"; 
    private static final String AUDIT_ADMIN = "AUDIT_ADMIN";
    
    // 三员角色ID（运行时查询）
    private static String systemAdminRoleId;
    private static String securityAdminRoleId;
    private static String auditAdminRoleId;

    @BeforeAll
    static void setup() throws Exception {
        // 获取管理员 token
        adminToken = loginAndGetToken("admin", "admin123");
        assertNotNull(adminToken, "管理员登录失败");
        System.out.println("✅ 管理员登录成功");
        
        // 查询三员角色ID
        systemAdminRoleId = getRoleIdByCode(SYSTEM_ADMIN);
        securityAdminRoleId = getRoleIdByCode(SECURITY_ADMIN);
        auditAdminRoleId = getRoleIdByCode(AUDIT_ADMIN);
        
        System.out.println("三员角色 ID:");
        System.out.println("  SYSTEM_ADMIN: " + (systemAdminRoleId != null ? systemAdminRoleId : "不存在"));
        System.out.println("  SECURITY_ADMIN: " + (securityAdminRoleId != null ? securityAdminRoleId : "不存在"));
        System.out.println("  AUDIT_ADMIN: " + (auditAdminRoleId != null ? auditAdminRoleId : "不存在"));
    }

    private static String loginAndGetToken(String username, String password) throws Exception {
        String loginUrl = BASE_URL + "/auth/login";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        
        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(loginUrl, request, String.class);
        
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("登录失败: " + response.getBody());
        }
        
        JsonNode json = objectMapper.readTree(response.getBody());
        return json.path("data").path("token").asText();
    }
    
    private static String getRoleIdByCode(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/admin/roles",
                HttpMethod.GET,
                request,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode json = objectMapper.readTree(response.getBody());
                JsonNode roles = json.path("data").path("records");
                for (JsonNode role : roles) {
                    if (code.equals(role.path("code").asText())) {
                        return role.path("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("查询角色失败: " + e.getMessage());
        }
        return null;
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ==================== 三员互斥测试 ====================

    @Test
    @Order(1)
    @DisplayName("创建用户同时分配系统管理员和安全管理员 - 应该被拒绝")
    void createUser_withSystemAndSecurityAdmin_shouldBeRejected() {
        Assumptions.assumeTrue(systemAdminRoleId != null && securityAdminRoleId != null, 
            "跳过测试：三员角色不存在");
        
        String uniqueUsername = "test_conflict_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"冲突测试用户\",\"roleIds\":[\"%s\",\"%s\"]}",
            uniqueUsername, systemAdminRoleId, securityAdminRoleId
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期应该返回错误，但创建用户成功了");
        } catch (HttpClientErrorException e) {
            // 预期 400 或 409 业务错误
            assertTrue(e.getStatusCode().is4xxClientError(), 
                "预期返回 4xx 错误，实际返回: " + e.getStatusCode());
            System.out.println("✅ 同时分配系统管理员和安全管理员被正确拒绝");
            System.out.println("   错误信息: " + e.getResponseBodyAsString());
        }
    }

    @Test
    @Order(2)
    @DisplayName("创建用户同时分配系统管理员和审计管理员 - 应该被拒绝")
    void createUser_withSystemAndAuditAdmin_shouldBeRejected() {
        Assumptions.assumeTrue(systemAdminRoleId != null && auditAdminRoleId != null, 
            "跳过测试：三员角色不存在");
        
        String uniqueUsername = "test_conflict_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"冲突测试用户\",\"roleIds\":[\"%s\",\"%s\"]}",
            uniqueUsername, systemAdminRoleId, auditAdminRoleId
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期应该返回错误，但创建用户成功了");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode().is4xxClientError(), 
                "预期返回 4xx 错误，实际返回: " + e.getStatusCode());
            System.out.println("✅ 同时分配系统管理员和审计管理员被正确拒绝");
        }
    }

    @Test
    @Order(3)
    @DisplayName("创建用户同时分配安全管理员和审计管理员 - 应该被拒绝")
    void createUser_withSecurityAndAuditAdmin_shouldBeRejected() {
        Assumptions.assumeTrue(securityAdminRoleId != null && auditAdminRoleId != null, 
            "跳过测试：三员角色不存在");
        
        String uniqueUsername = "test_conflict_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"冲突测试用户\",\"roleIds\":[\"%s\",\"%s\"]}",
            uniqueUsername, securityAdminRoleId, auditAdminRoleId
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期应该返回错误，但创建用户成功了");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode().is4xxClientError(), 
                "预期返回 4xx 错误，实际返回: " + e.getStatusCode());
            System.out.println("✅ 同时分配安全管理员和审计管理员被正确拒绝");
        }
    }

    @Test
    @Order(4)
    @DisplayName("创建用户同时分配所有三员角色 - 应该被拒绝")
    void createUser_withAllThreeAdminRoles_shouldBeRejected() {
        Assumptions.assumeTrue(
            systemAdminRoleId != null && securityAdminRoleId != null && auditAdminRoleId != null, 
            "跳过测试：三员角色不存在");
        
        String uniqueUsername = "test_conflict_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"冲突测试用户\",\"roleIds\":[\"%s\",\"%s\",\"%s\"]}",
            uniqueUsername, systemAdminRoleId, securityAdminRoleId, auditAdminRoleId
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            restTemplate.postForEntity(BASE_URL + "/admin/users", request, String.class);
            fail("预期应该返回错误，但创建用户成功了");
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode().is4xxClientError(), 
                "预期返回 4xx 错误，实际返回: " + e.getStatusCode());
            System.out.println("✅ 同时分配所有三员角色被正确拒绝");
        }
    }

    @Test
    @Order(10)
    @DisplayName("创建用户只分配系统管理员 - 应该成功")
    void createUser_withOnlySystemAdmin_shouldSucceed() {
        Assumptions.assumeTrue(systemAdminRoleId != null, "跳过测试：系统管理员角色不存在");
        
        String uniqueUsername = "test_sysadmin_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"Test@123!\",\"fullName\":\"系统管理员测试\",\"roleIds\":[\"%s\"]}",
            uniqueUsername, systemAdminRoleId
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/admin/users", request, String.class
            );
            assertEquals(HttpStatus.OK, response.getStatusCode());
            System.out.println("✅ 只分配系统管理员角色成功");
            
            // 清理：删除测试用户
            cleanupTestUser(response.getBody());
        } catch (HttpClientErrorException e) {
            fail("创建用户失败: " + e.getResponseBodyAsString());
        }
    }

    private void cleanupTestUser(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            String userId = json.path("data").path("id").asText();
            if (userId != null && !userId.isEmpty()) {
                HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
                restTemplate.exchange(
                    BASE_URL + "/admin/users/" + userId,
                    HttpMethod.DELETE,
                    request,
                    String.class
                );
                System.out.println("   已清理测试用户: " + userId);
            }
        } catch (Exception e) {
            System.out.println("   清理测试用户失败: " + e.getMessage());
        }
    }

    // ==================== 测试总结 ====================

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("三员互斥集成测试完成");
        System.out.println("========================================");
    }
}
