一旦我所属的文件夹有所变化，请更新我。
本目录存放技术复盘与知识沉淀。
用于问题案例与经验总结。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `2025-12-09-submission-flow-fix.md` | 文档 | 2025-12-09-submission-flow-fix 文档 |
| `2025-12-10-approval-ui-fix.md` | 文档 | 2025-12-10-approval-ui-fix 文档 |
| `2025-12-10-archive-classification-compliance.md` | 文档 | 2025-12-10-archive-classification-compliance 文档 |
| `2025-12-18-phase4-performance-tuning-integration.md` | 文档 | 2025-12-18-phase4-performance-tuning-integration 文档 |
| `compliance_standards.md` | 文档 | compliance_standards 文档 |
| `erp_integration.md` | 文档 | erp_integration 文档 |
| `expert_review.md` | 文档 | expert_review 文档 |
| `README.md` | 说明文档 | 本目录说明 |
| `tech_architecture.md` | 文档 | tech_architecture 文档 |
| `临时文件存储路径问题排查.md` | 文档 | 临时文件存储路径问题排查 文档 |

# Knowledge Items (项目知识库)

本目录汇总了本项目开发过程中的关键知识点、合规要求与架构决策。

## 目录

1.  [**虚拟专家组审查机制 (Expert Group Review)**](./expert_review.md)
    *   定义了合规专家、信创架构师、交付专家的职责与审查流程。

2.  [**合规与信创标准 (Compliance & Xinchuang)**](./compliance_standards.md)
    *   **红线**: OFD/PDF 双轨、四性检测、SM3/SM4 必须支持。
    *   **数据**: BigDecimal 金额精度、10Y/30Y/永久保管期限。

3.  [**技术架构与安全 (Architecture & Security)**](./tech_architecture.md)
    *   私有化部署原则、离线依赖管理。
    *   私有化部署原则、离线依赖管理。
    *   基于 SM4 的数据加密与防篡改审计日志。

4.  [**ERP 集成与电子文件生成 (ERP Integration)**](./erp_integration.md)
    *   **JSON 解析**: 兼容多种字段命名与嵌套结构。
    *   **文件生成**: 确保 PDF 包含完整分录信息，处理好数据库 `NOT NULL` 约束。


## 获取帮助
如需新增知识条目，请确保经过 **虚拟专家组** 确认。
