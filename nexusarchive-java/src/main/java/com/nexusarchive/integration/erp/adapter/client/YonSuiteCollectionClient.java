// Input: Spring Framework、Lombok、YonSuiteClient、ERP DTO
// Output: YonSuiteCollectionClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.yonsuite.client.YonSuiteClient;
import com.nexusarchive.integration.yonsuite.dto.YonCollectionBillRequest;
import com.nexusarchive.integration.yonsuite.dto.YonCollectionBillResponse;
import com.nexusarchive.integration.yonsuite.dto.YonCollectionDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite 收款单客户端
 * 负责处理收款单相关的操作：查询收款单列表、获取收款单详情
 *
 * @author Agent D (基础设施工程师)
 */
@Component("erpAdapterCollectionClient")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteCollectionClient {

    private final YonSuiteClient yonSuiteClient;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateFormat.DATETIME);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateFormat.DATE);

    /**
     * 同步收款单文件
     *
     * @param accessToken 访问令牌（可为空）
     * @param accbookCode 账套代码
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @return 收款单 DTO 列表
     */
    public List<VoucherDTO> syncCollectionFiles(String accessToken, String accbookCode,
                                                  LocalDate startDate, LocalDate endDate) {
        try {
            YonCollectionBillRequest request = buildCollectionBillRequest(accbookCode, startDate, endDate);

            log.info("YonSuite 收款单查询: 组织=[{}], 期间=[{} to {}]",
                    accbookCode, request.getOpen_billDate_begin(), request.getOpen_billDate_end());

            YonCollectionBillResponse response = yonSuiteClient.queryCollectionBills(accessToken, request);
            log.info("组织 {} 收款单查询结果: {} 条",
                    accbookCode,
                    (response != null && response.getData() != null) ? response.getData().getRecordCount() : 0);

            if (!hasValidData(response)) {
                log.info("组织 {} 无收款单数据或API错误: {}",
                        accbookCode, response != null ? response.getMessage() : "null response");
                return Collections.emptyList();
            }

            return fetchCollectionDetails(accessToken, response.getData().getRecordList(), accbookCode, startDate);
        } catch (Exception e) {
            log.error("组织 {} 收款单同步异常", accbookCode, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建收款单查询请求
     */
    private YonCollectionBillRequest buildCollectionBillRequest(String accbookCode,
                                                                 LocalDate startDate, LocalDate endDate) {
        YonCollectionBillRequest request = new YonCollectionBillRequest();
        request.setPageIndex(1);
        request.setPageSize(100);

        request.setOpen_billDate_begin(startDate.atStartOfDay().format(DATE_TIME_FORMATTER));
        request.setOpen_billDate_end(endDate.atTime(23, 59, 59).format(DATE_TIME_FORMATTER));

        // financeOrg 设为 null，使用 simple.financeOrg.code 传递组织编码
        request.setFinanceOrg(null);

        Map<String, Object> simple = new HashMap<>();
        simple.put("financeOrg.code", accbookCode);
        request.setSimple(simple);

        return request;
    }

    /**
     * 检查响应是否有效
     */
    private boolean hasValidData(YonCollectionBillResponse response) {
        return response != null
                && "200".equals(response.getCode())
                && response.getData() != null
                && response.getData().getRecordList() != null
                && !response.getData().getRecordList().isEmpty();
    }

    /**
     * 获取收款单详情列表
     */
    private List<VoucherDTO> fetchCollectionDetails(String accessToken,
                                                     List<YonCollectionBillResponse.Record> records,
                                                     String accbookCode,
                                                     LocalDate startDate) {
        List<VoucherDTO> result = new ArrayList<>();

        for (YonCollectionBillResponse.Record record : records) {
            String billId = record.getId();
            try {
                VoucherDTO dto = fetchCollectionDetail(accessToken, billId, accbookCode, startDate);
                if (dto != null) {
                    result.add(dto);
                }
            } catch (Exception e) {
                log.warn("获取收款单详情异常: id={}", billId, e);
            }
        }

        return result;
    }

    /**
     * 获取单个收款单详情
     */
    private VoucherDTO fetchCollectionDetail(String accessToken, String billId,
                                              String accbookCode, LocalDate startDate) {
        var detailResp = yonSuiteClient.queryCollectionDetail(accessToken, billId);

        if (detailResp == null || !"200".equals(detailResp.getCode()) || detailResp.getData() == null) {
            log.warn("获取收款单详情失败: id={}, 错误: {}",
                    billId, detailResp != null ? detailResp.getMessage() : "null response");
            return null;
        }

        var detail = detailResp.getData();
        LocalDate voucherDate = parseBillDate(detail.getBillDate());

        VoucherDTO dto = VoucherDTO.builder()
                .voucherId(detail.getId())
                .voucherNo(detail.getCode())
                .voucherWord(detail.getCode())
                .voucherDate(voucherDate)
                .status("COLLECTION_BILL")
                .summary(buildCollectionSummary(detail))
                .accountPeriod(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .debitTotal(new BigDecimal(
                        detail.getOriTaxIncludedAmount() != null ? detail.getOriTaxIncludedAmount() : 0.0))
                .accbookCode(accbookCode)
                .build();

        dto.setCreator(detail.getCreatorUserName());

        log.debug("同步收款单: {} - {} (组织: {})", detail.getCode(), detail.getCustomerName(), accbookCode);
        return dto;
    }

    /**
     * 解析单据日期
     */
    private LocalDate parseBillDate(String billDate) {
        if (billDate == null || billDate.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(billDate, DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 构建收款单摘要
     */
    private String buildCollectionSummary(YonCollectionDetailResponse.CollectionDetail detail) {
        return String.format("收款单: %s, 客户: %s, 金额: %.2f CNY",
                detail.getCode(),
                detail.getCustomerName() != null ? detail.getCustomerName() : "N/A",
                detail.getOriTaxIncludedAmount() != null ? detail.getOriTaxIncludedAmount() : 0.0);
    }
}
