// Input: Lombok、Jackson、Spring
// Output: SalesOutMapper 类
// Pos: YonSuite 集成 - 数据映射
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.constants.DateFormat;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.yonsuite.dto.SalesOutDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOutListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 销售出库单数据映射器
 * 将 YonSuite 销售出库单 API 响应映射为预归档文件记录
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesOutMapper {

    private static final String SOURCE_SYSTEM = "YonSuite";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DateFormat.DATE);
    private final ObjectMapper objectMapper;

    /**
     * 将列表响应记录映射为 ArcFileContent
     */
    public ArcFileContent toPreArchiveFile(SalesOutListResponse.SalesOutRecord record) {
        try {
            String businessDocNo = SOURCE_SYSTEM + "_SALESOUT_" + record.getId();
            String displayName = "销售出库单-" + record.getCode();

            ArcFileContent fileContent = ArcFileContent.builder()
                    .id(businessDocNo)
                    .archivalCode(generateArchivalCode())
                    .fileName(displayName + ".json")
                    .fileType("SALES_OUT")
                    .fileSize(0L)
                    .storagePath("pending/" + SOURCE_SYSTEM + "/salesout/" + businessDocNo + ".json")
                    .preArchiveStatus("PENDING_CHECK")
                    .sourceSystem(SOURCE_SYSTEM)
                    .businessDocNo(businessDocNo)
                    .erpVoucherNo(record.getCode())
                    .docDate(parseDate(record.getVouchdate()))
                    .summary("客户: " + record.getCustName() + ", 仓库: " + record.getWarehouseName())
                    .voucherWord("销售出库单")
                    .creator("system")
                    .build();

            // 设置原始数据
            try {
                fileContent.setSourceData(objectMapper.writeValueAsString(record));
            } catch (Exception e) {
                log.warn("Failed to set source data", e);
            }

            return fileContent;

        } catch (Exception e) {
            log.error("Failed to map sales out record: {}", record.getId(), e);
            return null;
        }
    }

    /**
     * 将详情响应映射为 ArcFileContent
     */
    public ArcFileContent toPreArchiveFile(SalesOutDetailResponse.SalesOutDetail detail) {
        try {
            String businessDocNo = SOURCE_SYSTEM + "_SALESOUT_" + detail.getId();
            String displayName = "销售出库单-" + detail.getCode();

            ArcFileContent fileContent = ArcFileContent.builder()
                    .id(businessDocNo)
                    .archivalCode(generateArchivalCode())
                    .fileName(displayName + ".json")
                    .fileType("SALES_OUT")
                    .fileSize(0L)
                    .storagePath("pending/" + SOURCE_SYSTEM + "/salesout/" + businessDocNo + ".json")
                    .preArchiveStatus("PENDING_CHECK")
                    .sourceSystem(SOURCE_SYSTEM)
                    .businessDocNo(businessDocNo)
                    .erpVoucherNo(detail.getCode())
                    .docDate(parseDate(detail.getVouchdate()))
                    .summary(buildSummary(detail))
                    .voucherWord("销售出库单")
                    .creator("system")
                    .build();

            // 设置原始数据
            try {
                fileContent.setSourceData(objectMapper.writeValueAsString(detail));
            } catch (Exception e) {
                log.warn("Failed to set source data", e);
            }

            return fileContent;

        } catch (Exception e) {
            log.error("Failed to map sales out detail: {}", detail.getId(), e);
            return null;
        }
    }

    /**
     * 生成档号
     */
    private String generateArchivalCode() {
        int year = LocalDate.now().getYear();
        return "SCP-" + year + "-" + System.currentTimeMillis();
    }

    /**
     * 解析日期字符串
     */
    private java.time.LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.split(" ")[0], DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    /**
     * 构建摘要信息
     */
    private String buildSummary(SalesOutDetailResponse.SalesOutDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("客户: ").append(detail.getCustName());
        sb.append(", 仓库: ").append(detail.getWarehouseName());
        if (detail.getOperatorName() != null) {
            sb.append(", 销售员: ").append(detail.getOperatorName());
        }
        if (detail.getDetails() != null && !detail.getDetails().isEmpty()) {
            sb.append(", 明细行数: ").append(detail.getDetails().size());
        }
        return sb.toString();
    }
}
