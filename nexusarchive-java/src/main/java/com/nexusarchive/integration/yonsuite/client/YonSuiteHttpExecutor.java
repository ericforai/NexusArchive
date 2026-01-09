// Input: Hutool HTTP, Jackson, Spring Framework
// Output: YonSuiteHttpExecutor 类
// Pos: 外部系统客户端 - HTTP 执行器

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * YonSuite HTTP 请求执行器
 * <p>
 * 封装与 YonSuite API 通信的通用逻辑：
 * <ul>
 *   <li>URL 构建（含 access_token）</li>
 *   <li>POST/GET 请求执行</li>
 *   <li>JSON 序列化/反序列化</li>
 *   <li>通用错误处理</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class YonSuiteHttpExecutor {

    private final ObjectMapper objectMapper;

    public YonSuiteHttpExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 构建带 access_token 的 URL
     */
    public String buildUrl(String baseUrl, String endpoint, String accessToken) {
        String token = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        return baseUrl + endpoint + "?access_token=" + token;
    }

    /**
     * 构建带 access_token 和查询参数的 URL
     */
    public String buildUrlWithParam(String baseUrl, String endpoint, String accessToken,
                                     String paramKey, String paramValue) {
        String token = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(paramValue, StandardCharsets.UTF_8);
        return baseUrl + endpoint + "?access_token=" + token + "&" + paramKey + "=" + encodedValue;
    }

    /**
     * 执行 POST 请求并解析响应
     */
    public <T> T post(String url, Object requestBody, Class<T> responseClass) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            log.debug("POST {} with body: {}", url, body);

            HttpResponse httpResponse = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute();

            return parseResponse(httpResponse, responseClass);
        } catch (Exception e) {
            log.error("Failed to execute POST request to: {}", url, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 POST 请求（使用 Hutool JSON）
     */
    public <T> T postWithHutoolJson(String url, cn.hutool.json.JSONObject requestBody, Class<T> responseClass) {
        try {
            log.debug("POST {} with body: {}", url, requestBody);

            HttpResponse httpResponse = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30_000)
                    .execute();

            return parseResponse(httpResponse, responseClass);
        } catch (Exception e) {
            log.error("Failed to execute POST request to: {}", url, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 GET 请求并解析响应
     */
    public <T> T get(String url, Class<T> responseClass) {
        try {
            log.debug("GET {}", url);

            HttpResponse httpResponse = HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .timeout(30_000)
                    .execute();

            return parseResponse(httpResponse, responseClass);
        } catch (Exception e) {
            log.error("Failed to execute GET request to: {}", url, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 POST 请求，返回原始响应字符串（用于需要特殊处理的场景）
     */
    public String postRaw(String url, Object requestBody) {
        try {
            String body = objectMapper.writeValueAsString(requestBody);
            log.debug("POST {} with body: {}", url, body);

            HttpResponse httpResponse = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute();

            String respStr = httpResponse.body();
            log.info("YonSuite Response [status={}]: {}", httpResponse.getStatus(), respStr);
            return respStr;
        } catch (Exception e) {
            log.error("Failed to execute POST request to: {}", url, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 POST 请求，返回原始响应字符串（使用 Hutool JSON）
     */
    public String postRawWithHutoonJson(String url, cn.hutool.json.JSONObject requestBody) {
        try {
            log.debug("POST {} with body: {}", url, requestBody);

            HttpResponse httpResponse = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30_000)
                    .execute();

            String respStr = httpResponse.body();
            log.info("YonSuite Response [status={}]: {}", httpResponse.getStatus(), respStr);
            return respStr;
        } catch (Exception e) {
            log.error("Failed to execute POST request to: {}", url, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 HTTP 响应
     */
    private <T> T parseResponse(HttpResponse httpResponse, Class<T> responseClass) {
        String respStr = httpResponse.body();
        log.debug("Response [status={}]: {}", httpResponse.getStatus(), respStr);

        if (respStr == null || respStr.isEmpty()) {
            log.warn("YonSuite API returned empty response");
            return null;
        }

        try {
            return objectMapper.readValue(respStr, responseClass);
        } catch (Exception e) {
            log.error("Failed to parse response: {}", respStr, e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
