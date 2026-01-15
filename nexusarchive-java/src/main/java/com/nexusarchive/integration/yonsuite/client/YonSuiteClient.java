// Input: cn.hutool、Jackson、Lombok、Spring Framework、等
// Output: YonSuiteClient 类
// Pos: 外部系统客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
     * 使用指定的 appKey/appSecret 获取 token
     * @param appKey 应用 Key
     * @param appSecret 应用密钥
     * @return access_token
     */
    public String getTokenWithCredentials(String appKey, String appSecret) {
        return yonAuthService.getAccessToken(appKey, appSecret);
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
     * 通用附件查询接口（别名方法）
     * 用于付款申请单、收款单等其他单据类型的附件查询
     */
    public YonAttachmentListResponse queryAttachments(String accessToken, String billId) {
        return queryVoucherAttachments(accessToken, billId);
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
     * 下载附件并返回字节数组 (自动关闭连接)
     * ✅ P1 修复: 返回 byte[] 而不是 InputStream,避免连接泄漏
     */
    public byte[] downloadFile(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        log.info("Downloading from: {}", url);
        
        try (var response = HttpRequest.get(url).timeout(60_000).execute()) {
            return response.bodyBytes();
        } catch (Exception e) {
            log.error("下载文件失败: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 下载附件并使用回调处理 (避免内存溢出)
     * ✅ P1 修复: 使用回调模式处理大文件
     */
    public <T> T downloadFileWithCallback(String url, 
            java.util.function.Function<java.io.InputStream, T> callback) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        log.info("Downloading from: {}", url);
        
        try (var response = HttpRequest.get(url).timeout(60_000).execute();
             var inputStream = response.bodyStream()) {
            return callback.apply(inputStream);
        } catch (Exception e) {
            log.error("下载文件失败: url={}, error={}", url, e.getMessage(), e);
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * @deprecated 使用 downloadFile() 或 downloadFileWithCallback() 代替
     * ⚠️ 此方法可能导致连接泄漏,仅用于向后兼容
     */
    @Deprecated
    public java.io.InputStream downloadStream(String url) {
        if (url == null)
            return null;
        log.warn("使用已废弃的 downloadStream() 方法,请迁移到 downloadFile() 或 downloadFileWithCallback()");
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
    /**
     * 付款单详情查询 (官方 API)
     * GET /yonbip/EFI/payment/detail
     *
     * @param accessToken 可选, 为null则自动获取
     * @param paymentId   付款单ID (必填)
     * @return 付款单详情
     */
    public YonPaymentDetailResponse queryPaymentDetail(String accessToken, String paymentId) {
        String token = getToken(accessToken);
        // 官方文档: /yonbip/EFI/payment/detail?access_token=访问令牌&id=xxx
        String url = baseUrl + "/yonbip/EFI/payment/detail"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&id=" + URLEncoder.encode(paymentId, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryPaymentDetail: id={}", paymentId);

        try {
            // GET 请求
            String respStr = HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite payment detail response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                log.warn("YonSuite queryPaymentDetail returned empty response for id={}", paymentId);
                return null;
            }

            YonPaymentDetailResponse response = objectMapper.readValue(respStr, YonPaymentDetailResponse.class);
            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite payment detail API warning: code={}, message={}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryPaymentDetail for id={}", paymentId, e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 批量查询凭证附件 (新 API)
     * POST /yonbip/EFI/rest/v1/openapi/queryBusinessFiles
     * 支持一次查询多个凭证的附件列表
     *
     * @param accessToken 可选, 为null则自动获取
     * @param businessIds 凭证ID列表
     * @return 凭证ID -> 附件列表的映射
     */
    public VoucherAttachmentResponse queryBusinessFiles(String accessToken, java.util.List<String> businessIds) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/rest/v1/openapi/queryBusinessFiles"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryBusinessFiles: businessIds count={}", businessIds.size());

        try {
            VoucherAttachmentRequest request = new VoucherAttachmentRequest();
            request.setBusinessIds(businessIds);

            String body = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite queryBusinessFiles response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                VoucherAttachmentResponse empty = new VoucherAttachmentResponse();
                empty.setCode("200");
                empty.setMessage("No data");
                empty.setData(new java.util.HashMap<>());
                return empty;
            }

            VoucherAttachmentResponse response = objectMapper.readValue(respStr, VoucherAttachmentResponse.class);
            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite queryBusinessFiles warning: code={}, message={}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryBusinessFiles", e);
            throw new RuntimeException("Failed to call YonSuite API: " + e.getMessage(), e);
        }
    }

    /**
     * 向 YonSuite 反馈归档状态 (模拟)
     * 
     * Phase 3 增强：返回结构化结果，记录详细日志
     * 在生产环境中，将调用用友的凭证修改接口或自定义存证接口
     * 
     * @param accessToken 访问令牌 (可选)
     * @param voucherId   凭证 ID
     * @param archivalCode 档号
     * @return FeedbackResult 结构化回写结果
     */
    public com.nexusarchive.integration.erp.dto.FeedbackResult feedbackArchivalStatus(
            String accessToken, String voucherId, String archivalCode) {
        
        String token = getToken(accessToken);
        // 模拟接口：POST /yonbip/fi/ficloud/openapi/voucher/feedback
        String url = baseUrl + "/yonbip/fi/ficloud/openapi/voucher/feedback"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("┌─────────────────────────────────────────────────────────────┐");
        log.info("│ [存证溯源] 开始回写归档状态至 YonSuite                        │");
        log.info("├─────────────────────────────────────────────────────────────┤");
        log.info("│ 凭证ID: {}", voucherId);
        log.info("│ 档号: {}", archivalCode);
        log.info("│ 目标URL: {}", url);
        log.info("│ 执行模式: MOCK (模拟)");
        log.info("└─────────────────────────────────────────────────────────────┘");

        try {
            JSONObject body = new JSONObject();
            body.putOnce("id", voucherId);
            body.putOnce("memo", "已归档至档案系统，档号: " + archivalCode);
            body.putOnce("archive_status", "ARCHIVED");
            body.putOnce("archived_at", java.time.LocalDateTime.now().toString());

            // 模拟模式：不实际执行 HTTP 请求
            // TODO: 生产环境切换为真实调用
            // String respStr = HttpRequest.post(url).body(body.toString()).execute().body();
            
            log.info("✓ [存证溯源] YonSuite 回写模拟成功 - voucher={}, archivalCode={}", 
                    voucherId, archivalCode);
            
            return com.nexusarchive.integration.erp.dto.FeedbackResult.success(
                    voucherId, archivalCode, "YONSUITE", true);
                    
        } catch (Exception e) {
            log.error("✗ [存证溯源] YonSuite 回写失败 - voucher={}, error={}", 
                    voucherId, e.getMessage(), e);
            
            return com.nexusarchive.integration.erp.dto.FeedbackResult.failure(
                    voucherId, archivalCode, "YONSUITE", e.getMessage());
        }
    }

    /**
     * 付款退款单文件下载查询
     * POST /yonbip/EFI/apRefund/file/url
     * 文档: 获取付款退款单文件
     *
     * @param accessToken 可选, 为null则自动获取
     * @param request 退款文件下载请求 (fileId 列表, 最多20个)
     * @return 退款文件下载信息
     */
    public YonRefundFileResponse queryRefundFileUrls(String accessToken, YonRefundFileRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/apRefund/file/url"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryRefundFileUrls: fileIds={}", request.getFileId());

        try {
            String body = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.info("YonSuite refund file url response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonRefundFileResponse emptyResponse = new YonRefundFileResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                return emptyResponse;
            }

            YonRefundFileResponse response = objectMapper.readValue(respStr, YonRefundFileResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite refund file url API warning: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryRefundFileUrls", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }

    /**
     * 付款退款单列表查询
     * POST /yonbip/EFI/apRefund/list
     * 文档: 付款退款单列表查询
     *
     * @param accessToken 可选, 为null则自动获取
     * @param request 退款单列表查询请求
     * @return 退款单列表
     */
    public YonRefundListResponse queryRefundList(String accessToken, YonRefundListRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/apRefund/list"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryRefundList: pageIndex={}, pageSize={}",
                request.getPageIndex(), request.getPageSize());

        try {
            String body = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.info("YonSuite refund list response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonRefundListResponse emptyResponse = new YonRefundListResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                emptyResponse.setData(new YonRefundListResponse.PageData());
                return emptyResponse;
            }

            YonRefundListResponse response = objectMapper.readValue(respStr, YonRefundListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite refund list API warning: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryRefundList", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }

    /**
     * 付款申请单文件下载查询
     * POST /yonbip/EFI/paymentApply/file/url
     *
     * @param accessToken 可选, 为null则自动获取
     * @param request 付款申请单文件下载请求 (fileId 列表, 最多20个)
     * @return 付款申请单文件下载信息
     */
    public YonPaymentApplyFileResponse queryPaymentApplyFileUrls(String accessToken, YonPaymentApplyFileRequest request) {
        String token = getToken(accessToken);
        String url = baseUrl + "/yonbip/EFI/paymentApply/file/url"
                + "?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("Calling YonSuite queryPaymentApplyFileUrls: fileIds={}", request.getFileId());

        try {
            String body = objectMapper.writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.info("YonSuite payment apply file url response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonPaymentApplyFileResponse emptyResponse = new YonPaymentApplyFileResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                return emptyResponse;
            }

            YonPaymentApplyFileResponse response = objectMapper.readValue(respStr, YonPaymentApplyFileResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite payment apply file url API warning: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryPaymentApplyFileUrls", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }
}
