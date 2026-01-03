// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/deploy/DatabaseRegistrationService.java
// Input: GeneratedCode
// Output: void (registers adapter in database)
// Pos: AI 模块 - 数据库注册服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.ai.deploy;

import com.nexusarchive.integration.erp.ai.generator.GeneratedCode;
import com.nexusarchive.integration.erp.ai.mapper.BusinessSemanticMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据库注册服务
 * <p>
 * 将生成的适配器信息注册到数据库
 * </p>
 */
@Slf4j
@Service
public class DatabaseRegistrationService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseRegistrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 将适配器注册到数据库
     *
     * @param code 生成的代码
     */
    @Transactional
    public void register(GeneratedCode code) {
        log.info("开始数据库注册: className={}", code.getClassName());

        String adapterId = code.getErpType().toLowerCase().replace(" ", "-");
        String adapterName = code.getErpName();
        String erpType = code.getErpType().toLowerCase();

        // 删除旧记录（如果存在）
        deleteExistingAdapter(adapterId);

        // 插入适配器主记录
        insertAdapter(adapterId, adapterName, erpType);

        // 插入场景映射
        insertScenarios(adapterId, code);

        log.info("数据库注册成功: adapterId={}", adapterId);
    }

    /**
     * 删除已存在的适配器记录
     */
    private void deleteExistingAdapter(String adapterId) {
        String deleteScenariosSql = "DELETE FROM sys_erp_adapter_scenario WHERE adapter_id = ?";
        int deletedScenarios = jdbcTemplate.update(deleteScenariosSql, adapterId);
        log.debug("删除旧场景记录: {} 条", deletedScenarios);

        String deleteAdapterSql = "DELETE FROM sys_erp_adapter WHERE adapter_id = ?";
        int deletedAdapter = jdbcTemplate.update(deleteAdapterSql, adapterId);
        log.debug("删除旧适配器记录: {} 条", deletedAdapter);
    }

    /**
     * 插入适配器主记录
     */
    private void insertAdapter(String adapterId, String adapterName, String erpType) {
        String sql = """
            INSERT INTO sys_erp_adapter (adapter_id, adapter_name, erp_type, base_url, enabled, created_time)
            VALUES (?, ?, ?, ?, true, CURRENT_TIMESTAMP)
            """;

        int rows = jdbcTemplate.update(sql,
            adapterId,
            adapterName,
            erpType,
            "https://api.example.com" // 默认值，用户需要手动配置
        );

        log.debug("插入适配器记录: {} 条", rows);
    }

    /**
     * 插入场景映射
     */
    private void insertScenarios(String adapterId, GeneratedCode code) {
        if (code.getMappings() == null || code.getMappings().isEmpty()) {
            log.warn("没有场景映射需要插入");
            return;
        }

        String sql = "INSERT INTO sys_erp_adapter_scenario (adapter_id, scenario_code) VALUES (?, ?)";

        int totalRows = 0;
        for (BusinessSemanticMapper.ScenarioMapping mapping : code.getMappings()) {
            if (mapping.getScenario() != null) {
                String scenarioCode = mapping.getScenario().getCode();
                int rows = jdbcTemplate.update(sql, adapterId, scenarioCode);
                totalRows += rows;
                log.debug("插入场景映射: {} -> {}", adapterId, scenarioCode);
            }
        }

        log.info("插入场景映射: {} 条", totalRows);
    }
}
