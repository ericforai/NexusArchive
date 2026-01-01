# 模块化重构执行指南

> **For Claude:** 在新会话中使用 `superpowers:executing-plans` skill 执行此计划

## 启动新会话

### 1. 创建 Git Worktree

```bash
cd /Users/user/nexusarchive
git worktree add ../nexusarchive-modularization main
cd ../nexusarchive-modularization
```

### 2. 验证环境

```bash
# 后端
cd nexusarchive-java
mvn clean compile

# 前端
cd ..
npm run build
```

### 3. 启动新会话

在新创建的工作树中启动 Claude Code：

```bash
cd /Users/user/nexusarchive-modularization
claude
```

---

## 在新会话中执行

### 第一步：加载 executing-plans skill

```
/superpowers:executing-plans docs/plans/2025-12-31-modularization-implementation-plan.md
```

### 第二步：执行计划

skill 将自动：
1. 解析计划文件
2. 按步骤执行任务
3. 在每个检查点暂停等待确认
4. 运行测试验证
5. 创建提交

---

## 计划位置

**主计划文件**: `docs/plans/2025-12-31-modularization-implementation-plan.md`

**Phase 1 任务**:
- Task 1: Modal 组件统一
- Task 2: Excel/CSV 处理模块
- Task 3: ComplianceCheckService 拆分
- Task 4: MetadataEditModal 提取

---

## 执行后清理

完成后删除工作树：

```bash
cd /Users/user/nexusarchive
git worktree remove ../nexusarchive-modularization
```

---

**准备时间**: ~2 分钟
**预计执行时间**: Phase 1 约 1-2 周
