# AI Brain - 跨设备记忆同步目录

此目录用于存储 AI 助手（Gemini/Claude）在开发过程中产生的工作文档，便于在公司和家之间通过 Git 保持记忆连续性。

## 📁 目录结构

```
docs/ai-brain/
├── README.md                 # 本说明文件
├── task.md                   # 当前任务清单和进度
├── implementation_plan.md    # 实施计划（开发前的设计文档）
├── walkthrough.md            # 工作成果总结（开发完成后的文档）
└── sessions/                 # 重要会话记录（可选）
    └── YYYY-MM-DD-topic.md
```

## 🔄 工作流程

1. **开始新任务时**：AI 会在此目录创建 `implementation_plan.md` 和 `task.md`
2. **开发过程中**：AI 会更新 `task.md` 的进度
3. **完成任务后**：AI 会创建/更新 `walkthrough.md` 记录成果
4. **离开前**：`git commit` + `git push` 同步到 GitHub
5. **换设备后**：`git pull` 获取最新文档，AI 可以继续工作

## 💡 使用建议

- 在新设备开始工作时，让 AI 先阅读 `task.md` 了解当前进度
- 重要的设计决策记录在 `implementation_plan.md` 中
- 已完成功能的验证结果记录在 `walkthrough.md` 中

---

*此目录内容由 AI 自动维护，请勿手动修改*
