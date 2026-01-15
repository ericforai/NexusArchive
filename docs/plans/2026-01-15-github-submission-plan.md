# 提交代码至 GitHub 实施计划

本计划旨在将当前本地的各项修复与改进方案正式提交并推送至 GitHub 仓库。

## 待提交变更说明

### [后端] 核心过滤器与数据种子
- #### [MODIFY] [FondsContextFilter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java)
    - 修复了附件下载/预览路径下的 401 错误。通过识别预览路径并跳过强制全宗匹配，确保文件能够通过 DataScopeService 进行二级鉴权。
- #### [MODIFY] [seed-data.sql](file:///Users/user/nexusarchive/nexusarchive-java/db/seed-data.sql)
    - 修正了 `attach-link-003` 的附件类型，从 `bank_slip` 改为 `invoice`（原始凭证），保持数据一致性。
- #### [NEW] [fix_archive_attachment_category.sql](file:///Users/user/nexusarchive/scripts/fix_archive_attachment_category.sql)
    - 提供用于修复现有库数据的 SQL 脚本。

### [前端] Dashboard 优化
- #### [MODIFY] [Dashboard.tsx](file:///Users/user/nexusarchive/src/pages/portal/Dashboard.tsx)
    - 实现了仪表盘数据随当前全宗切换自动刷新的功能。

### [其他]
- #### [MODIFY] [complexity-history.json](file:///Users/user/nexusarchive/docs/metrics/complexity-history.json)
    - 更新代码复杂度度量历史。
- #### [NEW] [2026-01-15-archive-attachment-category-fix.md](file:///Users/user/nexusarchive/docs/plans/2026-01-15-archive-attachment-category-fix.md)
    - 记录附件类别修复的详细方案。

## 验证计划

### 自动化验证
- `git status` 确认所有文件已就绪。
- `git diff --cached` 确认暂存内容准确无误。

### 手动验证
- 确认 Commit Message 符合项目规范。
- 推送后通过 `git log` 确认提交历史。
