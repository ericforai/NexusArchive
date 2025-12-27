---
description: 建立新的 ERP 数据源连接器 (Standard Operating Procedure)
---

# /add-connector: ERP 集成连接器开发规范

为了确保集成中心功能的专业性与可复制性，所有新接口的开发必须遵循以下层级命名与配置规范。

## 1. 架构层级定义

| 层级 | 术语 | 命名规范 | 示例 |
| :--- | :--- | :--- | :--- |
| **L2** | **实例配置 (Config)** | `[ERP类型] ([环境/部门])` | `用友 YonSuite (财务测试)` |
| **L3** | **业务场景 (Scenario)** | `[业务对象]获取/同步` | `凭证同步` |
| **L4** | **子接口 (Sub-Interface)** | `[技术动词][名词]` | `LIST_QUERY` |

## 2. 种子数据 (Seed Data) 模板

在编写 `V{n}__` 迁移脚本时，请使用以下 SQL 模板，避免直接透传技术 ID。

```sql
-- 1. 创建连接器实例 (L2)
INSERT INTO sys_erp_config (name, erp_type, config_json, is_active)
SELECT '新接入ERP (生产环境)', 'GENERIC', '{"baseUrl":"..."}', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_erp_config WHERE name = '新接入ERP (生产环境)');

-- 2. 创建业务场景 (L3)
INSERT INTO sys_erp_scenario (config_id, scenario_key, name, sync_strategy)
SELECT c.id, 'VOUCHER_SYNC', '记账凭证获取', 'MANUAL'
FROM sys_erp_config c WHERE c.name = '新接入ERP (生产环境)';

-- 3. 定义子接口 (L4)
INSERT INTO sys_erp_sub_interface (scenario_id, interface_key, interface_name)
SELECT s.id, 'DETAIL_GET', '凭证详情拉取'
FROM sys_erp_scenario s WHERE s.name = '记账凭证获取';
```

## 3. 验收标准清单 (DoD)

- [ ] **命名检查**：左侧列表不包含 `_SYNC` 或 `_CONFIG` 等后台标识符。
- [ ] **标题显示**：右侧详情页上方显示巨大的中文业务名（如“凭证同步”）。
- [ ] **适配器代码**：在 `com.nexusarchive.integration.erp.adapter` 包下实现对应的 `ErpAdapter`。
- [ ] **图标注册**：在前端 `ADAPTER_CONFIG` (IntegrationSettings.tsx) 中注册对应的图标颜色。

## 4. 专家审查提示

提交代码前，请确保通过 `/expert-review` 调取专家组进行“合规性与可交付性”审查。
- **电子会计档案专家**：核对元数据是否满足 DA/T 94 要求。
- **交付专家**：核对是否支持离线环境下的配置预置。
