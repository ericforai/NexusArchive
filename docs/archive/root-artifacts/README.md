# Root Artifacts Archive

该目录用于归档历史上放在仓库根目录的临时产物，避免根目录继续堆积。

## 目录说明

- `diagnostics/`: 诊断与编译检查输出（如 `compile_errors.txt`）
- `test-reports/`: 历史测试执行结果与汇总
- `samples/`: 示例测试输入文件

## 约定

- 新的测试报告优先放在 `docs/testing/`（面向阅读）或 `reports/`（面向工具输出）。
- 一次性诊断产物不要放根目录，优先放本目录。
- 如果文件仅本地调试使用且不需要版本控制，请放 `logs/` 或其他已忽略目录。
