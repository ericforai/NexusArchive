// Input: 领域业务逻辑
// Output: 极简架构说明
// Pos: src/features/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 领域业务逻辑层 (Features)

本目录按照业务领域组织复杂的逻辑，包括数据转换、业务规则验证和跨组件的状态交互。
部分模块（如 settings）采用 DDD-ish 分层（domain/application/infrastructure）。

## 目录结构

- `archives/`: 档案核心逻辑（归档、组卷、四性检测）。
- `borrowing/`: 借阅审批流程逻辑。
- `compliance/`: 合规性检查与报告生成逻辑。
- `settings/`: 字典配置、参数管理逻辑。

## 架构约束

1. **组合而非展示**: Features 层专注于 "如何做" (Logic)，而非 "怎么看" (UI)。
2. **禁止反向依赖**: **严禁**在此层引入 `src/components/*` 中的组件，以防止循环依赖。
3. **被调用者**: 主要被 `src/pages/` 调用，通过模块入口 `src/features/<module>/index.ts` 访问。
