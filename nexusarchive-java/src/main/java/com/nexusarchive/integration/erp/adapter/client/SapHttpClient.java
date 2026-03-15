// Input: Hutool、Jackson、Lombok、Java 标准库
// Output: SapHttpClient 类
// Pos: 基础设施层 - SAP S/4HANA OData HTTP 客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.sap.SapErrorResponse;
import com.nexusarchive.integration.erp.dto.sap.SapJournalEntryDto;
import com.nexusarchive.integration.erp.exception.ErpException;
import com.nexusarchive.common.constants.HttpConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * SAP S/4HANA OData HTTP 客户端
 *
 * <p>负责调用 SAP S/4HANA OData 服务接口</p>
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>基于 SAP S/4HANA API Journal Entry OData V4 服务</li>
 *   <li>支持凭证列表查询 (带日期过滤)</li>
 *   <li>支持凭证详情查询 (展开分录和附件)</li>
 *   <li>Basic 认证支持</li>
 *   <li>返回原始 SAP 响应 DTO，转换由 ErpMapper 处理</li>
 * </ul>
 *
 * @author Agent D (基础设施工程师)
 * @see <a href="https://help.sap.com/doc/">SAP S/4HANA OData API</a>
 */
@Component
@Slf4j
public class SapHttpClient {

    private final ObjectMapper objectMapper;

    /**
     * OData 服务路径
     * API_JOURNAL_ENTRY_SRV
     */
    private static final String JOURNAL_ENTRY_PATH =
        "/sap/opu/odata4/sap/api_journal_entry/srvd_a2x/sap/journal_entry/0001";

    /**
     * SAP 借贷标识映射
     * S = Soll (德语: 借方)
     * H = Haben (德语: 贷方)
     */
    private static final String DEBIT_CODE = "S";
    private static final String CREDIT_CODE = "H";

    public SapHttpClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 构建 OData 查询 URL
     *
     * @param config ERP 配置
     * @param startDate 起始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 完整的 OData 查询 URL
     */
    public String buildQueryUrl(ErpConfig config, String startDate, String endDate) {
        StringBuilder url = new StringBuilder(config.getBaseUrl());
        url.append(JOURNAL_ENTRY_PATH);
        url.append("/JournalEntry");

        // 构建 $filter 查询参数
        url.append("?$filter=CompanyCode%20eq%20'");
        url.append(urlEncode(config.getTenantId()));
        url.append("'");

        url.append("%20and%20PostingDate%20ge%20");
        url.append(urlEncode(startDate));

        url.append("%20and%20PostingDate%20le%20");
        url.append(urlEncode(endDate));

        log.debug("Built SAP query URL: {}", url);
        return url.toString();
    }

    /**
     * 构建凭证详情查询 URL
     *
     * @param config ERP 配置
     * @param journalEntry 凭证号
     * @param fiscalYear 会计年度
     * @return 完整的 OData 详情查询 URL (带 $expand)
     */
    public String buildDetailUrl(ErpConfig config, String journalEntry, String fiscalYear) {
        StringBuilder url = new StringBuilder(config.getBaseUrl());
        url.append(JOURNAL_ENTRY_PATH);
        url.append("/JournalEntry(JournalEntry='");
        url.append(urlEncode(journalEntry));
        url.append("',FiscalYear='");
        url.append(urlEncode(fiscalYear));
        url.append("')");

        // 扩展导航属性
        url.append("?$expand=to_JournalEntryItem,to_Attachment");

        log.debug("Built SAP detail URL: {}", url);
        return url.toString();
    }

    /**
     * 查询凭证列表
     *
     * @param config ERP 配置
     * @param startDate 起始日期
     * @param endDate 结束日期
     * @return 凭证列表响应
     * @throws IOException 网络或解析异常
     * @throws ErpException SAP 业务异常
     */
    public SapJournalEntryListResponse queryJournalEntries(
            ErpConfig config,
            String startDate,
            String endDate) throws IOException {

        String url = buildQueryUrl(config, startDate, endDate);

        HttpResponse response = HttpRequest.get(url)
            .header("Accept", HttpConstants.APPLICATION_JSON)
            .header("x-csrf-token", "Fetch") // SAP CSRF 保护
            .basicAuth(config.getAppKey(), config.getAppSecret())
            .timeout(30000)
            .execute();

        String json = response.body();
        int statusCode = response.getStatus();

        log.debug("SAP query response status: {}, body: {}", statusCode, json);

        if (statusCode >= 400) {
            handleErrorResponse(json);
        }

        return objectMapper.readValue(json, SapJournalEntryListResponse.class);
    }

    /**
     * 获取凭证详情 (包含分录和附件)
     *
     * @param config ERP 配置
     * @param journalEntry 凭证号
     * @param fiscalYear 会计年度
     * @return 凭证详情
     * @throws IOException 网络或解析异常
     * @throws ErpException SAP 业务异常
     */
    public SapJournalEntryDto getJournalEntryDetail(
            ErpConfig config,
            String journalEntry,
            String fiscalYear) throws IOException {

        String url = buildDetailUrl(config, journalEntry, fiscalYear);

        HttpResponse response = HttpRequest.get(url)
            .header("Accept", HttpConstants.APPLICATION_JSON)
            .basicAuth(config.getAppKey(), config.getAppSecret())
            .timeout(30000)
            .execute();

        String json = response.body();
        int statusCode = response.getStatus();

        log.debug("SAP detail response status: {}", statusCode);

        if (statusCode >= 400) {
            handleErrorResponse(json);
        }

        return objectMapper.readValue(json, SapJournalEntryDto.class);
    }

    /**
     * 处理错误响应
     */
    private void handleErrorResponse(String json) throws IOException {
        try {
            SapErrorResponse error = objectMapper.readValue(json, SapErrorResponse.class);
            throw new ErpException("SAP API error: " + error.getMessage()
                + " (code: " + error.getCode() + ")");
        } catch (Exception e) {
            throw new ErpException("SAP API error: " + json);
        }
    }

    /**
     * URL 编码
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 映射 SAP 借贷标识到标准值
     *
     * @param sapCode SAP 借贷码 (S=借, H=贷)
     * @return DEBIT/CREDIT 或 null
     */
    public static String mapDebitCreditCode(String sapCode) {
        if (DEBIT_CODE.equals(sapCode)) {
            return "DEBIT";
        } else if (CREDIT_CODE.equals(sapCode)) {
            return "CREDIT";
        }
        return null;
    }

    /**
     * SAP 凭证列表响应包装类
     * OData V4 JSON 格式
     */
    @lombok.Data
    public static class SapJournalEntryListResponse {
        /**
         * OData 响应包装
         */
        @com.fasterxml.jackson.annotation.JsonProperty("d")
        private SapJournalEntryCollection collection;

        /**
         * 结果列表快捷访问
         */
        public java.util.List<SapJournalEntryDto> getResults() {
            return collection != null ? collection.getResults() : java.util.Collections.emptyList();
        }

        @lombok.Data
        public static class SapJournalEntryCollection {
            @com.fasterxml.jackson.annotation.JsonProperty("results")
            private java.util.List<SapJournalEntryDto> results;
        }
    }
}
