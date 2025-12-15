package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpScenarioService {

    private final ErpScenarioMapper erpScenarioMapper;
    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final ObjectMapper objectMapper;

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
            throw new RuntimeException("ERP配置不存在: " + configId);
        }

        // 获取适配器
        ErpAdapter adapter = erpAdapterFactory.getAdapter(config.getErpType());

        // 获取该适配器的标准场景
        List<ErpScenario> availableScenarios = adapter.getAvailableScenarios();

        // 保存到数据库
        LocalDateTime now = LocalDateTime.now();
        for (ErpScenario scenario : availableScenarios) {
            scenario.setConfigId(configId);
            scenario.setCreatedTime(now);
            scenario.setLastModifiedTime(now);
            scenario.setLastSyncStatus("NONE");
            // 确保没有 ID (MyBatis Plus 会自动生成)
            scenario.setId(null);

            erpScenarioMapper.insert(scenario);
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

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    /**
     * 执行场景同步
     */
    @Transactional
    public void syncScenario(Long scenarioId) {
        ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new RuntimeException("场景不存在");
        }

        log.info("手动触发同步场景: {}", scenario.getName());

        try {
            // 1. 获取配置和适配器
            ErpConfig entityConfig = erpConfigMapper.selectById(scenario.getConfigId());
            if (entityConfig == null || entityConfig.getIsActive() != 1) {
                throw new RuntimeException("关联的 ERP 配置已禁用或不存在");
            }
            ErpAdapter adapter = erpAdapterFactory.getAdapter(entityConfig.getErpType());

            // 转换 Entity -> DTO
            com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = new com.nexusarchive.integration.erp.dto.ErpConfig();
            dtoConfig.setId(String.valueOf(entityConfig.getId()));
            dtoConfig.setName(entityConfig.getName());
            dtoConfig.setAdapterType(entityConfig.getErpType());

            // 解析 configJson
            if (entityConfig.getConfigJson() != null) {
                cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(entityConfig.getConfigJson());
                dtoConfig.setBaseUrl(json.getStr("baseUrl"));
                dtoConfig.setAppKey(json.getStr("clientId")); // 映射: DB(clientId) -> DTO(appKey)
                dtoConfig.setAppSecret(json.getStr("clientSecret"));
                dtoConfig.setAccbookCode(json.getStr("accbookCode"));
                dtoConfig.setExtraConfig(entityConfig.getConfigJson());
            }

            // 2. 确定同步时间范围 (默认同步最近30天，或基于上次同步时间)
            // 简单起见，这里固定同步本月数据，实际应根据场景参数
            java.time.LocalDate endDate = java.time.LocalDate.now();
            // 修正: 探针验证通过的日期是 2020-01-01 起，minusYears(5) 只能到 2020-12，导致漏掉年初数据
            java.time.LocalDate startDate = java.time.LocalDate.of(2020, 1, 1);

            // 3. 调用适配器同步
            log.info("调用适配器同步数据: {} - {}", startDate, endDate);
            List<com.nexusarchive.integration.erp.dto.VoucherDTO> vouchers;

            // Special handling for YonSuite Collection File Sync
            log.info("TriggerSync Check: key={}, adapter={}, isYonSuite={}",
                    scenario.getScenarioKey(), adapter.getClass().getName(),
                    (adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter));

            if ("COLLECTION_FILE_SYNC".equals(scenario.getScenarioKey())
                    && adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
                log.info("Dispatching to syncCollectionFiles...");
                vouchers = ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                        .syncCollectionFiles(dtoConfig, startDate, endDate);
            } else {
                vouchers = adapter.syncVouchers(dtoConfig, startDate, endDate);
            }

            // 4. 保存数据到预归档库
            int savedCount = 0;
            if (vouchers != null && !vouchers.isEmpty()) {
                VoucherMapper mapper = new VoucherMapper();
                for (com.nexusarchive.integration.erp.dto.VoucherDTO dto : vouchers) {
                    if (isVoucherExist(dto.getVoucherNo(), entityConfig.getId())) {
                        continue; // 避免重复 (简单的幂等)
                    }
                    com.nexusarchive.entity.ArcFileContent fileContent = mapper.toArcFileContent(dto);
                    fileContent.setSourceSystem(entityConfig.getName());

                    // 设置正确的存储路径 (使用相对路径)
                    if (fileContent.getStoragePath() == null || fileContent.getStoragePath().isEmpty()) {
                        String fondsCode = fileContent.getFondsCode() != null ? fileContent.getFondsCode() : "DEFAULT";
                        String fileName = fileContent.getFileName();
                        String storagePath = Paths.get(archiveRootPath, "pre-archive", fondsCode, fileName).toString();
                        fileContent.setStoragePath(storagePath);
                    }

                    // 必须字段填充 (DA/T 94 要求)
                    if (fileContent.getFondsCode() == null)
                        fileContent.setFondsCode("DEFAULT");
                    if (fileContent.getFiscalYear() == null)
                        fileContent.setFiscalYear(String.valueOf(startDate.getYear()));

                    // 保存
                    // 设置原始数据 (Source Data) - 用于后续按需生成PDF
                    try {
                        String voucherJson = objectMapper.writeValueAsString(dto);
                        fileContent.setSourceData(voucherJson);
                    } catch (Exception jsonEx) {
                        log.warn("序列化原始数据失败: {}", jsonEx.getMessage());
                    }

                    // 保存
                    arcFileContentMapper.insert(fileContent);
                    savedCount++;

                    // 4.1 保存金额元数据到 ArcFileMetadataIndex (用于列表显示金额)
                    java.math.BigDecimal amount = dto.getDebitTotal();
                    if (amount == null) {
                        amount = dto.getCreditTotal();
                    }
                    if (amount != null) {
                        ArcFileMetadataIndex metadataIndex = ArcFileMetadataIndex.builder()
                                .fileId(fileContent.getId())
                                .totalAmount(amount)
                                .invoiceNumber(dto.getVoucherNo())
                                .issueDate(
                                        dto.getVoucherDate() != null ? dto.getVoucherDate() : java.time.LocalDate.now())
                                .parsedTime(LocalDateTime.now())
                                .parserType("ERP_SYNC")
                                .build();
                        arcFileMetadataIndexMapper.insert(metadataIndex);
                        log.debug("Metadata saved: fileId={}, amount={}", fileContent.getId(), amount);
                    }

                    log.debug("Record saved with source_data: fileId={}", fileContent.getId());
                }
            }

            // 5. 更新状态
            scenario.setLastSyncTime(LocalDateTime.now());
            scenario.setLastSyncStatus("SUCCESS");
            scenario.setLastSyncMsg(
                    "同步成功: 获取 " + (vouchers == null ? 0 : vouchers.size()) + " 条，其中新增 " + savedCount + " 条 ("
                            + startDate + "至" + endDate + ")");

        } catch (Exception e) {
            log.error("同步失败", e);
            scenario.setLastSyncTime(LocalDateTime.now());
            scenario.setLastSyncStatus("FAIL");
            scenario.setLastSyncMsg("同步异常: " + e.getMessage());
        }

        erpScenarioMapper.updateById(scenario);
    }

    private boolean isVoucherExist(String voucherNo, Long configId) {
        // 简单查重：基于文件名（通常包含单号）或备注
        // 实际应有 dedicated source_id column
        // 这里暂时通过 FileName LIKE voucherNo 来模糊判断，或者假设 voucherNo 唯一
        // 为了演示，暂时认为如果文件名包含 voucherNo 则已存在
        return arcFileContentMapper.selectCount(
                new LambdaQueryWrapper<com.nexusarchive.entity.ArcFileContent>()
                        .like(com.nexusarchive.entity.ArcFileContent::getFileName, voucherNo)) > 0;
    }

    // 内部类或外部类：Mapper
    public static class VoucherMapper {
        public com.nexusarchive.entity.ArcFileContent toArcFileContent(
                com.nexusarchive.integration.erp.dto.VoucherDTO dto) {
            com.nexusarchive.entity.ArcFileContent content = new com.nexusarchive.entity.ArcFileContent();
            String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
            content.setId(uuid);
            content.setFileName(dto.getVoucherNo() + ".pdf"); // PDF格式 (由VoucherPdfGeneratorService生成)
            content.setFileType("PDF");
            content.setFileSize(1024L); // Mock size, 实际大小由PDF生成后更新
            content.setCreatedTime(LocalDateTime.now());
            content.setCreator(dto.getCreator());
            content.setPreArchiveStatus(com.nexusarchive.entity.enums.PreArchiveStatus.PENDING_ARCHIVE.getCode());

            // 设置ERP凭证号 (用户可读的单据编号)
            content.setErpVoucherNo(dto.getVoucherNo());
            // 设置凭证类型 (用于前端动态显示标签: COLLECTION_BILL, PAYMENT_BILL, VOUCHER 等)
            content.setVoucherType(dto.getStatus());
            // 设置来源系统
            content.setSourceSystem("用友YonSuite");

            // 生成临时档号 (YS-年月日-UUID前8位)
            String tempArchivalCode = "YS-"
                    + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + uuid.substring(0, 8).toUpperCase();
            content.setArchivalCode(tempArchivalCode);

            // 存储路径 (由PDF生成服务更新为实际路径)
            String storagePath = "/data/archives/pre-archive/" + tempArchivalCode + "/" + dto.getVoucherNo() + ".pdf";
            content.setStoragePath(storagePath);

            return content;
        }
    }

    /**
     * 获取所有集成通道（聚合视图）
     * 用于"资料收集/在线接收"页面
     */
    public List<com.nexusarchive.dto.IntegrationChannelDTO> listAllChannels() {
        List<com.nexusarchive.dto.IntegrationChannelDTO> channels = new java.util.ArrayList<>();

        // 1. 获取所有启用的 ERP 配置
        LambdaQueryWrapper<ErpConfig> configQuery = new LambdaQueryWrapper<>();
        configQuery.eq(ErpConfig::getIsActive, 1);
        List<ErpConfig> configs = erpConfigMapper.selectList(configQuery);

        // 2. 对每个配置获取其场景
        for (ErpConfig config : configs) {
            List<ErpScenario> scenarios = listScenariosByConfigId(config.getId());

            for (ErpScenario scenario : scenarios) {
                // 仅显示已启用的场景 (符合操作手册逻辑)
                if (scenario.getIsActive() == null || !scenario.getIsActive()) {
                    continue;
                }

                com.nexusarchive.dto.IntegrationChannelDTO channel = com.nexusarchive.dto.IntegrationChannelDTO
                        .builder()
                        .id(scenario.getId())
                        .name(scenario.getScenarioKey())
                        .displayName(scenario.getName())
                        .configName(config.getName())
                        .erpType(config.getErpType())
                        .frequency(convertSyncStrategyToFrequency(scenario.getSyncStrategy(),
                                scenario.getCronExpression()))
                        .lastSync(
                                scenario.getLastSyncTime() != null
                                        ? scenario.getLastSyncTime().format(
                                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                        : null)
                        .receivedCount(extractReceivedCount(scenario.getLastSyncMsg()))
                        .status(convertStatusToChannelStatus(scenario.getLastSyncStatus()))
                        .description(scenario.getDescription())
                        .apiEndpoint(getApiEndpointForType(config.getErpType(), scenario.getScenarioKey()))
                        .accbookCode(extractAccbookCode(config.getConfigJson()))
                        .lastSyncMsg(scenario.getLastSyncMsg())
                        .build();

                log.info("构建通道 DTO: ID={}, Name={}, Msg={}, Count={}",
                        channel.getId(), channel.getName(), channel.getLastSyncMsg(), channel.getReceivedCount());

                channels.add(channel);
            }
        }

        return channels;
    }

    private Integer extractReceivedCount(String msg) {
        if (msg == null || msg.isEmpty()) {
            return 0;
        }
        try {
            // 格式: 同步成功: 获取 X 条，其中心增 Y 条
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

    private String cronToHuman(String cron) {
        if (cron == null || cron.isEmpty())
            return "定时";
        String[] parts = cron.split(" ");
        if (parts.length < 6)
            return cron;
        String minute = parts[1];
        String hour = parts[2];
        if ("*".equals(parts[3]) && "*".equals(parts[4])) {
            try {
                int h = Integer.parseInt(hour);
                int m = Integer.parseInt(minute);
                if (m == 0)
                    return "每日 " + h + ":00";
                return "每日 " + h + ":" + String.format("%02d", m);
            } catch (NumberFormatException e) {
                /* ignore */ }
        }
        if ("*".equals(hour) && "*".equals(parts[3])) {
            return "每小时";
        }
        return "定时 (" + hour + ":" + minute + ")";
    }

    private String convertStatusToChannelStatus(String lastSyncStatus) {
        if ("SUCCESS".equals(lastSyncStatus)) {
            return "normal";
        } else if ("FAIL".equals(lastSyncStatus)) {
            return "error";
        }
        return "normal"; // NONE 或 null 也显示正常
    }

    private String getApiEndpointForType(String erpType, String scenarioKey) {
        if ("yonsuite".equalsIgnoreCase(erpType) && "VOUCHER_SYNC".equals(scenarioKey)) {
            return "/integration/yonsuite/vouchers/sync";
        }
        // 其他类型暂不支持实时 API 同步
        return null;
    }

    private String extractAccbookCode(String configJson) {
        if (configJson == null || configJson.isEmpty())
            return "BR01";
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(configJson);
            return json.getStr("accbookCode", "BR01");
        } catch (Exception e) {
            return "BR01";
        }
    }
}
