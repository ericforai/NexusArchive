// Input: SnakeYAML, Spring, Lombok, SLF4J
// Output: MappingConfigLoader 类
// Pos: 集成模块 - ERP 映射配置加载器

package com.nexusarchive.integration.erp.mapping;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;

/**
 * 映射配置加载器
 * 从 classpath:erp-mapping/ 目录加载 YAML 配置
 */
@Slf4j
@Component
public class MappingConfigLoader {

    private static final String MAPPING_BASE_PATH = "erp-mapping/";

    private final Yaml yaml;

    public MappingConfigLoader() {
        this.yaml = new Yaml(new Constructor(MappingConfig.class));
    }

    /**
     * 加载指定 ERP 的映射配置
     *
     * @param sourceSystem ERP 标识（如 yonsuite, kingdee）
     * @return 映射配置
     * @throws IOException 配置文件不存在或格式错误
     */
    public MappingConfig loadMapping(String sourceSystem) throws IOException {
        String configPath = MAPPING_BASE_PATH + sourceSystem + "-mapping.yml";
        log.info("Loading mapping config: {}", configPath);

        ClassPathResource resource = new ClassPathResource(configPath);

        if (!resource.exists()) {
            throw new MappingConfigNotFoundException(
                "Mapping config not found for system: " + sourceSystem +
                ", expected path: " + configPath
            );
        }

        try (InputStream inputStream = resource.getInputStream()) {
            MappingConfig config = yaml.load(inputStream);
            log.info("Loaded mapping config for {}, version: {}",
                sourceSystem, config.getVersion());
            return config;
        } catch (Exception e) {
            log.error("Failed to parse mapping config: {}", configPath, e);
            throw new MappingConfigParseException(
                "Failed to parse mapping config: " + e.getMessage(), e
            );
        }
    }

    /**
     * 检查配置是否存在
     */
    public boolean mappingExists(String sourceSystem) {
        String configPath = MAPPING_BASE_PATH + sourceSystem + "-mapping.yml";
        return new ClassPathResource(configPath).exists();
    }
}
