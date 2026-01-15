# 提交代码到 GitHub 成果总结

已成功将本地开发的各项修复与功能增强提交并推送至 GitHub 远端仓库。

## 提交内容汇总

### 核心功能修复
- **[后端] 鉴权优化**: 修复了 `FondsContextFilter.java` 中对附件预览路径（`/files/download/` 及 `/content`）的 401 拦截逻辑。现在该路径将跳过强制全宗匹配，由 `DataScopeService` 进行精确的二级权限校验，解决了预览失效问题。
- **[数据库] 种子数据修正**: 
    - 修改了 `nexusarchive-java/db/seed-data.sql` 和 `db/seed-data.sql`。
    - 将 `attach-link-003` 的类型由错误的 `bank_slip` 修正为 `invoice` (原始凭证)。
    - 提供了 `scripts/fix_archive_attachment_category.sql` 脚本用于存量数据修复。
- **[前端] Dashboard 体验提升**: 在 `Dashboard.tsx` 中增加了对 `currentFondsCode` 的监听。现在切换当前全宗后，仪表盘数据会自动触发刷新，保证数据实时性。

### 文档与度量
- **任务支撑文档**:
    - [2026-01-15-archive-attachment-category-fix.md](file:///Users/user/nexusarchive/docs/plans/2026-01-15-archive-attachment-category-fix.md)
    - [2026-01-15-github-submission-plan.md](file:///Users/user/nexusarchive/docs/plans/2026-01-15-github-submission-plan.md)
- **效能度量**: 更新了 `docs/metrics/complexity-history.json` 的代码复杂度历史。

## 推送验证结果

```text
To github.com:ericforai/NexusArchive.git
   d7a46647..39ccc329  main -> main
```
- **提交哈希**: `39ccc329`
- **文件变更**: 9 files changed, 2129 insertions(+), 42 deletions(-)
- **自动化检查**: 前端架构检查、模块清单验证及复杂度校验均已通过。
