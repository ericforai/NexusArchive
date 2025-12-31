# Sprint 0 技术验证总结报告

> **周期**: 2025-12-30 ~ 2025-12-31 (5 天)
> **目标**: 扫清 "信创适配" 与 "强合规" 的技术障碍，构建可验证的最小内核

---

## 🏆 核心成果

### Day 1: 质量门禁
| 成果 | 说明 |
| --- | --- |
| CheckStyle | 禁止硬编码 SQL、禁止 `@Select` 注解 |
| SpotBugs | 代码静态分析 |
| SQL Lint | 阻断 PG 特有语法 |
| MinIO | 对象存储集成 |

### Day 2: 全宗隔离
| 成果 | 说明 |
| --- | --- |
| `FondsContext` | ThreadLocal 全宗上下文 |
| `FondsIsolationInterceptor` | MyBatis 拦截器自动注入 |
| Red Team Tests | 模拟 SQL 注入绕过测试 |

### Day 3: 数据库适配与双键分片
| 成果 | 说明 |
| --- | --- |
| `DbAdapter` | 多厂商 DDL 生成 (PG/DM/KB) |
| `ArchiveYearContext` | 归档年度上下文 |
| `FondsYearComplexShardingAlgorithm` | 双键复合分片 |
| `SqlAuditGuard` | 可配置 SQL 审计守卫 |
| 字典优先策略 | 数据库规则优先于配置文件 |

### Day 4: 国密与四性检测
| 成果 | 说明 |
| --- | --- |
| BouncyCastle SM3 | 国密哈希算法集成 |
| `FileHashService` | 流式大文件哈希 |
| `MagicNumberValidator` | PDF/OFD/XML/JPEG 检测 |
| `FourNatureCheckService` | 四性检测引擎骨架 |

### Day 5: 整合验证
| 成果 | 说明 |
| --- | --- |
| `ArchiveSubmitService` | 隔离 + 四性整合示例 |
| 隔离决策最终版 | 拦截器为主、Sharding 储备 |

---

## 📊 性能基线

### SM3 vs SHA256 (软件 vs 硬件加速)
| Size | SM3 (ms) | SHA256 (ms) | Ratio |
| --- | --- | --- | --- |
| 1 KB | 0.129 | 0.006 | 21.5x |
| 10 KB | 0.199 | 0.010 | 19.9x |
| 100 KB | 1.709 | 0.053 | 32.3x |
| 1 MB | 5.203 | 0.507 | 10.3x |
| 10 MB | 56.227 | 5.135 | 11.0x |

**建议**: 大文件归档时异步计算哈希；非强合规场景提供 SHA256 降级。

---

## ⚠️ 风险清单

| 风险 | 影响 | 状态 | 缓解 |
| --- | --- | --- | --- |
| SM3 性能开销 | 归档延迟 | 已量化 | 异步+降级 |
| 复杂 SQL 正则边界 | 注入失败 | 低风险 | 增加测试 |
| 达梦 JSON 无索引 | 查询慢 | 已知限制 | 避免核心字段存 JSON |

---

## ✅ PRD 对齐验收

| 要求 | 状态 |
| --- | --- |
| 全宗隔离 (fonds_no) | ✅ |
| 年度隔离 (archive_year) | ✅ |
| 复合主键强制 | ✅ |
| SM3 国密支持 | ✅ |
| 四性检测 (真实性/可用性) | ✅ |
| 数据库多厂商适配 | ✅ |
| SQL Lint 阻断 PG 特有语法 | ✅ |

---

## 🚀 Sprint 1 建议 (对齐 Roadmap 阶段二: 合规攻关)

> 参考: [PRD v1.0 模块三](file:///Users/user/nexusarchive/docs/product/prd-v1.0.md) + [Roadmap 阶段二](file:///Users/user/nexusarchive/docs/planning/development_roadmap_v1.0.md)

### 核心任务 (P0)

| 任务 | PRD 来源 | 说明 |
| --- | --- | --- |
| **完整性检测** | PRD 3.1 | 校验 XML 元数据与 OFD/PDF 内容一致性（金额/日期） |
| **数字签名校验** | PRD 3.1 | SM2 签章验证（BouncyCastle 扩展） |
| **病毒扫描** | PRD 3.1 + Roadmap | ClamAV 接口集成 |
| **审计哈希链** | Roadmap 阶段二 | `curr_hash = SM3(prev_hash + data)` 链式校验 |
| **服务端水印** | PRD 2.2 + Roadmap | PDFBox/iText 流式加水印 |

### 次要任务 (P1)

| 任务 | 说明 |
| --- | --- |
| 坏文件样本库 | 准备改后缀 exe、签名失效 xml、金额不一致包 |
| 链式校验工具 | 模拟 DB 修改后报警 |

### 延后任务 (Sprint 2+)

| 任务 | 原因 |
| --- | --- |
| JWT archive_year 集成 | 基础设施优化，非合规核心 |
| 字典管理界面 | 运维便利性，可延后 |
| AIP 包结构 | 归入阶段三实物与业务 |
