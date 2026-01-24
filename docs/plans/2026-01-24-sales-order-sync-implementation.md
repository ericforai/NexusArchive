# Sales Order Sync Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现 YonSuite 销售订单同步功能，建立"销售订单 → 销售出库单 → 记账凭证"的全链路关联

**Architecture:** 遵循现有凭证同步模式，两步 API（列表 → 详情），独立表存储，外键+业务单号混合关联

**Tech Stack:** Java 17, Spring Boot 3.1.6, MyBatis-Plus 3.5.7, PostgreSQL

---

## Progress Overview

| Task | Description | Status |
|------|-------------|--------|
| 1 | 数据库迁移脚本 | Pending |
| 2 | 实体类创建 - SalesOrder | Pending |
| 3 | 实体类创建 - SalesOrderDetail | Pending |
| 4 | DTO 类创建 - SalesOrderListRequest | Pending |
| 5 | DTO 类创建 - SalesOrderListResponse | Pending |
| 6 | DTO 类创建 - SalesOrderDetailResponse | Pending |
| 7 | API Client 创建 | Pending |
| 8 | 数据映射器创建 | Pending |
| 9 | MyBatis Mapper 创建 | Pending |
| 10 | 同步服务创建 | Pending |
| 11 | REST Controller 创建 | ✅ Completed |
| 12 | 前端 API 客户端 | ✅ Completed |
| 13 | 前端同步组件 + UI 集成 | ✅ Completed |
| 14 | 集成测试 | Pending |
| 15 | 文档更新 | Pending |
| 16 | 最终验证 | Pending |

**Latest Commit:** `a3841cf1` - feat(sales-order): add access_token support and UI integration

---

## Task List Overview (Original)

1. 数据库迁移脚本
2. 实体类创建
3. DTO 类创建
4. API Client 创建
5. 数据映射器创建
6. MyBatis Mapper 创建
7. 同步服务创建
8. REST Controller 创建
9. 前端 API 客户端
10. 前端同步界面

---

## Task 1: 数据库迁移脚本

**Files:**
- Create: `nexusarchive-java/src/main/resources/db/migration/V102__create_sales_order_tables.sql`

**Step 1: Write the migration script**

