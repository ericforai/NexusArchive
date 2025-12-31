// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ErpScenarioService 类（场景管理协调层）
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.entity.ErpSubInterface;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.ErpSubInterfaceMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ERP 场景管理服务
 * <p>
 * 负责管理 ERP 业务场景的配置和查询。
 * 同步执行逻辑已委托给 {@link com.nexusarchive.service.erp.ErpSyncService}，
 * 通道聚合逻辑已委托给 {@link com.nexusarchive.service.erp.ErpChannelService}。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpScenarioService {

    private final ErpScenarioMapper erpScenarioMapper;
    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final SyncHistoryMapper syncHistoryMapper;
    private final ErpSubInterfaceMapper erpSubInterfaceMapper;
    private final ObjectMapper objectMapper;
    private final ErpSubInterfaceService erpSubInterfaceService;
    private final com.nexusarchive.service.erp.ErpSyncService erpSyncService;
    private final com.nexusarchive.service.erp.ErpChannelService erpChannelService;

    /**
     * 获取指定配置的所有业务场景
     * 如果数据库中没有记录，则尝试从适配器加载默认场景并初始化
     */
    @Transactional
    public List<ErpScenario> listScenariosByConfigId(Long configId) {
        // 1. 查询数据库
        LambdaQueryWrapper<ErpScenario> query = new LambdaQueryWrapper<>();
        query.eq(ErpScenario::getConfigId, configId);
        List<ErpScenario> scenarios = erpScenarioMapper.selectList(query);

        // 2. 如果非空，直接返回
        if (!scenarios.isEmpty()) {
            return scenarios;
        }

        // 3. 如果为空，初始化默认场景
        return initializeScenarios(configId);
    }

    private List<ErpScenario> initializeScenarios(Long configId) {
        log.info("初始化 ERP 配置 {} 的业务场景...", configId);

        // 获取配置信息
        ErpConfig config = erpConfigMapper.selectById(configId);
        if (config == null) {
            log.warn("ERP配置不存在: {}", configId);
            return java.util.Collections.emptyList();
        }

        try {
            // 获取适配器 (转小写以防万一)
            String erpType = config.getErpType() != null ? config.getErpType().toLowerCase() : "generic";
            if (!erpAdapterFactory.isSupported(erpType)) {
                log.warn("未找到对应类型的适配器: {}, 跳过场景初始化", erpType);
                return java.util.Collections.emptyList();
            }
            
            ErpAdapter adapter = erpAdapterFactory.getAdapter(erpType);

            // 获取该适配器的标准场景
            List<ErpScenario> availableScenarios = adapter.getAvailableScenarios();
            if (availableScenarios == null) {
                return java.util.Collections.emptyList();
            }

            // 保存到数据库
            LocalDateTime now = LocalDateTime.now();
            for (ErpScenario scenario : availableScenarios) {
                try {
                    scenario.setConfigId(configId);
                    scenario.setCreatedTime(now);
                    scenario.setLastModifiedTime(now);
                    scenario.setLastSyncStatus("NONE");
                    // 确保没有 ID (MyBatis Plus 会自动生成)
                    scenario.setId(null);

                    erpScenarioMapper.insert(scenario);
                } catch (Exception e) {
                    log.error("Failed to insert scenario: {}", scenario.getScenarioKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error initializing scenarios for config {}", configId, e);
        }

        // 再次查询并返回
        return erpScenarioMapper
                .selectList(new LambdaQueryWrapper<ErpScenario>().eq(ErpScenario::getConfigId, configId));
    }

    /**
     * 更新场景配置 (启用/禁用, 改变策略)
     */
    @Transactional
    public void updateScenario(ErpScenario scenario) {
        scenario.setLastModifiedTime(LocalDateTime.now());
        erpScenarioMapper.updateById(scenario);
    }

    // ============ 同步执行方法（委托给 ErpSyncService）============

    /**
     * 执行场景同步
     *
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
     */
    @Transactional
    public void syncScenario(Long scenarioId, String operatorId, String clientIp) {
        erpSyncService.syncScenario(scenarioId, operatorId, clientIp);
    }

    // ============ 通道聚合方法（委托给 ErpChannelService）============

    /**
     * 获取所有集成通道（聚合视图）
     * 用于"资料收集/在线接收"页面
     *
     * @return 集成通道列表
     */
    public List<com.nexusarchive.dto.IntegrationChannelDTO> listAllChannels() {
        return erpChannelService.listAllChannels();
    }

    // ============ 场景参数管理方法 ============

    /**
     * 更新场景参数配置
     * 将传入的参数 Map 序列化为 JSON 存储到 params_json 字段
     *
     * @param scenarioId 场景 ID
     * @param params 参数 Map
     */
    @Transactional
    public void updateScenarioParams(Long scenarioId, Map<String, Object> params) {
        ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new RuntimeException("场景不存在: " + scenarioId);
        }

        try {
            String paramsJson = objectMapper.writeValueAsString(params);
            scenario.setParamsJson(paramsJson);
            scenario.setLastModifiedTime(LocalDateTime.now());
            erpScenarioMapper.updateById(scenario);
            log.info("场景参数已更新: scenarioId={}, params={}", scenarioId, paramsJson);
        } catch (Exception e) {
            log.error("序列化场景参数失败", e);
            throw new RuntimeException("参数格式错误: " + e.getMessage());
        }
    }

    // ============ 子接口管理方法（委托给 ErpSubInterfaceService）============

    /**
     * 获取场景的子接口列表
     */
    public List<ErpSubInterface> listSubInterfaces(Long scenarioId) {
        return erpSubInterfaceService.listSubInterfaces(scenarioId);
    }

    /**
     * 更新子接口配置
     * 包含：时间戳、数据验证、审计日志
     */
    @Transactional
    public void updateSubInterface(ErpSubInterface subInterface, String operatorId, String clientIp) {
        erpSubInterfaceService.updateSubInterface(subInterface, operatorId, clientIp);
    }

    /**
     * 切换子接口启用状态
     */
    @Transactional
    public void toggleSubInterface(Long id, String operatorId, String clientIp) {
        erpSubInterfaceService.toggleSubInterface(id, operatorId, clientIp);
    }

    // ============ 同步历史方法 ============

    /**
     * 获取场景的同步历史（最近10条）
     */
    public List<SyncHistory> getSyncHistory(Long scenarioId) {
        return syncHistoryMapper.selectList(
            new LambdaQueryWrapper<SyncHistory>()
                .eq(SyncHistory::getScenarioId, scenarioId)
                .orderByDesc(SyncHistory::getSyncStartTime)
                .last("LIMIT 10")
        );
    }
}
