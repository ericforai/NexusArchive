# 任务规约 (Spec) - 系统架构治理与质量提升

## 目标
修复后端架构违规、后端单元测试失败、前端测试失败，并对前端 `IntegrationSettings.tsx` 超大组件进行重构。

## 成功标准
1. ✅ 后端所有 `QueryWrapper` 替换为 `LambdaQueryWrapper` (12+ 个文件)。
2. ✅ 后端架构测试 (`mvn test -Dgroups=architecture`) 全部通过。
3. ✅ 后端单元测试全部通过。
4. ✅ 前端 22 个失败的 Vitest 测试全部修复。
5. ✅ `IntegrationSettings.tsx` 拆分为 5-8 个子组件，单文件行数降至 300 行以下。
6. ✅ 前端所有测试全部通过。

## 实施路径
1. **[P0] 架构一致性**: 批量修复后端 `QueryWrapper` 违规。
2. **[P0] 质量回归**: 修复后端失败测试用例。
3. **[P1] 前端测试修复**: 解决 Vitest 运行环境与 Mock 问题。
4. **[P1] 熵减重构**: 拆分 `IntegrationSettings.tsx` 组件。
