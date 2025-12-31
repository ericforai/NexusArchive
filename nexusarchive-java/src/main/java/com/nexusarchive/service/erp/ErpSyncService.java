// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库
// Output: ErpSyncService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.engine.ErpMappingEngine;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArcFileMetadataIndexMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 同步执行服务
 * <p>
 * 负责执行 ERP 场景同步，包括数据获取、转换、保存和 PDF 生成。
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSyncService {

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
    private final ErpMappingEngine mappingEngine;

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    /**
     * 执行场景同步
     *
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
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
            com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = buildDtoConfig(entityConfig);

            // 2. 确定同步时间范围
            SyncDateRange dateRange = extractDateRange(scenario);
            LocalDate startDate = dateRange.startDate;
            LocalDate endDate = dateRange.endDate;

            // 3. 调用适配器同步
            log.info("调用适配器同步数据: {} - {}", startDate, endDate);
            List<VoucherDTO> vouchers = fetchVouchers(adapter, dtoConfig, scenario, startDate, endDate);

            totalFetched = vouchers != null ? vouchers.size() : 0;

            // 4. 保存数据到预归档库
            if (vouchers != null && !vouchers.isEmpty()) {
                savedCount = processVouchers(vouchers, scenario, entityConfig, adapter, startDate, endDate);
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
            history.setFailCount(totalFetched - savedCount);

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
            recordAuditLog(scenario, history, scenarioId, operatorId, clientIp);
        }
    }

    /**
     * 构建 ERP 配置 DTO
     */
    private com.nexusarchive.integration.erp.dto.ErpConfig buildDtoConfig(ErpConfig entityConfig) {
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
                List<String> codes = new java.util.ArrayList<>();
                for (int i = 0; i < accbookCodesArray.size(); i++) {
                    codes.add(accbookCodesArray.getStr(i));
                }
                dtoConfig.setAccbookCodes(codes);
            }

            dtoConfig.setExtraConfig(entityConfig.getConfigJson());
        }

        return dtoConfig;
    }

    /**
     * 提取同步日期范围
     */
    private SyncDateRange extractDateRange(ErpScenario scenario) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = LocalDate.of(2020, 1, 1);

        if (scenario.getParamsJson() != null) {
            try {
                cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
                String startStr = params.getStr("startDate");
                String endStr = params.getStr("endDate");
                if (cn.hutool.core.util.StrUtil.isNotEmpty(startStr)) {
                    startDate = LocalDate.parse(startStr);
                }
                if (cn.hutool.core.util.StrUtil.isNotEmpty(endStr)) {
                    endDate = LocalDate.parse(endStr);
                }
            } catch (Exception paramEx) {
                log.warn("解析场景参数失败，使用默认值: {}", paramEx.getMessage());
            }
        }

        return new SyncDateRange(startDate, endDate);
    }

    /**
     * 调用适配器获取凭证数据
     */
    private List<VoucherDTO> fetchVouchers(ErpAdapter adapter,
                                           com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig,
                                           ErpScenario scenario,
                                           LocalDate startDate,
                                           LocalDate endDate) {
        if ("COLLECTION_FILE_SYNC".equals(scenario.getScenarioKey())
                && adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
            return ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                    .syncCollectionFiles(dtoConfig, startDate, endDate);
        } else if ("PAYMENT_FILE_SYNC".equals(scenario.getScenarioKey())
                && adapter instanceof com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) {
            return ((com.nexusarchive.integration.erp.adapter.YonSuiteErpAdapter) adapter)
                    .syncPaymentFiles(dtoConfig, startDate, endDate);
        } else {
            return adapter.syncVouchers(dtoConfig, startDate, endDate);
        }
    }

    /**
     * 处理凭证列表
     */
    private int processVouchers(List<VoucherDTO> vouchers,
                                ErpScenario scenario,
                                ErpConfig entityConfig,
                                ErpAdapter adapter,
                                LocalDate startDate,
                                LocalDate endDate) {
        int savedCount = 0;

        // 解析场景中的映射配置
        cn.hutool.json.JSONObject mappingConfig = null;
        if (scenario.getParamsJson() != null) {
            cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
            mappingConfig = params.getJSONObject("mapping");
        }

        for (VoucherDTO dto : vouchers) {
            if (isVoucherExist(dto.getVoucherNo(), entityConfig.getId())) {
                continue;
            }

            ArcFileContent fileContent = mapToFileContent(dto, mappingConfig, adapter, startDate);
            fileContent.setSourceSystem(adapter.getName());

            setStoragePath(fileContent);
            setDefaultFields(fileContent, startDate);

            try {
                String voucherJson = objectMapper.writeValueAsString(dto);
                fileContent.setSourceData(voucherJson);
            } catch (Exception jsonEx) {
                log.warn("序列化原始数据失败: {}", jsonEx.getMessage());
            }

            arcFileContentMapper.insert(fileContent);
            savedCount++;

            // 生成 PDF 文件
            generatePdf(fileContent);

            // 创建档案记录
            Archive archive = createArchiveFromVoucher(dto, fileContent, entityConfig.getName());
            archiveMapper.insert(archive);
            log.info("创建档案记录: archiveCode={}, title={}", archive.getArchiveCode(), archive.getTitle());

            // 保存元数据索引
            saveMetadataIndex(fileContent, dto);
        }

        return savedCount;
    }

    /**
     * 映射 DTO 到文件内容
     */
    private ArcFileContent mapToFileContent(VoucherDTO dto,
                                            cn.hutool.json.JSONObject mappingConfig,
                                            ErpAdapter adapter,
                                            LocalDate startDate) {
        if (mappingConfig != null) {
            log.info("提取映射配置，执行动态字段映射...");
            cn.hutool.json.JSONObject sourceJson = cn.hutool.json.JSONUtil.parseObj(dto);
            return mappingEngine.mapToArcFileContent(sourceJson, mappingConfig);
        } else {
            return VoucherMapper.toArcFileContent(dto);
        }
    }

    /**
     * 设置存储路径
     */
    private void setStoragePath(ArcFileContent fileContent) {
        if (fileContent.getStoragePath() == null || fileContent.getStoragePath().isEmpty()) {
            String fondsCode = fileContent.getFondsCode() != null ? fileContent.getFondsCode() : "DEFAULT";
            String fileName = fileContent.getFileName();
            String storagePath = Paths.get(archiveRootPath, "pre-archive", fondsCode, fileName).toString();
            fileContent.setStoragePath(storagePath);
        }
    }

    /**
     * 设置默认字段
     */
    private void setDefaultFields(ArcFileContent fileContent, LocalDate startDate) {
        if (fileContent.getFondsCode() == null) {
            fileContent.setFondsCode("DEFAULT");
        }
        if (fileContent.getFiscalYear() == null) {
            fileContent.setFiscalYear(String.valueOf(startDate.getYear()));
        }
    }

    /**
     * 生成 PDF 文件
     */
    private void generatePdf(ArcFileContent fileContent) {
        try {
            String voucherJson = fileContent.getSourceData();
            if (voucherJson != null && !voucherJson.isEmpty()) {
                pdfGeneratorService.generatePdfForPreArchive(fileContent.getId(), voucherJson);
                log.info("PDF 生成成功: {}", fileContent.getErpVoucherNo());
            }
        } catch (Exception pdfEx) {
            log.warn("PDF 生成失败，不影响同步: {}", pdfEx.getMessage());
        }
    }

    /**
     * 保存元数据索引
     */
    private void saveMetadataIndex(ArcFileContent fileContent, VoucherDTO dto) {
        BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
        if (amount != null) {
            ArcFileMetadataIndex metadataIndex = ArcFileMetadataIndex.builder()
                    .fileId(fileContent.getId())
                    .totalAmount(amount)
                    .invoiceNumber(dto.getVoucherNo())
                    .issueDate(dto.getVoucherDate() != null ? dto.getVoucherDate() : LocalDate.now())
                    .parsedTime(LocalDateTime.now())
                    .parserType("ERP_SYNC")
                    .build();
            arcFileMetadataIndexMapper.insert(metadataIndex);
        }
    }

    /**
     * 检查凭证是否已存在
     */
    private boolean isVoucherExist(String voucherNo, Long configId) {
        // 精确查重：基于 erp_voucher_no 字段进行精确匹配
        // 避免 LIKE 模糊匹配导致 "记-1" 错误匹配 "记-10", "记-11" 等
        return arcFileContentMapper.selectCount(
                new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getErpVoucherNo, voucherNo)) > 0;
    }

    /**
     * 从 ERP 凭证 DTO 创建 acc_archive 记录
     * 使凭证在"凭证关联"页面可见，进入智能匹配流程
     */
    private Archive createArchiveFromVoucher(VoucherDTO dto, ArcFileContent fileContent, String sourceSystem) {
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
        BigDecimal amount = dto.getDebitTotal() != null ? dto.getDebitTotal() : dto.getCreditTotal();
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
     * 记录审计日志
     */
    private void recordAuditLog(ErpScenario scenario, SyncHistory history, Long scenarioId, String operatorId, String clientIp) {
        String auditDetails = String.format("ERP采集同步: 场景=%s, 结果=%s, 获取=%d, 成功=%d, 失败=%d",
                scenario.getName(), history.getStatus(), history.getTotalCount(), history.getSuccessCount(), history.getFailCount());
        if ("FAIL".equals(history.getStatus())) {
            auditDetails += ", 错误=" + history.getErrorMessage();
        }

        String auditUserId = operatorId != null && !operatorId.isEmpty() ? operatorId : "SYSTEM";
        String auditUsername = operatorId != null && !operatorId.isEmpty() ? "USER_" + operatorId : "SYSTEM";
        auditLogService.log(
                auditUserId,
                auditUsername,
                "CAPTURE",
                "ERP_SYNC",
                String.valueOf(scenarioId),
                history.getStatus(),
                auditDetails,
                clientIp
        );
    }

    /**
     * 同步日期范围
     */
    private static class SyncDateRange {
        final LocalDate startDate;
        final LocalDate endDate;

        SyncDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
}
