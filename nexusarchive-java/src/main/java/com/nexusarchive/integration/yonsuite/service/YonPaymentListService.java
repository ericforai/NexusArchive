package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * YonBIP Payment List Query Service (AI Generated)
 * Pass 2 Output: Implementation of query logic based on SIM JSON.
 *
 * @author AI Integration Agent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YonPaymentListService {

    private final YonAuthService yonAuthService;

    private static final int PAGE_SIZE = 100;

    /**
     * Query Payment Bill IDs for a given date range.
     *
     * @param config    ERP Configuration (contains BaseUrl, AppKey, etc.)
     * @param startDate Start Date
     * @param endDate   End Date
     * @return List of Payment Bill IDs
     */
    public List<String> queryPaymentIds(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 1. Validate Config
        if (config == null || config.getBaseUrl() == null) {
            log.warn("Invalid ErpConfig: BaseUrl is missing");
            return Collections.emptyList();
        }

        // 2. Construct URL
        // From SIM: endpoint = /yonbip/EFI/payment/list
        String fullUrl = config.getBaseUrl() + "/yonbip/EFI/payment/list";

        // 3. Prepare Request Body common fields
        JSONObject body = new JSONObject();
        body.set("pageSize", PAGE_SIZE);
        body.set("isSum", false);

        // Date Range
        if (startDate != null) {
            body.set("open_billDate_begin", startDate.atStartOfDay().format(DatePattern.NORM_DATETIME_FORMATTER));
        }
        if (endDate != null) {
            body.set("open_billDate_end", endDate.atTime(23, 59, 59).format(DatePattern.NORM_DATETIME_FORMATTER));
        }
        
        // 组织编码参数 (必填，参考收款单逻辑)
        // financeOrg 设为 null，使用 simple.financeOrg.code 传递编码
        if (config.getAccbookCode() != null && !config.getAccbookCode().isEmpty()) {
            JSONObject simple = new JSONObject();
            simple.set("financeOrg.code", config.getAccbookCode());
            body.set("simple", simple);
            log.info("付款单查询增加组织编码筛选: financeOrg.code={}", config.getAccbookCode());
        } else {
            log.warn("未配置 accbookCode，可能导致查询结果为空");
        }

        List<String> allIds = new ArrayList<>();
        int pageIndex = 1;

        // 4. Pagination Loop
        while (true) {
            body.set("pageIndex", pageIndex);

            log.info("Querying Payment List Page {}: {}", pageIndex, fullUrl);
            try {
                // 使用 YonAuthService 获取真实 Token
                String accessToken = yonAuthService.getAccessToken(config.getAppKey(), config.getAppSecret());
                
                // 将 access_token 作为 URL 查询参数（与 YonSuiteClient 保持一致）
                String urlWithToken = fullUrl + "?access_token=" + java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8);

                HttpResponse response = HttpRequest.post(urlWithToken)
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .timeout(10000)
                        .execute();

                if (!response.isOk()) {
                    log.error("Payment List Query Failed: Status {}", response.getStatus());
                    break;
                }

                String resBody = response.body();
                JSONObject jsonRes = JSONUtil.parseObj(resBody);

                // Check "code" field
                if (!"200".equals(jsonRes.getStr("code"))) {
                    log.error("Payment List Query Error: {}", jsonRes.getStr("message"));
                    break;
                }

                // Parse Data
                JSONObject data = jsonRes.getJSONObject("data");
                if (data == null) break;

                JSONArray recordList = data.getJSONArray("recordList");
                if (recordList == null || recordList.isEmpty()) {
                    break; // No more data
                }

                // Extract IDs
                for (int i = 0; i < recordList.size(); i++) {
                    JSONObject record = recordList.getJSONObject(i);
                    String id = record.getStr("id");
                    if (id != null) {
                        allIds.add(id);
                    }
                }

                // Check Pagination
                Long pageCount = data.getLong("pageCount");
                if (pageCount != null && pageIndex >= pageCount) {
                    break;
                }
                
                // Safety break
                if (pageIndex >= 100) break;

                pageIndex++;

            } catch (Exception e) {
                log.error("Exception during Payment List Query", e);
                break;
            }
        }

        log.info("Total Payment IDs found: {}", allIds.size());
        return allIds;
    }
}
