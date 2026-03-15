// Input: Spring Framework、Lombok、Hutool、Jackson、ERP DTO、Kingdee DTO
// Output: KingdeeVoucherClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.common.constants.HttpConstants;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeAuthResponse;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeVoucherDetailResponse;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeVoucherListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 金蝶云星空凭证客户端
 * 负责处理凭证相关的操作：认证、查询凭证列表、凭证详情
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>基于金蝶 K3Cloud WebAPI 实现</li>
 *   <li>支持会话认证 (ValidateUser)</li>
 *   <li>支持凭证列表查询 (ExecuteBillQuery)</li>
 *   <li>支持凭证详情查询 (View/Bill)</li>
 *   <li>返回原始金蝶响应 DTO，转换由 ErpMapper 处理</li>
 * </ul>
 *
 * <p>API 端点：</p>
 * <ul>
 *   <li>认证: /Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc</li>
 *   <li>查询: /Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc</li>
 * </ul>
 *
 * @author Agent D (基础设施工程师)
 */
@Component("kingdeeVoucherClient")
@Slf4j
public class KingdeeVoucherClient {

    private static final String AUTH_PATH = "/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_PATH = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";
    private static final String VIEW_PATH = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.View.common.kdsvc";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateFormat.DATE);
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ObjectMapper objectMapper;

    public KingdeeVoucherClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 认证并获取会话ID
     *
     * @param config ERP 配置
     * @return 认证响应，失败返回 null
     */
    public KingdeeAuthResponse authenticate(ErpConfig config) {
        try {
            String url = config.getBaseUrl() + AUTH_PATH;

            JSONObject params = JSONUtil.createObj()
                    .set("acctID", config.getTenantId())
                    .set("username", config.getAppKey())
                    .set("password", config.getAppSecret())
                    .set("lcid", 2052);

            log.debug("金蝶认证请求: url={}, acctID={}", url, config.getTenantId());

            String response = HttpRequest.post(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .body(params.toString())
                    .timeout(30000)
                    .execute()
                    .body();

            log.debug("金蝶认证响应: {}", response);

            KingdeeAuthResponse authResponse = objectMapper.readValue(response, KingdeeAuthResponse.class);

            if (authResponse.isSuccess()) {
                log.info("金蝶认证成功: sessionId={}", authResponse.getSessionId());
            } else {
                log.warn("金蝶认证失败: {}", authResponse.getMessage());
            }

            return authResponse;
        } catch (Exception e) {
            log.error("金蝶认证异常", e);
            return null;
        }
    }

    /**
     * 查询凭证列表
     *
     * @param config    ERP 配置
     * @param sessionId 会话ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 凭证列表响应
     */
    public KingdeeVoucherListResponse syncVouchers(ErpConfig config, String sessionId,
                                                   LocalDate startDate, LocalDate endDate) {
        try {
            String url = config.getBaseUrl() + QUERY_PATH;

            // 构建查询请求
            JSONObject request = buildVoucherListRequest(startDate, endDate);

            log.info("金蝶凭证查询: url={}, startDate={}, endDate={}", url, startDate, endDate);
            log.debug("请求体: {}", request.toString());

            String response = HttpRequest.post(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .body(request.toString())
                    .timeout(60000)
                    .execute()
                    .body();

            log.debug("金蝶凭证查询响应: {}", response);

            KingdeeVoucherListResponse listResponse = objectMapper.readValue(
                    response, KingdeeVoucherListResponse.class);

            if (listResponse.isSuccess()) {
                int count = listResponse.getData() != null &&
                        listResponse.getData().getVouchers() != null
                        ? listResponse.getData().getVouchers().size() : 0;
                log.info("金蝶凭证查询成功: 返回 {} 条记录", count);
            } else {
                log.warn("金蝶凭证查询失败: code={}, message={}",
                        listResponse.getCode(), listResponse.getMessage());
            }

            return listResponse;

        } catch (Exception e) {
            log.error("金蝶凭证查询异常", e);
            KingdeeVoucherListResponse errorResponse = new KingdeeVoucherListResponse();
            errorResponse.setCode("500");
            errorResponse.setMessage(e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 获取凭证详情
     *
     * @param config    ERP 配置
     * @param sessionId 会话ID
     * @param voucherId 凭证ID
     * @return 凭证详情响应
     */
    public KingdeeVoucherDetailResponse getVoucherDetail(ErpConfig config, String sessionId,
                                                         String voucherId) {
        try {
            String url = config.getBaseUrl() + VIEW_PATH;

            JSONObject request = JSONUtil.createObj()
                    .set("FormId", "GL_VOUCHER")
                    .set("Data", JSONUtil.createObj()
                            .set("FVoucherID", voucherId));

            log.info("金蝶凭证详情查询: voucherId={}", voucherId);
            log.debug("请求体: {}", request.toString());

            String response = HttpRequest.post(url)
                    .header(HttpConstants.CONTENT_TYPE, HttpConstants.APPLICATION_JSON)
                    .body(request.toString())
                    .timeout(30000)
                    .execute()
                    .body();

            log.debug("金蝶凭证详情响应: {}", response);

            KingdeeVoucherDetailResponse detailResponse = objectMapper.readValue(
                    response, KingdeeVoucherDetailResponse.class);

            if (detailResponse.isSuccess()) {
                log.info("金蝶凭证详情查询成功: voucherId={}", voucherId);
            } else {
                log.warn("金蝶凭证详情查询失败: code={}, message={}",
                        detailResponse.getCode(), detailResponse.getMessage());
            }

            return detailResponse;

        } catch (Exception e) {
            log.error("金蝶凭证详情查询异常: voucherId={}", voucherId, e);
            KingdeeVoucherDetailResponse errorResponse = new KingdeeVoucherDetailResponse();
            errorResponse.setCode("500");
            errorResponse.setMessage(e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 测试连接
     *
     * @param config ERP 配置
     * @return 连接是否成功
     */
    public boolean testConnection(ErpConfig config) {
        KingdeeAuthResponse authResponse = authenticate(config);
        return authResponse != null && authResponse.isSuccess();
    }

    /**
     * 构建凭证列表查询请求
     */
    private JSONObject buildVoucherListRequest(LocalDate startDate, LocalDate endDate) {
        // 构建字段列表
        List<String> fieldKeys = List.of(
                "FVoucherID",
                "FNumber",
                "FDate",
                "FYear",
                "FPeriod",
                "FVoucherGroup",
                "FCreator",
                "FCreatorName",
                "FAuditor",
                "FAuditorName",
                "FDocumentStatus",
                "FDebitTotal",
                "FCreditTotal",
                "FAttachmentCount",
                "FCreateDate",
                "FExplanation"
        );

        // 构建过滤条件
        String filterString = String.format(
                "FDate>='%s' AND FDate<='%s'",
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER)
        );

        return JSONUtil.createObj()
                .set("FormId", "GL_VOUCHER")
                .set("FieldKeys", String.join(",", fieldKeys))
                .set("FilterString", filterString)
                .set("OrderString", "FDate ASC")
                .set("TopRowCount", 100)
                .set("StartRow", 0);
    }
}
