// Input: Lombok、Spring Framework、Java 标准库
// Output: ErpAdapterFactory 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERP 适配器工厂
 * 自动注入所有 ErpAdapter 实现，提供统一获取入口
 * 
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ErpAdapterFactory {

    private final Map<String, ErpAdapter> adapters;

    /**
     * 获取指定类型的适配器
     * 
     * @param type 适配器类型标识
     * @return 适配器实例
     * @throws IllegalArgumentException 如果类型不存在
     */
    public ErpAdapter getAdapter(String type) {
        ErpAdapter adapter = adapters.get(type);
        if (adapter == null && type != null) {
            // Try lowercase fallback to be robust against "YONSUITE" vs "yonsuite"
            adapter = adapters.get(type.toLowerCase());
        }
        if (adapter == null) {
            throw new IllegalArgumentException("不支持的 ERP 类型: " + type + 
                "。可用类型: " + String.join(", ", adapters.keySet()));
        }
        return adapter;
    }

    /**
     * 检查是否支持指定类型
     */
    public boolean isSupported(String type) {
        return adapters.containsKey(type);
    }

    /**
     * 列出所有可用的适配器信息
     */
    public List<ErpAdapterInfo> listAvailableAdapters() {
        return adapters.values().stream()
            .map(adapter -> new ErpAdapterInfo(
                adapter.getIdentifier(),
                adapter.getName(),
                adapter.getDescription(),
                adapter.supportsWebhook()
            ))
            .collect(Collectors.toList());
    }

    /**
     * 适配器信息 DTO
     */
    public record ErpAdapterInfo(
        String identifier,
        String name,
        String description,
        boolean supportsWebhook
    ) {}
}
