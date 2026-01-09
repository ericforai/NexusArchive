// Input: cn.hutool、Lombok、Spring Framework、本地模块
// Output: ErpFeedbackServiceImpl 类
// Pos: 业务服务实现层 - ERP 反馈服务

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.FeedbackResult;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpFeedbackService;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

/**
 * ERP 反馈服务实现
 * <p>
 * 负责将归档状态反馈回 ERP 系统（存证溯源）
 * </p>
 * <p>
 * Phase 3 增强:
 * - 使用结构化 FeedbackResult
 * - 记录审计日志 (ERP_FEEDBACK 类型)
 * - 失败时可入队等待重试
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ErpFeedbackServiceImpl implements ErpFeedbackService {

    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;

    @Override
    public FeedbackResult triggerFeedback(ArcFileContent file, String archivalCode) {
        if (file.getSourceSystem() == null || file.getErpVoucherNo() == null) {
            log.debug("跳过 ERP 回写: 源系统或凭证号为空");
            return null;
        }

        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║            [存证溯源] 开始回写归档状态至 ERP                    ║");
        log.info("╠═══════════════════════════════════════════════════════════════╣");
        log.info("║ 源系统: {}", file.getSourceSystem());
        log.info("║ 凭证号: {}", file.getErpVoucherNo());
        log.info("║ 档号: {}", archivalCode);
        log.info("╚═══════════════════════════════════════════════════════════════╝");

        FeedbackResult result = null;

        try {
            // 通过源系统名称查找配置
            LambdaQueryWrapper<com.nexusarchive.entity.ErpConfig> query = new LambdaQueryWrapper<>();
            query.eq(com.nexusarchive.entity.ErpConfig::getName, file.getSourceSystem());
            com.nexusarchive.entity.ErpConfig configEntity = erpConfigMapper.selectOne(query);

            if (configEntity == null) {
                log.warn("未找到 ERP 配置: {}", file.getSourceSystem());
                result = FeedbackResult.failure(
                        file.getErpVoucherNo(), archivalCode, "UNKNOWN", "ERP 配置未找到: " + file.getSourceSystem());
            } else {
                ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
                if (adapter == null) {
                    log.warn("未找到适配器: {}", configEntity.getErpType());
                    result = FeedbackResult.failure(
                            file.getErpVoucherNo(), archivalCode, configEntity.getErpType(), "适配器未找到");
                } else {
                    // 转换实体配置为 DTO 配置
                    ErpConfig configDto = buildConfigDto(configEntity);

                    // 调用适配器回写 (返回 FeedbackResult)
                    result = adapter.feedbackArchivalStatus(configDto, file.getErpVoucherNo(), archivalCode, "ARCHIVED");
                }
            }
        } catch (Exception e) {
            log.error("ERP 回写过程异常", e);
            result = FeedbackResult.failure(
                    file.getErpVoucherNo(), archivalCode,
                    file.getSourceSystem() != null ? file.getSourceSystem() : "UNKNOWN",
                    e.getMessage());
        }

        // 记录结果日志
        if (result != null) {
            if (result.isSuccess()) {
                log.info("✓ [存证溯源] 回写成功 - voucher={}, archivalCode={}, mocked={}",
                        result.getVoucherId(), result.getArchivalCode(), result.isMocked());
            } else {
                log.warn("✗ [存证溯源] 回写失败 - voucher={}, error={}",
                        result.getVoucherId(), result.getErrorMessage());

                // TODO: 失败时可入队 sys_erp_feedback_queue 等待重试
                // 当前版本仅记录日志，Phase 4 可实现定时任务重试
            }
        }

        return result;
    }

    /**
     * 转换实体配置为 DTO 配置
     */
    private ErpConfig buildConfigDto(com.nexusarchive.entity.ErpConfig configEntity) {
        ErpConfig configDto = new ErpConfig();
        configDto.setId(String.valueOf(configEntity.getId()));
        configDto.setName(configEntity.getName());
        configDto.setAdapterType(configEntity.getErpType());

        if (configEntity.getConfigJson() != null) {
            JSONObject json = JSONUtil.parseObj(configEntity.getConfigJson());
            configDto.setBaseUrl(json.getStr("baseUrl"));

            String appKey = json.getStr("appKey");
            if (appKey == null || appKey.isEmpty()) {
                appKey = json.getStr("clientId");
            }
            configDto.setAppKey(appKey);

            String appSecret = json.getStr("appSecret");
            if (appSecret == null || appSecret.isEmpty()) {
                appSecret = json.getStr("clientSecret");
            }
            // 使用 SM4 解密 (严格模式)
            configDto.setAppSecret(SM4Utils.decryptStrict(appSecret));
            configDto.setAccbookCode(json.getStr("accbookCode"));
            configDto.setExtraConfig(configEntity.getConfigJson());
        }

        return configDto;
    }
}
