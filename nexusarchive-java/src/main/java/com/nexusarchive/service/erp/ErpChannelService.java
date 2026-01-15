// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ErpChannelService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.IntegrationChannelDTO;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.service.ErpScenarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ERP 集成通道聚合服务
 * <p>
 * 负责聚合所有 ERP 配置和场景，生成统一的集成通道视图。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpChannelService {

    private final ErpConfigMapper erpConfigMapper;
    private final ErpScenarioService erpScenarioService;

    /**
     * 获取所有集成通道（聚合视图）
     * 用于"资料收集/在线接收"页面
     *
     * @return 集成通道列表
     */
    public List<IntegrationChannelDTO> listAllChannels() {
        List<IntegrationChannelDTO> channels = new ArrayList<>();

        // 1. 获取所有启用的 ERP 配置
        LambdaQueryWrapper<ErpConfig> configQuery = new LambdaQueryWrapper<>();
        configQuery.eq(ErpConfig::getIsActive, 1);
        List<ErpConfig> configs = erpConfigMapper.selectList(configQuery);

        // 2. 对每个配置获取其场景
        for (ErpConfig config : configs) {
            try {
                List<ErpScenario> scenarios = erpScenarioService.listScenariosByConfigId(config.getId());

                for (ErpScenario scenario : scenarios) {
                    // 仅显示已启用的场景 (符合操作手册逻辑)
                    if (scenario.getIsActive() == null || !scenario.getIsActive()) {
                        continue;
                    }

                    IntegrationChannelDTO channel = buildChannelDTO(scenario, config);
                    channels.add(channel);

                    log.info("构建通道 DTO: ID={}, Name={}, Msg={}, Count={}",
                            channel.getId(), channel.getName(), channel.getLastSyncMsg(), channel.getReceivedCount());
                }
            } catch (Exception e) {
                log.error("Error processing config channel: {} ({})", config.getName(), config.getId(), e);
            }
        }

        return channels;
    }

    /**
     * 构建集成通道 DTO
     */
    private IntegrationChannelDTO buildChannelDTO(ErpScenario scenario, ErpConfig config) {
        return IntegrationChannelDTO.builder()
                .id(scenario.getId())
                .name(scenario.getScenarioKey())
                .displayName(scenario.getName())
                .configName(config.getName())
                .erpType(config.getErpType())
                .frequency(convertSyncStrategyToFrequency(scenario.getSyncStrategy(), scenario.getCronExpression()))
                .lastSync(formatLastSyncTime(scenario.getLastSyncTime()))
                .receivedCount(extractReceivedCount(scenario.getLastSyncMsg()))
                .status(convertStatusToChannelStatus(scenario.getLastSyncStatus()))
                .description(scenario.getDescription())
                .apiEndpoint(getApiEndpointForType(config.getErpType(), scenario.getScenarioKey()))
                .accbookCode(extractAccbookCode(config.getConfigJson()))
                .accbookCodes(extractAccbookCodes(config.getConfigJson(), config.getAccbookMapping()))
                .accbookMapping(extractAccbookMappingMap(config.getAccbookMapping()))
                .lastSyncMsg(scenario.getLastSyncMsg())
                .build();
    }

    /**
     * 格式化最后同步时间
     */
    private String formatLastSyncTime(java.time.LocalDateTime lastSyncTime) {
        if (lastSyncTime != null) {
            return lastSyncTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return null;
    }

    /**
     * 从同步消息中提取接收数量
     */
    private Integer extractReceivedCount(String msg) {
        if (msg == null || msg.isEmpty()) {
            return 0;
        }
        try {
            // 优先解析"新增 X 条"（实际入库数量）
            // 格式: 同步成功: 获取 32 条，其中新增 4 条
            if (msg.contains("新增")) {
                int start = msg.indexOf("新增") + 2;
                int end = msg.indexOf("条", start);
                if (start > 0 && end > start) {
                    String numStr = msg.substring(start, end).trim();
                    return Integer.parseInt(numStr);
                }
            }
            // 回退到"获取"（数据总量）
            if (msg.contains("获取")) {
                int start = msg.indexOf("获取") + 2;
                int end = msg.indexOf("条", start);
                if (start > 0 && end > start) {
                    String numStr = msg.substring(start, end).trim();
                    return Integer.parseInt(numStr);
                }
            }
        } catch (Exception e) {
            // ignore parse error
        }
        return 0;
    }

    /**
     * 转换同步策略到频率描述
     */
    private String convertSyncStrategyToFrequency(String strategy, String cronExpression) {
        if ("REALTIME".equals(strategy)) {
            return "实时/回调";
        } else if ("MANUAL".equals(strategy)) {
            return "手动触发";
        } else if ("CRON".equals(strategy) && cronExpression != null) {
            return cronToHuman(cronExpression);
        }
        return "手动触发";
    }

    /**
     * 将 Cron 表达式转换为可读格式
     */
    private String cronToHuman(String cron) {
        if (cron == null || cron.isEmpty()) {
            return "定时";
        }
        String[] parts = cron.split(" ");
        if (parts.length < 6) {
            return cron;
        }
        String minute = parts[1];
        String hour = parts[2];

        // 每日定时
        if ("*".equals(parts[3]) && "*".equals(parts[4])) {
            try {
                int h = Integer.parseInt(hour);
                int m = Integer.parseInt(minute);
                if (m == 0) {
                    return "每日 " + h + ":00";
                }
                return "每日 " + h + ":" + String.format("%02d", m);
            } catch (NumberFormatException e) {
                /* ignore */
            }
        }

        // 每小时
        if ("*".equals(hour) && "*".equals(parts[3])) {
            return "每小时";
        }

        return "定时 (" + hour + ":" + minute + ")";
    }

    /**
     * 转换同步状态到通道状态
     */
    private String convertStatusToChannelStatus(String lastSyncStatus) {
        if ("SUCCESS".equals(lastSyncStatus)) {
            return "normal";
        } else if ("FAIL".equals(lastSyncStatus)) {
            return "error";
        }
        return "normal"; // NONE 或 null 也显示正常
    }

    /**
     * 获取 API 端点
     */
    private String getApiEndpointForType(String erpType, String scenarioKey) {
        // 所有场景统一使用 /api/erp/scenario/{id}/sync 端点
        // 该端点会从数据库配置中正确获取 appKey/appSecret
        // 不再使用 /integration/yonsuite/vouchers/sync（它依赖环境变量配置）
        return null;
    }

    /**
     * 提取账簿代码
     */
    private String extractAccbookCode(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return "BR01";
        }
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configJson);
            return json.getStr("accbookCode", "BR01");
        } catch (Exception e) {
            return "BR01";
        }
    }

    /**
     * 提取账簿代码列表
     * 优先从 accbookMapping 提取（因为这是用户在 UI 配置的真实数据），
     * 如果为空则尝试从 configJson 读取
     */
    private List<String> extractAccbookCodes(String configJson, String accbookMapping) {
        // 1. 优先从 accbookMapping 提取账套代码（用户在 UI 配置的映射）
        if (accbookMapping != null && !accbookMapping.isEmpty()) {
            try {
                cn.hutool.json.JSONObject mapping = cn.hutool.json.JSONUtil.parseObj(accbookMapping);
                List<String> codes = new ArrayList<>();
                mapping.forEach((key, value) -> {
                    if (key != null) {
                        codes.add(key.toString());
                    }
                });
                if (!codes.isEmpty()) {
                    log.info("从 accbookMapping 提取到 {} 个账套代码: {}", codes.size(), codes);
                    return codes;
                }
            } catch (Exception e) {
                log.warn("解析 accbookMapping 失败: {}", e.getMessage());
            }
        }

        // 2. accbookMapping 为空时，尝试从 configJson 读取
        if (configJson != null && !configJson.isEmpty()) {
            try {
                cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configJson);
                cn.hutool.json.JSONArray codesArray = json.getJSONArray("accbookCodes");
                if (codesArray != null && !codesArray.isEmpty()) {
                    List<String> codes = new ArrayList<>();
                    for (int i = 0; i < codesArray.size(); i++) {
                        codes.add(codesArray.getStr(i));
                    }
                    return codes;
                }
                String singleCode = json.getStr("accbookCode");
                if (singleCode != null) {
                    return Collections.singletonList(singleCode);
                }
            } catch (Exception e) {
                log.warn("解析 configJson 中的 accbookCodes 失败: {}", e.getMessage());
            }
        }

        // 3. 默认返回 BR01
        return Collections.singletonList("BR01");
    }

    /**
     * 提取账套映射 Map
     */
    private java.util.Map<String, String> extractAccbookMappingMap(String accbookMapping) {
        if (accbookMapping == null || accbookMapping.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        try {
            cn.hutool.json.JSONObject mapping = cn.hutool.json.JSONUtil.parseObj(accbookMapping);
            java.util.Map<String, String> result = new java.util.HashMap<>();
            mapping.forEach((key, value) -> {
                if (key != null && value != null) {
                    result.put(key.toString(), value.toString());
                }
            });
            return result;
        } catch (Exception e) {
            log.warn("解析 accbookMapping Map 失败: {}", e.getMessage());
            return java.util.Collections.emptyMap();
        }
    }
}
