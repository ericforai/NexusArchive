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
        this.objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
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

    /**
     * 凭证附件查询
     * POST /yonbip/fi/ficloud/openapi/voucher/queryAttachments
     */
    public YonAttachmentListResponse queryVoucherAttachments(String accessToken, String voucherId) {
        String token = getToken(accessToken);
        // 尝试使用 YonBIP 通用附件查询接口 (Digital Model)
        String url = baseUrl + "/yonbip/digitalModel/adm/attachmentInfo/query"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite attachment query: id={}", voucherId);

        try {
            JSONObject body = new JSONObject();
            // 通用接口通常使用 id 或 businessId
            body.putOnce("id", voucherId);
            // body.putOnce("busiType", "???"); // 如果需要业务类型
            // 某些版本的接口可能需要 billType 等参数，这里先传基础的 ID

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite Attachments Response: {}", respStr);

            // 处理空响应
            if (respStr == null || respStr.isEmpty()) {
                return null;
            }

            YonAttachmentListResponse response = objectMapper.readValue(respStr, YonAttachmentListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite queryAttachments warning: {} - {}", response.getCode(), response.getMessage());
                // 不抛出异常，而是返回空，允许无附件的情况
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryVoucherAttachments", e);
            // 降级处理：附件获取失败不应阻断主流程，返回 null 或空列表
            return null;
        }
    }

    /**
     * 获取收款单文件
     * POST /yonbip/EFI/collection/file/url
     */
    public YonCollectionFileResponse queryCollectionFiles(String accessToken, YonCollectionFileRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/collection/file/url"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        try {
            String body = objectMapper.writeValueAsString(request);
            log.info("Calling YonSuite collection file url: fileIds={}", request.getFileId());

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite collection file response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonCollectionFileResponse empty = new YonCollectionFileResponse();
                empty.setCode("200");
                empty.setMessage("No data");
                empty.setData(java.util.Collections.emptyList());
                return empty;
            }

            YonCollectionFileResponse response = objectMapper.readValue(respStr, YonCollectionFileResponse.class);
            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite collection file API returned code {} message {}", response.getCode(),
                        response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite collection file API", e);
            throw new RuntimeException("Failed to call YonSuite collection file API: " + e.getMessage(), e);
        }
    }

    /**
     * 下载附件
     */
    public java.io.InputStream downloadStream(String url) {
        if (url == null)
            return null;
        log.info("Downloading from: {}", url);
        return HttpRequest.get(url).timeout(60_000).execute().bodyStream();
    }

    /**
     * 查询收款单列表
     * POST /yonbip/EFI/collection/list
     */
    public YonCollectionBillResponse queryCollectionBills(String accessToken, YonCollectionBillRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/collection/list"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        try {
            String body = objectMapper.writeValueAsString(request);
            log.info("Calling YonSuite queryCollectionBills: body={}", body);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.info("YonSuite collection list response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                return null;
            }

            YonCollectionBillResponse response = objectMapper.readValue(respStr, YonCollectionBillResponse.class);
            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite collection list API warning: {} - {}", response.getCode(), response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryCollectionBills", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }

    /**
     * 收款单详情查询 (官方 API)
     * GET /yonbip/EFI/collection/detail
     * 文档: docs/api/收款单详情查询.md
     * 
     * @param accessToken  可选, 为null则自动获取
     * @param collectionId 收款单ID (必填)
     * @return 收款单详情
     */
    public YonCollectionDetailResponse queryCollectionDetail(String accessToken, String collectionId) {
        String token = getToken(accessToken);
        // 官方示例: /yonbip/EFI/collection/detail?access_token=访问令牌&id=2326431654121728
        String url = baseUrl + "/yonbip/EFI/collection/detail"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&id=" + URLEncoder.encode(collectionId, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryCollectionDetail: id={}", collectionId);

        try {
            // 官方文档指定为 GET 请求
            String respStr = HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite collection detail response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                log.warn("YonSuite queryCollectionDetail returned empty response for id={}", collectionId);
                return null;
            }

            YonCollectionDetailResponse response = objectMapper.readValue(respStr, YonCollectionDetailResponse.class);
            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite collection detail API warning: code={}, message={}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryCollectionDetail for id={}", collectionId, e);
            return null; // 允许单个失败, 不阻断批量处理
        }
    }
}
