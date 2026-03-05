# System Self-Check

## 目标

提供一套可持续执行的系统级自查流程，输出“模块-测试-状态”矩阵，用于追踪：

- 哪些模块已有测试映射
- 哪些模块未覆盖
- 哪些模块在最近一次回归中失败

## 命令

- `npm run self-check:matrix`
  - 仅生成矩阵（不执行测试）
  - 默认输出：
    - `reports/self-check/latest/module-test-matrix.json`
    - `docs/reports/YYYY-MM-DD-system-self-check-matrix.md`

- `npm run self-check:run`
  - 启动本地环境并执行基础门禁回归（Playwright API/UI + Vitest）
  - 基于真实结果生成矩阵
  - 输出：
    - `reports/self-check/<timestamp>/...`
    - `reports/self-check/latest/module-test-matrix.json`
    - `reports/self-check/latest/module-test-matrix.md`
    - `docs/reports/YYYY-MM-DD-system-self-check-matrix.md`

- `npm run self-check:integration`
  - 启动本地环境并执行联调门禁（外部依赖/跨系统场景）
  - 当前覆盖：
    - `src/e2e/yonsuite-scenarios.spec.ts`
    - `src/e2e/yonsuite-verification.spec.ts`
    - `tests/playwright/delivery_v2.spec.ts`
  - 对 Playwright 结果执行硬校验：`failed/timedOut/interrupted/skipped` 必须为 0
  - 输出：
    - `reports/self-check/integration/<timestamp>/playwright-integration.json`

## 门禁拆分策略

- 基础门禁（`self-check:run`）
  - 目标：稳定、可持续、默认必须通过
  - 不承载外部联调依赖场景

- 联调门禁（`self-check:integration`）
  - 目标：跨系统真实联调验证
  - 通过独立流水线执行，不影响基础门禁稳定性

## 状态含义

- `passed`: 已映射测试且本轮无失败
- `failed`: 已映射测试且本轮存在失败/超时/中断
- `mapped_not_run`: 有测试映射，但当前结果中无运行状态
- `skipped_only`: 只有跳过，无通过/失败
- `uncovered`: 无测试映射
