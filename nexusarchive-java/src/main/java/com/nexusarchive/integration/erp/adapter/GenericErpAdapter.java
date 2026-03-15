// Input: cn.hutool、Lombok、Spring Framework、Java 标准库、等
// Output: GenericErpAdapter 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import com.nexusarchive.common.constants.HttpConstants;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用 REST API ERP 适配器
 * 支持配置化对接标准 REST API 的 ERP 系统
 *
 * @author Agent D (基础设施工程师)
 */
@ErpAdapterAnnotation(
    identifier = "generic",
    name = "通用ERP",
    description = "通用 ERP 适配器，支持标准接口协议",
    version = "1.0.0",
    erpType = "GENERIC",
    supportedScenarios = {"VOUCHER_SYNC"},
    supportsWebhook = false,
    priority = 100
)
@Service("generic")
@Slf4j
public class GenericErpAdapter implements ErpAdapter {

    @Override
    public String getIdentifier() {
        return "generic";
    }

    @Override
    public String getName() {
        return "通用REST接口";
    }

    @Override
    public String getDescription() {
        return "通用 REST API 适配器，支持自定义 API 端点配置，适用于标准 REST 接口的 ERP 系统";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            JSONObject extraConfig = parseExtraConfig(config.getExtraConfig());
            String testEndpoint = extraConfig.getStr("testEndpoint", "/api/health");
            
            String url = config.getBaseUrl() + testEndpoint;
            
            String response = HttpRequest.get(url)
                .header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER_PREFIX + config.getAppKey())
                .header("X-API-Key", config.getAppSecret())
                .timeout(10000)
                .execute()
                .body();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            return ConnectionTestResult.success("连接成功", responseTime);
            
        } catch (Exception e) {
            log.error("通用适配器连接测试失败", e);
            return ConnectionTestResult.fail(e.getMessage(), "CONNECTION_ERROR");
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("通用适配器凭证同步: {} ~ {}", startDate, endDate);
        
        try {
            JSONObject extraConfig = parseExtraConfig(config.getExtraConfig());
            String vouchersEndpoint = extraConfig.getStr("vouchersEndpoint", "/api/vouchers");
            String dateFormat = extraConfig.getStr("dateFormat", "yyyy-MM-dd");
            
            String url = String.format("%s%s?startDate=%s&endDate=%s",
                config.getBaseUrl(),
                vouchersEndpoint,
                startDate.format(DateTimeFormatter.ofPattern(dateFormat)),
                endDate.format(DateTimeFormatter.ofPattern(dateFormat))
            );
            
            String response = HttpRequest.get(url)
                .header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER_PREFIX + config.getAppKey())
                .header("X-API-Key", config.getAppSecret())
                .timeout(30000)
                .execute()
                .body();
            
            // 解析响应
            return parseVouchersResponse(response, extraConfig);
            
        } catch (Exception e) {
            log.error("通用适配器凭证同步异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        log.info("通用适配器获取凭证详情: {}", voucherNo);
        
        try {
            JSONObject extraConfig = parseExtraConfig(config.getExtraConfig());
            String detailEndpoint = extraConfig.getStr("voucherDetailEndpoint", "/api/vouchers/{id}");
            
            String url = config.getBaseUrl() + detailEndpoint.replace("{id}", voucherNo);
            
            String response = HttpRequest.get(url)
                .header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER_PREFIX + config.getAppKey())
                .header("X-API-Key", config.getAppSecret())
                .timeout(10000)
                .execute()
                .body();
            
            // TODO: 解析单个凭证
            return null;
            
        } catch (Exception e) {
            log.error("通用适配器获取凭证详情异常", e);
            return null;
        }
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        log.info("通用适配器获取凭证附件: {}", voucherNo);
        // TODO: 实现附件获取
        return Collections.emptyList();
    }

    private JSONObject parseExtraConfig(String extraConfigStr) {
        if (extraConfigStr == null || extraConfigStr.isEmpty()) {
            return JSONUtil.createObj();
        }
        try {
            return JSONUtil.parseObj(extraConfigStr);
        } catch (Exception e) {
            log.warn("解析 extraConfig 失败: {}", e.getMessage());
            return JSONUtil.createObj();
        }
    }

    private List<VoucherDTO> parseVouchersResponse(String response, JSONObject extraConfig) {
        List<VoucherDTO> vouchers = new ArrayList<>();
        
        try {
            // 支持配置响应路径
            String dataPath = extraConfig.getStr("responsePath", "data");
            
            JSONObject json = JSONUtil.parseObj(response);
            JSONArray dataArray = json.getJSONArray(dataPath);
            
            if (dataArray == null) {
                return vouchers;
            }
            
            // 字段映射
            String idField = extraConfig.getStr("idField", "id");
            String noField = extraConfig.getStr("noField", "voucherNo");
            String dateField = extraConfig.getStr("dateField", "date");
            String summaryField = extraConfig.getStr("summaryField", "summary");
            
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject item = dataArray.getJSONObject(i);
                VoucherDTO dto = VoucherDTO.builder()
                    .voucherId(item.getStr(idField))
                    .voucherNo(item.getStr(noField))
                    .summary(item.getStr(summaryField))
                    .build();
                vouchers.add(dto);
            }
            
        } catch (Exception e) {
            log.error("解析凭证响应失败", e);
        }
        
        return vouchers;
    }
}
