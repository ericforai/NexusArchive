# 预归档池 Bug 修复记录

## 1. 429 Rate Limit 错误
- **现象**: 访问 `/pool/list/status/PENDING_ARCHIVE` 返回 429 错误。
- **原因**: 
  1. `dev.sh` 启动脚本未通过 `-Dspring-boot.run.profiles=dev` 激活开发环境配置。
  2. 导致 `RateLimitFilter` 使用默认配置（Enabled=true）。
- **修复**:
  - 修改 `scripts/dev.sh` 添加 profile 参数。
  - 修改 `application-dev.yml` 显式禁用限流。

## 2. 数据加载为空 (状态不一致)
- **现象**: API 修复后返回 200，但列表无数据。
- **原因**:
  - 前端请求状态: `PENDING_ARCHIVE`
  - 数据库/后端实际状态: `READY_TO_ARCHIVE`
  - 后端直接使用请求参数查询，导致结果为空。
- **修复**:
  - [MODIFY] `src/features/archives/controllers/types.ts`: 更新 `PoolStatusFilter` 类型定义。
  - [MODIFY] `src/features/archives/controllers/utils.ts`: 更新状态标签映射。

## 3. 验证方法
1. **后端验证**: `curl` 测试 API 可用性 (已验证)。
2. **数据验证**: 数据库中存在 `READY_TO_ARCHIVE` 状态数据 (已验证)。
3. **前端验证**: 请刷新 http://localhost:15175/system/pre-archive/pool，检查数据是否正常显示。
