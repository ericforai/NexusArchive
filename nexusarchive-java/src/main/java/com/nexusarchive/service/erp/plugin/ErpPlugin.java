// Input: Spring Framework, Java 标准库
// Output: ErpPlugin 接口
// Pos: 服务层 - ERP 插件接口

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.service.erp.plugin.ErpPluginContext;
import com.nexusarchive.service.erp.plugin.ErpPluginResult;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 插件接口
 * <p>
 * 定义 ERP 系统集成的标准契约
 * </p>
 */
public interface ErpPlugin {

    /**
     * 插件标识符
     */
    String getPluginId();

    /**
     * 插件名称
     */
    String getPluginName();

    /**
     * 支持的 ERP 类型
     */
    String getSupportedErpType();

    /**
     * 执行同步
     *
     * @param context 同步上下文
     * @return 同步结果
     */
    ErpPluginResult sync(ErpPluginContext context);

    /**
     * 获取支持的同步场景列表
     */
    List<String> getSupportedScenarios();

    /**
     * 验证配置
     *
     * @param config ERP 配置
     * @return 验证结果
     */
    ErpPluginResult validateConfig(ErpConfig config);
}
