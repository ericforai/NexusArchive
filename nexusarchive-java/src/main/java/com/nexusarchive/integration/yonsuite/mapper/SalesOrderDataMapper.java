// Input: Lombok、Jackson
// Output: SalesOrderMapper
// Pos: YonSuite 集成 - 数据映射

package com.nexusarchive.integration.yonsuite.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.SalesOrder;
import com.nexusarchive.entity.SalesOrderDetail;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 销售订单数据映射器
 * <p>
 * 负责将 YonSuite API 响应 DTO 转换为实体类
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesOrderDataMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    /**
     * 列表记录 → SalesOrder 实体
     */
    public SalesOrder toSalesOrder(SalesOrderListResponse.SalesOrderRecord record) {
        if (record == null) {
            return null;
        }

        return SalesOrder.builder()
                .orderId(record.getId())
                .orderCode(record.getCode())
                .agentId(record.getAgentId())
                .agentName(record.getAgentName())
                .vouchdate(parseDate(record.getVouchdate()))
                .salesOrgId(record.getSalesOrgId())
                .totalMoney(toBigDecimal(record.getTotalMoney()))
                .payMoney(toBigDecimal(record.getPayMoney()))
                .realMoney(toBigDecimal(record.getRealMoney()))
                .nextStatusName(record.getNextStatusName())
                .yonOrderCode(record.getCode())
                .sourceSystem("YonSuite")
                .pubts(record.getPubts())
                .build();
    }

    /**
     * 详情响应 → SalesOrder 实体
     */
    public SalesOrder toSalesOrderFromDetail(SalesOrderDetailResponse.SalesOrderData data) {
        if (data == null) {
            return null;
        }

        return SalesOrder.builder()
                .orderId(data.getId())
                .orderCode(data.getCode())
                .agentId(data.getAgentId())
                .agentName(data.getAgentName())
                .vouchdate(parseDate(data.getVouchdate()))
                .orderDate(parseDate(data.getOrderDate()))
                .salesOrgId(data.getSalesOrgId())
                .totalMoney(toBigDecimal(data.getTotalMoney()))
                .promotionMoney(toBigDecimal(data.getPromotionMoney()))
                .rebateMoney(toBigDecimal(data.getRebateMoney()))
                .payMoney(toBigDecimal(data.getPayMoney()))
                .realMoney(toBigDecimal(data.getRealMoney()))
                .orderPayMoney(toBigDecimal(data.getOrderPayMoney()))
                .orderRealMoney(toBigDecimal(data.getOrderRealMoney()))
                .nextStatusName(data.getNextStatusName())
                .yonOrderCode(data.getCode())
                .sourceSystem("YonSuite")
                .pubts(data.getPubts())
                .build();
    }

    /**
     * 详情明细行 → SalesOrderDetail 实体列表
     */
    public List<SalesOrderDetail> toSalesOrderDetails(String orderId, List<SalesOrderDetailResponse.OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return new ArrayList<>();
        }

        // 解析 orderId（带异常处理）
        Long orderIdLong;
        try {
            orderIdLong = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            log.error("Invalid orderId format: {}", orderId);
            return new ArrayList<>();
        }

        List<SalesOrderDetail> result = new ArrayList<>();
        int lineNo = 1;

        for (SalesOrderDetailResponse.OrderDetail detail : details) {
            SalesOrderDetail entity = SalesOrderDetail.builder()
                    .orderId(orderIdLong)
                    .lineNo(lineNo++)
                    .productId(detail.getProductId())
                    .productCode(detail.getProductCode())
                    .productName(detail.getProductName())
                    .skuId(detail.getSkuId())
                    .skuCode(detail.getSkuCode())
                    .skuName(detail.getSkuName())
                    .qty(toBigDecimal(detail.getQty()))
                    .oriUnitPrice(toBigDecimal(detail.getOriUnitPrice()))
                    .oriMoney(toBigDecimal(detail.getOriMoney()))
                    .oriTax(toBigDecimal(detail.getOriTax()))
                    .natMoney(toBigDecimal(detail.getNatMoney()))
                    .natTax(toBigDecimal(detail.getNatTax()))
                    .taxRate(detail.getTaxRate())
                    .taxId(detail.getTaxId())
                    .stockId(detail.getStockId())
                    .stockName(detail.getStockName())
                    .hopeReceiveDate(parseDate(detail.getHopeReceiveDate()))
                    .build();

            result.add(entity);
        }

        return result;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            if (dateStr.length() > 10) {
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            }
            return LocalDate.parse(dateStr, DATE_FORMATTER_SHORT);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            return null;
        }
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value);
    }
}
