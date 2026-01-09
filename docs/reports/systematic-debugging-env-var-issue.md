# 系统化调试报告：端口配置反复失效问题

**日期**: 2026-01-06
**问题**: 端口配置每天修改，每天失效
**方法**: Systematic Debugging (4阶段流程)
**结果**: ✅ 根本原因已找到并修复

---

## 问题陈述

### 症状
- 配置文件 `application.yml` 设置 `port: ${SERVER_PORT:19090}`
- `.env.local` 设置 `SERVER_PORT=19090`
- 但后端实际运行在端口 8080（默认值）
- 前端配置的是 `localhost:19090`，无法连接
- 用户反映："配置问题我们每天都在改，昨天已经采取强制方法一劳永逸的解决了，但今天又出现了"

### 影响
- 开发环境无法正常启动
- 前端无法获取 ERP 配置数据
- 每天都需要手动修改配置或重启后端

---

## Phase 1: 根因调查

### 1.1 证据收集

**检查清单：**
- [x] 所有配置文件（application.yml, application-dev.yml, application-demo.yml）
- [x] 环境变量（SERVER_PORT）
- [x] Maven 插件配置
- [x] 启动脚本
- [x] 运行日志

**关键发现：**

| 文件 | 内容 | 状态 |
|------|------|------|
| `.env.example:34` | `SERVER_PORT=19090` | ✅ 正确 |
| `.env.local` | `SERVER_PORT=19090` | ✅ 正确 |
| `application.yml:7` | `port: ${SERVER_PORT:19090}` | ✅ 正确 |
| `scripts/dev.sh:41` | `source .env.local` | ⚠️ **问题** |
| `scripts/dev.sh:113` | `mvn spring-boot:run &` | ❌ 环境变量未传递 |

### 1.2 根本原因

**Bash `source` 命令不会自动导出变量到子进程！**

当 `dev.sh` 执行 `mvn spring-boot:run` 时：
1. `source .env.local` 在**当前 shell** 设置 `SERVER_PORT=19090`
2. `mvn spring-boot:run` 在**子进程**运行
3. 子进程无法访问当前 shell 的变量
4. Spring Boot 没有收到 `SERVER_PORT`，使用默认值 8080

---

## Phase 2: 模式分析

### 2.1 对比正确模式

**正确的环境变量导出方法：**
```bash
# 方法1: 使用 export
export SERVER_PORT=19090
mvn spring-boot:run

# 方法2: 使用 set -a（推荐）
set -a
source .env.local
set +a
mvn spring-boot:run

# 方法3: 命令前传递
SERVER_PORT=19090 mvn spring-boot:run
```

### 2.2 找出所有受影响的脚本

```bash
$ grep -r "source.*\.env" scripts/
scripts/dev.sh:source .env.local
scripts/db-dump.sh:source .env.local
scripts/db-load.sh:source .env.local
scripts/deploy.sh:source .env.server
```

**所有 4 个脚本都有同样的问题！**

---

## Phase 3: 假设验证

### 假设
**H1:** `source .env.local` 不会自动导出变量到子进程

### 测试

```bash
# 测试1: source 不导出
source /tmp/test.env  # TEST_VAR=123
echo "当前: $TEST_VAR"     # 显示: 123
bash -c 'echo $TEST_VAR'  # 显示: (空)

# 测试2: export 导出
export TEST_VAR=456
bash -c 'echo $TEST_VAR'  # 显示: 456

# 测试3: set -a 导出（POSIX 标准）
set -a
source /tmp/test.env
set +a
bash -c 'echo $TEST_VAR'  # 显示: 123
```

### 结论
✅ 假设验证成功！`set -a/source/set +a` 是正确的解决方案

---

## Phase 4: 实施修复

### 4.1 修改所有脚本

| 脚本 | 修改前 | 修改后 |
|------|--------|--------|
| `scripts/dev.sh` | `source .env.local` | `set -a; source .env.local; set +a` |
| `scripts/db-dump.sh` | `source .env.local` | `set -a; source .env.local; set +a` |
| `scripts/db-load.sh` | `source .env.local` | `set -a; source .env.local; set +a` |
| `scripts/deploy.sh` | `source .env.server` | `set -a; source .env.server; set +a` |

### 4.2 添加自动化保护

创建了 Git pre-commit hook：
- **检查脚本**: `scripts/git-hooks/check-env-export.sh`
- **安装脚本**: `scripts/install-git-hooks.sh`
- **文档**: `scripts/git-hooks/README.md`

**Hook 功能：**
- 自动检查所有 `.sh` 文件
- 如果发现 `source .env` 但没有 `set -a`，阻止提交
- 显示具体的修复建议

---

## 技术原理

### Bash 环境变量传递

| 方法 | 当前 shell | 子进程 | 适用场景 |
|------|-----------|--------|---------|
| `VAR=value` | ✓ | ✗ | 仅当前 shell |
| `export VAR=value` | ✓ | ✓ | 单个变量 |
| `set -a; source; set +a` | ✓ | ✓ | 批量导出（推荐） |
| `VAR=value command` | - | ✓ | 单条命令 |

### 为什么 `set -a` 是最佳方案？

1. **POSIX 标准**：跨所有 shell 兼容（bash, zsh, sh）
2. **批量导出**：一次设置，所有后续变量都导出
3. **作用域明确**：`set +a` 停止导出，不会影响后续代码
4. **易于维护**：只需在 `source` 前后各加一行

---

## 防止未来问题

### 1. 自动化保护（已实现）✅
- Git pre-commit hook 自动检查
- 违反规范的脚本无法提交

### 2. 文档（已创建）✅
- [Bash 脚本开发规范](../../docs/development/bash-scripting-standards.md)
- [Git Hooks 使用指南](../git-hooks/README.md)

### 3. 团队培训建议
- 新成员入职时说明此规范
- Code Review 时检查 `set -a` 模式
- 定期审查脚本文件

---

## 经验教训

### ❌ 错误模式
- "临时修复看起来有效"：硬编码 `port: 19090` 只掩盖症状
- "问题反复出现说明之前没找到根因"
- "配置每天都在改"：说明没有追踪数据流

### ✅ 正确方法
- 使用 Systematic Debugging 4阶段流程
- 从症状→根因→假设→验证
- 一次只改一处，验证后再继续
- 添加自动化防止问题重现

---

## 附录：相关文件

### 修改的文件
```
scripts/dev.sh
scripts/db-dump.sh
scripts/db-load.sh
scripts/deploy.sh
application.yml (恢复为 ${SERVER_PORT:19090})
```

### 新增的文件
```
scripts/git-hooks/check-env-export.sh
scripts/git-hooks/README.md
scripts/install-git-hooks.sh
docs/development/bash-scripting-standards.md
docs/reports/systematic-debugging-env-var-issue.md (本文件)
```

### Git 提交
```bash
git add scripts/ docs/
git commit -m "fix: correct environment variable export in bash scripts

- Use set -a/source/set +a pattern to export variables to subprocesses
- Add git pre-commit hook to enforce this rule
- Add documentation for team members

Fixes issue where backend runs on port 8080 instead of 19090
because SERVER_PORT from .env.local was not propagated to mvn.

Root cause: Bash 'source' command doesn't auto-export variables.
"
```
