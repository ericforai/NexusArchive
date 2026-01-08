// Input: Lombok、Spring Framework、Java 标准库、本地模块、ERP 客户端
// Output: KingdeeErpAdapter 类（门面模式）
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.client.KingdeeVoucherClient;
import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeAuthResponse;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeVoucherDetailResponse;
import com.nexusarchive.integration.erp.dto.kingdee.KingdeeVoucherListResponse;
import com.nexusarchive.integration.erp.mapping.ErpMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 金蝶云星空 ERP 适配器 - 门面模式
 * 将 KingdeeVoucherClient 组合为统一接口
 *
 * <p>重构说明：</p>
 * <ul>
 *   <li>移除原有的直接 HTTP 调用逻辑，委托给 KingdeeVoucherClient</li>
 *   <li>使用 ErpMapper 框架进行统一映射转换</li>
 *   <li>实现 ErpAdapter 接口，与 YonSuiteErpAdapter 保持一致</li>
 *   <li>会话管理：每次操作前先认证获取 SessionId</li>
 * </ul>
 *
 * @author Agent D (基础设施工程师)
 */
@ErpAdapterAnnotation(
    identifier = "kingdee",
    name = "金蝶云星空",
    description = "金蝶云星空 ERP 系统 (K3Cloud)，支持凭证查询和同步",
    version = "2.0.0",
    erpType = "KINGDEE",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC"},
    supportsWebhook = false,
    priority = 20
)
@Service("kingdee")
@RequiredArgsConstructor
@Slf4j
public class KingdeeErpAdapter implements ErpAdapter {

    private final KingdeeVoucherClient voucherClient;
    private final ErpMapper erpMapper;

    @Override
    public String getIdentifier() {
        return "kingdee";
    }

    @Override
    public String getName() {
        return "金蝶云星空";
    }

    @Override
    public String getDescription() {
        return "金蝶云星空 ERP 系统 (K3Cloud)，支持凭证查询和同步";
    }

    @Override
    public boolean supportsWebhook() {
        return false;
    }

