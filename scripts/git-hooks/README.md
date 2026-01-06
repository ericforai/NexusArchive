# Git Hooks 开发规范

## 📋 概述

本项目使用 Git pre-commit hooks 来强制执行开发规范，防止常见错误进入代码库。

## 🔌 已安装的 Hooks

### Pre-commit Hook
- **检查内容**: Bash 脚本的环境变量导出
- **脚本位置**: `scripts/git-hooks/check-env-export.sh`

#### 检查规则
如果脚本中使用 `source .env*` 加载环境变量，必须使用 `set -a` 包裹：

```bash
# ✅ 正确
set -a
source .env.local
set +a

# ❌ 错误
source .env.local
```

#### 为什么需要这个检查？
Bash 的 `source` 命令**不会自动导出变量到子进程**。如果脚本中使用 `mvn spring-boot:run` 等子进程命令，环境变量将无法传递。

## 🚀 安装和更新

### 首次安装
```bash
bash scripts/install-git-hooks.sh
```

### 更新 Hooks
如果修改了检查脚本，重新运行安装命令：
```bash
bash scripts/install-git-hooks.sh
```

## 🧪 测试 Hook

### 测试是否生效
创建一个测试脚本并尝试提交：

```bash
# 创建测试脚本（应该被阻止）
cat > test-bad.sh << 'EOF'
#!/bin/bash
# 错误示例：没有 set -a
source .env.local
mvn spring-boot:run
EOF

chmod +x test-bad.sh
git add test-bad.sh
git commit -m "test: add bad script"
# 应该被阻止，并显示修复建议

# 修复后（应该通过）
cat > test-good.sh << 'EOF'
#!/bin/bash
# 正确示例：有 set -a
set -a
source .env.local
set +a
mvn spring-boot:run
EOF

chmod +x test-good.sh
git add test-good.sh
git commit -m "test: add good script"
# 应该通过
```

## 🔓 跳过 Hook（不推荐）

如果确实需要跳过检查：
```bash
git commit --no-verify -m "message"
```

## 📚 相关文档

- [Bash 环境变量导出问题分析](../systematic-debugging-report.md)
- [项目开发规范](../../docs/CONTRIBUTING.md)

## 🔧 添加新 Hook

### 1. 创建检查脚本
在 `scripts/git-hooks/` 目录创建新的检查脚本。

### 2. 更新安装脚本
编辑 `scripts/install-git-hooks.sh`，添加新的 hook。

### 3. 安装和测试
```bash
bash scripts/install-git-hooks.sh
```

## 🐛 故障排查

### Hook 没有运行
1. 检查 hook 是否安装：
   ```bash
   ls -la .git/hooks/pre-commit
   ```
2. 重新安装：
   ```bash
   bash scripts/install-git-hooks.sh
   ```

### Hook 执行错误
查看详细错误信息：
```bash
bash scripts/git-hooks/check-env-export.sh
```

## 📝 最佳实践

### 编写 Bash 脚本时
1. **总是使用 `set -a/source/set +a`** 模式加载环境变量
2. **在脚本开头添加注释** 说明为何使用 `set -a`
3. **团队 Code Review** 时检查这个模式

### 示例模板
```bash
#!/bin/bash
# 加载环境变量（必须导出到子进程）
set -a  # 自动导出所有后续变量
source .env.local
set +a  # 停止自动导出

# 现在启动的子进程可以访问环境变量
mvn spring-boot:run
```