```sql
-- Input: SQL DDL
-- Output: V102__create_sales_order_tables.sql
-- Pos: Flyway 数据库迁移脚本

-- 销售订单主表
CREATE TABLE IF NOT EXISTS sd_sales_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    
    -- YonSuite 字段
    order_id VARCHAR(64) NOT NULL COMMENT 'YonSuite 订单ID',
    order_code VARCHAR(64) NOT NULL COMMENT '单据编号',
    agent_id VARCHAR(64) COMMENT '客户ID',
    agent_name VARCHAR(255) COMMENT '客户名称',
    vouchdate DATE COMMENT '单据日期',
    order_date DATE COMMENT '订单日期',
    next_status_name VARCHAR(32) COMMENT '订单状态',
    
    -- 金额字段
    total_money DECIMAL(18,2) COMMENT '总金额',
    promotion_money DECIMAL(18,2) COMMENT '总优惠金额',
    rebate_money DECIMAL(18,2) COMMENT '折扣返利金额',
    pay_money DECIMAL(18,2) COMMENT '含税金额',
    real_money DECIMAL(18,2) COMMENT '应收金额',
    
    -- 外键关联
    sales_out_id VARCHAR(64) COMMENT '关联销售出库单ID',
    voucher_id VARCHAR(64) COMMENT '关联记账凭证ID',
    
    -- 业务单号（YonSuite编码）
    yon_order_code VARCHAR(64) COMMENT 'YonSuite 订单编码',
    yon_sales_out_code VARCHAR(64) COMMENT 'YonSuite 出库单编码',
    yon_voucher_code VARCHAR(64) COMMENT 'YonSuite 凭证编码',
    
    -- 多字段匹配用
    agent_id_erp VARCHAR(64) COMMENT '客户ERP编码',
    sales_org_id VARCHAR(64) COMMENT '销售组织ID',
    
    -- 系统字段
    fonds_code VARCHAR(32) COMMENT '全宗代码',
    source_system VARCHAR(32) DEFAULT 'YonSuite' COMMENT '来源系统',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    pubts VARCHAR(32) COMMENT 'YonSuite 时间戳',
    
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_fonds_code (fonds_code),
    KEY idx_agent_id (agent_id),
    KEY idx_vouchdate (vouchdate),
    KEY idx_sales_out_id (sales_out_id),
    KEY idx_voucher_id (voucher_id)
) COMMENT='销售订单主表';

-- 订单明细表
CREATE TABLE IF NOT EXISTS sd_sales_order_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    order_id BIGINT NOT NULL COMMENT '关联主表ID',
    line_no INT COMMENT '行号',
    
    -- 产品信息
    product_id VARCHAR(64) COMMENT '产品ID',
    product_code VARCHAR(64) COMMENT '产品编码',
    product_name VARCHAR(255) COMMENT '产品名称',
    sku_id VARCHAR(64) COMMENT 'SKU ID',
    sku_code VARCHAR(64) COMMENT 'SKU编码',
    sku_name VARCHAR(255) COMMENT 'SKU名称',
    
    -- 数量金额
    qty DECIMAL(18,4) COMMENT '数量',
    ori_unit_price DECIMAL(18,4) COMMENT '原币单价',
    ori_money DECIMAL(18,2) COMMENT '原币金额',
    ori_tax DECIMAL(18,2) COMMENT '原币税额',
    nat_unit_price DECIMAL(18,4) COMMENT '本币单价',
    nat_money DECIMAL(18,2) COMMENT '本币金额',
    nat_tax DECIMAL(18,2) COMMENT '本币税额',
    
    -- 税务
    tax_rate VARCHAR(32) COMMENT '税率',
    tax_id VARCHAR(64) COMMENT '税目ID',
    
    -- 交付信息
    stock_id VARCHAR(64) COMMENT '仓库ID',
    stock_name VARCHAR(255) COMMENT '仓库名称',
    
    -- 系统字段
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    FOREIGN KEY (order_id) REFERENCES sd_sales_order(id) ON DELETE CASCADE,
    KEY idx_order_id (order_id)
) COMMENT='销售订单明细表';
```

**Step 2: Run migration to verify**

Run: `cd nexusarchive-java && mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
cd /Users/user/nexusarchive
git add nexusarchive-java/src/main/resources/db/migration/V102__create_sales_order_tables.sql
git commit -m "feat(sales-order): add database migration for sales order tables"
```

---

## Task 2: 实体类创建 - SalesOrder

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/SalesOrder.java`

**Step 1: Write the entity class**

```java
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
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/SalesOrder.java
git commit -m "feat(sales-order): add SalesOrder entity"
```

---

## Task 3: 实体类创建 - SalesOrderDetail

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/entity/SalesOrderDetail.java`

**Step 1: Write the entity class**

```java
// Input: Lombok、MyBatis-Plus
// Output: SalesOrderDetail 实体类
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
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/entity/SalesOrderDetail.java
git commit -m "feat(sales-order): add SalesOrderDetail entity"
```

---