    @Override
    public List<com.nexusarchive.entity.ErpScenario> getAvailableScenarios() {
        List<com.nexusarchive.entity.ErpScenario> scenarios = new ArrayList<>();

        // 场景1: 凭证同步
        com.nexusarchive.entity.ErpScenario voucher = new com.nexusarchive.entity.ErpScenario();
        voucher.setScenarioKey("VOUCHER_SYNC");
        voucher.setName("凭证同步");
        voucher.setDescription("自动采集金蝶云星空生成的财务凭证，并在归档系统中生成会计档案。支持附件自动下载。");
        voucher.setSyncStrategy("CRON");
        voucher.setCronExpression("0 0 23 * * ?"); // 默认每天23点
        voucher.setIsActive(false); // 默认不开启
        scenarios.add(voucher);

        // 场景2: 存货核算
        com.nexusarchive.entity.ErpScenario inventory = new com.nexusarchive.entity.ErpScenario();
        inventory.setScenarioKey("INVENTORY_SYNC");
        inventory.setName("存货核算同步");
        inventory.setDescription("同步期末存货余额表、收发存汇总表等报表数据。");
        inventory.setSyncStrategy("CRON");
        inventory.setCronExpression("0 0 1 * * ?"); // 默认每天凌晨1点
        inventory.setIsActive(false);
        scenarios.add(inventory);

        // 场景3: 费用报销
        com.nexusarchive.entity.ErpScenario expense = new com.nexusarchive.entity.ErpScenario();
        expense.setScenarioKey("EXPENSE_SYNC");
        expense.setName("费用报销单据同步");
        expense.setDescription("同步员工费用报销单据作为原始凭证。");
        expense.setSyncStrategy("MANUAL"); // 默认手动
        expense.setIsActive(false);
        scenarios.add(expense);

        return scenarios;
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            boolean connected = voucherClient.testConnection(config);
            long responseTime = System.currentTimeMillis() - startTime;

            if (connected) {
                return ConnectionTestResult.success("连接成功", responseTime);
            } else {
                return ConnectionTestResult.fail("认证失败", "AUTH_FAILED");
            }
        } catch (Exception e) {
            log.error("金蝶连接测试失败", e);
            return ConnectionTestResult.fail(e.getMessage(), "CONNECTION_ERROR");
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("金蝶凭证同步: {} ~ {}", startDate, endDate);

        List<VoucherDTO> allVouchers = new ArrayList<>();

        try {
            // 1. 认证获取会话
            KingdeeAuthResponse authResponse = voucherClient.authenticate(config);
            if (authResponse == null || !authResponse.isSuccess()) {
                log.error("金蝶认证失败");
                return Collections.emptyList();
            }

            String sessionId = authResponse.getSessionId();

            // 2. 查询凭证列表
            KingdeeVoucherListResponse listResponse = voucherClient.syncVouchers(
                    config, sessionId, startDate, endDate);

            if (!listResponse.isSuccess() || listResponse.getData() == null) {
                log.warn("金蝶凭证查询失败: {}", listResponse.getMessage());
                return Collections.emptyList();
            }

            // 3. 转换为 VoucherDTO
            if (listResponse.getData().getVouchers() != null) {
                for (var voucher : listResponse.getData().getVouchers()) {
                    try {
                        // 使用 ErpMapper 框架进行统一转换
                        AccountingSipDto sipDto = erpMapper.mapToSipDto(
                                voucher, "kingdee", config);
                        VoucherDTO voucherDto = convertSipToVoucherDto(sipDto);
                        allVouchers.add(voucherDto);
                    } catch (Exception e) {
                        log.warn("转换金蝶凭证失败 (ID: {}): {}",
                                voucher.getVoucherId(), e.getMessage());
                    }
                }
            }

            log.info("金蝶凭证同步完成: 共 {} 条", allVouchers.size());
            return allVouchers;

        } catch (Exception e) {
            log.error("金蝶凭证同步异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        log.info("金蝶获取凭证详情: {}", voucherNo);

        try {
            // 1. 认证获取会话
            KingdeeAuthResponse authResponse = voucherClient.authenticate(config);
            if (authResponse == null || !authResponse.isSuccess()) {
                log.error("金蝶认证失败");
                return null;
            }

            String sessionId = authResponse.getSessionId();

            // 2. 查询凭证详情
            KingdeeVoucherDetailResponse detailResponse = voucherClient.getVoucherDetail(
                    config, sessionId, voucherNo);

            if (!detailResponse.isSuccess() || detailResponse.getData() == null) {
                log.warn("金蝶凭证详情查询失败: {}", detailResponse.getMessage());
                return null;
            }

            // 3. 使用 ErpMapper 框架进行统一转换
            AccountingSipDto sipDto = erpMapper.mapToSipDto(
                    detailResponse.getData(), "kingdee", config);

            return convertSipToVoucherDto(sipDto);

        } catch (Exception e) {
            log.error("金蝶获取凭证详情异常", e);
            return null;
        }
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        log.info("金蝶获取凭证附件: {}", voucherNo);

        try {
            // 1. 认证获取会话
            KingdeeAuthResponse authResponse = voucherClient.authenticate(config);
            if (authResponse == null || !authResponse.isSuccess()) {
                log.error("金蝶认证失败");
                return Collections.emptyList();
            }

            String sessionId = authResponse.getSessionId();

            // 2. 查询凭证详情 (包含附件信息)
            KingdeeVoucherDetailResponse detailResponse = voucherClient.getVoucherDetail(
                    config, sessionId, voucherNo);

            if (!detailResponse.isSuccess() || detailResponse.getData() == null) {
                return Collections.emptyList();
            }

            // 3. 提取附件信息
            List<AttachmentDTO> attachments = new ArrayList<>();
            var detail = detailResponse.getData();

            if (detail.getAttachments() != null) {
                for (var att : detail.getAttachments()) {
                    AttachmentDTO attDto = AttachmentDTO.builder()
                            .attachmentId(att.getAttachmentId())
                            .fileName(att.getFileName())
                            .fileType(att.getFileExt())
                            .fileSize(att.getFileSize())
                            .downloadUrl(att.getDownloadUrl())
                            .build();
                    attachments.add(attDto);
                }
            }

            return attachments;

        } catch (Exception e) {
            log.error("金蝶获取附件异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 转换 AccountingSipDto 到 VoucherDTO (保持接口兼容性)
     *
     * @param sipDto 标准化 SIP DTO
     * @return VoucherDTO
     */
    private VoucherDTO convertSipToVoucherDto(AccountingSipDto sipDto) {
        VoucherDTO.VoucherDTOBuilder builder = VoucherDTO.builder()
                .voucherId(sipDto.getHeader().getVoucherNumber())
                .voucherNo(sipDto.getHeader().getVoucherNumber())
                .voucherDate(sipDto.getHeader().getVoucherDate())
                .accountPeriod(sipDto.getHeader().getAccountPeriod())
                .summary(sipDto.getHeader().getRemark())
                .debitTotal(sipDto.getHeader().getTotalAmount())
                .creditTotal(sipDto.getHeader().getTotalAmount())
                .creator(sipDto.getHeader().getIssuer())
                .auditor(sipDto.getHeader().getReviewer());

        // 转换分录
        if (sipDto.getEntries() != null && !sipDto.getEntries().isEmpty()) {
            List<VoucherDTO.VoucherEntryDTO> entries = new ArrayList<>();
            for (var entry : sipDto.getEntries()) {
                VoucherDTO.VoucherEntryDTO entryDto = VoucherDTO.VoucherEntryDTO.builder()
                        .lineNo(entry.getLineNo())
                        .summary(entry.getSummary())
                        .accountCode(entry.getSubjectCode())
                        .accountName(entry.getSubjectName())
                        .debit(entry.getDirection() == com.nexusarchive.common.enums.DirectionType.DEBIT
                                ? entry.getAmount() : null)
                        .credit(entry.getDirection() == com.nexusarchive.common.enums.DirectionType.CREDIT
                                ? entry.getAmount() : null)
                        .build();
                entries.add(entryDto);
            }
            builder.entries(entries);
        }

        // 转换附件
        if (sipDto.getAttachments() != null && !sipDto.getAttachments().isEmpty()) {
            List<AttachmentDTO> attachments = new ArrayList<>();
            for (var att : sipDto.getAttachments()) {
                AttachmentDTO attDto = AttachmentDTO.builder()
                        .fileName(att.getFileName())
                        .fileType(att.getFileType())
                        .fileSize(att.getFileSize())
                        .build();
                attachments.add(attDto);
            }
            builder.attachments(attachments);
            builder.attachmentCount(sipDto.getAttachments().size());
        }

        return builder.build();
    }

    @Override
    public FeedbackResult feedbackArchivalStatus(
            ErpConfig config, String voucherNo, String archivalCode, String status) {
        log.info("金蝶回写归档状态: voucherNo={}, archivalCode={}, status={}",
                voucherNo, archivalCode, status);

        // TODO: 实现金蝶回写逻辑
        // 金蝶可能需要调用自定义 WebAPI 或修改凭证备注
        return FeedbackResult.failure(voucherNo, archivalCode, "KINGDEE",
                "Kingdee feedback not implemented yet");
    }

    @Override
    public List<AccountSummaryDTO> fetchAccountSummary(
            ErpConfig config, String subjectCode, LocalDate startDate, LocalDate endDate) {
        log.info("执行金蝶科目汇总获取 (PoC Mock): subject={}, range={} to {}",
                subjectCode, startDate, endDate);

        // TODO: 实现金蝶科目汇总查询
        List<AccountSummaryDTO> summaries = new ArrayList<>();

        summaries.add(AccountSummaryDTO.builder()
                .subjectCode(subjectCode != null ? subjectCode : "1001")
                .subjectName("库存现金")
                .debitTotal(new BigDecimal("50000.00"))
                .creditTotal(new BigDecimal("30000.00"))
                .voucherCount(10)
                .currency("CNY")
                .build());

        return summaries;
    }
}
