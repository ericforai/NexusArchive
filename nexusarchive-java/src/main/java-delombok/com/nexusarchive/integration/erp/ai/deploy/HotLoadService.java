// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/HotLoadService.java
// Input: className
// Output: void (reloads Spring bean)
// Pos: AI 模块 - 热加载服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.service.erp.plugin.ErpPluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * 热加载服务
 * <p>
 * 在运行时重新加载适配器 Bean
 * MVP 版本：记录日志，标记需要重启
 * </p>
 */
@Slf4j
@Service
public class HotLoadService {

    private final ApplicationContext applicationContext;
    private final ErpPluginManager pluginManager;

    public HotLoadService(ApplicationContext applicationContext,
                         ErpPluginManager pluginManager) {
        this.applicationContext = applicationContext;
        this.pluginManager = pluginManager;
    }

    /**
     * 热加载适配器
     *
     * @param className 适配器类名
     */
    public void reloadAdapter(String className) {
        log.info("尝试热加载适配器: {}", className);

        // MVP 版本：实际热加载需要复杂的类加载器机制
        // 这里我们先实现通知机制，告知用户需要重启
        log.warn("⚠️ 热加载功能需要重启服务器才能生效");
        log.warn("⚠️ 生成的适配器类: {}", className);
        log.warn("⚠️ 请手动重启服务器以加载新适配器");

        // TODO: Phase 2 实现真正的热加载
        // 可能的方案：
        // 1. 使用自定义 ClassLoader 重新加载类
        // 2. 销毁旧 Bean，创建新 Bean
        // 3. 重新注册到 ErpPluginManager
    }

    /**
     * 检查是否支持热加载
     *
     * @return 是否支持热加载
     */
    public boolean isHotLoadSupported() {
        // MVP 版本返回 false
        return false;
    }

    /**
     * 获取热加载支持状态描述
     */
    public String getHotLoadStatus() {
        if (isHotLoadSupported()) {
            return "热加载已启用";
        } else {
            return "热加载未启用（MVP 版本需要手动重启服务器）";
        }
    }
}
