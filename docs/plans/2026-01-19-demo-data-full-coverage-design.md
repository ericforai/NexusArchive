# NexusArchive 全模块演示数据补全设计

**版本**: v1.0  
**编制日期**: 2026-01-19  
**适用范围**: 2025 年 / 7 全宗 / 最小规模 / 全模块覆盖  
**执行状态**: ✅ 设计确认

---

## 1. 背景与目标

现有演示数据体量小、覆盖面不足，导致多个模块无法完整演示。目标是在保持最小规模的前提下，为所有模块补齐真实业务链路的演示数据，并确保跨模块关系一致性与可复现性。

**目标要点**
- 覆盖全部模块入口：预归档、档案管理、全景联查、组卷、借阅、销毁、审计、检索、数据采集、系统设置相关统计
- 覆盖 7 个全宗（BR-GROUP/BR-SALES/BR-TRADE/BR-MFG/COMP001/BRJT/DEMO）
- 数据仅覆盖 2025 年，每个全宗 1 条主链路
- 跨表单据号、金额、日期、关联关系保持一致
- 附件真实感：关键节点高保真，其余简化

---

## 2. 设计原则

1. **全宗隔离**：所有 `fonds_no`/`fonds_code` 使用 `bas_fonds` 既有全宗码，跨全宗不互引。  
2. **统一命名**：ID 与单据号采用统一前缀，便于定位与清理。  
3. **最小规模**：每全宗 1 条主链路 + 1 条预归档记录 + 1 条案卷/批次记录。  
4. **可复现**：固定规则生成，支持重复执行与清理。  
5. **真实感优先级**：关键票据高保真，其余简化但字段齐全。

---

## 3. 全宗场景矩阵（2025）

| 全宗 | 业务定位 | 主链路主题 | 月份 |
| --- | --- | --- | --- |
| BR-GROUP | 资金管理/合并报表/审计 | 投融资付款→回单→合并报表 | 01 |
| BR-SALES | 销售开票/回款/应收 | 销售开票→回款→凭证 | 02 |
| BR-TRADE | 进出口/报关/结汇 | 进口采购→报关→结汇 | 03 |
| BR-MFG | 采购/入库/生产/成本 | 原材料采购→入库→成本结转 | 05 |
| COMP001 | 共享服务/报销/薪资/固资 | 员工差旅报销 | 07 |
| BRJT | 合资项目/进度款 | 合同→进度款→回单 | 09 |
| DEMO | 综合混合 | 服务合同→费用支付 | 11 |

---

## 4. 主链路模板（每全宗 1 条）

**节点链路（6-8 节点）**
1) 申请/合同  
2) 发票/原始单据  
3) 记账凭证  
4) 付款单  
5) 银行回单  
6) 账簿或报表  
7) 归档案卷/批次（归档登记）

**关系类型**
- BASIS（申请/合同 → 报销/付款）  
- ORIGINAL_VOUCHER（发票/原始单据 → 报销/凭证）  
- CASH_FLOW（付款单 → 银行回单）  
- ARCHIVE（凭证 → 报表/账簿）

**分类编码**
- `AC01`：凭证/单据类  
- `AC02`：账簿类（`custom_metadata.bookType`）  
- `AC03`：报表类（`custom_metadata.reportType`）

---

## 5. 模块覆盖与表清单

**预归档链路**
- `collection_batch`：每全宗 1 条采集批次
- `arc_original_voucher`：原始凭证池记录（与主链路单据一致）
- `arc_file_content`：附件文件记录（对齐附件路径/哈希/大小）
- `sys_ingest_request_status`：采集状态记录
- `arc_original_voucher_sequence`：凭证号序列更新

**档案管理/组卷/联查**
- `acc_archive`：主链路所有节点档案记录
- `acc_archive_relation`：主链路上下游关系
- `acc_archive_attachment`：档案与附件绑定
- `acc_archive_volume`：按年度案卷
- `archive_batch`：归档批次
- `sys_archival_code_sequence`：档号序列更新

