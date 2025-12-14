package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite (用友) ERP 适配器
 * 将现有 YonSuite 集成封装为标准适配器接口
 * 
 * @author Agent D (基础设施工程师)
 */
@Service("yonsuite")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteErpAdapter implements ErpAdapter {

    private final YonSuiteClient yonSuiteClient;

    @Override
    public String getIdentifier() {
        return "yonsuite";
    }

    @Override
    public String getName() {
        return "用友YonSuite";
    }

    @Override
    public String getDescription() {
        return "用友新一代企业云服务平台，支持凭证自动归档和 Webhook 实时推送";
    }

    @Override
    public boolean supportsWebhook() {
        return true;
    }

    @Override
    public List<com.nexusarchive.entity.ErpScenario> getAvailableScenarios() {
        List<com.nexusarchive.entity.ErpScenario> scenarios = new ArrayList<>();

        // 凭证同步场景
        com.nexusarchive.entity.ErpScenario voucherSync = new com.nexusarchive.entity.ErpScenario();
        voucherSync.setScenarioKey("VOUCHER_SYNC");
        voucherSync.setName("凭证同步");
        voucherSync.setDescription("从用友YonSuite同步会计凭证到档案系统");
        voucherSync.setIsActive(true);
        voucherSync.setSyncStrategy("MANUAL");
        scenarios.add(voucherSync);

        // 附件同步场景
        com.nexusarchive.entity.ErpScenario attachmentSync = new com.nexusarchive.entity.ErpScenario();
        attachmentSync.setScenarioKey("ATTACHMENT_SYNC");
        attachmentSync.setName("附件同步");
        attachmentSync.setDescription("同步凭证关联的电子发票和原始单据");
        attachmentSync.setIsActive(true);
        attachmentSync.setSyncStrategy("REALTIME");
        scenarios.add(attachmentSync);

        // 收款单文件同步场景
        com.nexusarchive.entity.ErpScenario collectionFileSync = new com.nexusarchive.entity.ErpScenario();
        collectionFileSync.setScenarioKey("COLLECTION_FILE_SYNC");
        collectionFileSync.setName("收款单文件同步");
        collectionFileSync.setDescription("从YonSuite获取收款单文件");
        collectionFileSync.setIsActive(true);
        collectionFileSync.setSyncStrategy("MANUAL");
        scenarios.add(collectionFileSync);

        return scenarios;
    }

    @Override
    public ConnectionTestResult testConnection(ErpConfig config) {
        long startTime = System.currentTimeMillis();

        try {
            // 尝试查询凭证列表（小范围）验证连接
            YonVoucherListRequest request = new YonVoucherListRequest();
            request.setAccbookCode(config.getAccbookCode());
            request.setPeriodStart(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            request.setPeriodEnd(request.getPeriodStart());

            // 使用嵌套的 Pager 类设置分页
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(1);
            pager.setPageSize(1);
            request.setPager(pager);

            YonVoucherListResponse response = yonSuiteClient.queryVouchers(null, request);

            long responseTime = System.currentTimeMillis() - startTime;

            if ("200".equals(response.getCode())) {
                return ConnectionTestResult.success("连接成功", responseTime);
            } else {
                return ConnectionTestResult.fail(
                        "API 返回错误: " + response.getMessage(),
                        response.getCode());
            }
        } catch (Exception e) {
            log.error("YonSuite 连接测试失败", e);
            return ConnectionTestResult.fail(e.getMessage(), "CONNECTION_ERROR");
        }
    }

    @Override
    public List<VoucherDTO> syncVouchers(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        try {
            YonVoucherListRequest request = new YonVoucherListRequest();
            request.setAccbookCode(config.getAccbookCode());
            request.setPeriodStart(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            request.setPeriodEnd(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            // 使用嵌套的 Pager 类设置分页
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(1);
            pager.setPageSize(100);
            request.setPager(pager);

            YonVoucherListResponse response = yonSuiteClient.queryVouchers(null, request);

            if (!"200".equals(response.getCode()) || response.getData() == null) {
                log.warn("YonSuite 同步凭证失败: {}", response.getMessage());
                return Collections.emptyList();
            }

            // 转换为标准 DTO，使用正确的 recordList 字段
            List<VoucherDTO> vouchers = new ArrayList<>();
            if (response.getData().getRecordList() != null) {
                for (var record : response.getData().getRecordList()) {
                    if (record.getHeader() != null) {
                        var header = record.getHeader();
                        VoucherDTO dto = VoucherDTO.builder()
                                .voucherId(header.getId())
                                .voucherNo(header.getDisplaybillcode())
                                .accountPeriod(header.getPeriod())
                                .summary(header.getDescription())
                                .status(header.getVoucherstatus())
                                .debitTotal(header.getTotalDebitOrg())
                                .creditTotal(header.getTotalCreditOrg())
                                .build();

                        if (header.getMaker() != null) {
                            dto.setCreator(header.getMaker().getName());
                        }
                        if (header.getAuditor() != null) {
                            dto.setAuditor(header.getAuditor().getName());
                        }
                        if (header.getTallyman() != null) {
                            dto.setPoster(header.getTallyman().getName());
                        }

                        vouchers.add(dto);
                    }
                }
            }

            return vouchers;

        } catch (Exception e) {
            log.error("YonSuite 同步凭证异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public VoucherDTO getVoucherDetail(ErpConfig config, String voucherNo) {
        try {
            // 注意：YonSuite 这里的 voucherNo 实际上往往需要是 voucherId
            // 如果传入的是显示的凭证号（如 "记-001"），则无法直接查询，通常上层 logic 会传递 ID
            YonVoucherDetailResponse response = yonSuiteClient.queryVoucherById(null, voucherNo);

            if (response == null || response.getData() == null) {
                return null;
            }

            var detail = response.getData();
            VoucherDTO dto = VoucherDTO.builder()
                    .voucherId(detail.getId())
                    .voucherNo(detail.getDisplayName())
                    .accountPeriod(detail.getPeriodUnion())
                    .summary(detail.getDescription())
                    .status(detail.getVoucherStatus())
                    .debitTotal(detail.getTotalDebitOrg())
                    .creditTotal(detail.getTotalCreditOrg())
                    .build();

            if (detail.getMakerObj() != null)
                dto.setCreator(detail.getMakerObj().getName());
            if (detail.getAuditorObj() != null)
                dto.setAuditor(detail.getAuditorObj().getName());
            if (detail.getTallyManObj() != null)
                dto.setPoster(detail.getTallyManObj().getName());

            return dto;
        } catch (Exception e) {
            log.error("YonSuite getVoucherDetail error", e);
            return null;
        }
    }

    @Override
    public List<AttachmentDTO> getAttachments(ErpConfig config, String voucherNo) {
        try {
            var response = yonSuiteClient.queryVoucherAttachments(null, voucherNo);

            if (response == null || response.getData() == null) {
                return Collections.emptyList();
            }

            List<AttachmentDTO> attachments = new ArrayList<>();
            for (var item : response.getData()) {
                AttachmentDTO att = AttachmentDTO.builder()
                        .attachmentId(item.getId())
                        .fileName(item.getFileName())
                        .fileType(item.getFileExtension())
                        .fileSize(item.getFileSize())
                        .downloadUrl(item.getUrl())
                        .build();
                attachments.add(att);
            }
            return attachments;
        } catch (Exception e) {
            log.error("YonSuite getAttachments error", e);
            return Collections.emptyList();
        }
    }

    public List<VoucherDTO> syncCollectionFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        // 1. 查询收款单列表获取单据ID
        try {
            YonCollectionBillRequest billReq = new YonCollectionBillRequest();
            billReq.setPageIndex(1);
            billReq.setPageSize(100); // 暂时限制100条

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            billReq.setOpen_billDate_begin(startDate.atStartOfDay().format(fmt));
            billReq.setOpen_billDate_end(endDate.atTime(23, 59, 59).format(fmt));

            // 默认查询已审批(2)的单据。为Debug暂时注释掉，查所有状态
            // billReq.setVerifyState(Collections.singletonList("2"));

            // 处理组织参数: 文档称 financeOrg 是必填 (通常指ID)
            // 但如果只有 Code (BRYS002), 传给 financeOrg (ID字段) 会导致查询为空
            // 尝试: 不传 financeOrg (null), 仅传 simple.financeOrg.code
            // 如果接口强校验 financeOrg 不为 null, 这里可能会报错, 需观察
            // 最终修正: financeOrg 设为 null (配合 Jackson NON_NULL 不发送该字段)
            billReq.setFinanceOrg(null);

            Map<String, Object> simple = new HashMap<>();
            simple.put("financeOrg.code", config.getAccbookCode());
            billReq.setSimple(simple);

            log.info("YonSuite Sync Request: Date=[{} to {}], OrgCode=[{}], VerifyState=[ALL (null)]",
                    billReq.getOpen_billDate_begin(), billReq.getOpen_billDate_end(), config.getAccbookCode());

            YonCollectionBillResponse billResp = yonSuiteClient.queryCollectionBills(null, billReq);
            log.info("Query Collection Bills Result Count: {}",
                    (billResp != null && billResp.getData() != null) ? billResp.getData().getRecordCount() : "null");

            if (billResp == null || !"200".equals(billResp.getCode()) || billResp.getData() == null
                    || billResp.getData().getRecordList() == null || billResp.getData().getRecordList().isEmpty()) {
                log.info("No collection bills found or API error: {}",
                        (billResp != null ? billResp.getMessage() : "null response"));
                return Collections.emptyList();
            }

            List<String> ids = new ArrayList<>();
            // Map billCode to ID for reference if needed
            for (YonCollectionBillResponse.Record record : billResp.getData().getRecordList()) {
                ids.add(record.getId());
            }

            // 2. 使用官方 API 获取每个收款单的详情
            // 官方接口: GET /yonbip/EFI/collection/detail
            // 文档: docs/api/收款单详情查询.md
            List<VoucherDTO> result = new ArrayList<>();

            for (YonCollectionBillResponse.Record record : billResp.getData().getRecordList()) {
                String billId = record.getId();
                try {
                    // 调用官方详情查询接口
                    var detailResp = yonSuiteClient.queryCollectionDetail(null, billId);

                    if (detailResp != null && "200".equals(detailResp.getCode()) && detailResp.getData() != null) {
                        var detail = detailResp.getData();

                        // 映射收款单详情到 VoucherDTO
                        VoucherDTO dto = VoucherDTO.builder()
                                .voucherId(detail.getId())
                                .voucherNo(detail.getCode()) // 单据编号
                                .status("COLLECTION_BILL")
                                .summary(String.format("收款单: %s, 客户: %s, 金额: %.2f CNY",
                                        detail.getCode(),
                                        detail.getCustomerName() != null ? detail.getCustomerName() : "N/A",
                                        detail.getOriTaxIncludedAmount() != null ? detail.getOriTaxIncludedAmount()
                                                : 0.0))
                                .accountPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                .build();

                        // 设置额外信息
                        dto.setCreator(detail.getCreatorUserName());

                        result.add(dto);
                        log.debug("Synced Collection Bill: {} - {}", detail.getCode(), detail.getCustomerName());
                    } else {
                        log.warn("Failed to fetch detail for collection bill id={}, code={}: {}",
                                billId, record.getCode(),
                                detailResp != null ? detailResp.getMessage() : "null response");
                    }
                } catch (Exception e) {
                    log.warn("Error fetching detail for collection bill id={}", billId, e);
                }
            }

            log.info("Sync Collection Files completed: {} items", result.size());
            return result;

        } catch (Exception e) {
            log.error("Sync Collection Files error", e);
            return Collections.emptyList();
        }
    }
}
