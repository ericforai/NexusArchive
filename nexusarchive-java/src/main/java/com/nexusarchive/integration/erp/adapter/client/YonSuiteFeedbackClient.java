// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO
// Output: YonSuiteFeedbackClient 类
// Pos: 集成模块 - ERP 适配器客户端

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.FeedbackResult;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * YonSuite 反馈客户端
 * 负责处理归档状态反馈相关的操作：回写归档状态到 ERP
 *
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteFeedbackClient {

    private final YonSuiteClient yonSuiteClient;

    /**
     * 反馈归档状态到 YonSuite
     *
     * @param voucherNo    凭证号
     * @param archivalCode 归档编号
     * @return 反馈结果
     */
    public FeedbackResult feedbackArchivalStatus(String voucherNo, String archivalCode) {
        try {
            log.info("┌── [YonSuite Feedback] 执行存证溯源 ──────────────────────────┐");
            log.info("│ voucherNo={}, archivalCode={}", voucherNo, archivalCode);
            log.info("└────────────────────────────────────────────────────────────┘");

            FeedbackResult result = yonSuiteClient.feedbackArchivalStatus(null, voucherNo, archivalCode);

            log.info("YonSuite 回写结果: success={}, mocked={}", result.isSuccess(), result.isMocked());
            return result;
        } catch (Exception e) {
            log.error("YonSuite feedbackArchivalStatus 异常", e);
            return FeedbackResult.failure(voucherNo, archivalCode, "YONSUITE", e.getMessage());
        }
    }

    /**
     * 反馈归档状态到 YonSuite（带配置）
     *
     * @param config       ERP 配置
     * @param voucherNo    凭证号
     * @param archivalCode 归档编号
     * @param status       状态
     * @return 反馈结果
     */
    public FeedbackResult feedbackArchivalStatus(ErpConfig config, String voucherNo,
                                                  String archivalCode, String status) {
        // status 参数暂不使用，保留接口兼容性
        return feedbackArchivalStatus(voucherNo, archivalCode);
    }
}
