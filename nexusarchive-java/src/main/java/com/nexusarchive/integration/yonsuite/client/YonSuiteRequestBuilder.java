// Input: Hutool HTTP, Jackson, Lombok
// Output: YonSuiteRequestBuilder (请求构建器辅助类)
// Pos: 集成层 - 客户端辅助
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class YonSuiteRequestBuilder {

    private final ObjectMapper objectMapper;

    public <T> T post(String url, String token, Object requestBody, Class<T> responseType) {
        String fullUrl = url + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        try {
            String jsonBody = (requestBody instanceof String) ? (String) requestBody : objectMapper.writeValueAsString(requestBody);
            log.debug("YonSuite POST URL: {}, Body: {}", fullUrl, jsonBody);

            try (HttpResponse resp = HttpRequest.post(fullUrl).header("Content-Type", "application/json").body(jsonBody).timeout(30_000).execute()) {
                String body = resp.body();
                return body != null && !body.isEmpty() ? objectMapper.readValue(body, responseType) : null;
            }
        } catch (Exception e) {
            log.error("YonSuite API Error [POST]: {}", url, e);
            throw new RuntimeException("YonSuite API Error: " + e.getMessage(), e);
        }
    }

    public <T> T get(String url, String token, String id, Class<T> responseType) {
        String fullUrl = url + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8) + "&id=" + URLEncoder.encode(id, StandardCharsets.UTF_8);
        log.debug("YonSuite GET URL: {}", fullUrl);
        try (HttpResponse resp = HttpRequest.get(fullUrl).header("Content-Type", "application/json").timeout(30_000).execute()) {
            String body = resp.body();
            return body != null && !body.isEmpty() ? objectMapper.readValue(body, responseType) : null;
        } catch (Exception e) {
            log.error("YonSuite API Error [GET]: {}", url, e);
            throw new RuntimeException("YonSuite API Error: " + e.getMessage(), e);
        }
    }
}
