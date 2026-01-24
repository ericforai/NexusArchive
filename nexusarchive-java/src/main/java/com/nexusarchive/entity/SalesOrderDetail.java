// Input: Lombok、MyBatis-Plus
// Output: SalesOrderDetail 实体类
// Pos: 实体层

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sd_sales_order_detail")
public class SalesOrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Integer lineNo;

    // 产品信息
    private String productId;
    private String productCode;
    private String productName;
    private String skuId;
    private String skuCode;
    private String skuName;

    // 数量金额
    private BigDecimal qty;
    private BigDecimal oriUnitPrice;
    private BigDecimal oriMoney;
    private BigDecimal oriTax;
    private BigDecimal natUnitPrice;
    private BigDecimal natMoney;
    private BigDecimal natTax;

    // 税务
    private String taxRate;
    private String taxId;

    // 交付信息
    private String stockId;
    private String stockName;
    private LocalDate hopeReceiveDate;

    // 系统字段
    private LocalDateTime createdTime;
}
