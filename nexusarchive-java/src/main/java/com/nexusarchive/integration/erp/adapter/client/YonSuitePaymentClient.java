// Input: Spring Framework、Lombok、ERP DTO、YonSuite 服务
// Output: YonSuitePaymentClient 类
// Pos: 集成模块 - ERP 适配器客户端
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.adapter.client;

import cn.hutool.json.JSONObject;
import com.nexusarchive.integration.erp.dto.VoucherDTO;
import com.nexusarchive.integration.erp.dto.ErpConfig;
import com.nexusarchive.integration.yonsuite.service.YonPaymentFileService;
import com.nexusarchive.integration.yonsuite.service.YonPaymentListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * YonSuite 付款单客户端
 * 负责处理付款单相关的操作：查询付款单列表、同步付款单文件
 *
 * @author Agent D (基础设施工程师)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuitePaymentClient {

    private final YonPaymentListService yonPaymentListService;
    private final YonPaymentFileService yonPaymentFileService;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 同步付款单文件
     *
     * @param config    ERP 配置
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 付款单 DTO 列表
     */
    public List<VoucherDTO> syncPaymentFiles(ErpConfig config, LocalDate startDate, LocalDate endDate) {
        log.info("执行付款单文件同步: startDate={}, endDate={}", startDate, endDate);

        // 1. 查询付款单 ID 列表
        List<String> fileIds = yonPaymentListService.queryPaymentIds(config, startDate, endDate);

        if (fileIds.isEmpty()) {
            log.info("未查询到任何付款单 ID，跳过文件同步");
            return Collections.emptyList();
        }

        log.info("查询到 {} 个付款单 ID，开始同步文件", fileIds.size());

        // 2. 同步详情并生成 PDF
        List<JSONObject> results = yonPaymentFileService.syncPaymentDetailsAndGeneratePdfs(
                config, fileIds);

        // 3. 转换为 VoucherDTO
        return convertToVoucherDTOs(results, startDate);
    }

    /**
     * 转换 JSON 结果为 VoucherDTO 列表
     */
    private List<VoucherDTO> convertToVoucherDTOs(List<JSONObject> results, LocalDate startDate) {
        List<VoucherDTO> vouchers = new ArrayList<>();

        for (JSONObject res : results) {
            String localId = res.getStr("localFileId");
            if (localId == null) {
                continue;
            }

            VoucherDTO dto = VoucherDTO.builder()
                    .voucherNo(res.getStr("fileId"))
                    .summary("付款单文件: " + res.getStr("fileId"))
                    .accountPeriod(startDate.format(PERIOD_FORMATTER))
                    .status("PAYMENT_FILE")
                    .debitTotal(BigDecimal.ZERO)
                    .creditTotal(BigDecimal.ZERO)
                    .build();

            vouchers.add(dto);
        }

        log.info("付款单文件同步完成: 共 {} 条", vouchers.size());
        return vouchers;
    }
}
