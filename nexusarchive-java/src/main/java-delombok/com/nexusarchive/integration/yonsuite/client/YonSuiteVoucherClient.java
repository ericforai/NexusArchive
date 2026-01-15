// Input: Hutool, Jackson, Spring Framework
// Output: YonSuiteVoucherClient 类
// Pos: 外部系统客户端 - 凭证相关

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.json.JSONObject;
import com.nexusarchive.integration.erp.dto.FeedbackResult;
import com.nexusarchive.integration.yonsuite.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * YonSuite 凭证 API 客户端
 * <p>
 * 处理凭证相关的 API 调用：
 * <ul>
 *   <li>凭证列表查询</li>
 *   <li>凭证详情查询</li>
 *   <li>凭证附件查询</li>
 *   <li>批量业务文件查询</li>
 *   <li>归档状态回写</li>
 * </ul>
 * </p>
 */
@Component
public class YonSuiteVoucherClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(YonSuiteVoucherClient.class);

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonSuiteHttpExecutor httpExecutor;

    public YonSuiteVoucherClient(YonSuiteHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    /**
     * 凭证列表查询
     * POST /yonbip/fi/ficloud/openapi/voucher/queryVouchers
     */
    public YonVoucherListResponse queryVouchers(String accessToken, YonVoucherListRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/fi/ficloud/openapi/voucher/queryVouchers", accessToken);

        log.info("Calling YonSuite queryVouchers: accbookCode={}, period={}-{}",
                request.getAccbookCode(), request.getPeriodStart(), request.getPeriodEnd());

        String respStr = httpExecutor.postRaw(url, request);

        if (respStr == null || respStr.isEmpty()) {
            log.warn("YonSuite API returned empty response");
            YonVoucherListResponse emptyResponse = new YonVoucherListResponse();
            emptyResponse.setCode("200");
            emptyResponse.setMessage("No data");
            emptyResponse.setData(new YonVoucherListResponse.PageData());
            return emptyResponse;
        }

        try {
            YonVoucherListResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonVoucherListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.error("YonSuite queryVouchers error: {} - {}", response.getCode(), response.getMessage());
                throw new RuntimeException("YonSuite API error: " + response.getMessage());
            }

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse queryVouchers response", e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }

    /**
     * 凭证详情查询
     * POST /yonbip/EFI/openapi/voucher/queryVoucherById
     */
    public YonVoucherDetailResponse queryVoucherById(String accessToken, String voucherId) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/openapi/voucher/queryVoucherById", accessToken);

        log.info("Calling YonSuite queryVoucherById: voucherId={}", voucherId);

        JSONObject body = new JSONObject();
        body.putOnce("voucherId", voucherId);

        String respStr = httpExecutor.postRawWithHutoonJson(url, body);

        if (respStr == null || respStr.isEmpty()) {
            log.warn("YonSuite queryVoucherById returned empty response");
            return null;
        }

        try {
            YonVoucherDetailResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonVoucherDetailResponse.class);

            if (!"200".equals(response.getCode())) {
                log.error("YonSuite queryVoucherById error: {} - {}", response.getCode(), response.getMessage());
                throw new RuntimeException("YonSuite API error: " + response.getMessage());
            }

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse queryVoucherById response", e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }

    /**
     * 凭证附件查询
     * POST /yonbip/digitalModel/adm/attachmentInfo/query
     */
    public YonAttachmentListResponse queryVoucherAttachments(String accessToken, String voucherId) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/digitalModel/adm/attachmentInfo/query", accessToken);

        log.info("Calling YonSuite attachment query: id={}", voucherId);

        JSONObject body = new JSONObject();
        body.putOnce("id", voucherId);

        String respStr = httpExecutor.postRawWithHutoonJson(url, body);

        if (respStr == null || respStr.isEmpty()) {
            return null;
        }

        try {
            YonAttachmentListResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonAttachmentListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite queryAttachments warning: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryVoucherAttachments", e);
            // 降级处理：附件获取失败不应阻断主流程
            return null;
        }
    }

    /**
     * 批量查询凭证附件
     * POST /yonbip/EFI/rest/v1/openapi/queryBusinessFiles
     */
    public VoucherAttachmentResponse queryBusinessFiles(String accessToken, java.util.List<String> businessIds) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/rest/v1/openapi/queryBusinessFiles", accessToken);

        log.info("Calling YonSuite queryBusinessFiles: businessIds count={}", businessIds.size());

        VoucherAttachmentRequest request = new VoucherAttachmentRequest();
        request.setBusinessIds(businessIds);

        String respStr = httpExecutor.postRaw(url, request);

        if (respStr == null || respStr.isEmpty()) {
            VoucherAttachmentResponse empty = new VoucherAttachmentResponse();
            empty.setCode("200");
            empty.setMessage("No data");
            empty.setData(new java.util.HashMap<>());
            return empty;
        }

        try {
            VoucherAttachmentResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, VoucherAttachmentResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite queryBusinessFiles warning: code={}, message={}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to parse queryBusinessFiles response", e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }

    /**
     * 向 YonSuite 反馈归档状态
     *
     * Phase 3 增强：返回结构化结果，记录详细日志
     * 在生产环境中，将调用用友的凭证修改接口或自定义存证接口
     */
    public FeedbackResult feedbackArchivalStatus(String accessToken, String voucherId, String archivalCode) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/fi/ficloud/openapi/voucher/feedback", accessToken);

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
            // httpExecutor.postRawWithHutoonJson(url, body);

            log.info("✓ [存证溯源] YonSuite 回写模拟成功 - voucher={}, archivalCode={}",
                    voucherId, archivalCode);

            return FeedbackResult.success(voucherId, archivalCode, "YONSUITE", true);

        } catch (Exception e) {
            log.error("✗ [存证溯源] YonSuite 回写失败 - voucher={}, error={}",
                    voucherId, e.getMessage(), e);

            return FeedbackResult.failure(voucherId, archivalCode, "YONSUITE", e.getMessage());
        }
    }
}
