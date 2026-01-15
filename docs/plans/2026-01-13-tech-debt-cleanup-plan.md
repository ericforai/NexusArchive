# 技术债务清理计划：ESLint 警告修复

## 问题概述

~~代码库中存在 **237 个 ESLint 问题**（214 个错误，23 个警告）~~

### 执行进度（2026-01-13 更新）

| 指标 | 数值 |
|------|------|
| 初始问题数 | 237 |
| 已修复 | 56 |
| 剩余 | 181 |
| 修复率 | 24% |

> [!TIP]
> 配置优化：ESLint 配置已添加 `argsIgnorePattern: "^_"` 支持下划线前缀变量忽略。

> [!NOTE]  
> 本次批量修复聚焦于路由、状态管理、API 层、核心组件和高频 pages 文件。剩余问题主要分布在 `pages/` 目录（约 100 个）和 `exhaustive-deps` 警告（23 个）。

## 问题分布分析

| 问题类型 | 数量 | 占比 | 风险等级 | 修复难度 |
|----------|------|------|----------|----------|
| `@typescript-eslint/no-unused-vars` | 190 | 80.2% | 低 | 简单 |
| `react-hooks/exhaustive-deps` | 23 | 9.7% | 中 | 需审慎 |
| `@typescript-eslint/no-require-imports` | 9 | 3.8% | 低 | 简单 |
| `react/display-name` | 6 | 2.5% | 低 | 简单 |
| `react/no-unescaped-entities` | 4 | 1.7% | 低 | 简单 |
| `react/jsx-key` | 3 | 1.3% | 中 | 简单 |
| 其他 | 2 | 0.8% | 低 | 简单 |

## 修复策略

### 阶段一：低风险批量修复（~180 个问题）

这些问题可以安全地批量处理，不影响业务逻辑：

#### 1.1 未使用变量 (`no-unused-vars`) - 190 个

**策略分类**：

| 场景 | 处理方式 | 示例 |
|------|----------|------|
| 未使用的导入 | 直接删除 | `import { unused } from 'lib'` |
| 未使用的局部变量 | 删除变量或标记为下划线前缀 | `const unused = 1` → 删除 |
| 函数参数未使用 | 使用下划线前缀 | `(event) =>` → `(_event) =>` |
| 解构中未使用 | 使用下划线前缀或 rest 运算符 | `const { unused, ...rest } = obj` |
| 已声明但未实现的懒加载页面 | 添加到路由或删除声明 | 路由文件中的页面组件 |

**自动修复命令**：
```bash
npx eslint src --ext .ts,.tsx --fix
```

> [!WARNING]
> `--fix` 仅能修复约 1 个问题。大部分 `no-unused-vars` 需要手动处理。

#### 1.2 require 导入 (`no-require-imports`) - 9 个

**处理方式**：将 `require()` 改为 ES6 `import` 语法

```diff
- const lib = require('lib')
+ import lib from 'lib'
```

#### 1.3 React 显示名称 (`display-name`) - 6 个

**处理方式**：为匿名组件添加 `displayName`

```javascript
const MyComponent = React.memo(() => <div />);
MyComponent.displayName = 'MyComponent';
```

#### 1.4 未转义字符 (`no-unescaped-entities`) - 4 个

**处理方式**：转义 JSX 中的特殊字符

```diff
- <p>It's working</p>
+ <p>It&apos;s working</p>
```

#### 1.5 缺少 key 属性 (`jsx-key`) - 3 个

**处理方式**：为列表渲染元素添加 `key` 属性

---

### 阶段二：需审慎处理（~23 个问题）

#### 2.1 Hook 依赖项 (`react-hooks/exhaustive-deps`) - 23 个

> [!CAUTION]
> 这类问题不能盲目添加依赖项，需要逐个分析：
> - 添加依赖可能导致无限循环
> - 可能需要使用 `useCallback`/`useMemo` 包装
> - 某些情况下可能需要添加 ESLint disable 注释（需说明原因）

**处理策略**：

| 情况 | 处理方式 |
|------|----------|
| 确实需要添加依赖 | 添加到依赖数组 |
| 添加会导致无限循环 | 使用 `useCallback`/`useMemo` 或添加条件判断 |
| 故意忽略（如初始化 effect） | 添加 `// eslint-disable-next-line` 并说明原因 |

---

## 实施步骤

### 步骤 1：创建修复分支
```bash
git checkout -b fix/tech-debt-cleanup-batch-1
```

### 步骤 2：按目录分批修复

建议按功能模块分批处理，确保每批可独立验证：

| 批次 | 目录 | 预估问题数 | 优先级 |
|------|------|------------|--------|
| 1 | `src/routes/` | 6 | 高 |
| 2 | `src/store/` | 3 | 高 |
| 3 | `src/api/` | ~10 | 高 |
| 4 | `src/components/` | ~50 | 中 |
| 5 | `src/pages/` | ~100 | 中 |
| 6 | `src/hooks/` | ~15 | 中 |
| 7 | `src/features/` | ~20 | 低 |

### 步骤 3：每批修复后验证

```bash
# 类型检查
npm run typecheck

# Lint 检查
npm run lint

# 单元测试
npm run test:run

# 确保应用可正常启动
npm run dev
```

---

## 验证计划

### 自动化验证

1. **Lint 检查**
   ```bash
   npm run lint
   # 预期：0 errors, 0 warnings
   ```

2. **类型检查**
   ```bash
   npm run typecheck
   # 预期：无错误
   ```

3. **单元测试**
   ```bash
   npm run test:run
   # 预期：所有测试通过
   ```

4. **构建验证**
   ```bash
   npm run build
   # 预期：构建成功
   ```

### 手动验证

1. 启动开发环境后，手动检查以下核心页面是否正常工作：
   - 登录页面 `/login`
   - 全景视图 `/system/panorama`
   - 预归档池 `/system/pre-archive/pool`
   - 系统设置 `/system/settings/fonds`

---

## 风险缓解

| 风险 | 缓解措施 |
|------|----------|
| 删除了实际使用的变量 | 每次提交前运行 `npm run typecheck` |
| Hook 依赖修改导致无限循环 | 修改前在开发环境测试，观察控制台 |
| 大量修改导致代码审查困难 | 按模块分批提交，每批 < 50 个文件 |

---

## 时间估算

| 阶段 | 预估时间 |
|------|----------|
| 阶段一（低风险修复） | 2-3 小时 |
| 阶段二（Hook 依赖审慎修复） | 1-2 小时 |
| 验证与回归测试 | 1 小时 |
| **总计** | **4-6 小时** |

---

## 后续建议

1. **启用 pre-commit hook**：确保 `husky` 配置的 pre-commit 检查包含 `npm run lint`
2. **CI/CD 集成**：在 CI 流水线中添加 `npm run lint --max-warnings=0` 检查
3. **定期债务清理**：每周留出时间处理新增的 lint 警告，避免债务累积

---

## 是否继续？

请确认以上计划，我将开始分批执行修复工作。
