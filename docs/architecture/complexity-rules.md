<!-- Input: 复杂度检查基础设施（ESLint、ArchUnit、CI/CD）
Output: 代码复杂度规则文档
Pos: docs/architecture/
一旦我所属的文件夹有所变化，请更新我。 -->

# 代码复杂度规则

> **版本**: 1.0.0
> **最后更新**: 2026-01-08
> **状态**: 已实施

---

## 📋 概述

本文档定义 NexusArchive 项目的代码复杂度规则，旨在保持代码的可维护性和可读性。

### 设计原则

1. **自动化检测** - 所有规则通过 CI/CD 自动检查
2. **非阻塞模式** - Pre-commit 钩子仅警告，不阻止开发
3. **分层阈值** - 不同类型文件有不同限制
4. **持续改进** - 定期回顾和调整阈值

---

## 🔧 规则配置

### 前端规则 (TypeScript/React)

| 规则 | 普通文件 | 页面文件 | 测试文件 | 说明 |
|------|----------|----------|----------|------|
| `max-lines` | 300 | 600 | 关闭 | 单文件最大行数 |
| `max-lines-per-function` | 50 | 50 | 关闭 | 单函数最大行数 |
| `max-depth` | 4 | 4 | 关闭 | 最大嵌套深度 |
| `max-params` | 10 | 10 | 10 | 最大参数数量 |
| `complexity` | 10 | 10 | 关闭 | 圈复杂度 |
| `max-nested-callbacks` | 4 | 4 | 关闭 | 最大嵌套回调数 |

### 后端规则 (Java/Spring)

| 规则 | Service | Controller | Entity | 说明 |
|------|---------|------------|--------|------|
| 类行数 | 500 | 600 | 400 | 单类最大行数 |
| 方法行数 | 50 | 50 | 50 | 单方法最大行数 |

---

## 🚀 使用方法

### 本地检查

```bash
# 前端复杂度检查
npm run complexity:check

# 生成复杂度报告
npm run complexity:report

# 后端复杂度检查
cd nexusarchive-java
mvn test -Dtest=ComplexityRulesTest
```

### CI/CD 检查

- **Pull Request**: 自动触发复杂度检查
- **Push**: 自动触发复杂度检查
- **手动触发**: GitHub Actions → Complexity Check → Run workflow

### Pre-commit 钩子

提交代码时自动运行复杂度检查（非阻塞模式）：

```bash
git commit  # 自动运行 complexity 检查
```

---

## 📁 相关文件

| 文件 | 说明 |
|------|------|
| `.eslintrc.complexity.cjs` | 前端 ESLint 复杂度配置 |
| `nexusarchive-java/.../ComplexityRulesTest.java` | 后端 ArchUnit 测试 |
| `scripts/complexity-report.sh` | 复杂度报告生成脚本 |
| `.husky/pre-commit-complexity` | Pre-commit 复杂度钩子 |
| `.github/workflows/complexity-check.yml` | CI 复杂度检查工作流 |

---

## 🛠️ 重构建议

### 高行数文件

1. **拆分大文件**: 将超过行数限制的文件拆分为多个模块
2. **提取组件**: 将大型组件拆分为更小的子组件
3. **提取 Hooks**: 将逻辑提取为自定义 Hooks

### 高复杂度函数

1. **提取方法**: 将长方法拆分为更小的、职责单一的方法
2. **早返回**: 使用早返回 (early return) 减少嵌套深度
3. **参数对象**: 使用参数对象替代过多的函数参数

### 架构层面

1. **模块化**: 确保每个模块职责单一
2. **依赖倒置**: 高层模块不应依赖低层模块
3. **边界清晰**: 严格遵循模块边界规则

---

## 📊 示例重构

### Before (高复杂度)

```tsx
// 150 行组件，嵌套深度 6
const LargeComponent: React.FC = () => {
  const [data, setData] = useState([]);
  
  return (
    <div>
      {data.map(item => (
        <div key={item.id}>
          {item.children.map(child => (
            <div key={child.id}>
              {child.items.map(sub => (
                <span key={sub.id}>
                  {sub.value}
                </span>
              ))}
            </div>
          ))}
        </div>
      ))}
    </div>
  );
};
```

### After (低复杂度)

```tsx
// 拆分为多个组件
const LargeComponent: React.FC = () => {
  const [data, setData] = useState([]);
  return <ItemList items={data} />;
};

const ItemList: React.FC<{ items: Item[] }> = ({ items }) => (
  <div>{items.map(item => <Item key={item.id} data={item} />)}</div>
);

const Item: React.FC<{ data: Item }> = ({ data }) => (
  <div><SubItemList items={data.children} /></div>
);
```

---

## 🔄 规则更新流程

1. **提议**: 在团队会议中讨论规则变更
2. **评估**: 评估变更对现有代码的影响
3. **实施**: 更新配置文件和文档
4. **通知**: 通知团队成员变更内容
5. **监控**: 监控 CI/CD 中的违规数量变化

---

*文档维护: 架构团队*
