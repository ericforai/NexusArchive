// Input: cn.hutool、Lombok、Spring Framework、Java 标准库、等
// Output: YonRefundListService 类
// Pos: 业务服务层
// 更新时请同步更新本文件注释及所属目录的 md

package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * YonSuite 付款退款单列表查询服务
 *
 * @author AI Integration Agent
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YonRefundListService {

    private final YonAuthService yonAuthService;

    private static final int PAGE_SIZE = 100;

    /**
     * 查询指定日期范围内的付款退款单 ID 列表
     *
     * @param config    ERP 配置（包含 BaseUrl、AppKey 等）
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 退款单 ID 列表
     */
    public List<String> queryRefundIds(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 1. 验证配置
        if (config == null || config.getBaseUrl() == null) {
            log.warn("Invalid ErpConfig: BaseUrl is missing");
            return Collections.emptyList();
        }

        // 2. 构建请求 URL
        String fullUrl = config.getBaseUrl() + "/yonbip/EFI/apRefund/list";

        // 3. 准备请求体公共字段
        JSONObject body = new JSONObject();
        body.set("pageSize", PAGE_SIZE);
        body.set("isSum", false);

        // 日期范围
        if (startDate != null) {
            body.set("open_billDate_begin", startDate.atStartOfDay().format(DatePattern.NORM_DATETIME_FORMATTER));
        }
        if (endDate != null) {
            body.set("open_billDate_end", endDate.atTime(23, 59, 59).format(DatePattern.NORM_DATETIME_FORMATTER));
        }

        // 单据状态：0-开立, 1-审批中, 2-已审批
        JSONArray verifyStates = new JSONArray();
        verifyStates.add("0");
        verifyStates.add("2");
        body.set("verifyState", verifyStates);

        // 组织编码（如果配置了）
        if (config.getAccbookCode() != null && !config.getAccbookCode().isEmpty()) {
            JSONObject simple = new JSONObject();
            simple.set("financeOrg.code", config.getAccbookCode());
            body.set("simple", simple);
            log.info("退款单查询增加组织编码筛选: financeOrg.code={}", config.getAccbookCode());
        }

        List<String> allIds = new ArrayList<>();
        int pageIndex = 1;

        // 4. 分页循环
        while (true) {
            body.set("pageIndex", String.valueOf(pageIndex));

            log.info("Querying Refund List Page {}: {}", pageIndex, fullUrl);
            try {
                // 获取 access token
                String accessToken = yonAuthService.getAccessToken(config.getAppKey(), config.getAppSecret());

                // 将 access_token 作为 URL 查询参数
                String urlWithToken = fullUrl + "?access_token="
                        + java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8);

                HttpResponse response = HttpRequest.post(urlWithToken)
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .timeout(10000)
                        .execute();

                if (!response.isOk()) {
                    log.error("Refund List Query Failed: Status {}", response.getStatus());
                    break;
                }

                String resBody = response.body();
                JSONObject jsonRes = JSONUtil.parseObj(resBody);

                // 检查返回码
                if (!"200".equals(jsonRes.getStr("code"))) {
                    log.error("Refund List Query Error: {}", jsonRes.getStr("message"));
                    break;
                }

                // 解析数据
                JSONObject data = jsonRes.getJSONObject("data");
                if (data == null)
                    break;

                JSONArray recordList = data.getJSONArray("recordList");
                if (recordList == null || recordList.isEmpty()) {
                    break; // 没有更多数据
                }

                // 提取 ID
                for (int i = 0; i < recordList.size(); i++) {
                    JSONObject record = recordList.getJSONObject(i);
                    if (i == 0) {
                        log.debug("YonRefundListService First Record: {}", record);
                    }
                    String id = record.getStr("id");
                    if (id != null) {
                        allIds.add(id);
                    }
                }

                // 检查分页
                Long pageCount = data.getLong("pageCount");
                if (pageCount != null && pageIndex >= pageCount) {
                    break;
                }

                // 安全退出
                if (pageIndex >= 100)
                    break;

                pageIndex++;

            } catch (Exception e) {
                log.error("Exception during Refund List Query", e);
                break;
            }
        }

        log.info("Total Refund IDs found: {}", allIds.size());
        return allIds;
    }

    /**
     * 查询指定日期范围内的付款退款单列表
     *
     * @param config    ERP 配置
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 退款单列表
     */
    public List<com.nexusarchive.integration.yonsuite.dto.YonRefundListResponse.RefundRecord> queryRefundList(
            ErpConfig config, LocalDate startDate, LocalDate endDate) {

        // 1. 验证配置
        if (config == null || config.getBaseUrl() == null) {
            log.warn("Invalid ErpConfig: BaseUrl is missing");
            return Collections.emptyList();
        }

        // 2. 构建请求 URL
        String fullUrl = config.getBaseUrl() + "/yonbip/EFI/apRefund/list";

        // 3. 准备请求体
        JSONObject body = new JSONObject();
        body.set("pageSize", PAGE_SIZE);
        body.set("isSum", false);

        // 日期范围
        if (startDate != null) {
            body.set("open_billDate_begin", startDate.atStartOfDay().format(DatePattern.NORM_DATETIME_FORMATTER));
        }
        if (endDate != null) {
            body.set("open_billDate_end", endDate.atTime(23, 59, 59).format(DatePattern.NORM_DATETIME_FORMATTER));
        }

        // 单据状态
        JSONArray verifyStates = new JSONArray();
        verifyStates.add("0");
        verifyStates.add("2");
        body.set("verifyState", verifyStates);

        // 组织编码
        if (config.getAccbookCode() != null && !config.getAccbookCode().isEmpty()) {
            JSONObject simple = new JSONObject();
            simple.set("financeOrg.code", config.getAccbookCode());
            body.set("simple", simple);
        }

        List<com.nexusarchive.integration.yonsuite.dto.YonRefundListResponse.RefundRecord> allRecords = new ArrayList<>();
        int pageIndex = 1;

        // 4. 分页循环
        while (true) {
            body.set("pageIndex", String.valueOf(pageIndex));

            log.info("Querying Refund List Page {}: {}", pageIndex, fullUrl);
            try {
                String accessToken = yonAuthService.getAccessToken(config.getAppKey(), config.getAppSecret());
                String urlWithToken = fullUrl + "?access_token="
                        + java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8);

                HttpResponse response = HttpRequest.post(urlWithToken)
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .timeout(10000)
                        .execute();

                if (!response.isOk()) {
                    log.error("Refund List Query Failed: Status {}", response.getStatus());
                    break;
                }

                String resBody = response.body();
                JSONObject jsonRes = JSONUtil.parseObj(resBody);

                if (!"200".equals(jsonRes.getStr("code"))) {
                    log.error("Refund List Query Error: {}", jsonRes.getStr("message"));
                    break;
                }

                JSONObject data = jsonRes.getJSONObject("data");
                if (data == null)
                    break;

                JSONArray recordList = data.getJSONArray("recordList");
                if (recordList == null || recordList.isEmpty()) {
                    break;
                }

                // 提取记录
                for (int i = 0; i < recordList.size(); i++) {
                    JSONObject record = recordList.getJSONObject(i);
                    com.nexusarchive.integration.yonsuite.dto.YonRefundListResponse.RefundRecord refundRecord =
                        JSONUtil.toBean(record, com.nexusarchive.integration.yonsuite.dto.YonRefundListResponse.RefundRecord.class);
                    allRecords.add(refundRecord);
                }

                // 检查分页
                Long pageCount = data.getLong("pageCount");
                if (pageCount != null && pageIndex >= pageCount) {
                    break;
                }

                if (pageIndex >= 100)
                    break;

                pageIndex++;

            } catch (Exception e) {
                log.error("Exception during Refund List Query", e);
                break;
            }
        }

        log.info("Total Refund Records found: {}", allRecords.size());
        return allRecords;
    }
}
