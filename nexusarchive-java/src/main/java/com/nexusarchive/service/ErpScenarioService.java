// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: ErpScenarioService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.SyncHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpScenarioService {

    private final ErpScenarioMapper erpScenarioMapper;
    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final ArcFileContentMapper arcFileContentMapper;
    private final ArcFileMetadataIndexMapper arcFileMetadataIndexMapper;
    private final SyncHistoryMapper syncHistoryMapper;
    private final ArchiveMapper archiveMapper;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final com.nexusarchive.engine.ErpMappingEngine mappingEngine;

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

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    /**
     * 执行场景同步
     */
    @Transactional
    public void syncScenario(Long scenarioId, String operatorId, String clientIp) {
        ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new RuntimeException("场景不存在");
        }

        log.info("触发同步场景: {} (Operator: {}, IP: {})", scenario.getName(), operatorId, clientIp);

        // 0. 创建并初始化同步历史记录
        SyncHistory history = new SyncHistory();
        history.setScenarioId(scenarioId);
        history.setSyncStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setOperatorId(operatorId);
        history.setClientIp(clientIp);
        history.setSyncParams(scenario.getParamsJson());
        history.setCreatedTime(LocalDateTime.now());
        syncHistoryMapper.insert(history);

        int totalFetched = 0;
        int savedCount = 0;

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
                String appKey = json.getStr("appKey");
                if (appKey == null || appKey.isEmpty()) {
                    appKey = json.getStr("clientId");
                }
                dtoConfig.setAppKey(appKey);

                String appSecret = json.getStr("appSecret");
                if (appSecret == null || appSecret.isEmpty()) {
                    appSecret = json.getStr("clientSecret");
                }
                // 使用 SM4 解密 (如果是明文则原样返回)
                dtoConfig.setAppSecret(com.nexusarchive.util.SM4Utils.decrypt(appSecret));
                dtoConfig.setAccbookCode(json.getStr("accbookCode"));

                // 解析多组织代码列表
                cn.hutool.json.JSONArray accbookCodesArray = json.getJSONArray("accbookCodes");
                if (accbookCodesArray != null && !accbookCodesArray.isEmpty()) {
                    java.util.List<String> codes = new java.util.ArrayList<>();
                    for (int i = 0; i < accbookCodesArray.size(); i++) {
                        codes.add(accbookCodesArray.getStr(i));
                    }
                    dtoConfig.setAccbookCodes(codes);
                }

                dtoConfig.setExtraConfig(entityConfig.getConfigJson());
            }

            // 2. 确定同步时间范围
            // 获取场景参数
            java.time.LocalDate endDate = java.time.LocalDate.now();
            java.time.LocalDate startDate = java.time.LocalDate.of(2020, 1, 1);
            
            if (scenario.getParamsJson() != null) {
                try {
                    cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
                    String startStr = params.getStr("startDate");
                    String endStr = params.getStr("endDate");
                    if (cn.hutool.core.util.StrUtil.isNotEmpty(startStr)) {
                        startDate = java.time.LocalDate.parse(startStr);
                    }
                    if (cn.hutool.core.util.StrUtil.isNotEmpty(endStr)) {
                        endDate = java.time.LocalDate.parse(endStr);
                    }
                } catch (Exception paramEx) {
                    log.warn("解析场景参数失败，使用默认值: {}", paramEx.getMessage());
                }
            }

            // 3. 调用适配器同步
            log.info("调用适配器同步数据: {} - {}", startDate, endDate);
            List<com.nexusarchive.integration.erp.dto.VoucherDTO> vouchers;

            if ("COLLECTION_FILE_SYNC".equals(scenario.getScenarioKey())
                    && adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
                vouchers = ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                        .syncCollectionFiles(dtoConfig, startDate, endDate);
            } else if ("PAYMENT_FILE_SYNC".equals(scenario.getScenarioKey())
                    && adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
                vouchers = ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                        .syncPaymentFiles(dtoConfig, startDate, endDate);
            } else {
                vouchers = adapter.syncVouchers(dtoConfig, startDate, endDate);
            }

            totalFetched = vouchers != null ? vouchers.size() : 0;

            // 4. 保存数据到预归档库
            if (vouchers != null && !vouchers.isEmpty()) {
                VoucherMapper mapper = new VoucherMapper();
                
                // 解析场景中的映射配置
                cn.hutool.json.JSONObject mappingConfig = null;
                if (scenario.getParamsJson() != null) {
                    cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
                    mappingConfig = params.getJSONObject("mapping");
                }

                for (com.nexusarchive.integration.erp.dto.VoucherDTO dto : vouchers) {
                    if (isVoucherExist(dto.getVoucherNo(), entityConfig.getId())) {
                        continue;
                    }

                    com.nexusarchive.entity.ArcFileContent fileContent;
                    if (mappingConfig != null) {
                        log.info("提取映射配置，执行动态字段映射...");
                        cn.hutool.json.JSONObject sourceJson = cn.hutool.json.JSONUtil.parseObj(dto);
                        fileContent = mappingEngine.mapToArcFileContent(sourceJson, mappingConfig);
                    } else {
                        fileContent = mapper.toArcFileContent(dto);
                    }
                    
                    // 使用适配器的用户友好名称，而非配置名称
                    String sourceSystemName = adapter.getName();
                    fileContent.setSourceSystem(sourceSystemName);

                    if (fileContent.getStoragePath() == null || fileContent.getStoragePath().isEmpty()) {
                        String fondsCode = fileContent.getFondsCode() != null ? fileContent.getFondsCode() : "DEFAULT";
                        String fileName = fileContent.getFileName();
                        String storagePath = Paths.get(archiveRootPath, "pre-archive", fondsCode, fileName).toString();
                        fileContent.setStoragePath(storagePath);
                    }

                    if (fileContent.getFondsCode() == null)
                        fileContent.setFondsCode("DEFAULT");
                    if (fileContent.getFiscalYear() == null)
                        fileContent.setFiscalYear(String.valueOf(startDate.getYear()));

                    try {
                        String voucherJson = objectMapper.writeValueAsString(dto);
                        fileContent.setSourceData(voucherJson);
                    } catch (Exception jsonEx) {
                        log.warn("序列化原始数据失败: {}", jsonEx.getMessage());
                    }

                    arcFileContentMapper.insert(fileContent);
                    savedCount++;

                    // ===== 同时创建 acc_archive 记录，使凭证关联页面可见 =====
                    Archive archive = createArchiveFromVoucher(dto, fileContent, entityConfig.getName());
                    archiveMapper.insert(archive);
                    log.info("创建档案记录: archiveCode={}, title={}", archive.getArchiveCode(), archive.getTitle());

                    // 保存元数据索引
                    java.math.BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
                    if (amount != null) {
                        ArcFileMetadataIndex metadataIndex = ArcFileMetadataIndex.builder()
                                .fileId(fileContent.getId())
                                .totalAmount(amount)
                                .invoiceNumber(dto.getVoucherNo())
                                .issueDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : java.time.LocalDate.now())
                                .parsedTime(LocalDateTime.now())
                                .parserType("ERP_SYNC")
                                .build();
                        arcFileMetadataIndexMapper.insert(metadataIndex);
                    }
                }
            }

            // 5. 更新状态和历史
            scenario.setLastSyncTime(LocalDateTime.now());
            scenario.setLastSyncStatus("SUCCESS");
            String msg = String.format("同步成功: 获取 %d 条，其中新增 %d 条 (%s至%s)", 
                                      totalFetched, savedCount, startDate, endDate);
            scenario.setLastSyncMsg(msg);

            history.setStatus("SUCCESS");
            history.setSyncEndTime(LocalDateTime.now());
            history.setTotalCount(totalFetched);
            history.setSuccessCount(savedCount);
            history.setFailCount(totalFetched - savedCount); // 这里的失败通常指已存在跳过或保存异常

        } catch (Exception e) {
            log.error("同步失败", e);
            scenario.setLastSyncTime(LocalDateTime.now());
            scenario.setLastSyncStatus("FAIL");
            scenario.setLastSyncMsg("同步异常: " + e.getMessage());

            history.setStatus("FAIL");
            history.setSyncEndTime(LocalDateTime.now());
            history.setErrorMessage(e.getMessage());
        } finally {
            erpScenarioMapper.updateById(scenario);
            syncHistoryMapper.updateById(history);

            // 记录合规审计日志 (GB/T 39362)
            String auditDetails = String.format("ERP采集同步: 场景=%s, 结果=%s, 获取=%d, 成功=%d, 失败=%d",
                    scenario.getName(), history.getStatus(), history.getTotalCount(), history.getSuccessCount(), history.getFailCount());
            if ("FAIL".equals(history.getStatus())) {
                auditDetails += ", 错误=" + history.getErrorMessage();
            }

            String auditUserId = operatorId != null && !operatorId.isEmpty() ? operatorId : "SYSTEM";
            String auditUsername = operatorId != null && !operatorId.isEmpty() ? "USER_" + operatorId : "SYSTEM";
            auditLogService.log(
                    auditUserId,
                    auditUsername, // 简化处理，实际应从上下文获取姓名
                    "CAPTURE",
                    "ERP_SYNC",
                    String.valueOf(scenarioId),
                    history.getStatus(),
                    auditDetails,
                    clientIp
            );
        }
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

            // ===== 新增: 填充显示字段 (凭证字号、摘要、业务日期) =====
            content.setVoucherWord(dto.getVoucherNo()); // 凭证字号 = 单据编号
            content.setDocDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : java.time.LocalDate.now()); // 业务日期
            
            // 生成摘要: 单据类型 + 供应商/客户
            String summary = dto.getSummary();
            if (summary == null || summary.isEmpty()) {
                String typeLabel = dto.getStatus() != null ? dto.getStatus() : "单据";
                summary = typeLabel + "-" + (dto.getVoucherNo() != null ? dto.getVoucherNo() : "");
            }
            content.setSummary(summary);
            // ===== END 新增 =====

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
     * 从 ERP 凭证 DTO 创建 acc_archive 记录
     * 使凭证在"凭证关联"页面可见，进入智能匹配流程
     */
    private Archive createArchiveFromVoucher(com.nexusarchive.integration.erp.dto.VoucherDTO dto,
                                              ArcFileContent fileContent, String sourceSystem) {
        Archive archive = new Archive();
        archive.setId(fileContent.getId()); // 使用相同 ID 便于关联

        // 档号
        archive.setArchiveCode(fileContent.getArchivalCode());

        // 题名
        String title = "会计凭证-" + dto.getVoucherNo();
        if (dto.getSummary() != null && !dto.getSummary().isEmpty()) {
            title = dto.getSummary();
        }
        archive.setTitle(title);

        // 摘要
        archive.setSummary(dto.getSummary());

        // 分类号 (AC01 = 会计凭证)
        archive.setCategoryCode("AC01");

        // 年度
        String fiscalYear = fileContent.getFiscalYear();
        if (fiscalYear == null && dto.getVoucherDate() != null) {
            fiscalYear = String.valueOf(dto.getVoucherDate().getYear());
        }
        if (fiscalYear == null) {
            fiscalYear = String.valueOf(LocalDate.now().getYear());
        }
        archive.setFiscalYear(fiscalYear);

        // 会计期间
        if (dto.getAccountPeriod() != null) {
            archive.setFiscalPeriod(dto.getAccountPeriod());
        }

        // 保管期限 (默认30年)
        archive.setRetentionPeriod("30Y");

        // 全宗号
        archive.setFondsNo(fileContent.getFondsCode() != null ? fileContent.getFondsCode() : "DEFAULT");

        // 立档单位
        archive.setOrgName(sourceSystem);

        // 金额
        java.math.BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
        archive.setAmount(amount);

        // 凭证日期
        archive.setDocDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now());

        // 制单人
        archive.setCreator(dto.getCreator());

        // 状态: draft (待匹配)
        archive.setStatus("draft");

        // 唯一业务 ID (防重)
        archive.setUniqueBizId(sourceSystem + "_" + dto.getVoucherId());

        // 存储凭证分录到 customMetadata (供匹配引擎识别业务场景)
        try {
            String entriesJson = objectMapper.writeValueAsString(dto);
            archive.setCustomMetadata(entriesJson);
        } catch (Exception e) {
            log.warn("序列化凭证分录失败: {}", e.getMessage());
        }

        return archive;
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
            try {
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
            } catch (Exception e) {
                log.error("Error processing config channel: {} ({})", config.getName(), config.getId(), e);
            }
        }

        return channels;
    }

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
        // 所有场景统一使用 /api/erp/scenario/{id}/sync 端点
        // 该端点会从数据库配置中正确获取 appKey/appSecret
        // 不再使用 /integration/yonsuite/vouchers/sync（它依赖环境变量配置）
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

    /**
     * 更新场景参数配置
     * 将传入的参数 Map 序列化为 JSON 存储到 params_json 字段
     */
    @Transactional
    public void updateScenarioParams(Long scenarioId, java.util.Map<String, Object> params) {
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
}
