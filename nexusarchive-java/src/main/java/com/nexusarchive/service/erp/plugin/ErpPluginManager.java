// Input: Spring Framework, Java 标准库
// Output: ErpPluginManager 类
// Pos: 服务层 - ERP 插件管理器

package com.nexusarchive.service.erp.plugin;

import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.service.erp.plugin.ErpPlugin;
import com.nexusarchive.service.erp.plugin.ErpPluginContext;
import com.nexusarchive.service.erp.plugin.ErpPluginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERP 插件管理器
 * <p>
 * 管理所有 ERP 插件的注册和调用
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpPluginManager {

    private final Map<String, ErpPlugin> plugins = new ConcurrentHashMap<>();
    private final List<ErpPlugin> pluginList;

    /**
     * 自动注册所有插件
     */
    @jakarta.annotation.PostConstruct
    public void registerPlugins() {
        for (ErpPlugin plugin : pluginList) {
            registerPlugin(plugin);
        }
        log.info("Registered {} ERP plugins: {}", plugins.size(), plugins.keySet());
    }

    /**
     * 注册插件
     */
    public void registerPlugin(ErpPlugin plugin) {
        plugins.put(plugin.getPluginId(), plugin);
        log.info("Registered ERP plugin: {} ({})", plugin.getPluginName(), plugin.getPluginId());
    }

    /**
     * 获取插件
     */
    public ErpPlugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 根据 ERP 类型获取插件
     */
    public ErpPlugin getPluginByErpType(String erpType) {
        return plugins.values().stream()
                .filter(p -> p.getSupportedErpType().equals(erpType))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有插件
     */
    public Map<String, ErpPlugin> getAllPlugins() {
        return Map.copyOf(plugins);
    }

    /**
     * 执行同步
     */
    public ErpPluginResult sync(String pluginId, ErpPluginContext context) {
        ErpPlugin plugin = getPlugin(pluginId);
        if (plugin == null) {
            return ErpPluginResult.failure("插件不存在: " + pluginId);
        }

        log.info("Executing ERP plugin: {} for scenario: {}", plugin.getPluginName(), context.getScenario().getName());
        return plugin.sync(context);
    }

    /**
     * 验证配置
     */
    public ErpPluginResult validateConfig(String pluginId, ErpConfig config) {
        ErpPlugin plugin = getPlugin(pluginId);
        if (plugin == null) {
            return ErpPluginResult.failure("插件不存在: " + pluginId);
        }

        return plugin.validateConfig(config);
    }
}
