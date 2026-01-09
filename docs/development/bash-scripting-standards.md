# Bash 脚本开发规范

## 🔒 强制规范（Git Hook 自动检查）

### 规则：使用 `set -a/source/set +a` 模式加载环境变量

**✅ 正确：**
```bash
#!/bin/bash
set -a          # 自动导出所有后续变量
source .env.local
set +a          # 停止自动导出

# 子进程可以访问环境变量
mvn spring-boot:run
```

**❌ 错误：**
```bash
#!/bin/bash
source .env.local

# 子进程无法访问 SERVER_PORT 等环境变量
mvn spring-boot:run  # 将使用默认配置
```

### 为什么？
Bash 的 `source` 不会自动导出变量到子进程。这会导致：
- 后端运行在 8080 而不是 19090
- 数据库连接失败
- 配置全部失效

## 🔧 安装 Git Hook

```bash
bash scripts/install-git-hooks.sh
```

## 🧪 测试

Hook 在每次提交时自动运行。尝试提交错误脚本会看到：

```
❌ 第 4 行: source .env 没有正确的 set -a 包裹
修复方法:
  将:
    source .env.local
  改为:
    set -a
    source .env.local
    set +a
```

## 🔓 跳过检查（不推荐）

```bash
git commit --no-verify -m "message"
```

## 📚 更多信息

- [完整文档](../scripts/git-hooks/README.md)
- [系统化调试报告](../docs/reports/systematic-debugging-env-var-issue.md)
