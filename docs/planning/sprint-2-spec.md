# Sprint 2 技术规格说明书 (Development Spec)

> ⚠️ **状态: DEFERRED (已延期)**
>
> 根据 [专家组审查报告](file:///Users/user/nexusarchive/docs/reports/expert-review-sprint2-scope.md)：
> 实物管理模块超出产品边界 (expert-group-rules.md 2.5 去实体化)。
> 请参阅 `sprint-2-revised-spec.md` 获取电子化核心功能规划。

---

> **版本**: v1.0
> **周期**: week 5-7 (预计 3 周) **[已延期]**
> **目标**: 实物与业务 (Physical & Business) - 补全业务流程
> **对齐**: [PRD v1.0 模块一](file:///Users/user/nexusarchive/docs/product/prd-v1.0.md) + [Roadmap 阶段三](file:///Users/user/nexusarchive/docs/planning/development_roadmap_v1.0.md)

---

## 🎯 Sprint 目标

| 目标 | 验收标准 |
| --- | --- |
| 实物位置管理 | 任一档案盒可精确定位到"格" |
| 状态机流转 | 完整 6 态流转留痕 |
| 装盒与标签 | 盒脊标签 PDF 生成正确 |
| 借阅审批 | 审批链完整，逾期预警 |
| 检索增强 | 核心元数据结构化索引 |

---

## 📦 交付物清单

### Week 1: 实物位置与装盒

| 交付物 | 优先级 | PRD 来源 |
| --- | --- | --- |
| 位置模型 DDL (`arc_location`) | P0 | PRD 1.5 |
| 装盒模型 DDL (`arc_box`, `arc_box_item`) | P0 | PRD 1.2 |
| `LocationService.java` | P0 | PRD 1.5 |
| `BoxService.java` | P0 | PRD 1.2 |
| `BoxLabelGenerator.java` | P1 | PRD 1.2 |

### Week 2: 状态机与借阅

| 交付物 | 优先级 | PRD 来源 |
| --- | --- | --- |
| `ArchiveStatus` 状态枚举 | P0 | PRD 1.5 |
| `ArchiveStateMachine.java` | P0 | PRD 1.5 |
| `BorrowService.java` | P0 | PRD 1.3 |
| `BorrowApprovalFlow.java` | P1 | PRD 1.3 |
| 逾期预警定时任务 | P1 | PRD 1.3 |

### Week 3: 盘点与检索

| 交付物 | 优先级 | PRD 来源 |
| --- | --- | --- |
| `InventoryService.java` | P1 | PRD 1.4 |
| `ArchiveSearchService.java` | P0 | PRD 2.1 |
| 结构化索引迁移 | P0 | PRD 3.2 |
| 脱敏规则引擎 | P1 | PRD 2.1 |

---

## 🏗 技术规格

### 1. 位置模型 (LocationService)

**PRD 来源**: PRD 1.5 - 库房位置管理与生命周期

#### 1.1 位置层级结构

```
Warehouse (库房)
  └── Aisle (排/列)
       └── Rack (密集架)
            └── Shelf (层)
                 └── Slot (格)
```

#### 1.2 数据模型

```sql
-- Flyway: V70__add_arc_location.sql
CREATE TABLE IF NOT EXISTS arc_location (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    warehouse_code VARCHAR(50) NOT NULL,
    warehouse_name VARCHAR(100),
    aisle VARCHAR(20),
    rack VARCHAR(20),
    shelf VARCHAR(20),
    slot VARCHAR(20),
    full_code VARCHAR(100) GENERATED ALWAYS AS 
        (warehouse_code || '-' || COALESCE(aisle,'') || '-' || COALESCE(rack,'') 
         || '-' || COALESCE(shelf,'') || '-' || COALESCE(slot,'')) STORED,
    qr_code VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no)
);
CREATE INDEX IF NOT EXISTS idx_arc_location_full_code 
    ON arc_location(fonds_no, full_code);
```

#### 1.3 接口设计

```java
public interface LocationService {
    /** 创建位置 */
    Location create(LocationCreateRequest request);
    
    /** 按层级查询 */
    List<Location> listByWarehouse(String fondsNo, String warehouseCode);
    
    /** 根据全编码查询 */
    Optional<Location> findByFullCode(String fondsNo, String fullCode);
    
    /** 生成位置二维码 */
    byte[] generateQrCode(String locationId);
}
```

---

### 2. 装盒服务 (BoxService)

**PRD 来源**: PRD 1.2 - 实物装盒与打印盒脊标签

#### 2.1 盒号编码规则

```
{fonds_no}-{archive_year}-{retention}-{serial}
示例: JD-2025-永久-001
```

#### 2.2 数据模型

```sql
-- Flyway: V71__add_arc_box.sql
CREATE TABLE IF NOT EXISTS arc_box (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    box_code VARCHAR(64) NOT NULL,
    retention_period VARCHAR(20) NOT NULL,  -- 10Y, 30Y, PERMANENT
    location_id VARCHAR(32),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, fonds_no, archive_year),
    UNIQUE (fonds_no, box_code)
);

CREATE TABLE IF NOT EXISTS arc_box_item (
    id VARCHAR(32) NOT NULL,
    fonds_no VARCHAR(50) NOT NULL,
    archive_year INT NOT NULL,
    box_id VARCHAR(32) NOT NULL,
    archive_object_id VARCHAR(32) NOT NULL,
    item_order INT,
    boxed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    boxed_by VARCHAR(32),
    PRIMARY KEY (id, fonds_no, archive_year)
);
```

#### 2.3 接口设计

```java
public interface BoxService {
    /** 创建新盒 */
    Box createBox(BoxCreateRequest request);
    
    /** 装盒 (批量) */
    void addItems(String boxId, List<String> archiveObjectIds);
    
    /** 关闭盒子 (不可再装) */
    void closeBox(String boxId);
    
    /** 绑定位置 */
    void bindLocation(String boxId, String locationId);
}
```

---

### 3. 盒脊标签生成 (BoxLabelGenerator)

**PRD 来源**: PRD 1.2 - 打印盒脊标签 PDF

#### 3.1 标签内容

| 元素 | 说明 |
| --- | --- |
| 盒号 | `{fonds_no}-{year}-{retention}-{serial}` |
| 全宗号 | 展示用 |
| 会计年度 | `archive_year` |
| 保管期限 | 10年/30年/永久 |
| 起止日期 | `start_date ~ end_date` |
| 二维码 | 编码盒号 |

#### 3.2 接口设计

```java
public interface BoxLabelGenerator {
    /** 生成单个标签 PDF */
    byte[] generateLabel(Box box);
    
    /** 批量生成标签 (多盒一页) */
    byte[] generateBatchLabels(List<Box> boxes, LabelLayout layout);
}

public enum LabelLayout {
    SINGLE_PER_PAGE,      // 1个/页
    TWO_PER_PAGE,         // 2个/页
    STICKER_3x10          // 3x10 不干胶
}
```

#### 3.3 实现要点

使用 PDFBox 3.x 绘制:
1. 固定页面大小 (A4 或自定义)
2. 坐标计算需精确到毫米
3. 中文字体嵌入 (SimSun)
4. 二维码使用 ZXing

---

### 4. 档案状态机 (ArchiveStateMachine)

**PRD 来源**: PRD 1.5 - 实物状态流转

#### 4.1 状态定义

```java
public enum ArchiveStatus {
    IN_STOCK,           // 在库
    BORROWED,           // 已借出
    PENDING_RETURN,     // 待归还
    RETURNED,           // 已归还
    TO_DESTROY,         // 待销毁
    DESTROYED           // 已销毁
}
```

#### 4.2 状态转换矩阵

| 当前状态 | 允许转换 | 触发动作 |
| --- | --- | --- |
| IN_STOCK | BORROWED, TO_DESTROY | 借阅审批通过 / 销毁申请 |
| BORROWED | PENDING_RETURN | 发起归还 |
| PENDING_RETURN | RETURNED | 确认归还 |
| RETURNED | IN_STOCK, TO_DESTROY | 入库 / 销毁申请 |
| TO_DESTROY | DESTROYED | 销毁执行 |
| DESTROYED | - (终态) | - |

#### 4.3 接口设计

```java
public interface ArchiveStateMachine {
    /** 状态转换 */
    void transition(String archiveId, ArchiveStatus targetStatus, String reason);
    
    /** 检查是否可转换 */
    boolean canTransition(ArchiveStatus from, ArchiveStatus to);
    
    /** 获取可用操作 */
    Set<ArchiveStatus> getAvailableTransitions(ArchiveStatus current);
}
```

---

### 5. 借阅服务 (BorrowService)

**PRD 来源**: PRD 1.3 - 实物借阅审批

#### 5.1 借阅流程

```
申请 -> 审批(双人复核) -> 出库登记 -> 使用中 -> 发起归还 -> 确认归还
```

#### 5.2 数据模型变更

```sql
-- Flyway: V72__enhance_borrow_record.sql
ALTER TABLE borrow_record ADD COLUMN IF NOT EXISTS approver_id VARCHAR(32);
ALTER TABLE borrow_record ADD COLUMN IF NOT EXISTS approval_time TIMESTAMP;
ALTER TABLE borrow_record ADD COLUMN IF NOT EXISTS pickup_time TIMESTAMP;
ALTER TABLE borrow_record ADD COLUMN IF NOT EXISTS pickup_by VARCHAR(32);
ALTER TABLE borrow_record ADD COLUMN IF NOT EXISTS overdue_notified BOOLEAN DEFAULT FALSE;
```

#### 5.3 接口设计

```java
public interface BorrowService {
    /** 申请借阅 */
    BorrowRecord apply(BorrowApplyRequest request);
    
    /** 审批 */
    void approve(String recordId, boolean approved, String comment);
    
    /** 出库登记 */
    void pickup(String recordId);
    
    /** 发起归还 */
    void initiateReturn(String recordId);
    
    /** 确认归还 */
    void confirmReturn(String recordId);
    
    /** 查询逾期记录 */
    List<BorrowRecord> findOverdue(String fondsNo);
}
```

#### 5.4 逾期预警定时任务

```java
@Scheduled(cron = "0 0 9 * * ?")  // 每天 9:00
public void checkOverdue() {
    List<BorrowRecord> overdue = borrowService.findOverdue(null);
    for (BorrowRecord record : overdue) {
        if (!record.isOverdueNotified()) {
            notificationService.sendOverdueAlert(record);
            borrowService.markNotified(record.getId());
        }
    }
}
```

---

### 6. 检索增强 (ArchiveSearchService)

**PRD 来源**: PRD 2.1 - 高级检索与脱敏

#### 6.1 结构化索引字段

| 字段 | 类型 | 索引类型 |
| --- | --- | --- |
| `amount` | DECIMAL(18,2) | BTree (范围查询) |
| `doc_date` | DATE | BTree |
| `counterparty` | VARCHAR(100) | BTree + 模糊 |
| `voucher_no` | VARCHAR(50) | BTree |
| `invoice_no` | VARCHAR(50) | BTree |

#### 6.2 脱敏规则

```java
public interface DataMaskingService {
    /** 脱敏处理 */
    String mask(String fieldName, String value, UserRole role);
}

// 规则示例
// bank_account: 中间 8 位替换为 ********
// id_card: 保留前3后4位
```

---

## 📅 里程碑

### Week 1 (Day 1-5)
- [ ] Day 1: 位置模型 DDL + `LocationService` 
- [ ] Day 2: 装盒模型 DDL + `BoxService`
- [ ] Day 3: `BoxLabelGenerator` + 标签 PDF 调试
- [ ] Day 4: 位置二维码 + 盒二维码生成
- [ ] Day 5: 集成测试 + API 文档

### Week 2 (Day 6-10)
- [ ] Day 6: `ArchiveStatus` 枚举 + 状态机
- [ ] Day 7: `BorrowService` 基础流程
- [ ] Day 8: 审批流 + 双人复核
- [ ] Day 9: 逾期预警定时任务
- [ ] Day 10: 借阅 API 集成测试

### Week 3 (Day 11-15)
- [ ] Day 11: 结构化索引迁移 (amount/doc_date/counterparty)
- [ ] Day 12: `ArchiveSearchService` 高级检索
- [ ] Day 13: `DataMaskingService` 脱敏规则
- [ ] Day 14: `InventoryService` 盘点 (基础)
- [ ] Day 15: 文档整理 + Sprint 2 验收

---

## ⚠️ 风险与缓解

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 标签坐标精度 | 打印偏移 | 提前测试多种打印机 |
| 状态机并发 | 脏数据 | 乐观锁 + 审计日志 |
| 脱敏规则复杂 | 维护困难 | 规则外置配置化 |
| 索引影响性能 | 写入变慢 | 分批迁移 + 监控 |

---

## 📚 参考资料

- [ZXing 二维码](https://github.com/zxing/zxing)
- [PDFBox 文本绘制](https://pdfbox.apache.org/)
- [Spring StateMachine](https://spring.io/projects/spring-statemachine) (可选)
