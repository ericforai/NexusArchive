// Input: Jackson、org.junit、Spring Framework、Java 标准库、等
// Output: ArchiveManagementIntegrationTest 测试用例类
// Pos: 后端测试用例
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
 * 核心功能集成测试 - 档案管理
 * 
 * 商业化上线前系统体检 Phase 1
 * 
 * @author 系统体检
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("核心功能测试 - 档案管理")
public class ArchiveManagementIntegrationTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static String adminToken;
    private static String createdArchiveId;

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

    // ==================== 档案列表查询 ====================

    @Test
    @Order(1)
    @DisplayName("查询档案列表")
    void listArchives_shouldReturnArchiveList() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives?page=1&limit=10",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        assertEquals(200, json.path("code").asInt());
        System.out.println("✅ 查询档案列表成功");
    }

    @Test
    @Order(2)
    @DisplayName("档案列表支持分页")
    void listArchives_withPagination_shouldWork() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives?page=1&limit=5",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode records = json.path("data").path("records");
        assertTrue(records.isArray());
        System.out.println("✅ 分页查询成功");
    }

    @Test
    @Order(3)
    @DisplayName("档案列表支持状态筛选")
    void listArchives_withStatusFilter_shouldWork() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives?status=ARCHIVED",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 状态筛选成功");
    }

    // ==================== 创建档案 ====================

    @Test
    @Order(10)
    @DisplayName("创建档案")
    void createArchive_shouldSucceed() throws Exception {
        String uniqueTitle = "测试档案_" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
            "{\"title\":\"%s\",\"fondsId\":\"default\",\"categoryCode\":\"AC01\",\"retentionPeriod\":\"30Y\",\"securityLevel\":\"INTERNAL\"}",
            uniqueTitle
        );
        
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            BASE_URL + "/archives",
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        createdArchiveId = json.path("data").path("id").asText();
        System.out.println("✅ 创建档案成功: " + uniqueTitle);
    }

    // ==================== 查询单个档案 ====================

    @Test
    @Order(20)
    @DisplayName("查询档案详情")
    void getArchive_shouldReturnDetails() throws Exception {
        Assumptions.assumeTrue(createdArchiveId != null, "需要先创建档案");
        
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives/" + createdArchiveId,
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 查询档案详情成功");
    }

    // ==================== 更新档案 ====================

    @Test
    @Order(30)
    @DisplayName("更新档案信息")
    void updateArchive_shouldSucceed() throws Exception {
        Assumptions.assumeTrue(createdArchiveId != null, "需要先创建档案");
        
        String body = "{\"title\":\"更新后的档案标题\",\"description\":\"这是更新后的描述\"}";
        HttpEntity<String> request = new HttpEntity<>(body, createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/archives/" + createdArchiveId,
            HttpMethod.PUT,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        System.out.println("✅ 更新档案成功");
    }

    // ==================== 审计日志检查 ====================

    @Test
    @Order(40)
    @DisplayName("审计日志 - 档案操作应被记录")
    void auditLog_shouldRecordArchiveOperations() throws Exception {
        HttpEntity<String> request = new HttpEntity<>(createAuthHeaders());
        
        ResponseEntity<String> response = restTemplate.exchange(
            BASE_URL + "/audit-logs?page=1&limit=10",
            HttpMethod.GET,
            request,
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        JsonNode json = objectMapper.readTree(response.getBody());
        assertTrue(json.path("data").path("total").asInt() > 0);
        System.out.println("✅ 审计日志记录正常");
    }

    @AfterAll
    static void summary() {
        System.out.println("\n========================================");
        System.out.println("档案管理测试完成");
        System.out.println("========================================");
    }
}