## Task 4: DTO 类创建 - SalesOrderListRequest

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderListRequest.java`

**Step 1: Write the DTO class**

```java
// Input: Lombok、Jackson
// Output: SalesOrderListRequest DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderListRequest {
    
    @JsonProperty("pageIndex")
    private Integer pageIndex = 1;
    
    @JsonProperty("pageSize")
    private Integer pageSize = 100;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("nextStatusName")
    private String nextStatusName;
    
    @JsonProperty("open_vouchdate_begin")
    private String vouchdateBegin;
    
    @JsonProperty("open_vouchdate_end")
    private String vouchdateEnd;
    
    @JsonProperty("simpleVOs")
    private List<SimpleVO> simpleVOs;
    
    @JsonProperty("queryOrders")
    private List<QueryOrder> queryOrders;
    
    @Data
    public static class SimpleVO {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("op")
        private String op;
        
        @JsonProperty("value1")
        private String value1;
        
        @JsonProperty("logicOp")
        private String logicOp;
    }
    
    @Data
    public static class QueryOrder {
        @JsonProperty("field")
        private String field;
        
        @JsonProperty("order")
        private String order;
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderListRequest.java
git commit -m "feat(sales-order): add SalesOrderListRequest DTO"
```

---

## Task 5: DTO 类创建 - SalesOrderListResponse

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderListResponse.java`

**Step 1: Write the DTO class**

```java
// Input: Lombok、Jackson
// Output: SalesOrderListResponse DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderListResponse {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private SalesOrderData data;
    
    @Data
    public static class SalesOrderData {
        @JsonProperty("pageIndex")
        private Integer pageIndex;
        
        @JsonProperty("pageSize")
        private Integer pageSize;
        
        @JsonProperty("recordCount")
        private Integer recordCount;
        
        @JsonProperty("pageCount")
        private Integer pageCount;
        
        @JsonProperty("recordList")
        private List<SalesOrderRecord> recordList;
    }
    
    @Data
    public static class SalesOrderRecord {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("vouchdate")
        private String vouchdate;
        
        @JsonProperty("agentId")
        private String agentId;
        
        @JsonProperty("agentId_name")
        private String agentName;
        
        @JsonProperty("salesOrgId")
        private String salesOrgId;
        
        @JsonProperty("totalMoney")
        private Double totalMoney;
        
        @JsonProperty("payMoney")
        private Double payMoney;
        
        @JsonProperty("realMoney")
        private Double realMoney;
        
        @JsonProperty("nextStatusName")
        private String nextStatusName;
        
        @JsonProperty("pubts")
        private String pubts;
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderListResponse.java
git commit -m "feat(sales-order): add SalesOrderListResponse DTO"
```

---

## Task 6: DTO 类创建 - SalesOrderDetailResponse

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderDetailResponse.java`

**Step 1: Write the DTO class**

```java
// Input: Lombok、Jackson
// Output: SalesOrderDetailResponse DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderDetailResponse {
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private SalesOrderData data;
    
    @Data
    public static class SalesOrderData {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("code")
        private String code;
        
        @JsonProperty("vouchdate")
        private String vouchdate;
        
        @JsonProperty("orderDate")
        private String orderDate;
        
        @JsonProperty("agentId")
        private String agentId;
        
        @JsonProperty("agentId_name")
        private String agentName;
        
        @JsonProperty("salesOrgId")
        private String salesOrgId;
        
        @JsonProperty("totalMoney")
        private Double totalMoney;
        
        @JsonProperty("promotionMoney")
        private Double promotionMoney;
        
        @JsonProperty("rebateMoney")
        private Double rebateMoney;
        
        @JsonProperty("payMoney")
        private Double payMoney;
        
        @JsonProperty("realMoney")
        private Double realMoney;
        
        @JsonProperty("orderPayMoney")
        private Double orderPayMoney;
        
        @JsonProperty("orderRealMoney")
        private Double orderRealMoney;
        
        @JsonProperty("nextStatusName")
        private String nextStatusName;
        
        @JsonProperty("orderPrices")
        private OrderPrices orderPrices;
        
        @JsonProperty("orderDetails")
        private List<OrderDetail> orderDetails;
        
        @JsonProperty("pubts")
        private String pubts;
    }
    
    @Data
    public static class OrderPrices {
        @JsonProperty("totalMoneyOrigTaxfree")
        private Double totalMoneyOrigTaxfree;
    }
    
    @Data
    public static class OrderDetail {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("productId")
        private String productId;
        
        @JsonProperty("productCode")
        private String productCode;
        
        @JsonProperty("productName")
        private String productName;
        
        @JsonProperty("skuId")
        private String skuId;
        
        @JsonProperty("skuCode")
        private String skuCode;
        
        @JsonProperty("skuName")
        private String skuName;
        
        @JsonProperty("qty")
        private Double qty;
        
        @JsonProperty("oriUnitPrice")
        private Double oriUnitPrice;
        
        @JsonProperty("oriMoney")
        private Double oriMoney;
        
        @JsonProperty("oriTax")
        private Double oriTax;
        
        @JsonProperty("natMoney")
        private Double natMoney;
        
        @JsonProperty("natTax")
        private Double natTax;
        
        @JsonProperty("taxRate")
        private String taxRate;
        
        @JsonProperty("taxId")
        private String taxId;
        
        @JsonProperty("stockId")
        private String stockId;
        
        @JsonProperty("stockName")
        private String stockName;
        
        @JsonProperty("hopeReceiveDate")
        private String hopeReceiveDate;
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/dto/SalesOrderDetailResponse.java
git commit -m "feat(sales-order): add SalesOrderDetailResponse DTO"
```

---

## Task 7: API Client 创建

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/client/YonSuiteSalesOrderClient.java`

**Step 1: Write the client class**

```java
// Input: Hutool、Jackson、Spring
// Output: YonSuiteSalesOrderClient
// Pos: YonSuite 集成 - 客户端

package com.nexusarchive.integration.yonsuite.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class YonSuiteSalesOrderClient {
    
    @Value("${yonsuite.base-url:https://dbox.yonyoucloud.com/iuap-api-gateway}")
    private String baseUrl;
    
    private final YonSuiteHttpExecutor httpExecutor;
    
    /**
     * 查询销售订单列表
     * POST /yonbip/sd/voucherorder/list
     */
    public SalesOrderListResponse querySalesOrders(String accessToken, SalesOrderListRequest request) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/sd/voucherorder/list", accessToken);
        
        log.info("Calling YonSuite querySalesOrders: vouchdateBegin={}, vouchdateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());
        
        String respStr = httpExecutor.postRaw(url, request);
        
        if (respStr == null || respStr.isEmpty()) {
            SalesOrderListResponse empty = new SalesOrderListResponse();
            empty.setCode("200");
            empty.setMessage("No data");
            return empty;
        }
        
        try {
            SalesOrderListResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, SalesOrderListResponse.class);
            
            if (!"200".equals(response.getCode())) {
                log.error("YonSuite querySalesOrders error: {} - {}", response.getCode(), response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to parse querySalesOrders response", e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询销售订单详情
     * GET /yonbip/sd/voucherorder/detail
     */
    public SalesOrderDetailResponse querySalesOrderById(String accessToken, String orderId) {
        String url = httpExecutor.buildUrl(baseUrl, "/yonbip/sd/voucherorder/detail", accessToken);
        
        log.info("Calling YonSuite querySalesOrderById: orderId={}", orderId);
        
        // 添加 id 参数
        url += "&id=" + orderId;
        
        String respStr = httpExecutor.getRaw(url);
        
        if (respStr == null || respStr.isEmpty()) {
            return null;
        }
        
        try {
            SalesOrderDetailResponse response = httpExecutor.getObjectMapper()
                    .readValue(respStr, SalesOrderDetailResponse.class);
            
            if (!"200".equals(response.getCode())) {
                log.error("YonSuite querySalesOrderById error: {} - {}", response.getCode(), response.getMessage());
            }
            
            return response;
        } catch (Exception e) {
            log.error("Failed to parse querySalesOrderById response", e);
            throw new RuntimeException("Failed to parse YonSuite API response: " + e.getMessage(), e);
        }
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/client/YonSuiteSalesOrderClient.java
git commit -m "feat(sales-order): add YonSuiteSalesOrderClient"
```

---

## Task 8: 数据映射器创建

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/mapper/SalesOrderMapper.java`

**Step 1: Write the mapper class**

```java
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

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesOrderMapper {
    
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
     * 详情明细行 → SalesOrderDetail 实体
     */
    public List<SalesOrderDetail> toSalesOrderDetails(String orderId, List<SalesOrderDetailResponse.OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SalesOrderDetail> result = new ArrayList<>();
        int lineNo = 1;
        
        for (SalesOrderDetailResponse.OrderDetail detail : details) {
            SalesOrderDetail entity = SalesOrderDetail.builder()
                    .orderId(Long.parseLong(orderId))
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
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/mapper/SalesOrderMapper.java
git commit -m "feat(sales-order): add SalesOrderMapper"
```

---

## Task 9: MyBatis Mapper 创建

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SalesOrderMapper.java`
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/SalesOrderDetailMapper.java`

**Step 1: Write SalesOrderMapper interface**

```java
// Input: MyBatis-Plus
// Output: SalesOrderMapper
// Pos: MyBatis Mapper

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
    
    /**
     * 根据订单ID查询
     */
    SalesOrder selectByOrderId(@Param("orderId") String orderId);
    
    /**
     * 查询需要关联的订单（用于关联匹配）
     */
    @Select("SELECT * FROM sd_sales_order WHERE agent_id = #{agentId} " +
            "AND vouchdate = #{vouchdate} AND sales_out_id IS NULL LIMIT 100")
    List<SalesOrder> selectForRelation(@Param("agentId") String agentId,
                                      @Param("vouchdate") String vouchdate);
}
```

**Step 2: Write SalesOrderDetailMapper interface**

```java
// Input: MyBatis-Plus
// Output: SalesOrderDetailMapper
// Pos: MyBatis Mapper

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SalesOrderDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesOrderDetailMapper extends BaseMapper<SalesOrderDetail> {
}
```

**Step 3: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/mapper/SalesOrderMapper.java nexusarchive-java/src/main/java/com/nexusarchive/mapper/SalesOrderDetailMapper.java
git commit -m "feat(sales-order): add MyBatis mappers for sales order"
```

---

## Task 10: 同步服务创建

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonSuiteSalesOrderSyncService.java`

**Step 1: Write the sync service**

```java
// Input: Spring、Lombok、MyBatis-Plus
// Output: YonSuiteSalesOrderSyncService
// Pos: 业务服务层

package com.nexusarchive.integration.yonsuite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.SalesOrder;
import com.nexusarchive.entity.SalesOrderDetail;
import com.nexusarchive.integration.yonsuite.client.YonSuiteSalesOrderClient;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import com.nexusarchive.integration.yonsuite.mapper.SalesOrderMapper;
import com.nexusarchive.mapper.SalesOrderDetailMapper;
import com.nexusarchive.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YonSuiteSalesOrderSyncService {
    
    private final YonSuiteSalesOrderClient salesOrderClient;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderDetailMapper salesOrderDetailMapper;
    private final SalesOrderMapper dataMapper;
    private final DataScopeService dataScopeService;
    
    /**
     * 同步销售订单
     */
    public SyncResult syncSalesOrders(String accessToken, SalesOrderListRequest request) {
        log.info("开始同步销售订单: vouchdateBegin={}, vouchdateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());
        
        SyncResult result = new SyncResult();
        int pageIndex = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            request.setPageIndex(pageIndex);
            
            SalesOrderListResponse response = salesOrderClient.querySalesOrders(accessToken, request);
            
            if (response.getData() == null || response.getData().getRecordList() == null) {
                break;
            }
            
            List<SalesOrderListResponse.SalesOrderRecord> records = response.getData().getRecordList();
            
            for (SalesOrderListResponse.SalesOrderRecord record : records) {
                try {
                    processOrderRecord(accessToken, record, result);
                } catch (Exception e) {
                    log.error("处理订单失败: {}", record.getId(), e);
                    result.addError(record.getCode(), e.getMessage());
                }
            }
            
            int totalPages = (int) Math.ceil((double) response.getData().getRecordCount() / request.getPageSize());
            hasMore = pageIndex < totalPages;
            pageIndex++;
        }
        
        log.info("销售订单同步完成: total={}, success={}, failed={}",
                result.total, result.success, result.failed);
        
        return result;
    }
    
    /**
     * 处理单条订单记录
     */
    @Transactional
    private void processOrderRecord(String accessToken, SalesOrderListResponse.SalesOrderRecord record, SyncResult result) {
        result.total++;
        
        // 1. 检查是否已存在
        SalesOrder existing = salesOrderMapper.selectByOrderId(record.getId());
        if (existing != null) {
            // 检查 pubts 是否变化
            if (record.getPubts() != null && record.getPubts().equals(existing.getPubts())) {
                result.skipped++;
                return;
            }
        }
        
        // 2. 获取详情
        SalesOrderDetailResponse detailResponse = salesOrderClient.querySalesOrderById(accessToken, record.getId());
        if (detailResponse == null || detailResponse.getData() == null) {
            result.addError(record.getCode(), "详情API返回空");
            return;
        }
        
        // 3. 映射数据
        SalesOrder order = dataMapper.toSalesOrderFromDetail(detailResponse.getData());
        
        // 设置全宗
        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        order.setFondsCode(scope.fondsCodes().iterator().next());
        
        // 4. 保存或更新
        if (existing != null) {
            order.setId(existing.getId());
            salesOrderMapper.updateById(order);
        } else {
            salesOrderMapper.insert(order);
        }
        
        // 5. 保存明细
        List<SalesOrderDetail> details = dataMapper.toSalesOrderDetails(
                order.getId().toString(), 
                detailResponse.getData().getOrderDetails()
        );
        
        for (SalesOrderDetail detail : details) {
            salesOrderDetailMapper.insert(detail);
        }
        
        result.success++;
    }
    
    /**
     * 同步结果
     */
    public static class SyncResult {
        public int total = 0;
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        public List<ErrorItem> errors = new ArrayList<>();
        
        public void addError(String code, String reason) {
            failed++;
            errors.add(new ErrorItem(code, reason));
        }
        
        public static class ErrorItem {
            public String orderCode;
            public String reason;
            
            public ErrorItem(String orderCode, String reason) {
                this.orderCode = orderCode;
                this.reason = reason;
            }
        }
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/service/YonSuiteSalesOrderSyncService.java
git commit -m "feat(sales-order): add YonSuiteSalesOrderSyncService"
```

---

## Task 11: REST Controller 创建 ✅ COMPLETED

**Status**: 已完成
**Commit**: `a3841cf1` - feat(sales-order): add access_token support and UI integration

**Implementation Notes:**
- Controller 已创建于 `nexusarchive-java/src/main/java/com/nexusarchive/controller/SalesOrderController.java`
- 使用 `YonAuthService.getAccessToken()` 自动获取加密的 access_token
- 支持自动刷新和缓存机制

**Original spec:**
```java
// Input: Spring Web、Lombok
// Output: SalesOrderController
// Pos: 控制器层

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.service.YonAuthService;
import com.nexusarchive.integration.yonsuite.service.YonSuiteSalesOrderSyncService;
import com.nexusarchive.service.DataScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 销售订单同步控制器
 */
@RestController
@RequestMapping("/sales-order")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "销售订单同步")
public class SalesOrderController {

    private final YonSuiteSalesOrderSyncService syncService;
    private final YonAuthService yonAuthService;
    private final DataScopeService dataScopeService;

    /**
     * 同步销售订单
     * POST /sales-order/sync
     */
    @PostMapping("/sync")
    @Operation(summary = "同步销售订单")
    public Result<YonSuiteSalesOrderSyncService.SyncResult> syncSalesOrders(
            @RequestBody SalesOrderListRequest request
    ) {
        // 从 YonAuthService 获取 access_token (自动刷新)
        String accessToken = yonAuthService.getAccessToken();

        log.info("收到销售订单同步请求: dateBegin={}, dateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());

        YonSuiteSalesOrderSyncService.SyncResult result = syncService.syncSalesOrders(accessToken, request);

        return Result.success(result);
    }
}
```

**Step 2: Run test to verify compilation**

Run: `cd nexusarchive-java && mvn compile -DskipTests`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/controller/SalesOrderController.java
git commit -m "feat(sales-order): add SalesOrderController"
```

---

## Task 12: 前端 API 客户端 ✅ COMPLETED

**Status**: 已完成
**Commit**: `a3841cf1` - feat(sales-order): add access_token support and UI integration

**Implementation Notes:**
- API 客户端已创建于 `src/api/sales-order.ts`
- 提供同步、查询、关联链路查询接口

**Original spec:**
```typescript
import { client } from './client';

export interface SalesOrderSyncRequest {
  dateBegin?: string;
  dateEnd?: string;
  statusCodes?: string[];
  agentId?: string;
  salesOrgId?: string;
}

export interface SalesOrderSyncResult {
  total: number;
  success: number;
  failed: number;
  skipped: number;
  relatedSalesOut?: number;
  relatedVoucher?: number;
  errors: Array<{ orderCode: string; reason: string }>;
}

export interface SalesOrder {
  id: number;
  orderCode: string;
  agentName: string;
  vouchdate: string;
  payMoney: number;
  nextStatusName: string;
}

export const salesOrderApi = {
  // 同步销售订单
  sync: (request: SalesOrderSyncRequest) =>
    client.post<SalesOrderSyncResult>('/api/sales-order/sync', request),

  // 查询订单详情
  get: (id: number) =>
    client.get<SalesOrder>(`/api/sales-order/${id}`),

  // 查询关联链路
  getRelations: (id: number) =>
    client.get(`/api/sales-order/${id}/relations`),
};
```

**Step 2: Run test to verify compilation**

Run: `npm run typecheck`
Expected: No errors (or only pre-existing errors)

**Step 3: Commit**

```bash
git add src/api/sales-order.ts
git commit -m "feat(sales-order): add sales order API client"
```

---

## Task 13: 前端同步组件 + UI 集成 ✅ COMPLETED

**Status**: 已完成
**Commit**: `a3841cf1` - feat(sales-order): add access_token support and UI integration

**Implementation Notes:**
- 组件已创建于 `src/components/settings/integration/components/SalesOrderSyncPanel.tsx`
- 已集成到 `IntegrationSettingsPage.tsx` 的"数据同步"选项卡
- 使用 Ant Design Tabs 组件组织 UI
- 包含完整的同步结果展示（总计、成功、失败、跳过统计）

**Original spec:**
```typescript
import { useState } from 'react';
import { Button, DatePicker, Select, Form, message } from 'antd';
import { salesOrderApi } from '@/api/sales-order';

export const SalesOrderSyncPanel: React.FC = () => {
  const [form] = Form.useForm();
  const [syncing, setSyncing] = useState(false);
  const [result, setResult] = useState<null | any>(null);

  const handleSync = async () => {
    const values = await form.validateFields();
    setSyncing(true);
    setResult(null);

    try {
      const response = await salesOrderApi.sync({
        dateBegin: values.dateRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
        dateEnd: values.dateRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
        statusCodes: values.statusCodes,
        agentId: values.agentId,
      });

      if (response.success) {
        message.success(`同步完成！成功 ${response.data.success} 条`);
        setResult(response.data);
      }
    } catch (error) {
      message.error('同步失败');
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div>
      <Form form={form} layout="vertical">
        <Form.Item label="日期范围" name="dateRange">
          <DatePicker.RangePicker showTime />
        </Form.Item>

        <Form.Item label="订单状态" name="statusCodes">
          <Select mode="multiple" placeholder="选择订单状态">
            <Select.Option value="ENDORDER">已完成</Select.Option>
            <Select.Option value="DELIVERY_PART">部分发货</Select.Option>
            <Select.Option value="CONFIRMORDER">开立</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item>
          <Button type="primary" onClick={handleSync} loading={syncing}>
            开始同步
          </Button>
        </Form.Item>
      </Form>

      {result && (
        <div style={{ marginTop: 16 }}>
          <h4>同步结果</h4>
          <p>总计: {result.total}</p>
          <p>成功: {result.success}</p>
          <p>失败: {result.failed}</p>
          <p>跳过: {result.skipped}</p>
        </div>
      )}
    </div>
  );
};
```

**Step 2: Run test to verify compilation**

Run: `npm run typecheck`
Expected: No errors (or only pre-existing errors)

**Step 3: Commit**

```bash
git add src/pages/settings/integration/components/SalesOrderSyncPanel.tsx
git commit -m "feat(sales-order): add SalesOrderSyncPanel component"
```

---

## Task 14: 集成测试

**Files:**
- Create: `nexusarchive-java/src/test/java/com/nexusarchive/integration/yonsuite/SalesOrderSyncTest.java`

**Step 1: Write the test**

```java
package com.nexusarchive.integration.yonsuite;

import com.nexusarchive.entity.SalesOrder;
import com.nexusarchive.integration.yonsuite.mapper.SalesOrderMapper;
import com.nexusarchive.mapper.SalesOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SalesOrderSyncTest {
    
    @Autowired
    private SalesOrderMapper dataMapper;
    
    @Autowired
    private com.nexusarchive.mapper.SalesOrderMapper mybatisMapper;
    
    @Test
    public void testMapperToSalesOrder() {
        // Given
        var record = new com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse.SalesOrderRecord();
        record.setId("test-order-123");
        record.setCode("UO-2024-001");
        record.setAgentId("customer-001");
        record.setAgentName("测试客户");
        record.setVouchdate("2024-01-15");
        record.setTotalMoney(10000.0);
        record.setPayMoney(10000.0);
        record.setRealMoney(10000.0);
        record.setNextStatusName("ENDORDER");
        record.setPubts("20240115120000");
        
        // When
        SalesOrder order = dataMapper.toSalesOrder(record);
        
        // Then
        assertNotNull(order);
        assertEquals("test-order-123", order.getOrderId());
        assertEquals("UO-2024-001", order.getOrderCode());
        assertEquals("测试客户", order.getAgentName());
    }
}
```

**Step 2: Run test**

Run: `cd nexusarchive-java && mvn test -Dtest=SalesOrderSyncTest`
Expected: PASS

**Step 3: Commit**

```bash
git add nexusarchive-java/src/test/java/com/nexusarchive/integration/yonsuite/SalesOrderSyncTest.java
git commit -m "test(sales-order): add integration test for sales order sync"
```

---

## Task 15: 文档更新

**Files:**
- Modify: `docs/ERP接口代码文档清单.md`

**Step 1: Update the documentation**

Add to the "1.2 计划文档" section:
```markdown
| `docs/plans/2026-01-24-sales-order-sync-design.md` | 销售订单同步设计 |
| `docs/plans/2026-01-24-sales-order-sync-implementation.md` | 销售订单同步实施计划 |
```

Add to "2.4 YonSuite 客户端" section:
```markdown
| `integration/yonsuite/client/YonSuiteSalesOrderClient.java` | 销售订单客户端 |
```

Add to "2.7 YonSuite 服务" section:
```markdown
| `integration/yonsuite/service/YonSuiteSalesOrderSyncService.java` | 销售订单同步服务 |
```

**Step 2: Commit**

```bash
git add docs/ERP接口代码文档清单.md
git commit -m "docs(sales-order): update ERP integration documentation"
```

---

## Task 16: 最终验证

**Files:**
- None (verification)

**Step 1: Full build test**

Run: `cd nexusarchive-java && mvn clean install`
Expected: BUILD SUCCESS

Run: `npm run typecheck`
Expected: No new errors

**Step 2: Create final commit**

```bash
git add -A
git commit -m "feat(sales-order): complete sales order sync implementation"
```

---

## Execution Notes

- 所有新创建的文件都放在正确的包路径下
- 遵循现有凭证同步的代码模式
- 每个任务都是独立的，可以单独提交
- 测试优先，确保每个组件都可验证
- 频繁提交，便于回滚和代码审查

## 相关文档

- 设计文档: `docs/plans/2026-01-24-sales-order-sync-design.md`
- 凭证同步参考: `YonSuiteVoucherSyncService.java`
- ERP集成指南: `docs/guides/用友集成.md`
