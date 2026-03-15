// Input: Lombok、Spring Framework、Java 标准库、本地模块、SAP 客户端
// Output: SapErpAdapter 类（门面模式）
// Pos: 集成模块 - SAP S/4HANA ERP 适配器
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.integration.erp.adapter.client.SapHttpClient;
import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.AccountSummaryDTO;
import com.nexusarchive.integration.erp.dto.AttachmentDTO;
import com.nexusarchive.integration.erp.dto.ConnectionTestResult;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.erp.dto.FeedbackResult;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.erp.dto.sap.SapJournalEntryDto;
import com.nexusarchive.integration.erp.exception.ErpException;
import com.nexusarchive.integration.erp.mapping.ErpMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SAP S/4HANA ERP 适配器 - 门面模式
 * 将 SapHttpClient 组合为统一接口
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>基于 SAP S/4HANA API Journal Entry OData V4 服务</li>
 *   <li>使用 ErpMapper 框架进行统一映射转换</li>
 *   <li>支持 Basic 认证</li>
 *   <li>实现 ErpAdapter 接口，与其他适配器保持一致</li>
 * </ul>
 *
 * <p>OData 服务路径：</p>
 * <ul>
 *   <li>凭证查询: /sap/opu/odata4/sap/api_journal_entry/.../JournalEntry</li>
 *   <li>凭证详情: /JournalEntry(JournalEntry='...',FiscalYear='...')?$expand=...</li>
 * </ul>
 *
 * @author Agent D (基础设施工程师)
 */
@ErpAdapterAnnotation(
    identifier = "sap",
    name = "SAP S/4HANA",
    description = "SAP S/4HANA ERP 系统，支持 OData V4 服务凭证查询和同步",
    version = "1.0.0",
    erpType = "SAP",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC"},
    supportsWebhook = false,
    priority = 30
)
@Service("sap")
@RequiredArgsConstructor
@Slf4j
public class SapErpAdapter implements ErpAdapter {

