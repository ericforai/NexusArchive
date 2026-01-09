# NexusArchive 复杂度报告

**生成时间**: `2026-01-09 11:02:47`
**报告版本**: `20260109-110245`

---

## 📊 执行摘要

| 层级 | 状态 | 违规数 | 详情 |
|------|------|--------|------|
| **前端 (React/TypeScript)** | ✅ 通过 | 551 | [查看详细 JSON](reports/complexity-report-20260109-110245.json) |
| **后端 (Java/Spring)** | ⚠️ 测试失败 | 1 | [查看 ArchUnit 输出](reports/archunit-backend-20260109-110245.txt) |

---

## 🔍 前端复杂度分析 (ESLint)

### 规则配置

| 规则 | 阈值 | 说明 |
|------|------|------|
| `max-lines` | 300 (普通) / 600 (页面) | 单文件最大行数 |
| `max-lines-per-function` | 50 | 单函数最大行数 |
| `max-depth` | 4 | 最大嵌套深度 |
| `max-params` | 10 | 最大参数数量 |
| `complexity` | 10 | 圈复杂度 |
| `max-nested-callbacks` | 4 | 最大嵌套回调数 |

### 违规详情


---

## ☕ 后端复杂度分析 (ArchUnit)

### 规则配置

| 规则 | 阈值 | 说明 |
|------|------|------|
| Service 类行数 | 500 | Service 类最大行数 |
| Controller 类行数 | 600 | Controller 类最大行数 |
| Entity 类行数 | 400 | Entity 类最大行数 |
| 方法行数 | 50 | 单个方法最大行数 |

### 分析统计

- **总类数**: 1160
- **违规数**: 1

### 违规详情

```
Found 1 complexity violation(s):

--- Integration Classes ---
  YonSuiteClient: 619 lines (limit: 500) - +119 lines over

Summary: 1 violation(s) across 1 type(s)
```

---

## 📈 历史对比

最近 5 次报告:
- [`complexity-report-20260109-110245.md`](reports/complexity-report-20260109-110245.md)
- [`complexity-report-latest.md`](reports/complexity-report-latest.md)
- [`complexity-report-20260108-190007.md`](reports/complexity-report-20260108-190007.md)
- [`complexity-report-20260108-185418.md`](reports/complexity-report-20260108-185418.md)
- [`complexity-report-20260108-152424.md`](reports/complexity-report-20260108-152424.md)

---

## 🛠️ 改进建议

### 高复杂度文件处理
1. **拆分大文件**: 将超过行数限制的文件拆分为多个模块
2. **提取方法**: 将长方法拆分为更小的、职责单一的方法
3. **减少嵌套**: 使用早返回 (early return) 减少嵌套深度
4. **参数对象**: 使用参数对象替代过多的函数参数

### 架构层面
1. **模块化**: 确保每个模块职责单一
2. **依赖倒置**: 高层模块不应依赖低层模块
3. **边界清晰**: 严格遵循模块边界规则

---

*此报告由 `scripts/complexity-report.sh` 自动生成*
