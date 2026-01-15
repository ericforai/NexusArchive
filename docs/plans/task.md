# 附件预览 404 故障修复任务清单

- [x] 分析 404 故障原因
- [x] 环境与端口逻辑确认 (Backend: 19090, Frontend: 15175)
- [x] 验证全宗上下文过滤逻辑
- [x] 编写实施计划 ([2026-01-13-fix-attachment-preview-404.md](file:///Users/user/nexusarchive/docs/plans/2026-01-13-fix-attachment-preview-404.md))
- [ ] 提交实施计划供用户审批 [/]
- [ ] 执行方案修改
    - [ ] 修改 `ArchiveFileContentService.java` 移除冗余校验
- [ ] 验证修复结果
    - [ ] 使用 `curl` 进行不带标头的下载测试
    - [ ] 前端全景视图手动预览验证
- [ ] 更新经验文档并结项
