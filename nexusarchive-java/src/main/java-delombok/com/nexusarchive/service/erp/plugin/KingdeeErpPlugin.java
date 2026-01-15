// Input: Spring Framework
// Output: KingdeeErpPlugin 类
// Pos: 服务层 - 金蝶插件实现

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
 * 金蝶 ERP 插件实现
 * <p>
 * 支持金蝶系统的同步场景
 * </p>
 */
@Slf4j
@Component
public class KingdeeErpPlugin extends AbstractErpPlugin {

    public KingdeeErpPlugin(ErpAdapterFactory erpAdapterFactory) {
        super(erpAdapterFactory);
    }

    @Override
    public String getPluginId() {
        return "kingdee";
    }

    @Override
    public String getPluginName() {
        return "金蝶 K/3 Cloud";
    }

    @Override
    public String getSupportedErpType() {
        return "KINGDEE";
    }

    @Override
    protected List<VoucherDTO> doSync(ErpAdapter adapter,
                                       com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                       ErpPluginContext context) {
        LocalDate startDate = context.getStartDate();
        LocalDate endDate = context.getEndDate();

        // 金蝶系统使用标准同步接口
        return adapter.syncVouchers(dtoConfig, startDate, endDate);
    }

    @Override
    public List<String> getSupportedScenarios() {
        return List.of("VOUCHER_SYNC");
    }

    @Override
    protected ErpPluginResult doValidateConfig(ErpConfig entityConfig) {
        // 金蝶特定的配置验证
        if (entityConfig.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entityConfig.getConfigJson());
            String accbookCode = json.getStr("accbookCode");
            if (accbookCode == null || accbookCode.isEmpty()) {
                return ErpPluginResult.failure("accbookCode (账套代码) 不能为空");
            }
        }
        return ErpPluginResult.success(0, null);
    }
}
