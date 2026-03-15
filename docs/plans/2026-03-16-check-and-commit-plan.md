# 代码检测与提交实施计划

本项目正处于阶段性验收前夕，需要对当前工作区进行深度清理、合规性检查，并将“无问题”的代码提交至远程仓库。

## 用户审核建议
> [!WARNING]
> 发现工作区中存在敏感文件（如 `private.pem`, `jwt_private.pem`, `truststore.p12`），这些文件虽然在 `.gitignore` 中有部分定义，但仍出现在未跟踪列表中。**计划默认不提交这些文件**。
> [!IMPORTANT]
> 发现了大量的测试脚本和备份文件（`.bak`, `.backup`），计划在提交前进行物理删除。

## 拟定的变更

### 工作区清理 [CLEANUP]
- **删除备份文件**: `*.bak`, `*.backup`
- **处理敏感文件**: 验证 `src/main/resources` 下的证书文件是否为测试用例必须，若非必须则移动到 `tmp/` 或确认已被 `.gitignore` 覆盖（避免 `git add .` 误触发）。
- **清理测试残留**: 删除 `nexusarchive-java/manual_backend_start.log`, `nexusarchive-java/nexusarchive-release.tar.gz` 等。

### 合规性检查 [COMPLIANCE]
- **金额精度**: 抽查关键 Service 确保使用 `BigDecimal`。
- **哈希算法**: 确认文件上传逻辑中使用 `SM3`。
- **文档同步**: 更新 `docs/CHANGELOG.md` 记录本次大规模变更。

### 提交 [GIT]
- **暂存变更**: 使用 `git add` 命令，跳过敏感目录。
- **提交信息**: 遵循约定式提交（Conventional Commits），例如 `feat: complete phase 3 remediation and enhance test coverage`.

## 验证计划

### 自动化测试
- **后端单元测试**: 运行 `mvn clean compile test` 确保 37 个受影响文件的相关逻辑无误。
- **代码扫描**: 检查 `SM3` 和 `BigDecimal` 的使用情况。

### 手动验证
- 检查 `git status` 确保没有任何不该提交的文件进入暂存区。
