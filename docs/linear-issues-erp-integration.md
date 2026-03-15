# ERP 集成问题清单

生成时间: 2026-03-15
来源: Enterprise Integration Review

---

## Issue 1: [CRITICAL] Webhook 事件监听器缺失 - YonSuiteVoucherEvent 未被消费

**优先级**: P0 - Critical
**类型**: Bug
**模块**: ERP 集成
**状态**: Backlog

### 问题描述

`YonSuiteWebhookController` 发布了 `YonSuiteVoucherEvent` 事件，但系统中没有对应的 `@EventListener` 来消费此事件。这导致 webhook 接收到的凭证变更事件实际上没有被处理。

### 影响范围

- YonSuite webhook 回调虽然成功接收，但业务逻辑未执行
- 凭证同步功能不完整

### 复现路径

1. 查看 `YonSuiteWebhookController.publishVoucherEvent()` 方法
2. 搜索 `YonSuiteVoucherEventListener` 类 → 不存在

### 建议方案

1. 创建 `YonSuiteVoucherEventListener` 类
2. 实现 `@EventListener` 处理 `YonSuiteVoucherEvent`
3. 添加对应的业务逻辑（凭证同步/状态更新）

### 相关文件

- `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonbip/webhook/YonSuiteWebhookController.java`

### 预估工作量

- 后端开发: 4-6 小时
- 测试: 2 小时

---

## Issue 2: [CRITICAL] Webhook Nonce 重放攻击防护使用内存存储

**优先级**: P0 - Critical
**类型**: 安全漏洞
**模块**: ERP 集成 / Webhook
**状态**: Backlog

### 问题描述

`WebhookNonceStore` 使用 `ConcurrentHashMap` 存储 nonce，应用重启后状态丢失。攻击者可以在应用重启后重放之前已处理的 webhook 请求。

### 影响范围

- 应用重启后 nonce 验证失效
- 可能导致重放攻击
- 安全合规风险

### 复现路径

1. 发送 webhook 请求，nonce 被记录
2. 重启应用
3. 使用相同 nonce 再次发送 → 请求通过验证

### 建议方案

1. 迁移 `WebhookNonceStore` 到 Redis 存储
2. 设置合理的过期时间（如 24 小时）
3. 添加启动时的持久化检查

### 相关文件

- `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonbip/webhook/WebhookNonceStore.java`

### 预估工作量

- 后端开发: 3-4 小时
- 测试: 2 小时

---

## Issue 3: [IMPORTANT] 分页同步缺少检查点/恢复支持

**优先级**: P1 - Important
**类型**: 功能改进
**模块**: ERP 集成
**状态**: Backlog

### 问题描述

`YonPaymentApplySyncService` 在处理大量分页数据时，如果同步中断（网络故障、应用重启），无法从断点恢复，只能从头开始。

### 影响范围

- 大批量数据同步中断后需要全量重跑
- 浪费 API 配额和时间
- 同步窗口过长

### 建议方案

1. 添加同步检查点记录（Redis/数据库）
2. 每处理完一页记录当前进度
3. 启动时检查未完成的同步任务
4. 支持手动触发从断点恢复

### 相关文件

- `nexusarchive-java/src/main/java/com/nexusarchive/integration/yonbip/sync/YonPaymentApplySyncService.java`

### 预估工作量

- 后端开发: 6-8 小时
- 测试: 3 小时

---

## Issue 4: [IMPORTANT] ERP 同步操作缺少幂等性保护

**优先级**: P1 - Important
**类型**: 功能改进
**模块**: ERP 集成
**状态**: Backlog

### 问题描述

ERP 同步操作没有幂等性保护，重试可能导致数据重复或状态错误。

### 影响范围

- 网络抖动导致的重试可能创建重复数据
- 无法安全重试失败的同步操作
- 数据一致性风险

### 建议方案

1. 为每个同步操作生成唯一 idempotency key
2. 同步前检查是否已处理
3. 使用数据库唯一约束或 Redis 去重

### 相关文件

- 所有 `integration/yonbip/sync/` 下的同步服务类

### 预估工作量

- 后端开发: 4-6 小时
- 测试: 2 小时

---

## Issue 5: [IMPORTANT] 缺少重试机制及瞬时/永久错误区分

**优先级**: P1 - Important
**类型**: 功能改进
**模块**: ERP 集成
**状态**: Backlog

### 问题描述

ERP 调用失败后没有重试机制，或者有重试但无法区分瞬时错误（可重试）和永久错误（不应重试）。

### 影响范围

- 瞬时故障（网络抖动）导致同步失败
- 永久错误（权限不足）无限重试浪费资源

### 建议方案

1. 定义错误分类：瞬时（5xx 网络）、永久（4xx 业务）
2. 瞬时错误使用指数退避重试
3. 永久错误快速失败并告警
4. 记录重试原因和次数到审计日志

### 相关文件

- 所有 `integration/yonbip/` 下的 API 客户端类

### 预估工作量

- 后端开发: 6-8 小时
- 测试: 3 小时

---

## 总结

| 优先级 | 数量 | 预估总工时 |
|--------|------|-----------|
| P0 - Critical | 2 | 11-15 小时 |
| P1 - Important | 3 | 25-31 小时 |
| **总计** | **5** | **36-46 小时** |

### 建议优先级排序

1. **Issue 2** (Nonce 存储迁移) - 安全问题，最高优先级
2. **Issue 1** (Webhook 监听器) - 功能完整性
3. **Issue 4** (幂等性保护) - 数据一致性基础
4. **Issue 3** (检查点恢复) - 大规模同步必需
5. **Issue 5** (重试机制) - 可靠性增强
