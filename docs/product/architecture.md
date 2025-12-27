# 电子会计档案管理系统 - 产品架构设计

> 版本: 1.0.0
> 更新日期: 2025-12-26
> 状态: 已评审通过

---

## 一、核心定位

**档案系统必须成为"最终保管库"，ERP 只是来源。**

ERP 产出的是业务/财务数据与附件；会计档案系统要做的是"归档、保管、检索、移交、销毁"这一套闭环。

---

## 二、三类核心对象

### A. 会计类对象（来自 ERP）

| 对象 | 说明 | 存储形态 |
|------|------|----------|
| 记账凭证 (Voucher) | 表头 + 分录 | 结构化 + 版式文件 |
| 会计账簿 (Ledger) | 总账/明细账/日记账，按期间生成快照 | 结构化 + 版式文件 |
| 财务报表 (Report) | 资产负债表/利润表/现金流量表，按期间生成 | 结构化 + 版式文件 |
| 其他会计资料 | 制度、说明、结账报告、调整表等 | 版式文件 |

**存储形态说明：**
- 结构化：便于穿透检索、审计追溯、规则校验
- 版式：便于长期阅读与监管检查（PDF/OFD/PDF-A）

### B. 原始单据对象（来自上传/多系统同步）

| 单据类型 | 来源 | 格式 |
|---------|------|------|
| 发票 | 税务系统/ERP | OFD/PDF/XML |
| 合同 | OA/上传 | PDF/图片/扫描件 |
| 送货单/入库单/出库单 | ERP/WMS | PDF/图片 |
| 付款单/收款单 | ERP/OA | PDF |
| 银行回单/对账单 | 银行/银企直连 | PDF/图片 |
| 其他附件 | 邮件/截图/签收单等 | 各类格式 |

**存储形态：**
- 文件：原件/多页/多版本（可含 OCR 结果、结构化抽取结果）
- 元数据：号码、金额、日期、对方、税号、项目、所属业务单据号等

### C. 归档对象（档案系统生成）

| 对象 | 说明 |
|------|------|
| 档案 (Archive) | 可移交、可借阅、可销毁的"档案包" |
| 档案项 (ArchiveItem) | 档案内的条目（凭证、账簿、报表、附件…） |
| 归档批次 (ArchiveBatch) | 按期间批量归档的控制单元 |
| 四性检测记录 | 哈希、签名、时间戳、操作链路等 |

---

## 三、两级库设计

### 3.1 预归档库（Staging / Working Set）

**目的：** 采集、清洗、识别、匹配、补全、异常治理
**特点：** 可修改、可撤回、可重复匹配、可补传

| 功能模块 | 说明 |
|---------|------|
| 记账凭证池 | ERP 同步来的凭证（结构化）+ 渲染件（HTML/PDF） |
| 单据池 | 上传/同步来的原始单据文件 |
| OCR/识别 | 对图片/PDF 做 OCR 与字段抽取（异步队列） |
| 智能关联 | 规则引擎/人工辅助，建立 voucher ↔ docs 关系 |
| 异常治理 | 缺附件、金额不一致、期间不匹配、重复单据、无法识别等 |

> 预归档库里，数据是"可治理的工作数据"，不是最终档案。

### 3.2 归档库（Official Archive / Immutable Repository）

**目的：** 固化、保管、检索、借阅、移交、销毁
**特点：** 不可修改（或仅允许追加更正记录）、全程留痕、满足合规

| 功能模块 | 说明 |
|---------|------|
| 档案检索与穿透 | 按期间/凭证号/科目/发票号/合同号/对方等 |
| 借阅与授权 | 流程 + 水印 + 下载控制 + 审计 |
| 移交 | 对接上级档案系统/监管要求的导出包 |
| 到期鉴定与销毁 | 保管期限策略 + 审批 + 销毁证明 |
| 四性检测 | 归档时固化，定期抽检 |

---

## 四、底层存储架构

### 4.1 元数据与关系：PostgreSQL

存储内容：
- 凭证/分录/账簿/报表的结构化数据
- 单据元数据（发票号、金额、日期、对方等）
- 关联关系（谁和谁关联、关联理由、规则命中、人工确认）
- 归档包结构、权限、审计日志、四性结果

