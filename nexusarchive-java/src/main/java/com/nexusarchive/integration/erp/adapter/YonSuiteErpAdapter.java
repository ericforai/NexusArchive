// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: YonSuiteErpAdapter 类
// Pos: 集成模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.annotation.ErpAdapterAnnotation;
import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.*;
import com.nexusarchive.integration.yonsuite.service.YonPaymentFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite (用友) ERP 适配器
 * 将现有 YonSuite 集成封装为标准适配器接口
 *
 * @author Agent D (基础设施工程师)
 */
@ErpAdapterAnnotation(
    identifier = "yonsuite",
    name = "用友YonSuite",
    description = "用友新一代企业云服务平台，支持凭证、附件、收款单、付款单、退款单同步和 Webhook 推送",
    version = "1.0.0",
    erpType = "YONSUITE",
    supportedScenarios = {"VOUCHER_SYNC", "ATTACHMENT_SYNC", "COLLECTION_FILE_SYNC", "PAYMENT_FILE_SYNC", "REFUND_FILE_SYNC"},
    supportsWebhook = true,
    priority = 10
)
@Service("yonsuite")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteErpAdapter implements ErpAdapter {

    private final YonSuiteClient yonSuiteClient;
    private final YonPaymentFileService yonPaymentFileService;
    private final com.nexusarchive.integration.yonsuite.service.YonPaymentListService yonPaymentListService;
    private final com.nexusarchive.integration.yonsuite.service.YonRefundListService yonRefundListService;

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

        // [AI Generated] 付款单文件获取场景
        com.nexusarchive.entity.ErpScenario paymentFileSync = new com.nexusarchive.entity.ErpScenario();
        paymentFileSync.setScenarioKey("PAYMENT_FILE_SYNC");
        paymentFileSync.setName("付款单文件获取");
        paymentFileSync.setDescription("从YonSuite获取资金结算文件 (AI集成)");
        paymentFileSync.setIsActive(true);
        paymentFileSync.setSyncStrategy("MANUAL");
        scenarios.add(paymentFileSync);

        // 付款退款单文件获取场景
        com.nexusarchive.entity.ErpScenario refundFileSync = new com.nexusarchive.entity.ErpScenario();
        refundFileSync.setScenarioKey("REFUND_FILE_SYNC");
        refundFileSync.setName("付款退款单文件获取");
        refundFileSync.setDescription("从YonSuite获取付款退款单文件");
        refundFileSync.setIsActive(true);
        refundFileSync.setSyncStrategy("MANUAL");
        scenarios.add(refundFileSync);

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
        List<VoucherDTO> allVouchers = new ArrayList<>();

        // 获取所有组织代码
        List<String> accbookCodes = config.resolveAllAccbookCodes();
        if (accbookCodes.isEmpty()) {
            // 兜底：使用配置中的单个代码
            accbookCodes = Collections.singletonList(config.getAccbookCode());
        }

        log.info("凭证同步: 共 {} 个组织待处理", accbookCodes.size());

        // 从配置中获取认证信息
        String appKey = config.getAppKey();
        String appSecret = config.getAppSecret();
        String accessToken = null;

        // 如果配置中有 appKey/appSecret，先获取 token
        if (appKey != null && !appKey.isEmpty() && appSecret != null && !appSecret.isEmpty()) {
            try {
                accessToken = yonSuiteClient.getTokenWithCredentials(appKey, appSecret);
                log.info("使用配置中的 appKey 获取 token 成功");
            } catch (Exception e) {
                log.warn("使用配置中的 appKey 获取 token 失败，将使用默认配置: {}", e.getMessage());
            }
        }

        // 遍历每个组织代码进行同步
        for (String accbookCode : accbookCodes) {
            log.info("同步组织账套: {} (期间: {} - {})", accbookCode, startDate, endDate);
            try {
                List<VoucherDTO> vouchers = syncVouchersForSingleOrg(accessToken, accbookCode, startDate, endDate);
                allVouchers.addAll(vouchers);
                log.info("组织 {} 同步完成: {} 条凭证", accbookCode, vouchers.size());
            } catch (Exception e) {
                log.error("组织 {} 同步失败: {}", accbookCode, e.getMessage(), e);
                // 继续处理其他组织，不中断
            }
        }

        log.info("所有组织同步完成，共 {} 条凭证", allVouchers.size());
        return allVouchers;
    }

    /**
     * 同步单个组织的凭证
     */
    private List<VoucherDTO> syncVouchersForSingleOrg(String accessToken, String accbookCode, LocalDate startDate, LocalDate endDate) {
        try {
            YonVoucherListRequest request = new YonVoucherListRequest();
            request.setAccbookCode(accbookCode);
            request.setPeriodStart(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            request.setPeriodEnd(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM")));

            // 使用嵌套的 Pager 类设置分页
            YonVoucherListRequest.Pager pager = new YonVoucherListRequest.Pager();
            pager.setPageIndex(1);
            pager.setPageSize(100);
            request.setPager(pager);

            YonVoucherListResponse response = yonSuiteClient.queryVouchers(accessToken, request);

            if (!"200".equals(response.getCode()) || response.getData() == null) {
                log.warn("YonSuite 同步凭证失败 (组织: {}): {}", accbookCode, response.getMessage());
                return Collections.emptyList();
            }

            // 转换为标准 DTO，使用正确的 recordList 字段
            List<VoucherDTO> vouchers = new ArrayList<>();
            if (response.getData().getRecordList() != null) {
                for (var record : response.getData().getRecordList()) {
                    if (record.getHeader() != null) {
                        var header = record.getHeader();
                        
                        // 解析凭证日期
                        LocalDate voucherDate = null;
                        if (header.getMaketime() != null && !header.getMaketime().isEmpty()) {
                            try {
                                voucherDate = LocalDate.parse(header.getMaketime(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            } catch (Exception e) {
                                // ignore parse error
                            }
                        }
                        
                        // 获取凭证号 (优先使用 displayname，如 "记-001")
                        String voucherNo = header.getDisplayname();
                        if (voucherNo == null || voucherNo.isEmpty()) {
                            voucherNo = header.getDisplaybillcode();
                        }
                        
                        // 凭证字 (从凭证类型获取)
                        String voucherWord = null;
                        if (header.getVouchertype() != null) {
                            voucherWord = header.getVouchertype().getVoucherstr(); // 如 "记"
                            if (voucherWord == null) {
                                voucherWord = header.getVouchertype().getName();
                            }
                        }
                        
                        // 摘要 (优先使用分录描述)
                        String summary = header.getDescription();
                        if ((summary == null || summary.isEmpty()) && record.getBody() != null && !record.getBody().isEmpty()) {
                            summary = record.getBody().get(0).getDescription();
                        }
                        
                        VoucherDTO dto = VoucherDTO.builder()
                                .voucherId(header.getId())
                                .voucherNo(voucherNo)
                                .voucherWord(voucherWord)
                                .voucherDate(voucherDate)
                                .accountPeriod(header.getPeriod())
                                .summary(summary)
                                .status(header.getVoucherstatus())
                                .debitTotal(header.getTotalDebitOrg())
                                .creditTotal(header.getTotalCreditOrg())
                                .accbookCode(accbookCode) // 记录来源组织
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
            log.error("YonSuite 同步凭证异常 (组织: {})", accbookCode, e);
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
        List<VoucherDTO> allResults = new ArrayList<>();

        // 获取所有组织代码 (与 syncVouchers 保持一致)
        List<String> accbookCodes = config.resolveAllAccbookCodes();
        if (accbookCodes.isEmpty()) {
            accbookCodes = Collections.singletonList(config.getAccbookCode());
        }

        log.info("收款单同步: 共 {} 个组织待处理", accbookCodes.size());

        // 从配置中获取认证信息
        String appKey = config.getAppKey();
        String appSecret = config.getAppSecret();
        String accessToken = null;

        // 如果配置中有 appKey/appSecret，先获取 token
        if (appKey != null && !appKey.isEmpty() && appSecret != null && !appSecret.isEmpty()) {
            try {
                accessToken = yonSuiteClient.getTokenWithCredentials(appKey, appSecret);
                log.info("使用配置中的 appKey 获取 token 成功");
            } catch (Exception e) {
                log.warn("使用配置中的 appKey 获取 token 失败，将使用默认配置: {}", e.getMessage());
            }
        }

        // 遍历每个组织代码进行同步
        for (String accbookCode : accbookCodes) {
            try {
                List<VoucherDTO> orgResults = syncCollectionFilesForSingleOrg(accessToken, accbookCode, startDate, endDate);
                allResults.addAll(orgResults);
                log.info("组织 {} 收款单同步完成: {} 条", accbookCode, orgResults.size());
            } catch (Exception e) {
                log.error("组织 {} 收款单同步失败: {}", accbookCode, e.getMessage(), e);
                // 继续处理其他组织，不中断
            }
        }

        log.info("所有组织收款单同步完成，共 {} 条", allResults.size());
        return allResults;
    }

    /**
     * 同步单个组织的收款单
     */
    private List<VoucherDTO> syncCollectionFilesForSingleOrg(String accessToken, String accbookCode, LocalDate startDate, LocalDate endDate) {
        try {
            YonCollectionBillRequest billReq = new YonCollectionBillRequest();
            billReq.setPageIndex(1);
            billReq.setPageSize(100);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            billReq.setOpen_billDate_begin(startDate.atStartOfDay().format(fmt));
            billReq.setOpen_billDate_end(endDate.atTime(23, 59, 59).format(fmt));

            // financeOrg 设为 null，使用 simple.financeOrg.code 传递组织编码
            billReq.setFinanceOrg(null);

            Map<String, Object> simple = new HashMap<>();
            simple.put("financeOrg.code", accbookCode);
            billReq.setSimple(simple);

            log.info("YonSuite 收款单查询: 组织=[{}], 期间=[{} to {}]",
                    accbookCode, billReq.getOpen_billDate_begin(), billReq.getOpen_billDate_end());

            // 使用传入的 accessToken（来自配置中的 appKey/appSecret）
            YonCollectionBillResponse billResp = yonSuiteClient.queryCollectionBills(accessToken, billReq);
            log.info("组织 {} 收款单查询结果: {} 条",
                    accbookCode,
                    (billResp != null && billResp.getData() != null) ? billResp.getData().getRecordCount() : 0);

            if (billResp == null || !"200".equals(billResp.getCode()) || billResp.getData() == null
                    || billResp.getData().getRecordList() == null || billResp.getData().getRecordList().isEmpty()) {
                log.info("组织 {} 无收款单数据或API错误: {}",
                        accbookCode, (billResp != null ? billResp.getMessage() : "null response"));
                return Collections.emptyList();
            }

            // 获取每个收款单的详情
            List<VoucherDTO> result = new ArrayList<>();

            for (YonCollectionBillResponse.Record record : billResp.getData().getRecordList()) {
                String billId = record.getId();
                try {
                    // 使用传入的 accessToken
                    var detailResp = yonSuiteClient.queryCollectionDetail(accessToken, billId);

                    if (detailResp != null && "200".equals(detailResp.getCode()) && detailResp.getData() != null) {
                        var detail = detailResp.getData();

                        // 解析单据日期
                        LocalDate voucherDate = null;
                        if (detail.getBillDate() != null && !detail.getBillDate().isEmpty()) {
                            try {
                                voucherDate = LocalDate.parse(detail.getBillDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            } catch (Exception e) {
                                // ignore parse error
                            }
                        }

                        VoucherDTO dto = VoucherDTO.builder()
                                .voucherId(detail.getId())
                                .voucherNo(detail.getCode())
                                .voucherWord(detail.getCode()) // 收款单号作为凭证字号
                                .voucherDate(voucherDate)      // 单据日期
                                .status("COLLECTION_BILL")
                                .summary(String.format("收款单: %s, 客户: %s, 金额: %.2f CNY",
                                        detail.getCode(),
                                        detail.getCustomerName() != null ? detail.getCustomerName() : "N/A",
                                        detail.getOriTaxIncludedAmount() != null ? detail.getOriTaxIncludedAmount() : 0.0))
                                .accountPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                                .debitTotal(new BigDecimal(
                                        detail.getOriTaxIncludedAmount() != null ? detail.getOriTaxIncludedAmount() : 0.0))
                                .accbookCode(accbookCode) // 记录来源组织
                                .build();

                        dto.setCreator(detail.getCreatorUserName());
                        result.add(dto);
                        log.debug("同步收款单: {} - {} (组织: {})", detail.getCode(), detail.getCustomerName(), accbookCode);
                    } else {
                        log.warn("获取收款单详情失败: id={}, code={}, 错误: {}",
                                billId, record.getCode(),
                                detailResp != null ? detailResp.getMessage() : "null response");
                    }
                } catch (Exception e) {
                    log.warn("获取收款单详情异常: id={}", billId, e);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("组织 {} 收款单同步异常", accbookCode, e);
            return Collections.emptyList();
        }
    }

    public List<VoucherDTO> syncPaymentFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("Executing AI-Generated Logic: syncPaymentFiles...");
        try {
            // 1. Determine File IDs to fetch
            // [AI Connector Factory - Pass 3 Integration]
            // We now call the generated Query Service to get real IDs based on date range.
            List<String> fileIds = yonPaymentListService.queryPaymentIds(config, startDate, endDate);

            if (fileIds.isEmpty()) {
                log.info("No payment bills found in range: {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            log.info("Found {} payment bills to fetch files for.", fileIds.size());

            // 2. Call AI Generated Service
            // Refactored Logic: Fetch Detail -> Store -> Generate PDF
            List<cn.hutool.json.JSONObject> results = yonPaymentFileService.syncPaymentDetailsAndGeneratePdfs(config,
                    fileIds);

            // 3. Map to VoucherDTO (to be saved as ArcFileContent in Pre-Archive)
            List<VoucherDTO> vouchers = new ArrayList<>();
            for (cn.hutool.json.JSONObject res : results) {
                // If localFileId is present, it's successful.
                String localId = res.getStr("localFileId");
                if (localId == null)
                    continue;

                VoucherDTO dto = VoucherDTO.builder()
                        .voucherNo(res.getStr("fileId")) // Payment ID
                        .summary("Generated Payment Record")
                        .accountPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .status("PENDING_CHECK")
                        .debitTotal(BigDecimal.ZERO)
                        .creditTotal(BigDecimal.ZERO)
                        .build();

                vouchers.add(dto);
            }
            log.info("Synced Payment Files: {}", vouchers.size());
            return vouchers;
        } catch (Exception e) {
            log.error("Sync Payment Files Logic Error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 同步付款退款单文件（按日期范围查询）
     *
     * 此方法会先查询指定日期范围内的退款单列表，获取 ID 后再同步文件
     *
     * @param config ERP 配置
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 同步结果
     */
    public List<VoucherDTO> syncRefundFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款退款单文件同步: startDate={}, endDate={}", startDate, endDate);

        // 1. 查询退款单 ID 列表
        List<String> refundIds = yonRefundListService.queryRefundIds(config, startDate, endDate);

        if (refundIds.isEmpty()) {
            log.info("未查询到任何退款单 ID，跳过文件同步");
            return Collections.emptyList();
        }

        log.info("查询到 {} 个退款单 ID，开始同步文件", refundIds.size());

        // 2. 调用现有的同步方法
        return syncRefundFiles(config, refundIds);
    }

    /**
     * 同步付款退款单文件
     *
     * 注意：此方法需要提供退款单的 fileId 列表
     * 实际使用时需要先从 YonSuite 查询退款单列表获取 fileId
     *
     * @param config ERP 配置
     * @param fileIds 退款单文件 ID 列表（最多20个）
     * @return 同步结果
     */
    public List<VoucherDTO> syncRefundFiles(ErpConfig config, List<String> fileIds) {
        log.info("执行付款退款单文件同步: fileIds={}", fileIds);

        if (fileIds == null || fileIds.isEmpty()) {
            log.info("没有提供文件 ID，跳过退款单文件同步");
            return Collections.emptyList();
        }

        // YonSuite API 限制单次最多20个文件ID
        if (fileIds.size() > 20) {
            log.warn("文件 ID 数量超过限制(20)，将分批处理");
            List<VoucherDTO> allResults = new ArrayList<>();

            // 分批处理
            for (int i = 0; i < fileIds.size(); i += 20) {
                int end = Math.min(i + 20, fileIds.size());
                List<String> batch = fileIds.subList(i, end);
                allResults.addAll(syncRefundFilesBatch(config, batch));
            }

            return allResults;
        }

        return syncRefundFilesBatch(config, fileIds);
    }

    /**
     * 批量同步退款单文件（单批次）
     */
    private List<VoucherDTO> syncRefundFilesBatch(ErpConfig config, List<String> fileIds) {
        List<VoucherDTO> results = new ArrayList<>();

        try {
            // 1. 获取 access token
            String appKey = config.getAppKey();
            String appSecret = config.getAppSecret();
            String accessToken = null;

            if (appKey != null && !appKey.isEmpty() && appSecret != null && !appSecret.isEmpty()) {
                try {
                    accessToken = yonSuiteClient.getTokenWithCredentials(appKey, appSecret);
                    log.info("使用配置中的 appKey 获取 token 成功");
                } catch (Exception e) {
                    log.warn("使用配置中的 appKey 获取 token 失败: {}", e.getMessage());
                }
            }

            // 2. 构建请求
            com.nexusarchive.integration.yonsuite.dto.YonRefundFileRequest request =
                new com.nexusarchive.integration.yonsuite.dto.YonRefundFileRequest();
            request.setFileId(fileIds);

            // 3. 调用 YonSuite API
            com.nexusarchive.integration.yonsuite.dto.YonRefundFileResponse response =
                yonSuiteClient.queryRefundFileUrls(accessToken, request);

            if (response == null || !"200".equals(response.getCode()) || response.getData() == null) {
                log.warn("查询退款单文件失败: {}", response != null ? response.getMessage() : "null response");
                return Collections.emptyList();
            }

            // 4. 解析结果
            for (com.nexusarchive.integration.yonsuite.dto.YonRefundFileResponse.RefundFileInfo fileInfo : response.getData()) {
                VoucherDTO dto = VoucherDTO.builder()
                        .voucherId(fileInfo.getId())
                        .voucherNo(fileInfo.getFileName())
                        .summary("付款退款单文件: " + fileInfo.getFileName())
                        .accountPeriod(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .status("REFUND_FILE")
                        .debitTotal(BigDecimal.ZERO)
                        .creditTotal(BigDecimal.ZERO)
                        .build();

                // 保存下载 URL 到附件
                List<AttachmentDTO> attachments = new ArrayList<>();
                AttachmentDTO attachment = AttachmentDTO.builder()
                        .attachmentId(fileInfo.getId())
                        .fileName(fileInfo.getFileName())
                        .downloadUrl(fileInfo.getDownLoadUrl())
                        .fileType(getFileExtension(fileInfo.getFileName()))
                        .build();
                attachments.add(attachment);
                dto.setAttachments(attachments);

                results.add(dto);
                log.info("成功获取退款单文件: fileName={}, downloadUrl={}",
                        fileInfo.getFileName(), fileInfo.getDownLoadUrl());
            }

            log.info("付款退款单文件同步完成: 共 {} 个文件", results.size());
            return results;

        } catch (Exception e) {
            log.error("同步付款退款单文件异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 从文件名提取扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    @Override
    public FeedbackResult feedbackArchivalStatus(ErpConfig config, String voucherNo, String archivalCode, String status) {
        try {
            log.info("┌── [YonSuite Adapter] 执行存证溯源 ──────────────────────────┐");
            log.info("│ voucherNo={}, archivalCode={}", voucherNo, archivalCode);
            log.info("└────────────────────────────────────────────────────────────┘");
            
            // 调用 YonSuiteClient 的增强反馈方法
            FeedbackResult result = yonSuiteClient.feedbackArchivalStatus(null, voucherNo, archivalCode);
            
            log.info("YonSuite 回写结果: success={}, mocked={}", result.isSuccess(), result.isMocked());
            return result;
        } catch (Exception e) {
            log.error("YonSuite feedbackArchivalStatus 异常", e);
            return FeedbackResult.failure(voucherNo, archivalCode, "YONSUITE", e.getMessage());
        }
    }


    @Override
    public List<com.nexusarchive.integration.erp.dto.AccountSummaryDTO> fetchAccountSummary(
            ErpConfig config, String subjectCode, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        log.info("Executing YonSuite Account Summary Fetch (PoC Mock): subject={}, range={} to {}", 
                subjectCode, startDate, endDate);
        
        // 模拟从 YonSuite 获取科目余额
        // 在实际生产中，这将调用 YonSuite 的余额表或辅助账接口
        List<com.nexusarchive.integration.erp.dto.AccountSummaryDTO> summaries = new java.util.ArrayList<>();
        
        summaries.add(com.nexusarchive.integration.erp.dto.AccountSummaryDTO.builder()
                .subjectCode(subjectCode != null ? subjectCode : "1001")
                .subjectName("库存现金")
                .debitTotal(new java.math.BigDecimal("50000.00"))
                .creditTotal(new java.math.BigDecimal("30000.00"))
                .voucherCount(10)
                .currency("CNY")
                .build());
        
        return summaries;
    }
}