    private final SapHttpClient sapClient;
    private final ErpMapper erpMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateFormat.DATE);

    @Override
    public String getIdentifier() {
        return "sap";
    }

    @Override
    public String getName() {
        return "SAP S/4HANA";
    }

    @Override
    public String getDescription() {
        return "SAP S/4HANA ERP 系统，支持 OData V4 服务凭证查询和同步";
    }

    @Override
    public boolean supportsWebhook() {
        return false;
    }

    @Override
    public List<ErpScenario> getAvailableScenarios() {
        List<ErpScenario> scenarios = new ArrayList<>();

        // 场景1: 凭证同步
        ErpScenario voucher = new ErpScenario();
        voucher.setScenarioKey("VOUCHER_SYNC");
        voucher.setName("凭证同步");
        voucher.setDescription("自动采集 SAP S/4HANA 生成的财务凭证，并在归档系统中生成会计档案。支持附件自动下载。");
        voucher.setSyncStrategy("CRON");
        voucher.setCronExpression("0 0 23 * * ?"); // 默认每天23点
        voucher.setIsActive(false); // 默认不开启
        scenarios.add(voucher);

        // 场景2: 附件同步
        ErpScenario attachment = new ErpScenario();
        attachment.setScenarioKey("ATTACHMENT_SYNC");
        attachment.setName("附件同步");
        attachment.setDescription("同步凭证关联的电子发票和原始单据。");
        attachment.setSyncStrategy("REALTIME");
        attachment.setIsActive(false);
        scenarios.add(attachment);

        return scenarios;
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试查询最近一天的凭证来验证连接
            String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
            String today = LocalDate.now().format(DATE_FORMATTER);

            var response = sapClient.queryJournalEntries(config, yesterday, today);
            long responseTime = System.currentTimeMillis() - startTime;

            return ConnectionTestResult.success("连接成功", responseTime);

        } catch (Exception e) {
            log.error("SAP 连接测试失败", e);
            long responseTime = System.currentTimeMillis() - startTime;

            String errorCode = "CONNECTION_ERROR";
            if (e instanceof ErpException) {
                errorCode = ((ErpException) e).getErrorCode() != null
                    ? ((ErpException) e).getErrorCode()
                    : "SAP_API_ERROR";
            }

            return ConnectionTestResult.fail(e.getMessage(), errorCode);
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("SAP 凭证同步: {} ~ {}", startDate, endDate);

        List<VoucherDTO> allVouchers = new ArrayList<>();

        try {
            String startDateStr = startDate.format(DATE_FORMATTER);
            String endDateStr = endDate.format(DATE_FORMATTER);

            // 1. 查询凭证列表
            SapHttpClient.SapJournalEntryListResponse listResponse =
                sapClient.queryJournalEntries(config, startDateStr, endDateStr);

            if (listResponse == null || listResponse.getResults().isEmpty()) {
                log.warn("SAP 凭证查询结果为空");
                return Collections.emptyList();
            }

            // 2. 转换为 VoucherDTO
            for (SapJournalEntryDto entry : listResponse.getResults()) {
                try {
                    // 使用 ErpMapper 框架进行统一转换
                    AccountingSipDto sipDto = erpMapper.mapToSipDto(entry, "sap", config);
                    VoucherDTO voucherDto = convertSipToVoucherDto(sipDto);
                    allVouchers.add(voucherDto);
                } catch (Exception e) {
                    log.warn("转换 SAP 凭证失败 (凭证号: {}): {}",
                        entry.getJournalEntry(), e.getMessage());
                }
            }

            log.info("SAP 凭证同步完成: 共 {} 条", allVouchers.size());
            return allVouchers;

        } catch (Exception e) {
            log.error("SAP 凭证同步异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        log.info("SAP 获取凭证详情: {}", voucherNo);

        try {
            // voucherNo 格式: "凭证号-年度"
            String[] parts = voucherNo.split("-");
            if (parts.length != 2) {
                log.error("SAP 凭证号格式错误: {}", voucherNo);
                return null;
            }

            String journalEntry = parts[0];
            String fiscalYear = parts[1];

            // 查询凭证详情 (展开分录和附件)
            SapJournalEntryDto sapEntry = sapClient.getJournalEntryDetail(
                config, journalEntry, fiscalYear);

            if (sapEntry == null) {
                log.warn("SAP 凭证详情查询失败: {}", voucherNo);
                return null;
            }

            // 使用 ErpMapper 框架进行统一转换
            AccountingSipDto sipDto = erpMapper.mapToSipDto(sapEntry, "sap", config);
            return convertSipToVoucherDto(sipDto);

        } catch (Exception e) {
            log.error("SAP 获取凭证详情异常", e);
            return null;
        }
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        log.info("SAP 获取凭证附件: {}", voucherNo);

        try {
            VoucherDTO voucher = getVoucherDetail(config, voucherNo);
            if (voucher == null) {
                return Collections.emptyList();
            }

            return voucher.getAttachments() != null
                ? voucher.getAttachments()
                : Collections.emptyList();

        } catch (Exception e) {
            log.error("SAP 获取附件异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public FeedbackResult feedbackArchivalStatus(
            ErpConfig config, String voucherNo, String archivalCode, String status) {
        log.info("SAP 回写归档状态: voucherNo={}, archivalCode={}, status={}",
                voucherNo, archivalCode, status);

        // SAP 回写可能需要:
        // 1. 调用 BAPI 函数修改凭证抬头文本
        // 2. 或使用 OData PATCH 更新自定义字段
        // 此处预留接口，待具体业务需求确定后实现

        return FeedbackResult.failure(voucherNo, archivalCode, "SAP",
                "SAP feedback not implemented yet - 需要确定 BAPI 或自定义字段");
    }

    @Override
    public List<AccountSummaryDTO> fetchAccountSummary(
            ErpConfig config, String subjectCode, LocalDate startDate, LocalDate endDate) {
        log.info("执行 SAP 科目汇总获取 (PoC Mock): subject={}, range={} to {}",
                subjectCode, startDate, endDate);

        // TODO: 实现 SAP 科目汇总查询
        // 可能需要调用 GL_ACCOUNT_LINE_ITEM OData 服务
        List<AccountSummaryDTO> summaries = new ArrayList<>();

        summaries.add(AccountSummaryDTO.builder()
                .subjectCode(subjectCode != null ? subjectCode : "100100")
                .subjectName("库存现金 (SAP)")
                .debitTotal(new BigDecimal("50000.00"))
                .creditTotal(new BigDecimal("30000.00"))
                .voucherCount(10)
                .currency("CNY")
                .build());

        return summaries;
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
                .attachmentCount(sipDto.getHeader().getAttachmentCount());

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
}
