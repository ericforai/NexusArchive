// Input: Lombok、Spring Framework、Java 标准库、本地模块、ERP 客户端
// Output: YonSuiteErpAdapter 类（门面模式）
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.adapter.client.*;
import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.entity.ErpScenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * YonSuite (用友) ERP 适配器 - 门面模式
 * 将各个专门的客户端组合为统一接口
 *
 * 重构说明：
 * - 原有 728 行代码已拆分为多个专门的客户端类
 * - 本类作为 Facade，委托调用到对应的客户端
 * - 保持原有 API 接口不变，确保向后兼容
 *
 * @author Agent D (基础设施工程师)
 */
@ErpAdapterAnnotation(
    identifier = "yonsuite",
    name = "用友YonSuite",
    description = "用友新一代企业云服务平台，支持凭证、附件、收款单、付款单、退款单同步和 Webhook 推送",
    version = "1.0.0",
    erpType = "YONSUITE",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC", "COLLECTION_FILE_SYNC", "PAYMENT_FILE_SYNC", "REFUND_FILE_SYNC"},
    supportsWebhook = true,
    priority = 10
)
@Service("yonsuite")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteErpAdapter implements ErpAdapter {

    // 专门的客户端（通过依赖注入）
    private final YonSuiteAuthClient authClient;
    private final YonSuiteVoucherClient voucherClient;
    private final YonSuiteCollectionClient collectionClient;
    private final YonSuitePaymentClient paymentClient;
    private final YonSuiteRefundClient refundClient;
    private final YonSuiteFeedbackClient feedbackClient;

    @Override
    public String getIdentifier() {
        return "yonsuite";
    }

    @Override
    public String getName() {
        return "用友YonSuite";
    }

    @Override
    public String getDescription() {
        return "用友新一代企业云服务平台，支持凭证自动归档和 Webhook 实时推送";
    }

    @Override
    public boolean supportsWebhook() {
        return true;
    }

    @Override
    public List<ErpScenario> getAvailableScenarios() {
        List<ErpScenario> scenarios = new ArrayList<>();

        scenarios.add(createScenario("VOUCHER_SYNC", "凭证同步",
                "从用友YonSuite同步会计凭证到档案系统", "MANUAL"));

        scenarios.add(createScenario("ATTACHMENT_SYNC", "附件同步",
                "同步凭证关联的电子发票和原始单据", "REALTIME"));

        scenarios.add(createScenario("COLLECTION_FILE_SYNC", "收款单文件同步",
                "从YonSuite获取收款单文件", "MANUAL"));

        scenarios.add(createScenario("PAYMENT_FILE_SYNC", "付款单文件获取",
                "从YonSuite获取资金结算文件 (AI集成)", "MANUAL"));

        scenarios.add(createScenario("REFUND_FILE_SYNC", "付款退款单文件获取",
                "从YonSuite获取付款退款单文件", "MANUAL"));

        return scenarios;
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            List<String> accbookCodes = config.resolveAllAccbookCodes();
            if (accbookCodes.isEmpty()) {
                accbookCodes = Collections.singletonList(config.getAccbookCode());
            }

            String accessToken = authClient.getAccessTokenOrNull(config.getAppKey(), config.getAppSecret());
            boolean connected = voucherClient.testConnection(accessToken, accbookCodes.get(0));

            long responseTime = System.currentTimeMillis() - startTime;

            if (connected) {
                return ConnectionTestResult.success("连接成功", responseTime);
            } else {
                return ConnectionTestResult.fail("API 返回错误", "CONNECTION_FAILED");
            }
        } catch (Exception e) {
            log.error("YonSuite 连接测试失败", e);
            return ConnectionTestResult.fail(e.getMessage(), "CONNECTION_ERROR");
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        List<VoucherDTO> allVouchers = new ArrayList<>();

        List<String> accbookCodes = config.resolveAllAccbookCodes();
        if (accbookCodes.isEmpty()) {
            accbookCodes = Collections.singletonList(config.getAccbookCode());
        }

        log.info("凭证同步: 共 {} 个组织待处理", accbookCodes.size());

        String accessToken = authClient.getAccessTokenOrNull(config.getAppKey(), config.getAppSecret());

        for (String accbookCode : accbookCodes) {
            try {
                List<VoucherDTO> vouchers = voucherClient.syncVouchers(accessToken, accbookCode, startDate, endDate);
                allVouchers.addAll(vouchers);
                log.info("组织 {} 同步完成: {} 条凭证", accbookCode, vouchers.size());
            } catch (Exception e) {
                log.error("组织 {} 同步失败: {}", accbookCode, e.getMessage(), e);
            }
        }

        log.info("所有组织同步完成，共 {} 条凭证", allVouchers.size());
        return allVouchers;
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        return voucherClient.getVoucherDetail(null, voucherNo);
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        return voucherClient.getAttachments(null, voucherNo);
    }

    public List<VoucherDTO> syncCollectionFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        List<VoucherDTO> allResults = new ArrayList<>();

        List<String> accbookCodes = config.resolveAllAccbookCodes();
        if (accbookCodes.isEmpty()) {
            accbookCodes = Collections.singletonList(config.getAccbookCode());
        }

        log.info("收款单同步: 共 {} 个组织待处理", accbookCodes.size());

        String accessToken = authClient.getAccessTokenOrNull(config.getAppKey(), config.getAppSecret());

        for (String accbookCode : accbookCodes) {
            try {
                List<VoucherDTO> orgResults = collectionClient.syncCollectionFiles(
                        accessToken, accbookCode, startDate, endDate);
                allResults.addAll(orgResults);
                log.info("组织 {} 收款单同步完成: {} 条", accbookCode, orgResults.size());
            } catch (Exception e) {
                log.error("组织 {} 收款单同步失败: {}", accbookCode, e.getMessage(), e);
            }
        }

        log.info("所有组织收款单同步完成，共 {} 条", allResults.size());
        return allResults;
    }

    public List<VoucherDTO> syncPaymentFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款单文件同步: startDate={}, endDate={}", startDate, endDate);
        return paymentClient.syncPaymentFiles(config, startDate, endDate);
    }

    public List<VoucherDTO> syncRefundFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款退款单文件同步: startDate={}, endDate={}", startDate, endDate);
        return refundClient.syncRefundFiles(config, startDate, endDate);
    }

    public List<VoucherDTO> syncRefundFiles(ErpConfig config, List<String> fileIds) {
        log.info("执行付款退款单文件同步: fileIds={}", fileIds);
        return refundClient.syncRefundFilesByIds(config, fileIds);
    }

    @Override
    public FeedbackResult feedbackArchivalStatus(ErpConfig config, String voucherNo, String archivalCode, String status) {
        return feedbackClient.feedbackArchivalStatus(config, voucherNo, archivalCode, status);
    }

    @Override
    public List<AccountSummaryDTO> fetchAccountSummary(ErpConfig config, String subjectCode, LocalDate startDate, LocalDate endDate) {
        log.info("执行 YonSuite 科目汇总获取 (PoC Mock): subject={}, range={} to {}",
                subjectCode, startDate, endDate);

        List<AccountSummaryDTO> summaries = new ArrayList<>();

        summaries.add(AccountSummaryDTO.builder()
                .subjectCode(subjectCode != null ? subjectCode : "1001")
                .subjectName("库存现金")
                .debitTotal(new BigDecimal("50000.00"))
                .creditTotal(new BigDecimal("30000.00"))
                .voucherCount(10)
                .currency("CNY")
                .build());

        return summaries;
    }

    /**
     * 创建场景对象
     */
    private ErpScenario createScenario(String key, String name, String description, String strategy) {
        ErpScenario scenario = new ErpScenario();
        scenario.setScenarioKey(key);
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setIsActive(true);
        scenario.setSyncStrategy(strategy);
        return scenario;
    }
}