### 4.2 文件本体：对象存储（MinIO / 本地 S3 / NAS 抽象层）

存储内容：
- 原始单据文件（OFD/PDF/JPG/PNG…）
- 凭证/账簿/报表的版式文件（PDF/OFD/PDF-A）
- OCR 结果文件（可选）
- 归档导出包（可选）

**关键策略：**

| 策略 | 说明 |
|------|------|
| 内容寻址/去重 | `file_hash = SM3(file_bytes)`，避免重复上传与便于一致性校验 |
| WORM/不可变 | 归档后写入不可变桶或启用保留策略 |
| 版本管理 | 预归档允许版本；归档后只允许"追加更正/补充说明"，不覆盖原件 |

---

## 五、关联关系模型

### 5.1 核心表：VoucherRelation

```sql
CREATE TABLE voucher_relation (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,           -- 记账凭证 ID
    doc_id BIGINT NOT NULL,               -- 原始单据 ID
    relation_type VARCHAR(32),            -- 发票/回单/合同/其他
    evidence_role VARCHAR(32),            -- 证据角色
    confidence INT DEFAULT 0,             -- 置信度评分 (0-100)
    rule_id VARCHAR(64),                  -- 命中规则 ID
    rule_hit_detail JSONB,                -- 规则命中详情
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING/CONFIRMED/REJECTED/ARCHIVED
    confirmed_by BIGINT,
    confirmed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 5.2 匹配流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        智能关联流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. 采集        ERP 凭证入池 → 单据入池                          │
│       ↓                                                         │
│  2. 预处理      OCR/字段抽取 → 哈希去重 → 归一化                  │
│       ↓                                                         │
│  3. 候选召回    按金额、日期、对方、单号、摘要关键词召回           │
│       ↓                                                         │
│  4. 规则打分    匹配引擎评分（金额/日期/交易对手/关键字段）         │
│       ↓                                                         │
│  5. 人工校验    只处理低置信度与异常                              │
│       ↓                                                         │
│  6. 冻结归档    归档时把关系与证据链一起固化                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、关键表结构

### 6.1 会计凭证（结构化）

```sql
-- 记账凭证
CREATE TABLE voucher (
    id BIGSERIAL PRIMARY KEY,
    fonds_id BIGINT NOT NULL,             -- 全宗/公司
    ledger_id BIGINT,                     -- 账套
    period VARCHAR(7) NOT NULL,           -- 期间 2024-01
    voucher_word VARCHAR(10),             -- 凭证字
    voucher_no INT NOT NULL,              -- 凭证号
    voucher_date DATE NOT NULL,           -- 凭证日期
    summary TEXT,                         -- 摘要
    total_debit DECIMAL(18,2),            -- 借方合计
    total_credit DECIMAL(18,2),           -- 贷方合计
    preparer_id BIGINT,                   -- 制单人
    reviewer_id BIGINT,                   -- 审核人
    bookkeeper_id BIGINT,                 -- 记账人
    status VARCHAR(20) DEFAULT 'DRAFT',   -- 状态
    erp_source_key VARCHAR(128),          -- ERP 来源键
    created_at TIMESTAMP DEFAULT NOW()
);

-- 凭证分录
CREATE TABLE voucher_entry (
    id BIGSERIAL PRIMARY KEY,
    voucher_id BIGINT NOT NULL,
    entry_no INT NOT NULL,                -- 分录序号
    account_code VARCHAR(32) NOT NULL,    -- 科目编码
    account_name VARCHAR(128),            -- 科目名称
    direction VARCHAR(10) NOT NULL,       -- DEBIT/CREDIT
    amount DECIMAL(18,2) NOT NULL,
    auxiliary JSONB,                      -- 辅助核算
    summary TEXT
);
```

### 6.2 原始单据（单据池）

```sql
-- 原始单据（证据文档）
CREATE TABLE source_document (
    id BIGSERIAL PRIMARY KEY,
    fonds_id BIGINT NOT NULL,
    doc_type VARCHAR(32) NOT NULL,        -- INVOICE/CONTRACT/BANK_RECEIPT/...
    doc_no VARCHAR(64),                   -- 单据号
    counterparty VARCHAR(256),            -- 交易对手
    amount DECIMAL(18,2),                 -- 金额
    doc_date DATE,                        -- 单据日期
    currency VARCHAR(10) DEFAULT 'CNY',
    source VARCHAR(32),                   -- ERP/UPLOAD/OCR/BANK
    ocr_status VARCHAR(20),               -- PENDING/COMPLETED/FAILED
    dedup_hash VARCHAR(64),               -- SM3 去重哈希
    metadata JSONB,                       -- 扩展元数据
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW()
);

