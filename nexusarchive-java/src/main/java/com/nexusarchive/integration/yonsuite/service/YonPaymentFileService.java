package com.nexusarchive.integration.yonsuite.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonPaymentDetailResponse;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * YonBIP 资金结算 - 付款单同步服务
 * <p>
 * Logic Refactored: Fetch Detail -> Store Data -> Generate PDF locally
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class YonPaymentFileService {

    private final YonAuthService yonAuthService; // Inject Auth Service
    private final YonSuiteClient yonSuiteClient;
    private final ArcFileContentMapper arcFileContentMapper;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private final ObjectMapper objectMapper;

    // Mapping Constants
    private static final String SOURCE_SYSTEM = "YonSuite";
    private static final String VOUCHER_TYPE_PAYMENT = "PAYMENT"; // 付款单

    /**
     * 同步付款单详情并生成版式文件
     *
     * @param config  ERP配置
     * @param fileIds 付款单ID列表 (Payment Bill IDs)
     * @return 成功生成文件的ID列表
     */
    public List<JSONObject> syncPaymentDetailsAndGeneratePdfs(ErpConfig config, List<String> fileIds) {
        if (CollUtil.isEmpty(fileIds)) {
            return Collections.emptyList();
        }

        List<JSONObject> results = new ArrayList<>();
        // FIX: ErpConfig doesn't have getAccessToken(). Use Auth Service.
        String accessToken = yonAuthService.getAccessToken(config.getAppKey(), config.getAppSecret());

        log.info("开始处理付款单详情同步与生成，总数: {}", fileIds.size());

        for (String paymentId : fileIds) {
            try {
                // 1. 获取付款单详情
                YonPaymentDetailResponse response = yonSuiteClient.queryPaymentDetail(accessToken, paymentId);

                if (response == null || response.getData() == null) {
                    log.warn("未能获取付款单详情: id={}", paymentId);
                    continue;
                }

                // 2. 转换为预归档记录并保存
                String preArchiveFileId = processPaymentRecord(response.getData());

                if (preArchiveFileId != null) {
                    // 3. 生成 PDF 版式文件
                    generatePaymentPdf(preArchiveFileId, response.getData());

                    // 4. 构建返回结果
                    JSONObject result = new JSONObject();
                    result.putOpt("fileId", paymentId);
                    result.putOpt("localFileId", preArchiveFileId);
                    result.putOpt("status", "GENERATED");
                    results.add(result);
                }

            } catch (Exception e) {
                log.error("付款单处理失败: id={}", paymentId, e);
            }
        }

        return results;
    }

    /**
     * 处理付款单数据，保存到 arc_file_content
     */
    @Transactional
    public String processPaymentRecord(YonPaymentDetailResponse.PaymentDetail detail) {
        // 幂等性检查：基于业务单据号 (code)
        String businessDocNo = detail.getCode();

        ArcFileContent existing = arcFileContentMapper.selectOne(
                new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getBusinessDocNo, businessDocNo));

        if (existing != null && "ARCHIVED".equals(existing.getPreArchiveStatus())) {
            log.info("付款单已归档，跳过: {}", businessDocNo);
            return null; // Don't process if already archived
        }

        ArcFileContent content = existing != null ? existing : new ArcFileContent();

        // 映射基本字段
        content.setSourceSystem(SOURCE_SYSTEM);
        content.setBusinessDocNo(businessDocNo);
        content.setErpVoucherNo(businessDocNo); // 付款单号
        content.setVoucherType(VOUCHER_TYPE_PAYMENT); // "PAYMENT"

        // 映射业务字段
        content.setFondsCode(detail.getFinanceOrg());

        // FIX: ArcFileContent 实体缺少 amount, voucherDate, fiscalPeriod 等字段。
        // DA/T 94 要求这些元数据，但 Entities 定义可能仅包含核心字段。
        // 关键数据存储在 sourceData JSON 中供后续使用。
        try {
            content.setSourceData(objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
        }

        // Use setCreator for Creator Name mapping
        content.setCreator(detail.getCreatorUserName());

        content.setFiscalYear(extractYear(detail.getBillDate()));

        // Generate Temporary Archival Code (Critical Fix for Not-Null Constraint)
        // Format: PAY-[Year]-[Code] or similar.
        // detail.getBillDate() -> 2025-10-31
        String year = extractYear(detail.getBillDate());
        String code = detail.getCode();
        content.setArchivalCode("PAY-" + year + "-" + code);

        // 初始状态
        content.setFileName(businessDocNo + ".pdf"); // 预期文件名
        content.setFileType("pdf");

        // Satisfy DB Constraints for Pending Files
        content.setFileSize(0L);
        content.setFileHash(""); // Placeholder
        content.setHashAlgorithm("SM3"); // Default
        content.setStoragePath(""); // Placeholder for pending generation

        content.setPreArchiveStatus("PENDING_GENERATION"); // 待生成
        content.setCreatedTime(LocalDateTime.now());
        // content.setLastModifiedTime(LocalDateTime.now()); // Field missing

        // 保存
        if (existing != null) {
            arcFileContentMapper.updateById(content);
        } else {
            arcFileContentMapper.insert(content);
        }

        return content.getId();
    }

    /**
     * 调用 PDF 生成服务
     */
    private void generatePaymentPdf(String fileId, YonPaymentDetailResponse.PaymentDetail detail) {
        try {
            // 将详情对象序列化为 JSON 字符串传递给生成器
            String jsonData = objectMapper.writeValueAsString(detail);

            // 复用凭证 PDF 生成器 (假设可以用同一个模板或内部做逻辑分流)
            // 更好的做法是在 pdfGeneratorService 中增加 generatePaymentPdf 方法
            // 暂时使用 generatePdfForPreArchive，它处理通用 "Voucher" JSON
            pdfGeneratorService.generatePdfForPreArchive(fileId, jsonData);

            log.info("付款单 PDF 生成触发成功: fileId={}", fileId);

        } catch (Exception e) {
            log.error("付款单 PDF 生成触发失败: fileId={}", fileId, e);
        }
    }

    // Helper methods
    private java.time.LocalDate parseDate(String dateStr) {
        try {
            if (dateStr == null)
                return java.time.LocalDate.now();
            // Format: yyyy-MM-dd HH:mm:ss -> yyyy-MM-dd
            if (dateStr.length() > 10) {
                return java.time.LocalDate.parse(dateStr.substring(0, 10));
            }
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            return java.time.LocalDate.now();
        }
    }

    private String extractYear(String dateStr) {
        return String.valueOf(parseDate(dateStr).getYear());
    }

    private String extractPeriod(String dateStr) {
        return String.format("%02d", parseDate(dateStr).getMonthValue());
    }
}
