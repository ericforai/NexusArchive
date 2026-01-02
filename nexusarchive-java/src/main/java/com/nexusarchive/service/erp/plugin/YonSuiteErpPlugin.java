// Input: Spring Framework
// Output: YonSuiteErpPlugin 类
// Pos: 服务层 - 用友插件实现

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 用友 ERP 插件实现
 * <p>
 * 支持用友系统的同步场景
 * </p>
 */
@Slf4j
@Component
public class YonSuiteErpPlugin extends AbstractErpPlugin {

    public YonSuiteErpPlugin(ErpAdapterFactory erpAdapterFactory) {
        super(erpAdapterFactory);
    }

    @Override
    public String getPluginId() {
        return "yonsuite";
    }

    @Override
    public String getPluginName() {
        return "用友 YonSuite";
    }

    @Override
    public String getSupportedErpType() {
        return "YONSUITE";
    }

    @Override
    protected List<VoucherDTO> doSync(ErpAdapter adapter,
                                       com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                       ErpPluginContext context) {
        String scenarioKey = context.getScenario().getScenarioKey();
        LocalDate startDate = context.getStartDate();
        LocalDate endDate = context.getEndDate();

        if ("COLLECTION_FILE_SYNC".equals(scenarioKey)) {
            if (adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
                return ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                        .syncCollectionFiles(dtoConfig, startDate, endDate);
            }
        } else if ("PAYMENT_FILE_SYNC".equals(scenarioKey)) {
            if (adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
                return ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                        .syncPaymentFiles(dtoConfig, startDate, endDate);
            }
        }

        return adapter.syncVouchers(dtoConfig, startDate, endDate);
    }

    @Override
    public List<String> getSupportedScenarios() {
        return List.of("COLLECTION_FILE_SYNC", "PAYMENT_FILE_SYNC", "VOUCHER_SYNC");
    }

    @Override
    protected ErpPluginResult doValidateConfig(ErpConfig entityConfig) {
        // 用友特定的配置验证
        if (entityConfig.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entityConfig.getConfigJson());
            String baseUrl = json.getStr("baseUrl");
            if (baseUrl == null || baseUrl.isEmpty()) {
                return ErpPluginResult.failure("baseUrl 不能为空");
            }
        }
        return ErpPluginResult.success(0, null);
    }
}