-- 单据文件
CREATE TABLE source_document_file (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,              -- 引用 file_object
    page_count INT DEFAULT 1,
    mime_type VARCHAR(64),
    is_primary BOOLEAN DEFAULT TRUE,      -- 是否主文件
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 6.3 文件统一抽象

```sql
CREATE TABLE file_object (
    id BIGSERIAL PRIMARY KEY,
    storage_backend VARCHAR(20) NOT NULL, -- LOCAL/MINIO/S3/NAS
    bucket VARCHAR(64),
    object_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(256),
    size_bytes BIGINT,
    mime_type VARCHAR(64),
    hash_sm3 VARCHAR(64),                 -- SM3 哈希
    hash_md5 VARCHAR(32),                 -- MD5（兼容）
    retention_lock BOOLEAN DEFAULT FALSE, -- WORM 锁定
    retention_until DATE,                 -- 保留期限
    created_at TIMESTAMP DEFAULT NOW()
);
```

### 6.4 归档固化

```sql
-- 归档批次
CREATE TABLE archive_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(32) NOT NULL UNIQUE,
    fonds_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    scope_type VARCHAR(20) DEFAULT 'PERIOD', -- PERIOD/CUSTOM
    status VARCHAR(20) DEFAULT 'PENDING',    -- PENDING/VALIDATING/APPROVED/ARCHIVED/FAILED
    voucher_count INT DEFAULT 0,
    doc_count INT DEFAULT 0,
    file_count INT DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    validation_report JSONB,              -- 归档前校验报告
    integrity_report JSONB,               -- 四性检测报告
    archived_at TIMESTAMP,
    archived_by BIGINT,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 档案包
CREATE TABLE archive (
    id BIGSERIAL PRIMARY KEY,
    archive_no VARCHAR(64) NOT NULL,      -- 档号
    batch_id BIGINT,                      -- 所属批次
    fonds_id BIGINT NOT NULL,
    category VARCHAR(32) NOT NULL,        -- VOUCHER/LEDGER/REPORT/OTHER/SOURCE_DOC
    title VARCHAR(256),
    period VARCHAR(7),
    retention_period VARCHAR(20),         -- 保管期限：PERMANENT/30Y/10Y
    security_level VARCHAR(20),           -- 密级
    status VARCHAR(20) DEFAULT 'ACTIVE',
    archived_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- 档案条目
CREATE TABLE archive_item (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL,       -- VOUCHER/LEDGER/REPORT/SOURCE_DOC/FILE
    ref_id BIGINT NOT NULL,               -- 引用 ID
    item_no INT,                          -- 条目序号
    title VARCHAR(256),
    hash_sm3 VARCHAR(64),                 -- 归档时哈希
    created_at TIMESTAMP DEFAULT NOW()
);

-- 四性检测结果
CREATE TABLE integrity_check (
    id BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,     -- BATCH/ARCHIVE/FILE
    target_id BIGINT NOT NULL,
    check_type VARCHAR(32) NOT NULL,      -- AUTHENTICITY/INTEGRITY/USABILITY/SECURITY
    result VARCHAR(20) NOT NULL,          -- PASS/FAIL/WARNING
    hash_expected VARCHAR(64),
    hash_actual VARCHAR(64),
    signature_valid BOOLEAN,
    details JSONB,
    checked_at TIMESTAMP DEFAULT NOW(),
    checked_by BIGINT
);

-- 期间锁定
CREATE TABLE period_lock (
    id BIGSERIAL PRIMARY KEY,
    fonds_id BIGINT NOT NULL,
    period VARCHAR(7) NOT NULL,           -- 2024-01
    lock_type VARCHAR(20) NOT NULL,       -- ERP_CLOSED/ARCHIVED/AUDIT_LOCKED
    locked_at TIMESTAMP NOT NULL,
    locked_by BIGINT,
    reason TEXT,
    UNIQUE(fonds_id, period, lock_type)
);

-- 归档后更正记录
CREATE TABLE archive_amendment (
    id BIGSERIAL PRIMARY KEY,
    archive_id BIGINT NOT NULL,
    amendment_type VARCHAR(20) NOT NULL,  -- CORRECTION/SUPPLEMENT/ANNOTATION
    reason TEXT NOT NULL,
    original_content JSONB,
    amended_content JSONB,
    attachment_ids BIGINT[],
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    approved_by BIGINT,
    approved_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

---

## 七、产品信息架构

```
┌─────────────────────────────────────────────────────────┐
│  电子会计档案管理系统 - 导航结构                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  📊 工作台（首页看板）                                   │
│                                                         │
│  📥 数据采集                                            │
│     ├── ERP 同步任务                                    │
│     ├── 手工上传                                        │
│     ├── 扫描集成                                        │
│     └── 数据质量面板                                    │
│                                                         │
│  🔄 预归档库（工作区）                                   │
│     ├── 记账凭证池                                      │
│     ├── 单据池                                          │
│     ├── OCR 识别                                        │
│     ├── 智能关联                                        │
│     └── 异常治理                                        │
│                                                         │
│  📁 会计档案（正式库）                                   │
│     ├── 记账凭证                                        │
│     ├── 会计账簿                                        │
│     ├── 财务报告                                        │
│     ├── 其他会计资料                                    │
│     └── 原始凭证                                        │
│                                                         │
│  🔧 档案作业                                            │
│     ├── 归档审批（批次归档入口）                         │
│     ├── 档案组卷                                        │
│     ├── 借阅管理                                        │
│     ├── 移交导出                                        │
│     └── 到期鉴定/销毁                                   │
│                                                         │
│  🔍 档案利用                                            │
│     ├── 全文检索                                        │
│     ├── 穿透联查                                        │
│     └── 统计报表                                        │
│                                                         │
│  ⚙️ 系统设置                                            │
│     ├── 规则引擎                                        │
│     ├── 四性检测                                        │
│     ├── 用户与权限                                      │
│     └── 审计日志                                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 八、产品策略

### 推荐：全托管存储（方案 A）

| 项目 | 说明 |
|------|------|
| 存储内容 | 凭证/账簿/报表版式件 + 原始单据原件 |
| ERP 定位 | 仅作为来源与反查入口 |
| 优点 | 满足"长期保管、不可变、脱离业务系统仍可用" |
| 代价 | 存储成本与同步改造更大 |

### 不推荐：仅引用 ERP（方案 B）

| 项目 | 说明 |
|------|------|
| 存储内容 | 仅元数据与链接 |
| 风险 | ERP 生命周期、权限、不可变、格式迁移、历史可读性都不可控 |
| 适用场景 | 仅作为过渡/轻量模式，需 ERP 提供不可变保管证明 |

---

## 九、实施路线图

### Phase 1: 基础能力（已完成）
- [x] 凭证池基础结构
- [x] 原始单据管理
- [x] 智能关联规则引擎
- [x] 基础 CRUD API

### Phase 2: 归档能力（进行中）
- [ ] 归档批次管理
- [ ] 期间锁定机制
- [ ] 四性检测
- [ ] 归档审批流程

### Phase 3: 合规能力
- [ ] WORM 存储对接
- [ ] 移交导出包生成
- [ ] 销毁流程与审计
- [ ] 更正记录管理

### Phase 4: 高级能力
- [ ] 全文检索优化
- [ ] AI 辅助识别
- [ ] 多账套支持
- [ ] 多租户隔离

---

## 附录：术语对照表

| 术语 | 英文 | 说明 |
|------|------|------|
| 记账凭证 | Voucher | 会计分录的载体 |
| 原始单据 | Source Document | 发票、合同、回单等证据文件 |
| 预归档库 | Staging Area | 可编辑的工作区 |
| 归档库 | Archive Repository | 不可变的正式档案库 |
| 归档批次 | Archive Batch | 按期间批量归档的控制单元 |
| 四性检测 | Integrity Check | 真实性、完整性、可用性、安全性 |
| 证据链 | Evidence Chain | 凭证与单据的关联关系 |
| 全宗 | Fonds | 档案来源单位（公司/组织） |
