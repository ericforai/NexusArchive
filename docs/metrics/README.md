一旦我所属的文件夹有所变化，请更新我。

// Input: 无
// Output: 指标数据目录说明
// Pos: docs/metrics/
// 一旦我所属的文件夹有所变化，请更新我。

本目录存放项目质量指标的历史数据。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `complexity-history.json` | 数据 | 代码复杂度历史快照（自动生成） |

## 数据说明

### complexity-history.json

由 `scripts/complexity-snapshot.js` 在每次 pre-commit 检查通过后自动生成。

格式：参见 [复杂度仪表板设计](../plans/2026-01-08-complexity-dashboard-design.md)
