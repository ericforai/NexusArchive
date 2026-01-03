// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/DatabaseRegistrationService.java
// Input: GeneratedCode, targetConfigId, fileName, ScenarioMapping list
// Output: RegistrationResult with configId and scenario counts
// Pos: AI 模块 - 数据库注册服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.identifier.ErpTypeIdentifier;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioName;
import com.nexusarchive.integration.erp.ai.identifier.ScenarioNamer;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 数据库注册服务
 * <p>
 * 将生成的适配器信息注册到数据库的 sys_erp_config 和 sys_erp_scenario 表
 * 支持两种模式：
 * 1. 创建新连接器（targetConfigId 为 null）
 * 2. 向现有连接器添加场景（targetConfigId 非空）
 * </p>
 */
@Slf4j
@Service
public class DatabaseRegistrationService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErpTypeIdentifier erpTypeIdentifier = new ErpTypeIdentifier();
    private final ScenarioNamer scenarioNamer = new ScenarioNamer();

    public DatabaseRegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 将适配器注册到数据库（旧版方法，保持向后兼容）
     *
     * @param code 生成的代码
     * @deprecated 使用 {@link #registerScenarios(Long, String, List)} 替代
     */
    @Transactional
    @Deprecated
    public void register(GeneratedCode code) {
        log.info("开始数据库注册: className={}", code.getClassName());

        String erpType = code.getErpType().toLowerCase();
        String adapterName = code.getErpName();

        // 删除旧记录（如果存在）
        deleteExistingConfig(erpType, adapterName);

        // 插入配置记录
        Long configId = insertConfig(erpType, adapterName);

        // 插入场景记录
        if (configId != null && code.getMappings() != null && !code.getMappings().isEmpty()) {
            insertScenarios(configId, code.getMappings());
        }

        log.info("数据库注册成功: configId={}", configId);
    }

    /**
     * 注册场景到数据库
     * <p>
     * 支持两种模式：
     * 1. 如果 targetConfigId 为 null，创建新的连接器配置并添加场景
     * 2. 如果 targetConfigId 非空，向现有连接器添加场景（跳过重复场景）
     * </p>
     *
     * @param targetConfigId 目标连接器配置 ID（可选，null 表示创建新配置）
     * @param fileName 文件名，用于识别 ERP 类型
     * @param mappings 场景映射列表
     * @return 注册结果，包含配置 ID、ERP 类型、创建和跳过的场景数量
     */
    @Transactional
    public RegistrationResult registerScenarios(Long targetConfigId, String fileName,
                                               List<BusinessSemanticMapper.ScenarioMapping> mappings) {
        // Input validation
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName cannot be null or empty");
        }
        if (mappings == null) {
            mappings = List.of(); // Treat null as empty list
        }

        log.info("开始注册场景: targetConfigId={}, fileName={}, mappingsCount={}",
                 targetConfigId, fileName, mappings.size());

        // Step 1: 识别 ERP 类型
        ErpTypeIdentifier.ErpType erpType = erpTypeIdentifier.identify(fileName, null);
        log.info("识别到 ERP 类型: {}", erpType.getCode());

        // Step 2: 确定或创建配置 ID
        Long configId;
        if (targetConfigId == null) {
            // 创建新配置
            configId = createNewConfig(erpType);
            if (configId == null) {
                log.error("创建配置失败");
                return new RegistrationResult(null, erpType.getCode(), 0, 0);
            }
            log.info("创建新配置: configId={}", configId);
        } else {
            // 使用现有配置
            configId = targetConfigId;
            log.info("使用现有配置: configId={}", configId);
        }

        // Step 3: 插入场景（跳过重复）
        int createdCount = 0;
        int skippedCount = 0;

        if (mappings != null && !mappings.isEmpty()) {
            for (BusinessSemanticMapper.ScenarioMapping mapping : mappings) {
                // 生成场景名称
                ScenarioName scenarioName = scenarioNamer.generateScenarioName(mapping.getApiDefinition());
                String scenarioKey = scenarioName.scenarioKey();

                // 检查场景是否已存在
                if (scenarioExists(configId, scenarioKey)) {
                    log.debug("场景已存在，跳过: configId={}, scenarioKey={}", configId, scenarioKey);
                    skippedCount++;
                    continue;
                }

                // 插入新场景
                if (insertScenario(configId, scenarioName)) {
                    createdCount++;
                    log.debug("插入场景成功: configId={}, scenarioKey={}", configId, scenarioKey);
                } else {
                    log.warn("插入场景失败: configId={}, scenarioKey={}", configId, scenarioKey);
                }
            }
        }

        log.info("场景注册完成: configId={}, createdCount={}, skippedCount={}",
                 configId, createdCount, skippedCount);

        return new RegistrationResult(configId, erpType.getCode(), createdCount, skippedCount);
    }

    /**
     * 删除已存在的配置记录
     */
    private void deleteExistingConfig(String erpType, String name) {
        // 先查找配置ID
        String findSql = "SELECT id FROM sys_erp_config WHERE erp_type = ? AND name = ?";
        List<Long> ids = jdbcTemplate.queryForList(findSql, Long.class, erpType, name);

        if (!ids.isEmpty()) {
            for (Long configId : ids) {
                // 删除场景
                String deleteScenariosSql = "DELETE FROM sys_erp_scenario WHERE config_id = ?";
                int deletedScenarios = jdbcTemplate.update(deleteScenariosSql, configId);
                log.info("删除旧场景记录: {} 条", deletedScenarios);
            }

            // 删除配置
            String deleteConfigSql = "DELETE FROM sys_erp_config WHERE erp_type = ? AND name = ?";
            int deletedConfig = jdbcTemplate.update(deleteConfigSql, erpType, name);
            log.info("删除旧配置记录: {} 条", deletedConfig);
        }
    }

    /**
     * 插入配置记录到 sys_erp_config（旧版方法）
     */
    private Long insertConfig(String erpType, String name) {
        String sql = """
            INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
            VALUES (?, ?, ?::jsonb, 1, CURRENT_TIMESTAMP)
            """;

        // 创建默认配置
        Map<String, Object> config = Map.of(
            "baseUrl", "https://api.example.com",
            "appKey", "",
            "appSecret", "",
            "accbookCode", "",
            "description", "AI 生成的 " + erpType + " 适配器"
        );

        try {
            String configJson = objectMapper.writeValueAsString(config);

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, erpType);
                ps.setString(3, configJson);
                return ps;
            }, keyHolder);

            if (rows > 0) {
                Number key = keyHolder.getKey();
                if (key != null) {
                    Long configId = key.longValue();
                    log.info("插入配置记录成功: id={}, name={}", configId, name);
                    return configId;
                }
            }
        } catch (Exception e) {
            log.error("插入配置记录失败", e);
        }

        return null;
    }

    /**
     * 插入场景记录到 sys_erp_scenario（旧版方法）
     */
    private void insertScenarios(Long configId, List<BusinessSemanticMapper.ScenarioMapping> mappings) {
        String sql = """
            INSERT INTO sys_erp_scenario (
                config_id, scenario_key, name, description, is_active, sync_strategy, created_time
            ) VALUES (?, ?, ?, ?, true, 'MANUAL', CURRENT_TIMESTAMP)
            """;

        int totalRows = 0;
        for (BusinessSemanticMapper.ScenarioMapping mapping : mappings) {
            if (mapping.getScenario() != null && mapping.getScenario() != com.nexusarchive.integration.erp.ai.mapper.StandardScenario.UNKNOWN) {
                try {
                    String description = "AI 自动识别: " +
                        (mapping.getApiDefinition().getSummary() != null ?
                            mapping.getApiDefinition().getSummary() : mapping.getApiDefinition().getOperationId());

                    int rows = jdbcTemplate.update(sql,
                        configId,
                        mapping.getScenario().getCode(),
                        mapping.getScenario().getDescription(),
                        description
                    );
                    totalRows += rows;
                    log.info("插入场景: {} -> {}", configId, mapping.getScenario().getCode());
                } catch (Exception e) {
                    log.warn("插入场景失败: {} - {}", mapping.getScenario().getCode(), e.getMessage());
                }
            }
        }

        log.info("插入场景记录: {} 条", totalRows);
    }

    /**
     * 创建新的连接器配置
     *
     * @param erpType ERP 类型
     * @return 新创建的配置 ID，失败返回 null
     */
    private Long createNewConfig(ErpTypeIdentifier.ErpType erpType) {
        String sql = """
            INSERT INTO sys_erp_config (name, erp_type, config_json, is_active, created_time)
            VALUES (?, ?, ?::jsonb, 1, CURRENT_TIMESTAMP)
            """;

        // 创建默认配置
        String adapterName = "AI 生成的 " + erpType.getDisplayName() + " 适配器";
        Map<String, Object> config = Map.of(
            "baseUrl", "https://api.example.com",
            "appKey", "",
            "appSecret", "",
            "accbookCode", "",
            "description", adapterName
        );

        try {
            String configJson = objectMapper.writeValueAsString(config);

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            int rows = jdbcTemplate.update(connection -> {
                var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, adapterName);
                ps.setString(2, erpType.getCode());
                ps.setString(3, configJson);
                return ps;
            }, keyHolder);

            if (rows > 0) {
                Number key = keyHolder.getKey();
                if (key != null) {
                    Long configId = key.longValue();
                    log.info("创建新配置成功: id={}, erpType={}", configId, erpType.getCode());
                    return configId;
                }
            }
        } catch (Exception e) {
            log.error("创建新配置失败: erpType={}", erpType.getCode(), e);
        }

        return null;
    }

    /**
     * 检查场景是否已存在
     *
     * @param configId 配置 ID
     * @param scenarioKey 场景标识码
     * @return 如果场景存在返回 true，否则返回 false
     */
    private boolean scenarioExists(Long configId, String scenarioKey) {
        String sql = "SELECT COUNT(*) FROM sys_erp_scenario WHERE config_id = ? AND scenario_key = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, configId, scenarioKey);
        return count != null && count > 0;
    }

    /**
     * 插入单个场景记录
     * <p>
     * Uses ON CONFLICT DO NOTHING to handle race conditions where multiple threads
     * might try to insert the same scenario simultaneously.
     * </p>
     *
     * @param configId 配置 ID
     * @param scenarioName 场景名称对象
     * @return 插入成功返回 true，失败或已存在返回 false
     */
    private boolean insertScenario(Long configId, ScenarioName scenarioName) {
        String sql = """
            INSERT INTO sys_erp_scenario (
                config_id, scenario_key, name, description, is_active, sync_strategy, created_time
            ) VALUES (?, ?, ?, ?, true, 'MANUAL', CURRENT_TIMESTAMP)
            ON CONFLICT (config_id, scenario_key) DO NOTHING
            """;

        try {
            int rows = jdbcTemplate.update(sql,
                configId,
                scenarioName.scenarioKey(),
                scenarioName.displayName(),
                scenarioName.description()
            );
            return rows > 0;
        } catch (Exception e) {
            log.warn("插入场景失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 注册结果记录
     *
     * @param configId 配置 ID（新建或已存在的）
     * @param erpType ERP 类型代码
     * @param createdCount 成功创建的场景数量
     * @param skippedCount 跳过的场景数量（重复）
     */
    public record RegistrationResult(
            Long configId,
            String erpType,
            int createdCount,
            int skippedCount
    ) {
    }
}
