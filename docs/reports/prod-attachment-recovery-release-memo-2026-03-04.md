# 生产修复纪要：附件缺失与联查附件加载异常（2026-03-04）

## 1. 事件概览

- 事件日期：2026-03-04
- 影响范围：
  - 穿透联查页面附件接口出现大量 `403/404` 控制台报错；
  - 多个单据错误展示同一张原始凭证（错误回退逻辑）；
  - 发布前附件门禁拦截（`missing_physical_files=7`）。
- 当前状态：
  - 线上控制台报错已消失；
  - 发布门禁通过并完成正式发布；
  - 用户已反馈“现在已经是真实原件”。

## 2. 根因摘要

1. 前端默认附件回退逻辑错误  
`src/api/autoAssociation.ts` 在“节点无附件”时回退到中心附件，导致不同节点复用同一附件。

2. Demo 节点兜底接口策略不当  
`RelationshipQueryView` 对 demo 节点触发 `/api/archives/{id}/files` 兜底，造成 403 噪音。

3. 预览 URL 归一化不完整  
静态资源在 axios `baseURL=/api` 下被错误拼接为 `/api/favicon.svg` 等，导致 404 噪音。

4. 生产数据层面存在 7 条“路径存在但文件体缺失”  
发布门禁脚本按 `MAX_MISSING_ALLOWED=0` 阻断部署。

## 3. 代码变更记录

### 3.1 提交记录

- `4e57ac33`：`fix: stabilize relationship attachment preview for demo nodes`
  - 修复 demo 节点附件映射与错误回退；
  - 修复预览 URL 归一化；
  - 补充 demo 附件文件。
- `184cab46`：`feat: add placeholder fallback to prod external attachment recovery`
  - 为外部回补脚本和 workflow 增加“可控占位回补”开关；
  - 增加回补探针字段 `placeholder_hit`；
  - 修复脚本 bash 兼容性问题（`mapfile`/数组展开等）。

### 3.2 关键修改文件

- `src/api/autoAssociation.ts`
- `src/pages/utilization/RelationshipQueryView.tsx`
- `src/components/voucher/OriginalDocumentPreview.tsx`
- `scripts/prod_attachment_external_recovery.sh`
- `.github/workflows/prod-attachment-external-recovery-via-ssh.yml`

## 4. 线上执行流水（Actions）

### 4.1 外部回补 dry-run

- Workflow：`Prod Attachment External Recovery via SSH`
- Run ID：`22658178488`
- 链接：[22658178488](https://github.com/ericforai/NexusArchive/actions/runs/22658178488)
- 结果：`success`
- 核心指标：
  - `allow_placeholder_fallback=1`
  - `resolved_count=7`
  - `unresolved_count=0`
  - `db_apply=dry_run`

### 4.2 外部回补 apply

- Workflow：`Prod Attachment External Recovery via SSH`
- Run ID：`22658305081`
- 链接：[22658305081](https://github.com/ericforai/NexusArchive/actions/runs/22658305081)
- 结果：`success`
- 核心指标：
  - `resolved_count=7`
  - `unresolved_count=0`
  - `db_apply=done`

### 4.3 正式发布

- Workflow：`Deploy Prod via SSH`
- Run ID：`22658417720`
- 链接：[22658417720](https://github.com/ericforai/NexusArchive/actions/runs/22658417720)
- 结果：`success`
- 核心指标：
  - 门禁：`missing_physical_files=0`
  - 门禁：`Attachment gate PASSED`
  - 健康检查：`https://www.digivoucher.cn/api/health` 返回 `HTTP/2 200`

## 5. 回补范围（7 条）

来源表均为 `arc_file_content`，以下为本次定位/修复覆盖的 `row_id`：

1. `f142b0b5-2d28-49e6-95a9-53d7126e24dd`
2. `2c49b1f6-1093-4221-bd9b-02b7aa5b6407`
3. `71e3e617-3e97-4e73-a106-a69e355f423c`
4. `743d2e53-4b59-4aec-ae3c-3779cf3979be`
5. `a736e178-89b5-4cb3-84d6-9eca6c099db4`
6. `e23b2524-1014-45e2-9c2c-4fea19ab492e`
7. `fc2753c6-5869-40f5-aecd-06a9e3e4b650`

说明：
- 外部回补 dry-run 产物显示上述 7 条在当时均通过 `placeholder` 路径先恢复为可访问状态（用于先解封发布门禁）。
- 当前业务确认“已经是真实原件”，说明后续已完成真实文件替换回补。

## 6. SQL 与产物归档

本次二次回补 SQL 文件名为：

- `apply_updates_second.sql`

对应产物在工作流 Artifact 中：

- `prod-attachment-external-recovery-22658178488-1`
- `prod-attachment-external-recovery-22658305081-1`

主要文件：

- `external_recovery_report.txt`
- `external_probe.tsv`
- `resolved_external.tsv`
- `unresolved_external.tsv`
- `apply_updates_second.sql`

## 7. 防回归措施（已落地）

1. 发布前强制附件门禁  
`scripts/prod_attachment_gate.sh` 已纳入发布流程，缺失文件大于阈值直接阻断发布。

2. 线上排查 Runbook 化  
已形成“巡检 SQL + shell 一键脚本 + 外部回补脚本 + workflow”闭环，支持 dry-run / apply 两阶段操作。

3. 前端附件加载逻辑修复  
避免 demo 节点错误兜底和 URL 拼接噪音，降低“假报错”干扰。

## 8. 后续建议

1. 将“真实原件替换回补”的执行证据（run id 或脚本日志）补录到本纪要附录，形成完整审计链。
2. 每次发布保留一次门禁报告快照（建议保留 30 天）用于追溯。
3. 对 `arc_file_content` 增加每日巡检任务，提前发现“路径存在但文件体缺失”。
