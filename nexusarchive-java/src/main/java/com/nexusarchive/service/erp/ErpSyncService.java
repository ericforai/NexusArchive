// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、凭证 JSON
// Output: ErpSyncService 类（含全宗-账套路由校验）
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.erp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ErpConfig;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.entity.SyncHistory;
import com.nexusarchive.engine.ErpMappingEngine;
import com.nexusarchive.integration.erp.adapter.ErpAdapter;
import com.nexusarchive.integration.erp.adapter.ErpAdapterFactory;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ErpConfigMapper;
import com.nexusarchive.mapper.ErpScenarioMapper;
import com.nexusarchive.mapper.SyncHistoryMapper;
import com.nexusarchive.security.FondsContext;
import com.nexusarchive.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 同步执行服务
 * <p>
 * 负责执行 ERP 场景同步，协调数据获取、转换、保存等流程。
 * </p>
 *
 * <p>具体实现已委托给：</p>
 * <ul>
 *   <li>{@link ErpConfigDtoBuilder} - 配置 DTO 构建</li>
 *   <li>{@link VoucherFetcher} - 凭证数据获取</li>
 *   <li>{@link VoucherPersistenceService} - 凭证持久化</li>
 *   <li>{@link SyncDateRangeExtractor} - 日期范围提取</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ErpSyncService {

    private final ErpScenarioMapper erpScenarioMapper;
    private final ErpConfigMapper erpConfigMapper;
    private final ErpAdapterFactory erpAdapterFactory;
    private final SyncHistoryMapper syncHistoryMapper;
    private final ArchiveMapper archiveMapper;
    private final AuditLogService auditLogService;

    // 提取的服务
    private final ErpConfigDtoBuilder configDtoBuilder;
    private final VoucherFetcher voucherFetcher;
    private final VoucherPersistenceService voucherPersistence;
    private final SyncDateRangeExtractor dateRangeExtractor;
    private final ErpMappingEngine mappingEngine;
    private final ObjectMapper objectMapper;

    /**
     * 执行场景同步
     *
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
     */
    @Transactional
    public void syncScenario(Long scenarioId, String operatorId, String clientIp) {
        syncScenario(scenarioId, operatorId, clientIp, null, null);
    }

    /**
     * 执行场景同步（支持临时日期参数）
     *
     * @param scenarioId 场景 ID
     * @param operatorId 操作人 ID
     * @param clientIp 客户端 IP
     * @param tempStartDate 临时开始日期（格式：yyyy-MM-dd，前端传递时优先使用）
     * @param tempEndDate 临时结束日期（格式：yyyy-MM-dd，前端传递时优先使用）
     */
    @Transactional
    public void syncScenario(Long scenarioId, String operatorId, String clientIp,
                             String tempStartDate, String tempEndDate) {
        ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
        if (scenario == null) {
            throw new RuntimeException("场景不存在");
        }

        log.info("触发同步场景: {} (Operator: {}, IP: {}, 临时日期: {} ~ {})",
                scenario.getName(), operatorId, clientIp, tempStartDate, tempEndDate);

        // 创建并初始化同步历史记录
        SyncHistory history = createSyncHistory(scenarioId, operatorId, clientIp, scenario.getParamsJson());

        int totalFetched = 0;
        int savedCount = 0;

        try {
            // 获取配置和适配器
            ErpConfig entityConfig = getAndValidateConfig(scenario);
            ErpAdapter adapter = erpAdapterFactory.getAdapter(entityConfig.getErpType());
            com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig = configDtoBuilder.buildDtoConfig(entityConfig);

            // ✅ 校验全宗与账套映射一致性（Guard Clause）
            validateFondsAccbookMapping(entityConfig, dtoConfig);

            // 确定同步时间范围：优先使用临时参数，否则从数据库配置读取
            SyncDateRangeExtractor.DateRange dateRange = determineDateRange(scenario, tempStartDate, tempEndDate);

            // 调用适配器同步数据
            log.info("调用适配器同步数据: {} - {}", dateRange.startDate(), dateRange.endDate());
            List<VoucherDTO> vouchers = voucherFetcher.fetchVouchers(adapter, dtoConfig, scenario,
                    dateRange.startDate(), dateRange.endDate());

            totalFetched = vouchers != null ? vouchers.size() : 0;

            // 保存数据到预归档库
            if (vouchers != null && !vouchers.isEmpty()) {
                savedCount = processVouchers(vouchers, scenario, entityConfig, adapter, dateRange.startDate());
            }

            // 更新状态和历史
            updateSuccessStatus(scenario, history, totalFetched, savedCount, dateRange);

        } catch (Exception e) {
            log.error("同步失败", e);
            updateFailureStatus(scenario, history, e);
        } finally {
            erpScenarioMapper.updateById(scenario);
            syncHistoryMapper.updateById(history);
            recordAuditLog(scenario, history, scenarioId, operatorId, clientIp);
        }
    }

    /**
     * 确定同步日期范围
     * 优先使用临时参数（前端传递），否则从数据库场景配置读取
     *
     * @param scenario 场景配置
     * @param tempStartDate 临时开始日期
     * @param tempEndDate 临时结束日期
     * @return 日期范围
     */
    private SyncDateRangeExtractor.DateRange determineDateRange(ErpScenario scenario,
                                                                 String tempStartDate, String tempEndDate) {
        // 如果前端传了临时日期参数，优先使用
        if (tempStartDate != null && !tempStartDate.isEmpty() && tempEndDate != null && !tempEndDate.isEmpty()) {
            try {
                // 支持 yyyy-MM 格式
                if (tempStartDate.length() == 7) {
                    tempStartDate += "-01";
                }
                if (tempEndDate.length() == 7) {
                    // 简单的处理：如果是 yyyy-MM，直接 append -01 作为开始，或者计算月底作为结束
                    // 这里为了简单及配合 fetchVouchers 的逻辑（通常包含），我们 append -01
                    // 更好的做法是计算该月最后一天，但 VoucherFetcher 可能只看 include
                    tempEndDate += "-01"; 
                }

                java.time.LocalDate start = java.time.LocalDate.parse(tempStartDate);
                java.time.LocalDate end = java.time.LocalDate.parse(tempEndDate);
                
                // 如果是 yyyy-MM 格式且作为结束日期，应调整为该月最后一天
                if (tempEndDate.endsWith("-01") && tempEndDate.length() == 10 && end.getDayOfMonth() == 1) {
                     end = end.withDayOfMonth(end.lengthOfMonth());
                }

                log.info("使用前端传递的临时日期范围: {} ~ {}", start, end);
                return new SyncDateRangeExtractor.DateRange(start, end);
            } catch (Exception e) {
                log.warn("解析临时日期失败，使用数据库配置: {}", e.getMessage());
            }
        }

        // 否则从数据库场景配置读取
        SyncDateRangeExtractor.DateRange dateRange = dateRangeExtractor.extractDateRange(scenario);
        log.info("使用数据库配置的日期范围: {} ~ {}", dateRange.startDate(), dateRange.endDate());
        return dateRange;
    }

    /**
     * 创建同步历史记录
     */
    private SyncHistory createSyncHistory(Long scenarioId, String operatorId, String clientIp, String paramsJson) {
        SyncHistory history = new SyncHistory();
        history.setScenarioId(scenarioId);
        history.setSyncStartTime(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setOperatorId(operatorId);
        history.setClientIp(clientIp);
        history.setSyncParams(paramsJson);
        history.setCreatedTime(LocalDateTime.now());
        syncHistoryMapper.insert(history);
        return history;
    }

    /**
     * 获取并验证 ERP 配置
     */
    private ErpConfig getAndValidateConfig(ErpScenario scenario) {
        ErpConfig entityConfig = erpConfigMapper.selectById(scenario.getConfigId());
        if (entityConfig == null || entityConfig.getIsActive() != 1) {
            throw new RuntimeException("关联的 ERP 配置已禁用或不存在");
        }
        return entityConfig;
    }

    /**
     * 校验当前全宗与账套映射是否一致
     * 如果配置了映射但当前全宗不匹配，则抛出异常阻止同步
     *
     * @param entityConfig 数据库实体配置
     * @param dtoConfig DTO 配置（包含解析后的账套列表）
     */
    private void validateFondsAccbookMapping(ErpConfig entityConfig,
                                             com.nexusarchive.integration.erp.dto.ErpConfig dtoConfig) {
        String currentFonds = FondsContext.getCurrentFondsNo();
        if (currentFonds == null || currentFonds.isEmpty()) {
            log.warn("当前全宗上下文为空，跳过路由校验");
            return;
        }

        // 获取所有将被同步的账套代码
        java.util.List<String> accbookCodes = dtoConfig.resolveAllAccbookCodes();
        if (accbookCodes == null || accbookCodes.isEmpty()) {
            log.debug("未配置账套代码，跳过路由校验");
            return;
        }

        // 逐一校验每个账套的映射关系
        for (String accbookCode : accbookCodes) {
            if (!entityConfig.isAccbookMappedToFonds(accbookCode, currentFonds)) {
                String expectedFonds = entityConfig.getFondsForAccbook(accbookCode);
                String errorMsg = String.format(
                    "路由校验失败：账套 %s 配置的全宗为 %s，但当前全宗为 %s。请切换到正确的全宗后重试。",
                    accbookCode, expectedFonds, currentFonds
                );
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
        }

        log.info("路由校验通过：{} 个账套 -> 全宗 {}", accbookCodes.size(), currentFonds);
    }


    /**
     * 处理凭证列表
     */
    private int processVouchers(List<VoucherDTO> vouchers,
                                ErpScenario scenario,
                                ErpConfig entityConfig,
                                ErpAdapter adapter,
                                java.time.LocalDate startDate) {
        int savedCount = 0;

        // 解析场景中的映射配置
        cn.hutool.json.JSONObject mappingConfig = extractMappingConfig(scenario);

        for (VoucherDTO dto : vouchers) {
            String currentFonds = FondsContext.getCurrentFondsNo();
            // 修复：应使用凭证自身的日期来确定会计年度进行查重，而不是同步任务的开始日期
            LocalDate voucherDate = dto.getVoucherDate() != null ? dto.getVoucherDate() : startDate;
            String fiscalYear = String.valueOf(voucherDate.getYear());

            if (voucherPersistence.isVoucherExist(dto.getVoucherNo(), currentFonds, fiscalYear)) {
                continue;
            }

            // 先序列化源数据
            String voucherJson = serializeVoucherJson(dto);

            // 保存凭证
            voucherPersistence.saveVoucher(
                dto,
                mappingConfig,
                adapter,
                startDate,
                entityConfig.getName(),
                voucherJson
            );

            savedCount++;
        }

        return savedCount;
    }

    /**
     * 提取映射配置
     */
    private cn.hutool.json.JSONObject extractMappingConfig(ErpScenario scenario) {
        if (scenario.getParamsJson() != null) {
            cn.hutool.json.JSONObject params = cn.hutool.json.JSONUtil.parseObj(scenario.getParamsJson());
            return params.getJSONObject("mapping");
        }
        return null;
    }

    /**
     * 序列化凭证为 JSON
     */
    private String serializeVoucherJson(VoucherDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception jsonEx) {
            log.warn("序列化原始数据失败: {}", jsonEx.getMessage());
            return null;
        }
    }

    /**
     * 更新成功状态
     */
    private void updateSuccessStatus(ErpScenario scenario, SyncHistory history,
                                     int totalFetched, int savedCount,
                                     SyncDateRangeExtractor.DateRange dateRange) {
        scenario.setLastSyncTime(LocalDateTime.now());
        scenario.setLastSyncStatus("SUCCESS");
        String msg = String.format("同步成功: 获取 %d 条，其中新增 %d 条 (%s至%s)",
                totalFetched, savedCount, dateRange.startDate(), dateRange.endDate());
        scenario.setLastSyncMsg(msg);

        history.setStatus("SUCCESS");
        history.setSyncEndTime(LocalDateTime.now());
        history.setTotalCount(totalFetched);
        history.setSuccessCount(savedCount);
        history.setFailCount(totalFetched - savedCount);
    }

    /**
     * 更新失败状态
     */
    private void updateFailureStatus(ErpScenario scenario, SyncHistory history, Exception e) {
        scenario.setLastSyncTime(LocalDateTime.now());
        scenario.setLastSyncStatus("FAIL");
        scenario.setLastSyncMsg("同步异常: " + e.getMessage());

        history.setStatus("FAIL");
        history.setSyncEndTime(LocalDateTime.now());
        history.setErrorMessage(e.getMessage());
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
}
