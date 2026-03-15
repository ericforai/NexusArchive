// Input: JUnit 5、Mockito、Spring Boot Test
// Output: YonPaymentQueryHelperTest 测试类
// Pos: YonSuite 集成 - 单元测试

package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpRequest;
import com.nexusarchive.common.constants.HttpConstants;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentApplyListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * YonPaymentQueryHelper 单元测试
 * <p>
 * 测试覆盖：
 * 1. 查询构建逻辑
 * 2. API 响应处理
 * 3. 分页逻辑
 * 4. 并行查询
 * 5. 错误处理
 * 6. 边界条件
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class YonPaymentQueryHelperTest {

    @Mock
    private YonAuthService yonAuthService;

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private YonPaymentQueryHelper helper;

    private ErpConfig testConfig;
    private LocalDate testStartDate;
    private LocalDate testEndDate;

    @BeforeEach
    void setUp() {
        testConfig = ErpConfig.builder()
                .id("test-config-1")
                .name("Test YonSuite")
                .adapterType("yonsuite")
                .baseUrl("https://test.yonsuite.com")
                .appKey("test-app-key")
                .appSecret("test-app-secret")
                .enabled(true)
                .build();

        testStartDate = LocalDate.of(2024, 1, 1);
        testEndDate = LocalDate.of(2024, 1, 31);
    }

    // ==================== 测试用例：queryPaymentApplyIdsParallel ====================

    @Test
    void testQueryPaymentApplyIdsParallel_NullConfig() {
        // When
        List<String> result = helper.queryPaymentApplyIdsParallel(null, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(yonAuthService, never()).getAccessToken(any(), any());
    }

    @Test
    void testQueryPaymentApplyIdsParallel_NullBaseUrl() {
        // Given
        testConfig.setBaseUrl(null);

        // When
        List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(yonAuthService, never()).getAccessToken(any(), any());
    }

    @Test
    void testQueryPaymentApplyIdsParallel_EmptyBaseUrl() {
        // Given
        testConfig.setBaseUrl("");

        // When
        List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryPaymentApplyIdsParallel_SinglePage_Success() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.getStatus()).thenReturn(200);
            when(response.body()).thenReturn(createSinglePageJsonResponse());

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains("payment-001"));
            assertTrue(result.contains("payment-002"));
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_MultiplePages_ParallelQuery() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse firstPageResponse = mock(HttpResponse.class);
            HttpResponse secondPageResponse = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);

            // 第一次调用返回第一页，后续调用返回第二页
            when(request.execute())
                    .thenReturn(firstPageResponse)
                    .thenReturn(secondPageResponse);

            when(firstPageResponse.isOk()).thenReturn(true);
            when(firstPageResponse.getStatus()).thenReturn(200);
            when(firstPageResponse.body()).thenReturn(createMultiPageJsonResponse(1, 3));

            when(secondPageResponse.isOk()).thenReturn(true);
            when(secondPageResponse.getStatus()).thenReturn(200);
            when(secondPageResponse.body()).thenReturn(createSingleRecordPageJsonResponse(2));

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.size() >= 1);
            assertTrue(result.contains("payment-001"));
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_FirstPageFailed() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(false);
            when(response.getStatus()).thenReturn(500);

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_InvalidResponse() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn("invalid json");

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_NullResponseData() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createNullDataResponse());

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_EmptyRecordList() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createEmptyRecordListResponse());

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 测试用例：queryPaymentApplyListParallel ====================

    @Test
    void testQueryPaymentApplyListParallel_NullConfig() {
        // When
        List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                helper.queryPaymentApplyListParallel(null, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryPaymentApplyListParallel_SinglePage_Success() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.getStatus()).thenReturn(200);
            when(response.body()).thenReturn(createSinglePageJsonResponse());

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("payment-001", result.get(0).getId());
            assertEquals("payment-002", result.get(1).getId());
        }
    }

    @Test
    void testQueryPaymentApplyListParallel_MultiplePages_ParallelQuery() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse firstPageResponse = mock(HttpResponse.class);
            HttpResponse secondPageResponse = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);

            when(request.execute())
                    .thenReturn(firstPageResponse)
                    .thenReturn(secondPageResponse);

            when(firstPageResponse.isOk()).thenReturn(true);
            when(firstPageResponse.getStatus()).thenReturn(200);
            when(firstPageResponse.body()).thenReturn(createMultiPageJsonResponse(1, 3));

            when(secondPageResponse.isOk()).thenReturn(true);
            when(secondPageResponse.getStatus()).thenReturn(200);
            when(secondPageResponse.body()).thenReturn(createSingleRecordPageJsonResponse(2));

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.size() >= 1);
        }
    }

    @Test
    void testQueryPaymentApplyListParallel_FirstPageFailed() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(false);
            when(response.getStatus()).thenReturn(500);

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testQueryPaymentApplyListParallel_InvalidResponse() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn("invalid json");

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void testQueryPaymentApplyListParallel_NullStartDate() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createSinglePageJsonResponse());

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, null, testEndDate);

            // Then
            assertNotNull(result);
            // 验证请求被发送（即使日期为 null 也应该发送请求）
            verify(request, atLeastOnce()).execute();
        }
    }

    @Test
    void testQueryPaymentApplyListParallel_NullEndDate() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createSinglePageJsonResponse());

            // When
            List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                    helper.queryPaymentApplyListParallel(testConfig, testStartDate, null);

            // Then
            assertNotNull(result);
            verify(request, atLeastOnce()).execute();
        }
    }

    // ==================== 测试用例：边界条件和错误处理 ====================

    @Test
    void testQueryPaymentApplyIdsParallel_ExceptionHandling() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenThrow(new RuntimeException("Network error"));

        // When
        List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryPaymentApplyListParallel_ExceptionHandling() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenThrow(new RuntimeException("Network error"));

        // When
        List<YonPaymentApplyListResponse.PaymentApplyRecord> result =
                helper.queryPaymentApplyListParallel(testConfig, testStartDate, testEndDate);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testQueryPaymentApplyIdsParallel_ResponseWithNullIds() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createResponseWithNullIds());

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty()); // null IDs should be filtered out
        }
    }

    @Test
    void testQueryPaymentApplyIdsParallel_MaxPageLimit() {
        // Given
        String accessToken = "test-access-token";
        when(yonAuthService.getAccessToken("test-app-key", "test-app-secret"))
                .thenReturn(accessToken);

        try (MockedStatic<HttpRequest> mockedHttpRequest = mockStatic(HttpRequest.class)) {
            HttpRequest request = mock(HttpRequest.class);
            HttpResponse response = mock(HttpResponse.class);

            mockedHttpRequest.when(() -> HttpRequest.post(anyString())).thenReturn(request);
            when(request.header((String)any(), (String)any())).thenReturn(request);
            when(request.body(anyString())).thenReturn(request);
            when(request.timeout(anyInt())).thenReturn(request);
            when(request.execute()).thenReturn(response);

            when(response.isOk()).thenReturn(true);
            when(response.body()).thenReturn(createMultiPageJsonResponse(1, 150)); // 150 pages

            // When
            List<String> result = helper.queryPaymentApplyIdsParallel(testConfig, testStartDate, testEndDate);

            // Then
            assertNotNull(result);
            // 验证只查询了最多 100 页（第 1 页 + 99 页）
            verify(request, times(100)).execute();
        }
    }

    // ==================== 辅助方法：创建测试数据 ====================

    private String createSinglePageJsonResponse() {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"recordList\": ["
                + "    {"
                + "      \"id\": \"payment-001\","
                + "      \"code\": \"PAY-001\","
                + "      \"billDate\": \"2024-01-15\","
                + "      \"applyAmount\": \"10000.00\","
                + "      \"creatorName\": \"张三\""
                + "    },"
                + "    {"
                + "      \"id\": \"payment-002\","
                + "      \"code\": \"PAY-002\","
                + "      \"billDate\": \"2024-01-16\","
                + "      \"applyAmount\": \"20000.00\","
                + "      \"creatorName\": \"李四\""
                + "    }"
                + "  ],"
                + "  \"pageCount\": 1,"
                + "  \"totalCount\": 2"
                + "}"
                + "}";
    }

    private String createMultiPageJsonResponse(int currentPage, int totalPages) {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"recordList\": ["
                + "    {"
                + "      \"id\": \"payment-001\","
                + "      \"code\": \"PAY-001\","
                + "      \"billDate\": \"2024-01-15\""
                + "    }"
                + "  ],"
                + "  \"pageCount\": " + totalPages + ","
                + "  \"totalCount\": " + (totalPages * 100)
                + "}"
                + "}";
    }

    private String createSingleRecordPageJsonResponse(int pageIndex) {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"recordList\": ["
                + "    {"
                + "      \"id\": \"payment-00" + pageIndex + "\","
                + "      \"code\": \"PAY-00" + pageIndex + "\","
                + "      \"billDate\": \"2024-01-" + String.format("%02d", 15 + pageIndex) + "\""
                + "    }"
                + "  ],"
                + "  \"pageCount\": 3,"
                + "  \"totalCount\": 300"
                + "}"
                + "}";
    }

    private String createNullDataResponse() {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": null"
                + "}";
    }

    private String createEmptyRecordListResponse() {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"recordList\": [],"
                + "  \"pageCount\": 1,"
                + "  \"totalCount\": 0"
                + "}"
                + "}";
    }

    private String createResponseWithNullIds() {
        return "{"
                + "\"code\": \"200\","
                + "\"message\": \"success\","
                + "\"data\": {"
                + "  \"recordList\": ["
                + "    {"
                + "      \"id\": null,"
                + "      \"code\": \"PAY-001\""
                + "    },"
                + "    {"
                + "      \"id\": null,"
                + "      \"code\": \"PAY-002\""
                + "    }"
                + "  ],"
                + "  \"pageCount\": 1,"
                + "  \"totalCount\": 2"
                + "}"
                + "}";
    }
}
