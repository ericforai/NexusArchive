# YonSuite 销售订单同步设计文档

> **创建时间**: 2026-01-24
> **目的**: 实现销售订单同步，建立"销售订单 → 销售出库单 → 记账凭证"的全链路关联
> **核心需求**: 业务关联和穿透联查

---

## 目录

- [一、需求概述](#一需求概述)
- [二、数据库设计](#二数据库设计)
- [三、后端架构设计](#三后端架构设计)
- [四、同步流程设计](#四同步流程设计)
- [五、前端界面设计](#五前端界面设计)
- [六、错误处理](#六错误处理)
- [七、API 文档](#七api-文档)
- [八、测试计划](#八测试计划)

---

## 一、需求概述

### 1.1 目标

实现 YonSuite 销售订单数据同步，建立销售订单、销售出库单、记账凭证之间的业务关联关系，支持穿透联查。

### 1.2 核心要点

| 要点 | 说明 |
|------|------|
| **数据来源** | YonSuite 销售订单 API |
| **同步方式** | 手动触发，两步 API（列表 → 详情） |
| **筛选条件** | 日期范围 + 订单状态 + 客户（组合筛选） |
| **存储方式** | 独立的 `sd_sales_order` 表存储 |
| **关联方式** | 外键 + 业务单号（YonSuite 编码）双保险 |
| **关联链路** | 销售订单 → 销售出库单 → 记账凭证 |

### 1.3 YonSuite API

| 接口 | 路径 | 方法 | 说明 |
|------|------|------|------|
| 列表查询 | `/yonbip/sd/voucherorder/list` | POST | 分页、筛选 |
| 详情查询 | `/yonbip/sd/voucherorder/detail` | GET | 按 id 查询 |

---

## 二、数据库设计

### 2.1 销售订单主表

```sql
CREATE TABLE sd_sales_order (
    -- 主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    -- YonSuite 字段
    order_id VARCHAR(64) NOT NULL COMMENT 'YonSuite 订单ID',
    order_code VARCHAR(64) NOT NULL COMMENT '单据编号',
    agent_id VARCHAR(64) COMMENT '客户ID',
    agent_name VARCHAR(255) COMMENT '客户名称',
    vouchdate DATE COMMENT '单据日期',
    order_date DATE COMMENT '订单日期',
    next_status_name VARCHAR(32) COMMENT '订单状态: CONFIRMORDER/DELIVERY_PART/DELIVERGOODS/TAKEDELIVERY/ENDORDER/OPPOSE/APPROVING',

    -- 金额字段
    total_money DECIMAL(18,2) COMMENT '总金额',
    promotion_money DECIMAL(18,2) COMMENT '总优惠金额',
    rebate_money DECIMAL(18,2) COMMENT '折扣返利金额',
    pay_money DECIMAL(18,2) COMMENT '含税金额',
    order_pay_money DECIMAL(18,2) COMMENT '商品实付金额',
    real_money DECIMAL(18,2) COMMENT '应收金额',
    order_real_money DECIMAL(18,2) COMMENT '商品应付金额',

    -- 外键关联
    sales_out_id VARCHAR(64) COMMENT '关联销售出库单ID (本地)',
    voucher_id VARCHAR(64) COMMENT '关联记账凭证ID (本地)',

    -- 业务单号（YonSuite编码，软关联）
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

    -- 索引
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_fonds_code (fonds_code),
    KEY idx_agent_id (agent_id),
    KEY idx_vouchdate (vouchdate),
    KEY idx_sales_out_id (sales_out_id),
    KEY idx_voucher_id (voucher_id)
) COMMENT='销售订单主表';
```

### 2.2 订单明细表

```sql
CREATE TABLE sd_sales_order_detail (
    -- 主键
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
    sub_qty DECIMAL(18,4) COMMENT '辅助数量',
    price_qty DECIMAL(18,4) COMMENT '计价数量',
    ori_unit_price DECIMAL(18,4) COMMENT '原币单价',
    ori_money DECIMAL(18,2) COMMENT '原币金额',
    ori_tax DECIMAL(18,2) COMMENT '原币税额',
    nat_unit_price DECIMAL(18,4) COMMENT '本币单价',
    nat_money DECIMAL(18,2) COMMENT '本币金额',
    nat_tax DECIMAL(18,2) COMMENT '本币税额',

    -- 税务
    tax_rate VARCHAR(32) COMMENT '税率',
    tax_id VARCHAR(64) COMMENT '税目ID',
    tax_items VARCHAR(64) COMMENT '税务项目',

    -- 交付信息
    stock_id VARCHAR(64) COMMENT '仓库ID',
    stock_name VARCHAR(255) COMMENT '仓库名称',
    hope_receive_date DATE COMMENT '希望到货日期',

    -- 系统字段
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键
    FOREIGN KEY (order_id) REFERENCES sd_sales_order(id) ON DELETE CASCADE,
    KEY idx_order_id (order_id)
) COMMENT='销售订单明细表';
```

### 2.3 数据库迁移脚本

文件路径: `src/main/resources/db/migration/V102__create_sales_order_tables.sql`

---

## 三、后端架构设计

### 3.1 组件结构

```
nexusarchive-java/src/main/java/com/nexusarchive/
├── integration/yonsuite/
│   ├── dto/
│   │   ├── SalesOrderListRequest.java       -- 列表查询请求
│   │   ├── SalesOrderListResponse.java      -- 列表查询响应
│   │   └── SalesOrderDetailResponse.java    -- 详情查询响应
│   ├── client/
│   │   └── YonSuiteSalesOrderClient.java    -- 销售订单API客户端
│   ├── mapper/
│   │   └── SalesOrderMapper.java             -- 数据映射器
│   └── service/
│       └── YonSuiteSalesOrderSyncService.java -- 同步服务
├── entity/
│   ├── SalesOrder.java                      -- 订单实体
│   └── SalesOrderDetail.java                -- 订单明细实体
├── mapper/
│   ├── SalesOrderMapper.java                -- MyBatis Mapper
│   └── SalesOrderDetailMapper.java          -- MyBatis Mapper
└── controller/
    └── SalesOrderController.java            -- REST API
```

### 3.2 核心类职责

**YonSuiteSalesOrderClient**
- `querySalesOrders(accessToken, request)` - 列表查询（分页）
- `querySalesOrderById(accessToken, orderId)` - 详情查询

**SalesOrderMapper**
- `toSalesOrder(record)` - 列表记录 → 实体
- `toSalesOrderFromDetail(detail)` - 详情响应 → 实体
- `toSalesOrderDetail(detailLine)` - 明细行 → 明细实体

**YonSuiteSalesOrderSyncService**
- `syncSalesOrders(accessToken, request)` - 主同步入口
- `processOrderRecord(record)` - 处理单条记录
- `fetchOrderDetails(orderId)` - 获取订单详情
- `buildRelations(order)` - 建立单据关联
- `checkIdempotency(orderId)` - 幂等性检查

---

## 四、同步流程设计

### 4.1 主流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     用户触发同步                                  │
│              (日期范围 + 状态 + 客户筛选)                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  1. 调用列表 API                                                  │
│     POST /yonbip/sd/voucherorder/list                            │
│     分页遍历，获取订单列表                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  2. 遍历每条订单记录                                               │
│     - 幂等性检查（order_id + pubts）                              │
│     - 已存在且未变更则跳过                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  3. 调用详情 API                                                  │
│     GET /yonbip/sd/voucherorder/detail?id=xxx                    │
│     获取完整订单信息（含 orderDetails）                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  4. 数据映射与存储                                                  │
│     - 写入 sd_sales_order 主表                                    │
│     - 写入 sd_sales_order_detail 明细表                            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  5. 建立单据关联                                                    │
│     - 通过 agentId + vouchdate + 产品匹配销售出库单                  │
│     - 通过出库单关联记账凭证                                         │
│     - 更新外键字段和业务单号                                         │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  6. 返回同步结果                                                    │
│     - 同步成功数量                                                 │
│     - 关联成功的出库单/凭证数量                                      │
│     - 失败记录及原因                                               │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 关联匹配逻辑

```java
// 关联匹配伪代码
void buildRelations(SalesOrder order) {
    // 1. 匹配销售出库单
    // 条件：agentId + vouchdate 相同，且出库单包含相同产品
    List<SalesOut> salesOuts = salesOutMapper.findByOrderInfo(
        order.getAgentId(),
        order.getAgentIdErp(),
        order.getVouchdate(),
        order.getProductIds()  // 从明细中提取产品ID列表
    );

    if (salesOuts.isEmpty()) {
        log.info("未找到关联的销售出库单: orderCode={}", order.getOrderCode());
        return;
    }

    // 2. 选择最匹配的出库单（产品匹配度最高的）
    SalesOut matchedOut = findBestMatch(salesOuts, order);

    // 3. 通过出库单关联凭证
    Voucher voucher = voucherMapper.findBySalesOutCode(matchedOut.getYonCode());

    if (voucher != null) {
        // 更新外键和业务单号
        order.setSalesOutId(matchedOut.getId());
        order.setVoucherId(voucher.getId());
        order.setYonSalesOutCode(matchedOut.getYonCode());
        order.setYonVoucherCode(voucher.getYonCode());

        salesOrderMapper.updateById(order);
        log.info("关联成功: 订单→出库单→凭证");
    }
}
```

---

## 五、前端界面设计

### 5.1 同步操作界面

在 **集成设置中心** 添加销售订单同步功能：

```
┌─────────────────────────────────────────────────────────────────┐
│  集成设置 > YonSuite > 销售订单同步                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  筛选条件                                                          │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  日期范围: [2024-01-01] 至 [2024-12-31]                    │ │
│  │  订单状态: [全部 ▼]                                         │ │
│  │           □ 开立  □ 待发货  □ 部分发货  □ 已完成             │ │
│  │  客户:     [请选择客户 ▼]                                    │ │
│  │  组织:     [销售组织 ▼]                                      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐                               │
│  │  开始同步    │  │  同步历史    │                               │
│  └─────────────┘  └─────────────┘                               │
│                                                                  │
│  同步进度: [████████░░] 80% (80/100)                            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 订单关联展示

在凭证/出库单详情页展示业务链路：

```
┌─────────────────────────────────────────────────────────────────┐
│  业务单据链路                                                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  销售订单 → 销售出库单 → 记账凭证                                   │
│                                                                  │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │
│  │ UO-2024-001  │───▶│ SO-2024-003  │───▶│ 记-01-0123   │      │
│  │ 2024-01-15   │    │ 2024-01-18   │    │ 2024-01-20   │      │
│  │ ¥50,000.00   │    │ ¥50,000.00   │    │ ¥50,000.00   │      │
│  │ [查看详情]    │    │ [查看详情]    │    │ [当前]       │      │
│  └──────────────┘    └──────────────┘    └──────────────┘      │
│                                                                  │
│  客户：XX科技有限公司  销售员：张三                                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 前端组件结构

```
src/
├── pages/settings/integration/
│   └── components/
│       └── SalesOrderSyncPanel.tsx       -- 销售订单同步面板
├── features/sales-order/
│   ├── hooks/
│   │   └── useSalesOrderSync.ts          -- 同步 Hook
│   └── api/
│       └── salesOrder.ts                 -- API 客户端
├── components/voucher/
│   └── BusinessChainView.tsx             -- 业务链路视图（复用）
└── api/
    └── sales-order.ts                    -- API 定义
```

---

## 六、错误处理

### 6.1 API 调用失败处理

| 场景 | 处理方式 |
|------|----------|
| 列表 API 失败 | 记录日志，返回错误信息，不中断流程 |
| 详情 API 失败 | 跳过该订单，记录到失败列表，继续处理 |
| API 超时 | 设置 30 秒超时，超时后重试 1 次 |
| 认证失败 | 返回 401，提示用户重新授权 |

### 6.2 数据验证处理

| 场景 | 处理方式 |
|------|----------|
| 必填字段缺失 | 记录警告，跳过该订单 |
| 金额格式异常 | 使用默认值 0，记录警告 |
| 日期解析失败 | 使用当前日期，记录警告 |
| 编码异常 | 记录错误，跳过该订单 |

### 6.3 关联失败处理

| 场景 | 处理方式 |
|------|----------|
| 未找到出库单 | 仅保存订单，关联字段为空，记录信息 |
| 找到多个出库单 | 选择第一个，记录警告 |
| 凭证关联失败 | 不影响订单和出库单的保存 |

### 6.4 错误响应格式

```json
{
  "code": "200",
  "message": "同步完成",
  "data": {
    "total": 100,
    "success": 95,
    "failed": 5,
    "skipped": 10,
    "relatedSalesOut": 80,
    "relatedVoucher": 75,
    "errors": [
      {
        "orderCode": "UO-2024-001",
        "reason": "详情API超时"
      },
      {
        "orderCode": "UO-2024-002",
        "reason": "未找到关联的销售出库单"
      }
    ]
  }
}
```

---

## 七、API 文档

### 7.1 同步销售订单

**请求**
```
POST /api/sales-order/sync
Content-Type: application/json

{
  "dateBegin": "2024-01-01 00:00:00",
  "dateEnd": "2024-12-31 23:59:59",
  "statusCodes": ["ENDORDER", "DELIVERY_PART"],
  "agentId": "customer123",
  "salesOrgId": "org001"
}
```

**响应**
```json
{
  "code": "200",
  "message": "同步完成",
  "data": {
    "total": 100,
    "success": 95,
    "failed": 5,
    "skipped": 10,
    "relatedSalesOut": 80,
    "relatedVoucher": 75,
    "errors": []
  }
}
```

### 7.2 查询订单详情

**请求**
```
GET /api/sales-order/{id}
```

**响应**
```json
{
  "code": "200",
  "data": {
    "id": "123",
    "orderCode": "UO-2024-001",
    "agentName": "XX科技有限公司",
    "vouchdate": "2024-01-15",
    "payMoney": 50000.00,
    "salesOut": {
      "id": "456",
      "yonCode": "SO-2024-003"
    },
    "voucher": {
      "id": "789",
      "yonCode": "记-01-0123"
    },
    "details": [...]
  }
}
```

### 7.3 查询关联链路

**请求**
```
GET /api/sales-order/{id}/relations
```

**响应**
```json
{
  "code": "200",
  "data": {
    "salesOrder": {
      "id": "123",
      "code": "UO-2024-001",
      "date": "2024-01-15",
      "amount": 50000.00
    },
    "salesOut": {
      "id": "456",
      "code": "SO-2024-003",
      "date": "2024-01-18",
      "amount": 50000.00
    },
    "voucher": {
      "id": "789",
      "code": "记-01-0123",
      "date": "2024-01-20",
      "amount": 50000.00
    }
  }
}
```

---

## 八、测试计划

### 8.1 单元测试

| 测试类 | 覆盖内容 |
|--------|----------|
| `SalesOrderMapperTest` | 数据映射逻辑（列表/详情 → 实体） |
| `RelationBuilderTest` | 关联匹配逻辑（订单→出库单→凭证） |
| `YonSuiteSalesOrderClientTest` | API 客户端调用 |
| `SalesOrderSyncServiceTest` | 同步流程核心逻辑 |

### 8.2 集成测试

| 测试类 | 覆盖内容 |
|--------|----------|
| `SalesOrderSyncIntegrationTest` | 完整同步流程（Mock YonSuite API） |
| `SalesOrderControllerTest` | REST API 端到端测试 |

### 8.3 E2E 测试

| 测试场景 | 验证点 |
|----------|--------|
| 手动触发同步 | 数据正确写入数据库 |
| 幂等性测试 | 重复同步不产生重复数据 |
| 关联展示 | 凭证详情页正确展示业务链路 |
| 错误处理 | API 失败时正确记录错误 |

---

## 附录

### A. 订单状态对照表

| 状态码 | 状态名称 | 说明 |
|--------|----------|------|
| CONFIRMORDER | 开立 | 订单已确认，待发货 |
| DELIVERY_PART | 部分发货 | 部分商品已发货 |
| DELIVERY_TAKE_PART | 部分发货待收货 | 部分发货，待客户签收 |
| DELIVERGOODS | 待发货 | 等待发货 |
| TAKEDELIVERY | 待收货 | 等待客户签收 |
| ENDORDER | 已完成 | 订单完成 |
| OPPOSE | 已取消 | 订单已取消 |
| APPROVING | 审批中 | 订单审批流程中 |

### B. 参考文档

- [YonSuite 销售订单 API 文档](https://nexusarchive/docs/api/yonsuite/sales-order)
- [凭证同步服务](../integration/yonsuite/service/YonSuiteVoucherSyncService.java)
- [销售出库单映射](../integration/yonsuite/mapper/SalesOutMapper.java)
