Long-term Preservation (EEGS Compliance)
=========================================

该包包含电子档案长期保存相关的核心逻辑，特别是符合 DA/T 标准的 "四性检测" (Four Natures Check)。

## 核心组件

- **FourNaturesCheck**: 检测接口规范。
- **CheckResult**: 检测结果封装。
- **PreservationService**: 长期保存服务门面。

## 目录结构
- `impl/`: 具体检测策略实现 (Integrity, Authenticity, etc.)
- `job/`: 定期巡检任务

一旦我所属的文件夹有所变化，请更新我。
