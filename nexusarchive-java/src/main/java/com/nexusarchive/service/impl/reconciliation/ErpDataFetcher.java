// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework
// Output: ErpDataFetcher 类
// Pos: 对账服务 - ERP 数据获取层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.reconciliation;

import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.dto.AccountSummaryDTO;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.util.SM4Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 数据获取器
 * <p>
 * 负责从 ERP 系统获取科目汇总和凭证数据
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpDataFetcher {

    private final com.nexusarchive.mapper.ErpConfigMapper erpConfigMapper;
    private final com.nexusarchive.integration.erp.adapter.ErpAdapterFactory erpAdapterFactory;

    /**
     * 获取 ERP 科目汇总
     *
     * @param configId   ERP 配置 ID
     * @param subjectCode 科目代码
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 科目汇总结果
     */
    public ErpSummaryResult fetchAccountSummary(Long configId, String subjectCode,
                                                LocalDate startDate, LocalDate endDate) {
        com.nexusarchive.entity.ErpConfig configEntity = erpConfigMapper.selectById(configId);
        if (configEntity == null) {
            return ErpSummaryResult.error("ERP配置不存在");
        }

        ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
        if (adapter == null) {
            return ErpSummaryResult.error("ERP适配器未注册: " + configEntity.getErpType());
        }

        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig;
        try {
            dtoConfig = convertToDto(configEntity);
        } catch (RuntimeException e) {
            return ErpSummaryResult.error("ERP配置解析失败: " + e.getMessage());
        }

        return fetchErpSummary(adapter, dtoConfig, subjectCode, startDate, endDate);
    }

    /**
     * 获取 ERP 凭证数量
     *
     * @param configId  ERP 配置 ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 凭证数量结果
     */
    public ErpSummaryResult fetchVoucherCount(Long configId, LocalDate startDate, LocalDate endDate) {
        com.nexusarchive.entity.ErpConfig configEntity = erpConfigMapper.selectById(configId);
        if (configEntity == null) {
            return ErpSummaryResult.error("ERP配置不存在");
        }

        ErpAdapter adapter = erpAdapterFactory.getAdapter(configEntity.getErpType());
        if (adapter == null) {
            return ErpSummaryResult.error("ERP适配器未注册: " + configEntity.getErpType());
        }

        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig;
        try {
            dtoConfig = convertToDto(configEntity);
        } catch (RuntimeException e) {
            return ErpSummaryResult.error("ERP配置解析失败: " + e.getMessage());
        }

        return fetchErpVoucherCount(adapter, dtoConfig, startDate, endDate);
    }

    /**
     * 转换 ERP 配置实体为 DTO
     */
    private com.nexusarchive.integration.erp.dto.ErpConfig convertToDto(com.nexusarchive.entity.ErpConfig entity) {
        com.nexusarchive.integration.erp.dto.ErpConfig dto = new com.nexusarchive.integration.erp.dto.ErpConfig();
        dto.setId(String.valueOf(entity.getId()));
        dto.setName(entity.getName());
        dto.setAdapterType(entity.getErpType());

        if (entity.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entity.getConfigJson());
            dto.setBaseUrl(json.getStr("baseUrl"));
            dto.setAppKey(json.getStr("appKey", json.getStr("clientId")));
            String secret = json.getStr("appSecret", json.getStr("clientSecret"));
            try {
                dto.setAppSecret(SM4Utils.decryptStrict(secret));
            } catch (Exception e) {
                log.error("ERP配置 [{}] 密钥解密失败，请检查SM4_KEY配置", entity.getName());
                throw new RuntimeException("ERP密钥解密失败", e);
            }
            dto.setAccbookCode(json.getStr("accbookCode"));
            dto.setExtraConfig(entity.getConfigJson());
        }
        return dto;
    }

    /**
     * 获取 ERP 科目汇总数据
     */
    private ErpSummaryResult fetchErpSummary(ErpAdapter adapter,
                                              com.nexusarchive.integration.erp.dto.ErpConfig config,
                                              String subjectCode, LocalDate startDate, LocalDate endDate) {
        try {
            List<AccountSummaryDTO> summaries = adapter.fetchAccountSummary(config, subjectCode, startDate, endDate);
            if (summaries == null) {
                return ErpSummaryResult.error("ERP返回空结果(适配器未实现或异常)");
            }
            if (summaries.isEmpty()) {
                return ErpSummaryResult.empty();
            }

            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            int voucherCount = 0;
            String subjectName = "";

            for (AccountSummaryDTO summary : summaries) {
                if (summary == null) {
                    continue;
                }
                debit = debit.add(nullToZero(summary.getDebitTotal()));
                credit = credit.add(nullToZero(summary.getCreditTotal()));
                voucherCount += summary.getVoucherCount() == null ? 0 : summary.getVoucherCount();
                if (!hasText(subjectName) && hasText(summary.getSubjectName())) {
                    subjectName = summary.getSubjectName();
                }
            }

            return ErpSummaryResult.ok(debit, credit, voucherCount, subjectName);
        } catch (Exception e) {
            return ErpSummaryResult.error("ERP拉取失败: " + e.getMessage());
        }
    }

    /**
     * 获取 ERP 凭证数量
     */
    private ErpSummaryResult fetchErpVoucherCount(ErpAdapter adapter,
                                                   com.nexusarchive.integration.erp.dto.ErpConfig config,
                                                   LocalDate startDate, LocalDate endDate) {
        try {
            List<VoucherDTO> vouchers = adapter.syncVouchers(config, startDate, endDate);
            if (vouchers == null) {
                return ErpSummaryResult.error("ERP凭证同步返回空结果");
            }
            return ErpSummaryResult.ok(BigDecimal.ZERO, BigDecimal.ZERO, vouchers.size(), "");
        } catch (Exception e) {
            return ErpSummaryResult.error("ERP凭证同步失败: " + e.getMessage());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * ERP 汇总结果
     */
    public static class ErpSummaryResult {
        public final BigDecimal debitTotal;
        public final BigDecimal creditTotal;
        public final int voucherCount;
        public final String subjectName;
        public final String errorMessage;

        private ErpSummaryResult(BigDecimal debitTotal, BigDecimal creditTotal, int voucherCount,
                                 String subjectName, String errorMessage) {
            this.debitTotal = debitTotal;
            this.creditTotal = creditTotal;
            this.voucherCount = voucherCount;
            this.subjectName = subjectName;
            this.errorMessage = errorMessage;
        }

        public static ErpSummaryResult ok(BigDecimal debitTotal, BigDecimal creditTotal, int voucherCount,
                                          String subjectName) {
            return new ErpSummaryResult(debitTotal, creditTotal, voucherCount, subjectName, null);
        }

        public static ErpSummaryResult empty() {
            return new ErpSummaryResult(BigDecimal.ZERO, BigDecimal.ZERO, 0, "", null);
        }

        public static ErpSummaryResult error(String message) {
            return new ErpSummaryResult(BigDecimal.ZERO, BigDecimal.ZERO, 0, "", message);
        }

        public boolean hasError() {
            return errorMessage != null;
        }
    }
}
