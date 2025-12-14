package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.dto.*;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 金蝶云星空 ERP 适配器
 * 对接金蝶 K3Cloud API
 * 
 * @author Agent D (基础设施工程师)
 */
@Service("kingdee")
@Slf4j
public class KingdeeAdapter implements ErpAdapter {

    private static final String AUTH_PATH = "/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
    private static final String QUERY_PATH = "/Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc";

    @Override
    public String getIdentifier() {
        return "kingdee";
    }

    @Override
    public String getName() {
        return "金蝶云星空";
    }

    @Override
    public String getDescription() {
        return "金蝶云星空 ERP 系统，支持凭证查询和同步";
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            String url = config.getBaseUrl() + AUTH_PATH;

            JSONObject params = JSONUtil.createObj()
                    .set("acctID", config.getTenantId())
                    .set("username", config.getAppKey())
                    .set("password", config.getAppSecret())
                    .set("lcid", 2052);

            String response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(params.toString())
                    .timeout(10000)
                    .execute()
                    .body();

            long responseTime = System.currentTimeMillis() - startTime;

            JSONObject result = JSONUtil.parseObj(response);

            if (result.containsKey("LoginResultType") &&
                    result.getInt("LoginResultType") == 1) {
                return ConnectionTestResult.success("连接成功", responseTime);
            } else {
                String msg = result.getStr("Message", "认证失败");
                return ConnectionTestResult.fail(msg, "AUTH_FAILED");
            }

        } catch (Exception e) {
            log.error("金蝶连接测试失败", e);
            return ConnectionTestResult.fail(e.getMessage(), "CONNECTION_ERROR");
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("金蝶凭证同步: {} ~ {}", startDate, endDate);

        try {
            // 首先认证获取会话
            String authResult = authenticate(config);
            if (authResult == null) {
                log.error("金蝶认证失败");
                return Collections.emptyList();
            }

            // 查询凭证
            String url = config.getBaseUrl() + QUERY_PATH;

            // 构建查询请求
            JSONObject queryParams = JSONUtil.createObj()
                    .set("FormId", "GL_VOUCHER")
                    .set("FieldKeys", "FVoucherID,FDate,FNumber,FAttachments,FEXPLANATION")
                    .set("FilterString", String.format(
                            "FDate>='%s' AND FDate<='%s'",
                            startDate.format(DateTimeFormatter.ISO_DATE),
                            endDate.format(DateTimeFormatter.ISO_DATE)))
                    .set("OrderString", "FDate ASC")
                    .set("TopRowCount", 100)
                    .set("StartRow", 0);

            String response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(queryParams.toString())
                    .timeout(30000)
                    .execute()
                    .body();

            // 解析结果
            List<VoucherDTO> vouchers = new ArrayList<>();
            // TODO: 解析金蝶返回的凭证数据

            return vouchers;

        } catch (Exception e) {
            log.error("金蝶凭证同步异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        log.info("金蝶获取凭证详情: {}", voucherNo);
        // TODO: 实现凭证详情查询
        return null;
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        log.info("金蝶获取凭证附件: {}", voucherNo);
        // TODO: 实现附件获取
        return Collections.emptyList();
    }

    private String authenticate(ErpConfig config) {
        try {
            String url = config.getBaseUrl() + AUTH_PATH;

            JSONObject params = JSONUtil.createObj()
                    .set("acctID", config.getTenantId())
                    .set("username", config.getAppKey())
                    .set("password", config.getAppSecret())
                    .set("lcid", 2052);

            String response = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(params.toString())
                    .timeout(10000)
                    .execute()
                    .body();

            JSONObject result = JSONUtil.parseObj(response);

            if (result.containsKey("LoginResultType") &&
                    result.getInt("LoginResultType") == 1) {
                return "OK";
            }

            return null;
        } catch (Exception e) {
            log.error("金蝶认证失败", e);
            return null;
        }
    }

    @Override
    public List<com.nexusarchive.entity.ErpScenario> getAvailableScenarios() {
        List<com.nexusarchive.entity.ErpScenario> scenarios = new ArrayList<>();

        // 场景1: 凭证同步
        com.nexusarchive.entity.ErpScenario voucher = new com.nexusarchive.entity.ErpScenario();
        voucher.setScenarioKey("VOUCHER_SYNC");
        voucher.setName("凭证同步");
        voucher.setDescription("自动采集金蝶云星空生成的财务凭证，并在归档系统中生成会计档案。支持附件自动下载。");
        voucher.setSyncStrategy("CRON");
        voucher.setCronExpression("0 0 23 * * ?"); // 默认每天23点
        voucher.setIsActive(false); // 默认不开启
        scenarios.add(voucher);

        // 场景2: 存货核算
        com.nexusarchive.entity.ErpScenario inventory = new com.nexusarchive.entity.ErpScenario();
        inventory.setScenarioKey("INVENTORY_SYNC");
        inventory.setName("存货核算同步");
        inventory.setDescription("同步期末存货余额表、收发存汇总表等报表数据。");
        inventory.setSyncStrategy("CRON");
        inventory.setCronExpression("0 0 1 * * ?"); // 默认每天凌晨1点
        inventory.setIsActive(false);
        scenarios.add(inventory);

        // 场景3: 费用报销
        com.nexusarchive.entity.ErpScenario expense = new com.nexusarchive.entity.ErpScenario();
        expense.setScenarioKey("EXPENSE_SYNC");
        expense.setName("费用报销单据同步");
        expense.setDescription("同步员工费用报销单据作为原始凭证。");
        expense.setSyncStrategy("MANUAL"); // 默认手动
        expense.setIsActive(false);
        scenarios.add(expense);

        return scenarios;
    }
}
