package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.yonsuite.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * YonSuite API Client
 * Handles HTTP communication with YonSuite (YS) financial system.
 */
@Service
@Slf4j
public class YonSuiteClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final ObjectMapper objectMapper;
    private final com.nexusarchive.integration.yonsuite.service.YonAuthService yonAuthService;

    public YonSuiteClient(com.nexusarchive.integration.yonsuite.service.YonAuthService yonAuthService) {
        this.yonAuthService = yonAuthService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 获取有效的 access_token
     */
    private String getToken(String accessToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            return accessToken;
        }
        return yonAuthService.getAccessToken();
    }

    /**
     * 凭证列表查询
     * POST /yonbip/fi/ficloud/openapi/voucher/queryVouchers
     */
    public YonVoucherListResponse queryVouchers(String accessToken, YonVoucherListRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/fi/ficloud/openapi/voucher/queryVouchers"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryVouchers: accbookCode={}, period={}-{}", 
                request.getAccbookCode(), request.getPeriodStart(), request.getPeriodEnd());

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", requestBody);

            var httpResp = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(30_000)
                    .execute();
            
            String respStr = httpResp.body();
            log.info("YonSuite Response [status={}]: {}", httpResp.getStatus(), respStr);
            
            // 处理空响应
            if (respStr == null || respStr.isEmpty()) {
                log.warn("YonSuite API returned empty response");
                YonVoucherListResponse emptyResponse = new YonVoucherListResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                emptyResponse.setData(new YonVoucherListResponse.PageData());
                return emptyResponse;
            }
            
            YonVoucherListResponse response = objectMapper.readValue(respStr, YonVoucherListResponse.class);
            
            if (!"200".equals(response.getCode())) {
                log.error("YonSuite queryVouchers error: {} - {}", response.getCode(), response.getMessage());
                throw new RuntimeException("YonSuite API error: " + response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryVouchers", e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 凭证详情查询
     * POST /yonbip/EFI/openapi/voucher/queryVoucherById
     */
    public YonVoucherDetailResponse queryVoucherById(String accessToken, String voucherId) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/openapi/voucher/queryVoucherById"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryVoucherById: voucherId={}", voucherId);

        try {
            JSONObject body = new JSONObject();
            body.putOnce("voucherId", voucherId);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("Response: {}", respStr);
            
            YonVoucherDetailResponse response = objectMapper.readValue(respStr, YonVoucherDetailResponse.class);
            
            if (!"200".equals(response.getCode())) {
                log.error("YonSuite queryVoucherById error: {} - {}", response.getCode(), response.getMessage());
                throw new RuntimeException("YonSuite API error: " + response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryVoucherById", e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }
}
