# 档案装盒功能设计 (非必要功能)

> **重要说明**: 此功能对纯电子会计档案系统**非必要**。
>
> 根据 DA/T 94-2022《电子会计档案管理规范》，电子档案不需要物理装盒，
> 其"保管"概念是存储介质管理、备份策略、迁移等，而非物理盒装。
>
> 仅当系统需要管理**纸质档案数字化后的原件存放位置**时才需实现此功能。

## 1. 业务背景

### 装盒 vs 组卷区别

| 维度 | 组卷 (Volume) | 装盒 (Box) |
|------|---------------|------------|
| 性质 | 逻辑组织 | 物理存储 |
| 依据 | 业务期间/类型 | 盒容量/物理位置 |
| 输出 | 案卷号 | 盒号 + 位置编码 |
| 适用 | 电子/纸质档案 | 仅纸质档案 |

### 现有架构支持

- `Location` 实体已支持 `type=BOX` 作为库房位置层级
- `Volume` (案卷) 处理逻辑组卷
- `WarehouseService` 管理库房/货架

## 2. 数据模型设计

```sql
-- 档案盒表
CREATE TABLE arc_archive_box (
    id              VARCHAR(32) PRIMARY KEY,
    box_code        VARCHAR(50) NOT NULL UNIQUE,  -- 盒号: BOX-2025-0001
    box_label       VARCHAR(100),                  -- 盒脊标签
    fonds_no        VARCHAR(20),                   -- 全宗号
    fiscal_year     VARCHAR(10),                   -- 年度
    category_code   VARCHAR(20),                   -- 分类号
    retention_period VARCHAR(20),                  -- 保管期限 (取盒内最长)
    capacity        INT DEFAULT 100,               -- 额定容量 (件)
    used_count      INT DEFAULT 0,                 -- 已装数量
    location_id     VARCHAR(32),                   -- 存放位置 (bas_location)
    status          VARCHAR(20) DEFAULT 'OPEN',    -- OPEN/SEALED/TRANSFERRED
    sealed_at       TIMESTAMP,                     -- 封盒时间
    sealed_by       VARCHAR(32),                   -- 封盒人
    rfid_tag        VARCHAR(50),                   -- RFID 标签
    created_time    TIMESTAMP DEFAULT NOW(),
    last_modified_time TIMESTAMP DEFAULT NOW(),
    deleted         INT DEFAULT 0
);

-- 盒内档案关联表
CREATE TABLE arc_box_item (
    id              BIGSERIAL PRIMARY KEY,
    box_id          VARCHAR(32) NOT NULL,
    archive_id      VARCHAR(32) NOT NULL,          -- 档案/案卷 ID
    item_type       VARCHAR(20) DEFAULT 'ARCHIVE', -- ARCHIVE/VOLUME
    sequence_no     INT,                           -- 盒内顺序号
    packed_at       TIMESTAMP DEFAULT NOW(),
    packed_by       VARCHAR(32),
    UNIQUE (box_id, archive_id)
);

CREATE INDEX idx_box_item_box ON arc_box_item(box_id);
CREATE INDEX idx_box_item_archive ON arc_box_item(archive_id);
```

## 3. API 设计

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/boxes` | 创建档案盒 |
| GET | `/api/boxes` | 分页查询档案盒 |
| GET | `/api/boxes/{id}` | 获取盒详情 |
| POST | `/api/boxes/{id}/items` | 添加档案到盒 |
| DELETE | `/api/boxes/{id}/items/{archiveId}` | 从盒移除档案 |
| GET | `/api/boxes/{id}/items` | 获取盒内档案列表 |
| POST | `/api/boxes/{id}/seal` | 封盒 |
| POST | `/api/boxes/{id}/transfer` | 移交 |
| POST | `/api/boxes/auto-pack` | 智能装盒 |
| GET | `/api/boxes/{id}/label` | 生成盒脊标签 PDF |

## 4. 核心业务逻辑

```java
// 智能装盒算法
public List<ArchiveBox> autoPack(String fondsNo, String fiscalYear, String categoryCode) {
    // 1. 查询待装盒档案 (已归档但未装盒)
    // 2. 按保管期限分组 (同期限装同盒)
    // 3. 按容量拆分创建盒
    // 4. 生成盒号: BOX-{全宗}-{年度}-{序号}
    // 5. 返回装盒结果
}

// 封盒
public void sealBox(String boxId, String operatorId) {
    // 1. 验证盒内档案数 > 0
    // 2. 更新状态为 SEALED
    // 3. 记录封盒时间和操作人
    // 4. 生成并保存盒脊标签
}
```

## 5. 实现优先级

| 优先级 | 功能 | 工作量 |
|--------|------|--------|
| P0 | 创建盒 + 手动添加档案 | 4h |
| P0 | 盒列表 + 详情查询 | 2h |
| P1 | 智能装盒算法 | 4h |
| P1 | 封盒 + 标签生成 | 3h |
| P2 | 移交 + RFID 集成 | 4h |

## 6. 相关标准

- DA/T 22-2015《归档文件整理规则》
- GB/T 18894-2016《电子文件归档与电子档案管理规范》

## 7. 替代方案 (电子档案)

对于纯电子档案系统，以下功能已覆盖"保管"需求：

- **组卷** (`VolumeService`) - 逻辑组织
- **库房管理** (`WarehouseService`) - 存储位置管理
- **AIP 包导出** - 长期保存格式封装

---

*文档创建时间: 2025-12-28*
*状态: 规划中 (非必要功能)*
