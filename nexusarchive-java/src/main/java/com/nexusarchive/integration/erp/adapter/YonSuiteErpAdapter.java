package com.nexusarchive.integration.erp.adapter;

import com.nexusarchive.integration.erp.dto.*;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListRequest;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherListResponse;
import com.nexusarchive.integration.yonsuite.dto.YonVoucherDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
