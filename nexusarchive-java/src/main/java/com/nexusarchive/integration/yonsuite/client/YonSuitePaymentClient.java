// Input: Hutool, Jackson, Spring Framework
// Output: YonSuitePaymentClient 类
// Pos: 外部系统客户端 - 付款/退款单相关

package com.nexusarchive.integration.yonsuite.client;

import com.nexusarchive.integration.yonsuite.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * YonSuite 付款/退款单 API 客户端
 * <p>
 * 处理付款和退款单相关的 API 调用：
 * <ul>
 *   <li>付款单详情查询</li>
 *   <li>退款单文件下载查询</li>
 *   <li>退款单列表查询</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class YonSuitePaymentClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonSuiteHttpExecutor httpExecutor;

    public YonSuitePaymentClient(YonSuiteHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    /**
     * 付款单详情查询 (官方 API)
     * GET /yonbip/EFI/payment/detail
     */
    public YonPaymentDetailResponse queryPaymentDetail(String accessToken, String paymentId) {
        String url = httpExecutor.buildUrlWithParam(baseUrl, "/yonbip/EFI/payment/detail",
                accessToken, "id", paymentId);

        log.info("Calling YonSuite queryPaymentDetail: id={}", paymentId);

        try {
            // GET 请求
            String respStr = cn.hutool.http.HttpRequest.get(url)
                    .header("Content-Type", "application/json")
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite payment detail response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                log.warn("YonSuite queryPaymentDetail returned empty response for id={}", paymentId);
                return null;
            }

            YonPaymentDetailResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonPaymentDetailResponse.class);

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
     * 付款退款单文件下载查询
     * POST /yonbip/EFI/apRefund/file/url
     * 文档: 获取付款退款单文件
     */
    public YonRefundFileResponse queryRefundFileUrls(String accessToken, YonRefundFileRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/apRefund/file/url", accessToken);

        log.info("Calling YonSuite queryRefundFileUrls: fileIds={}", request.getFileId());

        try {
            String body = httpExecutor.getObjectMapper().writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = httpExecutor.postRaw(url, request);

            log.info("YonSuite refund file url response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonRefundFileResponse emptyResponse = new YonRefundFileResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                return emptyResponse;
            }

            YonRefundFileResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonRefundFileResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite refund file url API warning: {} - {}",
                        response.getCode(), response.getMessage());
            }

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryRefundFileUrls", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }

    /**
     * 付款退款单列表查询
     * POST /yonbip/EFI/apRefund/list
     * 文档: 付款退款单列表查询
     */
    public YonRefundListResponse queryRefundList(String accessToken, YonRefundListRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/apRefund/list", accessToken);

        log.info("Calling YonSuite queryRefundList: pageIndex={}, pageSize={}",
                request.getPageIndex(), request.getPageSize());

        try {
            String body = httpExecutor.getObjectMapper().writeValueAsString(request);
            log.debug("Request body: {}", body);

            String respStr = httpExecutor.postRaw(url, request);

            log.info("YonSuite refund list response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonRefundListResponse emptyResponse = new YonRefundListResponse();
                emptyResponse.setCode("200");
                emptyResponse.setMessage("No data");
                emptyResponse.setData(new YonRefundListResponse.PageData());
                return emptyResponse;
            }

            YonRefundListResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonRefundListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite refund list API warning: {} - {}",
                        response.getCode(), response.getMessage());
            }

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryRefundList", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }
}
