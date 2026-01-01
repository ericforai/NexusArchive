// Input: Spring Framework
// Output: AbstractErpPlugin 类
// Pos: 服务层 - ERP 插件基类

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 插件抽象基类
 * <p>
 * 提供插件的通用实现，子类只需实现特定逻辑
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractErpPlugin implements ErpPlugin {

    private final ErpAdapterFactory erpAdapterFactory;

    @Override
    public ErpPluginResult sync(ErpPluginContext context) {
        try {
            // 构建 DTO 配置
            com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = buildDtoConfig(context.getConfig());

            // 获取适配器
            ErpAdapter adapter = erpAdapterFactory.getAdapter(context.getConfig().getErpType());

            // 执行同步
            List<VoucherDTO> vouchers = doSync(adapter, dtoConfig, context);

            return ErpPluginResult.success(vouchers != null ? vouchers.size() : 0, vouchers);

        } catch (Exception e) {
            log.error("ERP plugin sync failed: {}", getPluginName(), e);
            return ErpPluginResult.failure("同步失败: " + e.getMessage());
        }
    }

    @Override
    public ErpPluginResult validateConfig(ErpConfig config) {
        // 基本验证
        if (config == null) {
            return ErpPluginResult.failure("配置不能为空");
        }
        if (config.getConfigJson() == null || config.getConfigJson().isEmpty()) {
            return ErpPluginResult.failure("配置 JSON 不能为空");
        }

        // 子类可以覆盖此方法进行自定义验证
        return doValidateConfig(config);
    }

    /**
     * 子类实现：执行具体的同步逻辑
     */
    protected abstract List<VoucherDTO> doSync(ErpAdapter adapter,
                                                 com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                                 ErpPluginContext context);

    /**
     * 子类实现：自定义配置验证
     */
    protected ErpPluginResult doValidateConfig(ErpConfig entityConfig) {
        return ErpPluginResult.success(0, null);
    }

    /**
     * 构建 DTO 配置
     */
    protected com.nexusarchive.integration.erp.dto.ErpConfig buildDtoConfig(ErpConfig entityConfig) {
        com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = new com.nexusarchive.integration.erp.dto.ErpConfig();
        dtoConfig.setId(String.valueOf(entityConfig.getId()));
        dtoConfig.setName(entityConfig.getName());
        dtoConfig.setAdapterType(entityConfig.getErpType());

        if (entityConfig.getConfigJson() != null) {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entityConfig.getConfigJson());
            dtoConfig.setBaseUrl(json.getStr("baseUrl"));

            String appKey = json.getStr("appKey");
            if (appKey == null || appKey.isEmpty()) {
                appKey = json.getStr("clientId");
            }
            dtoConfig.setAppKey(appKey);

            String appSecret = json.getStr("appSecret");
            if (appSecret == null || appSecret.isEmpty()) {
                appSecret = json.getStr("clientSecret");
            }
            dtoConfig.setAppSecret(com.nexusarchive.util.SM4Utils.decrypt(appSecret));
            dtoConfig.setAccbookCode(json.getStr("accbookCode"));
            dtoConfig.setExtraConfig(entityConfig.getConfigJson());
        }

        return dtoConfig;
    }
}
