一旦我所属的文件夹有所变化，请更新我。
本目录存放 Git Hooks 配置。
用于代码提交前的自动化检查与验证。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `pre-commit` | Git Hook | 提交前架构检查（dependency-cruiser、模块清单、ArchUnit）|
| `pre-commit-complexity` | Git Hook | 提交前复杂度检查（非阻塞警告模式）|
| `_/` | 目录 | Husky 内部目录（勿修改）|

## 工作流程

```
git commit
    ↓
pre-commit (阻塞模式)
    ├─ 前端架构检查 (check:arch)
    ├─ 前端模块清单验证 (modules:validate)
    └─ 后端架构检查 (ArchitectureTest)
    ↓
pre-commit-complexity (非阻塞模式)
    ├─ 前端复杂度检查 (complexity:check)
    └─ 后端复杂度检查 (ComplexityRulesTest)
    ↓
提交完成
```

## 相关文档

- [复杂度规则](../docs/architecture/complexity-rules.md)
- [架构边界](../docs/architecture/module-boundaries.md)
- [前端依赖规则](../.dependency-cruiser.cjs)
