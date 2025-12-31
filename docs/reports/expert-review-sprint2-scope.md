# 虚拟专家组联合审查报告

> **审查主题**: Sprint 2 范围评估 - 实物与业务 (Physical & Business)
> **审查日期**: 2025-12-31
> **触发方式**: 用户请求 /expert-review

---

## Step 0: 假设与审查边界声明

### ✅ 已知前提

1. **产品定位**: 电子会计档案系统，私有化部署
2. **当前进度**: Sprint 0 (隔离内核) → Sprint 1 (合规引擎) 已完成
3. **架构规则**: 已定义于 `expert-group-rules.md`
4. **Sprint 2 Spec**: 已生成 `sprint-2-spec.md`，覆盖实物位置、装盒、借阅、盘点

### ⚠️ 合理假设

- 用户后续可能扩展至实物管理，但当前 MVP 聚焦电子化

### 🛑 关键发现：产品边界冲突

根据 [expert-group-rules.md](file:///Users/user/nexusarchive/docs/planning/expert-group-rules.md) 第 2.5 节：

> **产品定位与边界（负向约束）**
> - **仅电子化**：管理对象为电子数据
> - **去实体化**：
>   - ❌ 不包含实体库房
>   - ❌ 不包含温湿度 / IoT
>   - ❌ 不包含实体借阅流转
> - **单套制**：以电子形式为主的归档模式

**结论**: Sprint 2 的以下内容 **Out of Scope**:

| Sprint 2 交付物 | 边界判定 |
| --- | --- |
| `arc_location` 位置模型 | ❌ 实体库房 → Out of Scope |
| `arc_box` 装盒 | ❌ 实物管理 → Out of Scope |
| `BoxLabelGenerator` 盒脊标签 | ❌ 实物管理 → Out of Scope |
| 状态机 `BORROWED/RETURNED` | ❌ 实体借阅流转 → Out of Scope |
| `BorrowService` 实物借阅 | ❌ 实体借阅流转 → Out of Scope |
| `InventoryService` 盘点 | ❌ 实体库房 → Out of Scope |
| `ArchiveSearchService` 检索增强 | ✅ 电子化核心 → **In Scope** |
| `DataMaskingService` 脱敏 | ✅ 电子化核心 → **In Scope** |

---

## Step 1-3: 联合审查（按专家分工）

### 1️⃣ 合规专家 (Compliance Authority)

#### 法规对标

| 检查项 | 状态 | 说明 |
| --- | --- | --- |
| 电子会计档案元数据 | ✅ 已实现 | Sprint 0/1 覆盖 |
| 电子凭证原件管理 | ✅ 已实现 | OFD/PDF 双轨 |
| 四性检测 | ✅ 已实现 | 完整性/真实性/可用性/安全性 |
| 30年长期保存 | ⚠️ 待验证 | 格式迁移策略待完善 |

#### 专家建议

> **实物档案管理不属于电子会计档案核心范畴。**
> 根据《会计档案管理办法》第8条，电子会计档案可单套制管理，
> 无需强制配套实物管理模块。
>
> **建议**: 将 Sprint 2 实物模块标记为 `DEFERRED`，
> 优先完善电子化核心功能（检索、脱敏、流式预览）。

---

### 2️⃣ 架构专家 (Architecture & Security Expert)

#### 四性技术实现评估

| 四性 | Sprint 1 状态 | 说明 |
| --- | --- | --- |
| 真实性 | ✅ | SM3 哈希 + 签名验证 |
| 完整性 | ✅ | XML/OFD/PDF 一致性校验 |
| 可用性 | ✅ | Magic Number + Dry Parse |
| 安全性 | ✅ | ClamAV/Mock 病毒扫描 |

#### 待优化项

| 模块 | 现状 | 优化建议 | 难度 |
| --- | --- | --- | --- |
| 审计哈希链 | 内存存储 | 持久化到 DB | 中 |
| 签名验证 | OFD 模拟 | 集成完整 ofdrw-sign | 高 |
| 流式预览 | 未实现 | PDFBox 分页流 | 中 |

#### 专家建议

> 当前架构已能支撑电子档案核心场景。
> 实物管理会引入额外复杂度（位置编码、二维码打印、状态机并发），
> 且与主业务解耦较差。
>
> **建议**: 若未来需要实物管理，作为独立模块开发，
> 通过 API 与核心系统集成，而非耦合到主代码库。

---

### 3️⃣ 交付专家 (Delivery Strategist)

#### 私有化交付评估

| 检查项 | 状态 | 说明 |
| --- | --- | --- |
| Docker 开发环境 | ✅ | docker-compose.dev.yml |
| 离线安装能力 | ⚠️ | 待完善 |
| 数据迁移工具 | ❌ | 未实现 |
| License 管理 | ❌ | 未实现 |

#### 专家建议

> 增加"实物管理"模块将显著增加交付复杂度：
> - 标签打印需适配多种打印机
> - 盘点需手持终端/扫码枪集成
> - 借阅流转需现场培训
>
> **建议**: MVP 阶段剔除实物模块，降低交付成本。
> 后续可作为"增值模块"单独销售/部署。

---

## Step 4: 阻断点与结论

### 🛑 阻断点 (Showstoppers)

| # | 问题 | 来源 | 严重级别 |
| --- | --- | --- | --- |
| 1 | Sprint 2 实物管理超出产品边界 | expert-group-rules.md 2.5 | 🛑 致命 (Out of Scope) |

### ✅ 可继续的工作

| 模块 | 状态 | 下一步 |
| --- | --- | --- |
| 高级检索 | In Scope | 提取为新 Sprint |
| 数据脱敏 | In Scope | 提取为新 Sprint |
| 流式预览 | In Scope | 提取为新 Sprint |
| 动态水印 | In Scope | Sprint 1 已部分完成 |

---

## ⚖️ 专家联合建议

### 🏛️ 合规专家
> 电子会计档案系统应聚焦**电子化核心**，实物管理可作为后期扩展。
> 建议按《会计档案管理办法》单套制要求，优先完善归档、检索、长期保存。

### 🔐 架构专家
> 将 Sprint 2 实物模块**标记为 DEFERRED**。
> 重新规划 Sprint 2 为"检索与利用增强"，对齐 PRD 模块二。

### 📦 交付专家
> MVP 剔除实物模块，减少交付复杂度。
> 后续可作为独立"物管插件"提供。

---

## 📋 行动建议

1. **立即执行**: 将 `sprint-2-spec.md` 标记为 `DEFERRED`
2. **重新规划**: 创建 `sprint-2-revised-spec.md` 聚焦检索/脱敏/预览
3. **更新 PRD**: 在 PRD 中明确实物管理为"可选扩展模块"
4. **Sprint 2 新范围**:
   - 高级检索 (结构化索引)
   - 数据脱敏 (敏感字段规则)
   - 流式预览 (大文件分页)
   - 动态水印增强

---

## 附录：审查依据

- [expert-group-rules.md](file:///Users/user/nexusarchive/docs/planning/expert-group-rules.md)
- [expert-group-workflow.md](file:///Users/user/nexusarchive/docs/planning/expert-group-workflow.md)
- [PRD v1.0](file:///Users/user/nexusarchive/docs/product/prd-v1.0.md)
- [Development Roadmap](file:///Users/user/nexusarchive/docs/planning/development_roadmap_v1.0.md)
