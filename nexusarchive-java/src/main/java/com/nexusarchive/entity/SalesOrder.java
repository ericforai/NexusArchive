// Input: Lombok、MyBatis-Plus、JPA
// Output: SalesOrder 实体类
// Pos: 实体层

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName("sd_sales_order")
public class SalesOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    // YonSuite 字段
    private String orderId;
    private String orderCode;
    private String agentId;
    private String agentName;
    private LocalDate vouchdate;
    private LocalDate orderDate;
    private String nextStatusName;

    // 金额字段
    private BigDecimal totalMoney;
    private BigDecimal promotionMoney;
    private BigDecimal rebateMoney;
    private BigDecimal payMoney;
    private BigDecimal realMoney;

    // 额外金额字段（详情API返回）
    private BigDecimal orderPayMoney;
    private BigDecimal orderRealMoney;

    // 外键关联
    private String salesOutId;
    private String voucherId;

    // 业务单号
    private String yonOrderCode;
    private String yonSalesOutCode;
    private String yonVoucherCode;

    // 匹配字段
    private String agentIdErp;
    private String salesOrgId;

    // 系统字段
    private String fondsCode;
    private String sourceSystem;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String pubts;
}
