// Input: Spring Framework, ErpAdapter 注解
// Output: ErpMetadataRegistry 类
// Pos: integration.erp.registry 包

package com.nexusarchive.integration.erp.registry;

import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.ErpMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ERP 适配器元数据注册中心
 * <p>
 * 负责自动发现、注册和查询 ERP 适配器元数据
 * </p>
 */
@Slf4j
@Component
public class ErpMetadataRegistry {

    /**
     * 按标识索引的元数据存储
     */
    private final Map<String, ErpMetadata> byIdentifier = new ConcurrentHashMap<>();

    /**
     * 按 ERP 类型索引的元数据存储
     */
    private final Map<String, List<ErpMetadata>> byErpType = new ConcurrentHashMap<>();

    /**
     * 注册适配器元数据
     *
     * @param adapterClass 适配器实现类
     */
    public void register(Class<?> adapterClass) {
        ErpAdapterAnnotation annotation = adapterClass.getAnnotation(ErpAdapterAnnotation.class);
        if (annotation == null) {
            log.warn("Class {} is not annotated with @ErpAdapterAnnotation, skipping registration",
                adapterClass.getName());
            return;
        }

        ErpMetadata metadata = ErpMetadata.builder()
            .identifier(annotation.identifier())
            .name(annotation.name())
            .description(annotation.description())
            .version(annotation.version())
            .erpType(annotation.erpType())
            .supportedScenarios(Set.of(annotation.supportedScenarios()))
            .supportsWebhook(annotation.supportsWebhook())
            .priority(annotation.priority())
            .implementationClass(adapterClass.getName())
            .registeredAt(LocalDateTime.now())
            .enabled(true)
            .build();

        // 按标识注册
        byIdentifier.put(metadata.getIdentifier(), metadata);

        // 按 ERP 类型注册
        byErpType.computeIfAbsent(metadata.getErpType(), k -> new ArrayList<>())
            .add(metadata);

        // 按优先级排序
        byErpType.get(metadata.getErpType()).sort(Comparator.comparingInt(ErpMetadata::getPriority));

        log.info("Registered ERP adapter: {} ({})",
            metadata.getIdentifier(), metadata.getName());
    }

    /**
     * 批量注册适配器
     *
     * @param adapterClasses 适配器实现类列表
     */
    public void registerAll(Collection<Class<?>> adapterClasses) {
        adapterClasses.forEach(this::register);
        log.info("Registered {} ERP adapters in total", adapterClasses.size());
    }

    /**
     * 根据标识获取元数据
     *
     * @param identifier 适配器标识
     * @return 元数据，如果不存在返回 null
     */
    public ErpMetadata getByIdentifier(String identifier) {
        return byIdentifier.get(identifier);
    }

    /**
     * 根据 ERP 类型获取所有适配器元数据
     *
     * @param erpType ERP 类型
     * @return 适配器元数据列表（按优先级排序）
     */
    public List<ErpMetadata> getByErpType(String erpType) {
        return byErpType.getOrDefault(erpType, Collections.emptyList());
    }

    /**
     * 获取所有已注册的适配器元数据
     *
     * @return 元数据列表
     */
    public Collection<ErpMetadata> getAll() {
        return byIdentifier.values();
    }

    /**
     * 检查适配器是否已注册
     *
     * @param identifier 适配器标识
     * @return 是否已注册
     */
    public boolean isRegistered(String identifier) {
        return byIdentifier.containsKey(identifier);
    }

    /**
     * 获取已注册适配器数量
     *
     * @return 数量
     */
    public int size() {
        return byIdentifier.size();
    }
}
