// Input: Hutool、Jackson、Spring
// Output: YonSuiteSalesOrderClient
// Pos: YonSuite 集成 - 客户端

package com.nexusarchive.integration.yonsuite.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.nexusarchive.integration.erp.exception.ErpException;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nexusarchive.common.constants.HttpConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * YonSuite 销售订单 API 客户端
 * <p>
 * 处理销售订单相关的 API 调用：
 * <ul>
 *   <li>销售订单列表查询</li>
 *   <li>销售订单详情查询</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteSalesOrderClient {

    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;

    private final YonSuiteHttpExecutor httpExecutor;

    /**
     * 查询销售订单列表
     * POST /yonbip/sd/voucherorder/list
     */
    public SalesOrderListResponse querySalesOrders(String accessToken, SalesOrderListRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/sd/voucherorder/list", accessToken);

        log.info("Calling YonSuite querySalesOrders: vouchdateBegin={}, vouchdateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());

        String respStr = httpExecutor.postRaw(url, request);

        if (respStr == null || respStr.isEmpty()) {
            SalesOrderListResponse empty = new SalesOrderListResponse();
            empty.setCode("200");
            empty.setMessage("No data");
            return empty;
        }

        try {
            SalesOrderListResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, SalesOrderListResponse.class);

            if (!"200".equals(response.getCode())) {
                log.error("YonSuite querySalesOrders error: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to parse querySalesOrders response", e);
            throw new ErpException("YonSuite API 响应解析失败: " + e.getMessage(), "PARSE_ERROR", "YonSuite", e);
        }
    }

    /**
     * 查询销售订单详情
     * GET /yonbip/sd/voucherorder/detail
     */
    public SalesOrderDetailResponse querySalesOrderById(String accessToken, String orderId) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/sd/voucherorder/detail", accessToken);

        log.info("Calling YonSuite querySalesOrderById: orderId={}", orderId);

        // 添加 id 参数（URL 编码防止注入）
        url += "&id=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8);

        try {
            HttpResponse httpResponse = HttpRequest.get(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .timeout(30_000)
                    .execute();

            String respStr = httpResponse.body();

            if (respStr == null || respStr.isEmpty()) {
                return null;
            }

            SalesOrderDetailResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, SalesOrderDetailResponse.class);

            if (!"200".equals(response.getCode())) {
                log.error("YonSuite querySalesOrderById error: {} - {}", response.getCode(), response.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to parse querySalesOrderById response", e);
            throw new ErpException("YonSuite API 响应解析失败: " + e.getMessage(), "PARSE_ERROR", "YonSuite", e);
        }
    }
}
