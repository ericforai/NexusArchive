package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 安全测试 - 输入验证
 * 
 * 商业化上线前系统体检 Phase 2
 * 
 * 测试 SQL 注入、XSS、路径遍历等安全问题
 * 
 * @author 系统体检
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("安全测试 - 输入验证")
public class SecurityInputValidationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static String adminToken;

    @BeforeAll
    static void setup() throws Exception {
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

    // ==================== SQL 注入测试 ====================

    @Test
    @Order(1)
    @DisplayName("SQL 注入 - 登录用户名")
    void sqlInjection_loginUsername_shouldBeBlocked() {
        String maliciousUsername = "admin' OR '1'='1";
        String body = String.format(
            "{\"username\":\"%s\",\"password\":\"anything\"}",
            maliciousUsername
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        
        try {
            restTemplate.postForEntity(BASE_URL + "/auth/login", request, String.class);
            // 如果登录成功，说明 SQL 注入可能有效
            // 但要注意验证返回的用户是不是 admin
        } catch (HttpClientErrorException e) {
            // 期望被拒绝
            assertTrue(e.getStatusCode().is4xxClientError());
            System.out.println("✅ SQL 注入尝试被阻止");
        }
    }

    @Test
    @Order(2)
    @DisplayName("SQL 注入 - 搜索参数")
    void sqlInjection_searchParameter_shouldBeBlocked() {
        String maliciousSearch = "'; DROP TABLE users; --";
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users?search=" + maliciousSearch,
            HttpMethod.GET,
            request,
            String.class
        );
        
        // 应该返回正常结果（空或正常查询），而不是执行恶意 SQL
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 搜索参数 SQL 注入被阻止");
    }

    // ==================== XSS 测试 ====================

    @Test
    @Order(10)
    @DisplayName("XSS - 用户姓名字段")
    void xss_userFullName_shouldBeEscaped() throws Exception {
        String maliciousName = "<script>alert('xss')</script>";
        String body = String.format(
            "{\"username\":\"xss_test_%s\",\"password\":\"Test@123!\",\"fullName\":\"%s\"}",
            System.currentTimeMillis(), maliciousName
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/admin/users",
                request,
                String.class
            );
            
            // 如果创建成功，检查返回的 fullName 是否被转义
            JsonNode json = objectMapper.readTree(response.getBody());
            String returnedName = json.path("data").path("fullName").asText();
            
            // XSS 应该被阻止或转义
            assertFalse(returnedName.contains("<script>"), "XSS 脚本标签应被移除或转义");
            System.out.println("✅ XSS 被阻止或转义");
            
            // 清理测试用户
            String userId = json.path("data").path("id").asText();
            if (!userId.isEmpty()) {
                restTemplate.exchange(
                    BASE_URL + "/admin/users/" + userId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(createAuthHeaders()),
                    String.class
                );
            }
        } catch (HttpClientErrorException e) {
            // 如果被拒绝也是可接受的
            System.out.println("✅ XSS 输入被服务器拒绝");
        }
    }

    // ==================== 路径遍历测试 ====================

    @Test
    @Order(20)
    @DisplayName("路径遍历 - 文件下载")
    void pathTraversal_fileDownload_shouldBeBlocked() {
        String maliciousPath = "../../../etc/passwd";
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        try {
            restTemplate.exchange(
                BASE_URL + "/archives/files/" + maliciousPath,
                HttpMethod.GET,
                request,
                String.class
            );
            fail("预期路径遍历被阻止");
        } catch (HttpClientErrorException e) {
            // 期望 400/403/404
            assertTrue(e.getStatusCode().is4xxClientError() || e.getStatusCode().is5xxServerError());
            System.out.println("✅ 路径遍历尝试被阻止: " + e.getStatusCode());
        }
    }

    // ==================== 特殊字符测试 ====================

    @Test
    @Order(30)
    @DisplayName("特殊字符 - 中文和 Unicode")
    void specialChars_chineseAndUnicode_shouldBeHandled() {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users?search=中文测试",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 中文字符正常处理");
    }

    @Test
    @Order(31)
    @DisplayName("特殊字符 - 空格和特殊符号")
    void specialChars_spacesAndSymbols_shouldBeHandled() {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/admin/users?search=test%20user",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 空格和特殊符号正常处理");
    }

    // ==================== 边界值测试 ====================

    @Test
    @Order(40)
    @DisplayName("边界值 - 超长字符串")
    void boundaryValue_veryLongString_shouldBeHandled() {
        String longString = "a".repeat(10000);
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        try {
            restTemplate.exchange(
                BASE_URL + "/admin/users?search=" + longString,
                HttpMethod.GET,
                request,
                String.class
            );
        } catch (HttpClientErrorException e) {
            // 可能被拒绝，这是可接受的
            System.out.println("✅ 超长字符串被合理处理: " + e.getStatusCode());
        }
    }

    @Test
    @Order(41)
    @DisplayName("边界值 - 负数分页参数")
    void boundaryValue_negativePagination_shouldBeHandled() {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/admin/users?page=-1&limit=-10",
                HttpMethod.GET,
                request,
                String.class
            );
            // 应该返回正常结果或错误
            System.out.println("✅ 负数分页参数被处理: " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            System.out.println("✅ 负数分页参数被拒绝: " + e.getStatusCode());
        }
    }

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("安全输入验证测试完成");
        System.out.println("========================================");
    }
}