**借阅/销毁/审计/统计**
- `biz_borrowing`：每全宗 1 条借阅申请
- `destruction_log`：每全宗 1 条销毁清册（模拟）
- `sys_audit_log`：关键操作审计记录
- `search_performance_stats`：检索统计
- `storage_capacity_stats`：容量统计
- `arc_reconciliation_record`：对账统计记录

---

## 6. 一致性规则

1. **全宗字段一致**：`fonds_no` 与 `fonds_code` 均使用 `bas_fonds.fonds_code`。  
2. **ID 命名规则**：`demo-<fonds>-<scene>-<type>-<seq>`，统一前缀便于清理。  
3. **档号规则**：`<FONDS>-2025-30Y-FIN-AC0X-0001`，并同步更新 `sys_archival_code_sequence`。  
4. **单据号一致**：`voucher_no`/`invoice_no`/`business_doc_no` 在 `acc_archive`、`arc_original_voucher`、`arc_file_content` 内一致。  
5. **金额/日期一致**：金额、业务日期在主链路上下游保持一致。  
6. **案卷/批次对齐**：`acc_archive_volume.file_count` 与链路档案数量一致；`archive_batch` 的数量字段匹配。  
7. **关系完整性**：`acc_archive_relation` 所有 source/target 必须存在于 `acc_archive`。

---

## 7. 附件策略

- 复用 `docs/demo数据` 现有 PDF 作为高保真附件（发票/报销/回单等）。  
- 新增附件采用简化模板生成 PDF（字段齐全、可预览）。  
- 统一存储路径：`uploads/demo/<fonds>/<doc_no>/<filename>`。  
- `arc_file_content` 的 `file_hash`、`file_size` 通过实际文件生成，保证四性检测可通过。  

---

## 8. 双通道落地与生成

**单一来源**
- 建立 `scripts/demo-data/spec.json`（或同级目录）作为数据规范来源。  

**生成输出**
1) Flyway 迁移脚本  
`nexusarchive-java/src/main/resources/db/migration/Vxxx__seed_demo_full_data.sql`  
2) 还原用种子文件更新  
`db/seed-data.sql`（补齐对应 COPY 数据段）  
3) 附件文件  
`uploads/demo/**`（高保真 + 简化生成）

**幂等策略**
- 迁移脚本使用 `ON CONFLICT DO NOTHING` 或明确前缀清理后插入。  
- ID 前缀固定，便于回滚脚本统一清理。

---

## 9. 异常处理与回滚

- **ID 冲突**：使用全宗前缀 + 场景码避免冲突，必要时先清理 `demo-` 前缀数据。  
- **附件缺失**：脚本自动生成占位 PDF，并标记 `check_result`。  
- **回滚**：提供按前缀删除的清理脚本，保证可控回退。

---

## 10. 验证与验收

**SQL 校验**
- 关联完整性：`acc_archive_relation` source/target 全部可 JOIN。  
- 数量一致：案卷/批次数量字段与实际数量一致。  
- 附件可用：`arc_file_content.storage_path` 存在且哈希匹配。

**UI 验证**
- 预归档列表可见、附件预览正常  
- 全景联查可展示上下游关系  
- 组卷、借阅、销毁、审计、检索、统计模块均有数据展示

---

## 11. 关联文档

- `docs/plans/2026-01-15-relationship-query-demo-data-plan.md`  
- `docs/plans/2026-01-13-demo-data-attachment-plan.md`

---

## 12. 预期改动文件清单

```
docs/plans/2026-01-19-demo-data-full-coverage-design.md  (本设计文档)
nexusarchive-java/src/main/resources/db/migration/Vxxx__seed_demo_full_data.sql
db/seed-data.sql
scripts/demo-data/spec.json
scripts/demo-data/generate_demo_data.(ts|py)
uploads/demo/** (新增演示附件)
docs/plans/README.md
docs/CHANGELOG.md
```
