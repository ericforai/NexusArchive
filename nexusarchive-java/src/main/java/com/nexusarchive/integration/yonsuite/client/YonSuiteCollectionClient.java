// Input: Hutool, Jackson, Spring Framework
// Output: YonSuiteCollectionClient 类
// Pos: 外部系统客户端 - 收款单相关

package com.nexusarchive.integration.yonsuite.client;

import com.nexusarchive.integration.yonsuite.dto.*;
import lombok.extern.slf4j.Slf4j;
import com.nexusarchive.common.constants.HttpConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * YonSuite 收款单 API 客户端
 * <p>
 * 处理收款单相关的 API 调用：
 * <ul>
 *   <li>收款单文件查询</li>
 *   <li>收款单列表查询</li>
 *   <li>收款单详情查询</li>
 * </ul>
 * </p>
 */
@Component
@Slf4j
public class YonSuiteCollectionClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonSuiteHttpExecutor httpExecutor;

    public YonSuiteCollectionClient(YonSuiteHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    /**
     * 获取收款单文件
     * POST /yonbip/EFI/collection/file/url
     */
    public YonCollectionFileResponse queryCollectionFiles(String accessToken, YonCollectionFileRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/collection/file/url", accessToken);

        try {
            String body = httpExecutor.getObjectMapper().writeValueAsString(request);
            log.info("Calling YonSuite collection file url: fileIds={}", request.getFileId());

            String respStr = httpExecutor.postRaw(url, request);

            log.debug("YonSuite collection file response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                YonCollectionFileResponse empty = new YonCollectionFileResponse();
                empty.setCode("200");
                empty.setMessage("No data");
                empty.setData(java.util.Collections.emptyList());
                return empty;
            }

            YonCollectionFileResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonCollectionFileResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite collection file API returned code {} message {}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call YonSuite collection file API", e);
            throw new RuntimeException("Failed to call YonSuite collection file API: " + e.getMessage(), e);
        }
    }

    /**
     * 查询收款单列表
     * POST /yonbip/EFI/collection/list
     */
    public YonCollectionBillResponse queryCollectionBills(String accessToken, YonCollectionBillRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/EFI/collection/list", accessToken);

        try {
            String body = httpExecutor.getObjectMapper().writeValueAsString(request);
            log.info("Calling YonSuite queryCollectionBills: body={}", body);

            String respStr = httpExecutor.postRaw(url, request);

            log.info("YonSuite collection list response: {}", respStr);

            if (respStr == null || respStr.isEmpty()) {
                return null;
            }

            YonCollectionBillResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, YonCollectionBillResponse.class);

            if (!"200".equals(response.getCode())) {
                log.warn("YonSuite collection list API warning: {} - {}",
                        response.getCode(), response.getMessage());
            }
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call YonSuite queryCollectionBills", e);
            throw new RuntimeException("YonSuite API Error", e);
        }
    }

    /**
     * 收款单详情查询 (官方 API)
     * GET /yonbip/EFI/collection/detail
     * 文档: docs/api/收款单详情查询.md
     */
    public YonCollectionDetailResponse queryCollectionDetail(String accessToken, String collectionId) {
        String url = httpExecutor.buildUrlWithParam(baseUrl, "/yonbip/EFI/collection/detail",
                accessToken, "id", collectionId);

        log.info("Calling YonSuite queryCollectionDetail: id={}", collectionId);

        try {
            String respStr = httpExecutor.postRaw(url, null);
            // 使用 GET 请求
            String respStrGet = cn.hutool.http.HttpRequest.get(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .timeout(30_000)
                    .execute()
                    .body();

            log.debug("YonSuite collection detail response: {}", respStrGet);

            if (respStrGet == null || respStrGet.isEmpty()) {
                log.warn("YonSuite queryCollectionDetail returned empty response for id={}", collectionId);
                return null;
            }

            YonCollectionDetailResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStrGet, YonCollectionDetailResponse.class);

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
