// Input: Java 标准库、本地模块
// Output: ErpAdapter 接口
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.VoucherDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 适配器统一接口
 * 所有 ERP 集成（YonSuite、金蝶、通用等）都实现此接口
 * 
 * @author Agent D (基础设施工程师)
 */
public interface ErpAdapter {

    /**
     * 获取适配器唯一标识
     * 例如: yonsuite, kingdee, generic
     */
    String getIdentifier();

    /**
     * 获取适配器显示名称
     * 例如: 用友YonSuite, 金蝶云星空
     */
    String getName();

    /**
     * 获取适配器描述
     */
    default String getDescription() {
        return getName() + " ERP 集成适配器";
    }

    /**
     * 测试 ERP 连接
     * 
     * @param config ERP 配置
     * @return 连接测试结果
     */
    ConnectionTestResult testConnection(ErpConfig config);

    /**
     * 同步指定日期范围的凭证
     * 
     * @param config    ERP 配置
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 凭证列表
     */
    List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate);

    /**
     * 获取单个凭证详情
     * 
     * @param config    ERP 配置
     * @param voucherNo 凭证编号
     * @return 凭证详情
     */
    VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo);

    /**
     * 获取凭证附件列表
     * 
     * @param config    ERP 配置
     * @param voucherNo 凭证编号
     * @return 附件列表
     */
    List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo);

    /**
     * 是否支持 Webhook 推送
     */
    default boolean supportsWebhook() {
        return false;
    }

    /**
     * 验证 Webhook 签名
     */
    default boolean verifyWebhookSignature(String payload, String signature, ErpConfig config) {
        return false;
    }

    /**
     * 获取该适配器支持的标准业务场景列表 (Layer 2 定义)
     * 系统初始化时会调用此方法，将场景写入 sys_erp_scenario 表
     */
    default java.util.List<com.nexusarchive.entity.ErpScenario> getAvailableScenarios() {
        return java.util.Collections.emptyList();
    }

    /**
     * 将归档状态、档号或存证哈希回写至 ERP 系统 (存证溯源)
     * 
     * Phase 3 增强：返回结构化结果 FeedbackResult
     * 
     * @param config ERP 配置
     * @param voucherNo 凭证/单据编号
     * @param archivalCode 生成的档号
     * @param status 归档状态 (如 ARCHIVED)
     * @return FeedbackResult 回写结果（含成功/失败状态、时间戳、错误信息）
     */
    default com.nexusarchive.integration.erp.dto.FeedbackResult feedbackArchivalStatus(
            ErpConfig config, String voucherNo, String archivalCode, String status) {
        return com.nexusarchive.integration.erp.dto.FeedbackResult.failure(
                voucherNo, archivalCode, "UNKNOWN", "Adapter does not implement feedbackArchivalStatus");
    }


    /**
     * 获取财务科目汇总数据 (用于三位一体核对)
     * 
     * @param config ERP配置
     * @param subjectCode 科目代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 汇总数据列表
     */
    default java.util.List<com.nexusarchive.integration.erp.dto.AccountSummaryDTO> fetchAccountSummary(
            ErpConfig config, String subjectCode, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return null;
    }
}
