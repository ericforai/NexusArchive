// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: BorrowingIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心功能集成测试 - 借阅管理
 * 
 * 商业化上线前系统体检 Phase 1
 * 
 * @author 系统体检
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("核心功能测试 - 借阅管理")
public class BorrowingIntegrationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static String adminToken;
    private static String createdBorrowingId;
    private static String testArchiveId;

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
        
        // 查询一个现有档案用于借阅测试
        findExistingArchive();
    }
    
    private static void findExistingArchive() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives?page=1&limit=1",
            HttpMethod.GET,
            request,
            String.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode records = json.path("data").path("records");
            if (records.isArray() && !records.isEmpty()) {
                testArchiveId = records.get(0).path("id").asText();
                System.out.println("找到测试档案: " + testArchiveId);
            }
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(adminToken);
        return headers;
    }

    // ==================== 借阅列表查询 ====================

    @Test
    @Order(1)
    @DisplayName("查询借阅列表")
    void listBorrowings_shouldReturnBorrowingList() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/borrowing?page=1&limit=10",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(200, json.path("code").asInt());
        System.out.println("✅ 查询借阅列表成功");
    }

    // ==================== 创建借阅申请 ====================

    @Test
    @Order(10)
    @DisplayName("创建借阅申请")
    void createBorrowing_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(testArchiveId != null, "需要有可借阅的档案");
        
        String expectedReturnDate = LocalDate.now().plusDays(30).toString();
        String body = String.format(
            "{\"archiveId\":\"%s\",\"reason\":\"系统测试借阅\",\"expectedReturnDate\":\"%s\"}",
            testArchiveId, expectedReturnDate
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            BASE_URL + "/borrowing",
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        createdBorrowingId = json.path("data").path("id").asText();
        System.out.println("✅ 创建借阅申请成功: " + createdBorrowingId);
    }

    // ==================== 审批借阅 ====================

    @Test
    @Order(20)
    @DisplayName("审批借阅申请 - 批准")
    void approveBorrowing_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdBorrowingId != null, "需要先创建借阅申请");
        
        String body = "{\"approved\":true,\"comment\":\"系统测试批准\"}";
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/borrowing/" + createdBorrowingId + "/approve",
            HttpMethod.POST,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 审批借阅成功");
    }

    // ==================== 归还档案 ====================

    @Test
    @Order(30)
    @DisplayName("归还档案")
    void returnBorrowing_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdBorrowingId != null, "需要先创建借阅申请");
        
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/borrowing/" + createdBorrowingId + "/return",
            HttpMethod.POST,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 归还档案成功");
    }

    // ==================== 借阅状态查询 ====================

    @Test
    @Order(40)
    @DisplayName("按状态筛选借阅")
    void listBorrowings_withStatusFilter_shouldWork() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/borrowing?status=PENDING",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 按状态筛选借阅成功");
    }

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("借阅管理测试完成");
        System.out.println("========================================");
    }
}
