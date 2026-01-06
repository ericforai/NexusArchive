# 前端功能缺失分析报告

> **分析日期**: 2025-01  
> **分析基准**: PRD v1.0 + 后端已实现功能  
> **分析范围**: 所有后端已实现但前端缺失的功能

---

## 📊 总体情况

**后端功能实现**: 99.5%  
**前端功能实现**: 约 60%  
**前端功能缺失**: 约 40%

---

## ✅ 已实现的前端功能

### 1. 基础功能 ✅
- ✅ 档案列表和检索（`ArchiveListPage.tsx`）
- ✅ 档案详情查看（`ArchiveDetailModal.tsx`）
- ✅ 档案预览（`ArchivePreviewModal.tsx`）
- ✅ 动态水印组件（`WatermarkOverlay.tsx`）
- ✅ 跨全宗授权票据申请（`AuthTicketApplyPage.tsx`）
- ✅ 全宗沿革管理（`FondsHistoryPage.tsx`, `FondsHistoryListPage.tsx`）
- ✅ 销毁流程（`DestructionView.tsx`）
- ✅ 销毁清册（`DestructionRepositoryView.tsx`）
- ✅ 用户设置（`UserSettingsPage.tsx`）
- ✅ 角色设置（`RoleSettingsPage.tsx`）
- ✅ 审计日志查看（`AuditLogView.tsx`）

### 2. 已实现但需增强的功能 ⚠️
- ⚠️ 档案预览（需要支持 `mode=rendered` 服务端渲染水印）
- ⚠️ 动态水印（已实现，但需要与后端服务端渲染模式集成）
- ⚠️ 授权票据管理（已实现申请页，列表/审批/详情待补）
- ⚠️ 全宗沿革管理（已实现核心操作与历史查询，导出与审计关联待补）

---

## ❌ 缺失的前端功能（按优先级）

### P0（必须实现 - 核心业务功能）

#### 1. 跨全宗访问授权票据管理 ⚠️
**后端实现**: ✅ 完整实现（`AuthTicketService`, `AuthTicketController`）  
**前端现状**: ⚠️ 页面已建立（申请/列表/审批/详情），列表 API 与数据联动待补

**仍需完善的功能**:
- [ ] **列表 API 联动**（状态筛选、按全宗筛选、分页）
- [ ] **审批链/详情渲染**（审批记录展示、审计日志关联）
- [ ] **状态更新与刷新**（审批/撤销后刷新列表）

**API 集成**:
- `POST /api/auth-ticket/apply` - 申请授权票据
- `GET /api/auth-ticket/list` - 获取票据列表
- `POST /api/auth-ticket/{id}/approve` - 审批票据
- `GET /api/auth-ticket/{id}` - 获取票据详情
- `POST /api/auth-ticket/{id}/revoke` - 撤销票据

---

#### 2. 审计证据链验真界面 ❌
**后端实现**: ✅ 完整实现（`AuditLogVerificationService`, `AuditEvidencePackageService`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **审计验真页面** (`src/pages/audit/AuditVerificationPage.tsx`)
  - 单条日志验真
  - 批量日志验真
  - 按条件验真（全宗、时间范围、操作类型）
  - 验真结果展示（通过/失败、失败原因）
  
- [ ] **证据包导出页面** (`src/pages/audit/AuditEvidencePackagePage.tsx`)
  - 选择导出条件（全宗、时间范围、操作类型）
  - 导出格式选择（JSON/XML/ZIP）
  - 导出进度显示
  - 下载证据包

**API 集成**:
- `POST /api/audit-log/verify/single` - 单条日志验真
- `POST /api/audit-log/verify/batch` - 批量验真
- `POST /api/audit-log/verify/chain` - 链路验真
- `POST /api/audit-log/evidence-package/export` - 导出证据包
- `GET /api/audit-log/sampling/random` - 随机抽检

---

#### 3. 档案销毁流程增强 ❌
**后端实现**: ✅ 完整实现（`ArchiveExpirationService`, `ArchiveAppraisalService`, `DestructionApprovalService`）  
**前端现状**: ⚠️ 部分实现（`DestructionView.tsx` 存在但功能不完整）

**需要增强的功能**:
- [ ] **到期档案识别页面** (`src/pages/operations/ExpiredArchivesPage.tsx`)
  - 展示到期档案列表
  - 筛选：按全宗、按年度、按保管期限
  - 批量操作：批量生成鉴定清单
  
- [ ] **鉴定清单生成页面** (`src/pages/operations/AppraisalListPage.tsx`)
  - 展示鉴定清单
  - 支持导出（Excel/PDF）
  - 鉴定意见填写
  
- [ ] **销毁审批页面** (`src/pages/operations/DestructionApprovalPage.tsx`)
  - 待审批销毁申请列表
  - 审批表单：审批意见、批准/拒绝
  - 双人审批流程展示
  
- [ ] **销毁执行页面** (`src/pages/operations/DestructionExecutionPage.tsx`)
  - 已审批的销毁任务列表
  - 执行销毁操作
  - 销毁进度显示

**API 集成**:
- `GET /api/archive/expired` - 获取到期档案列表
- `POST /api/archive/appraisal/generate` - 生成鉴定清单
- `GET /api/archive/appraisal/list` - 获取鉴定清单列表
- `POST /api/destruction/approval/approve` - 审批销毁申请
- `POST /api/destruction/execution/execute` - 执行销毁

---

### P1（重要功能 - 安全与合规）

#### 4. MFA 多因素认证 ❌
**后端实现**: ✅ 完整实现（`MfaService`, `MfaController`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **MFA 设置页面** (`src/pages/settings/MfaSettingsPage.tsx`)
  - 启用/禁用 MFA
  - TOTP 二维码展示（使用 `qrcode.react` 库）
  - 备用码生成和展示
  - 备用码下载/打印
  
- [ ] **MFA 验证页面** (`src/pages/auth/MfaVerifyPage.tsx`)
  - TOTP 码输入框
  - 备用码输入选项
  - 验证失败提示

**API 集成**:
- `POST /api/mfa/setup` - 设置 MFA
- `POST /api/mfa/verify` - 验证 TOTP 码
- `POST /api/mfa/verify-backup` - 验证备用码
- `GET /api/mfa/backup-codes` - 获取备用码

---

#### 5. 用户生命周期管理 ❌
**后端实现**: ✅ 完整实现（`UserLifecycleService`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **用户生命周期管理页面** (`src/pages/admin/UserLifecyclePage.tsx`)
  - 入职触发：选择员工、自动创建账号、分配角色
  - 离职触发：选择员工、停用账号、回收权限
  - 调岗触发：选择员工、调整角色和权限
  
- [ ] **定期复核（Access Review）页面** (`src/pages/admin/AccessReviewPage.tsx`)
  - 复核任务列表
  - 复核执行：查看用户权限、确认/撤销权限
  - 复核历史记录

**API 集成**:
- `POST /api/user-lifecycle/onboard` - 入职触发
- `POST /api/user-lifecycle/offboard` - 离职触发
- `POST /api/user-lifecycle/transfer` - 调岗触发
- `GET /api/access-review/tasks` - 获取复核任务
- `POST /api/access-review/execute` - 执行复核

---

#### 6. 冻结/保全管理 ❌
**后端实现**: ✅ 完整实现（`ArchiveFreezeService`, `ArchiveFreezeController`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **冻结/保全管理页面** (`src/pages/operations/FreezeHoldPage.tsx`)
  - 冻结/保全申请：选择档案、填写原因、设置期限
  - 冻结/保全列表：展示所有冻结/保全的档案
  - 解除冻结/保全：审批解除申请
  
- [ ] **冻结/保全详情页面** (`src/pages/operations/FreezeHoldDetailPage.tsx`)
  - 冻结/保全原因展示
  - 冻结/保全期限展示
  - 相关审计日志

**API 集成**:
- `POST /api/archive/freeze` - 申请冻结
- `POST /api/archive/hold` - 申请保全
- `GET /api/archive/freeze/list` - 获取冻结列表
- `POST /api/archive/freeze/{id}/release` - 解除冻结

---

### P2（辅助功能 - 运维与监控）

#### 7. 文件存储策略配置 ❌
**后端实现**: ✅ 完整实现（`FileStoragePolicyService`, `FileHashDedupService`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **文件存储策略配置页面** (`src/pages/settings/FileStoragePolicyPage.tsx`)
  - 不可变策略配置：启用/禁用、保留期限
  - 保留策略配置：保留期限设置
  - 策略应用到全宗
  
- [ ] **哈希去重范围配置页面** (`src/pages/settings/FileHashDedupPage.tsx`)
  - 去重范围选择：同全宗/授权范围/全局
  - 去重规则配置

**API 集成**:
- `POST /api/file-storage-policy/create` - 创建存储策略
- `GET /api/file-storage-policy/{fondsNo}` - 查询全宗策略
- `POST /api/file-hash-dedup/configure` - 配置去重范围

---

#### 8. 性能指标监控 ❌
**后端实现**: ✅ 完整实现（`PerformanceMetricsService`, `PerformanceMetricsController`）  
**前端缺失**: ❌ 完全缺失

**需要实现的页面/组件**:
- [ ] **性能指标监控页面** (`src/pages/monitor/PerformanceMetricsPage.tsx`)
  - 实时指标展示：单全宗容量、并发检索数、最大文件大小
  - 性能报告：预览首屏时间、日志留存周期
  - 指标趋势图表（使用 ECharts 或 Chart.js）
  - 告警阈值配置

**API 集成**:
- `GET /api/performance-metrics/report` - 获取性能报告
- `GET /api/performance-metrics/current` - 获取当前指标
- `GET /api/performance-metrics/trend` - 获取指标趋势

---

#### 9. 服务端渲染水印预览支持 ⚠️
**后端实现**: ✅ 完整实现（`StreamingPreviewService.renderWithWatermark()`）  
**前端现状**: ⚠️ 部分支持（需要增强 `ArchivePreviewModal.tsx`）

**需要增强的功能**:
- [ ] **预览模式选择** (`src/pages/preview/ArchivePreviewModal.tsx`)
  - 根据档案 `security_level` 自动选择预览模式
  - `security_level=SECRET` 时强制使用 `mode=rendered`
  - 手动切换预览模式（如果权限允许）
  
- [ ] **服务端渲染水印预览** (`src/components/ServerRenderedPreview.tsx`)
  - 调用 `POST /api/archive/preview?mode=rendered&page={pageNumber}`
  - 按页加载带水印的 PDF
  - 分页导航
  - 显示水印元数据（从响应头读取）

**API 集成**:
- `POST /api/archive/preview?mode=rendered&page={pageNumber}` - 服务端渲染水印预览

---

## 📋 前端功能实现清单

### 必须实现（P0）

1. ❌ 跨全宗访问授权票据管理（4个页面）
2. ❌ 全宗沿革管理（2个页面）
3. ❌ 审计证据链验真界面（2个页面）
4. ⚠️ 档案销毁流程增强（4个页面）

### 重要实现（P1）

5. ❌ MFA 多因素认证（2个页面）
6. ❌ 用户生命周期管理（2个页面）
7. ❌ 冻结/保全管理（2个页面）

### 辅助实现（P2）

8. ❌ 文件存储策略配置（2个页面）
9. ❌ 性能指标监控（1个页面）
10. ⚠️ 服务端渲染水印预览支持（增强现有组件）

---

## 🎯 实现优先级建议

### 第一阶段（P0 - 核心业务功能）
1. **跨全宗访问授权票据管理** - 核心安全功能，必须实现
2. **全宗沿革管理** - 核心业务功能，必须实现
3. **审计证据链验真界面** - 合规要求，必须实现
4. **档案销毁流程增强** - 完善现有功能

### 第二阶段（P1 - 安全与合规）
5. **MFA 多因素认证** - 安全增强
6. **用户生命周期管理** - 自动化管理
7. **冻结/保全管理** - 合规要求

### 第三阶段（P2 - 运维与监控）
8. **文件存储策略配置** - 运维配置
9. **性能指标监控** - 运维监控
10. **服务端渲染水印预览支持** - 功能增强

---

## 📝 技术实现建议

### 1. 组件库
- 继续使用 **Ant Design 6**（已在使用）
- 图表使用 **ECharts** 或 **Chart.js**

### 2. 状态管理
- 使用 **React Query**（`@tanstack/react-query`）进行 API 状态管理
- 使用 **Zustand** 或 **Context API** 进行全局状态管理

### 3. 路由管理
- 使用 **React Router**（已在使用）
- 添加新的路由配置

### 4. API 集成
- 创建统一的 API 客户端（`src/api/client.ts`）
- 为每个功能模块创建对应的 API hooks（`src/hooks/useAuthTicketApi.ts` 等）

### 5. 类型定义
- 为所有后端 DTO 创建 TypeScript 类型定义
- 使用 `src/types/` 目录统一管理

---

## 🔧 具体实现步骤

### Step 1: 创建 API 客户端和类型定义
```typescript
// src/api/authTicketApi.ts
export const authTicketApi = {
  apply: (data: AuthTicketApplyRequest) => axios.post('/api/auth-ticket/apply', data),
  list: (params: AuthTicketListParams) => axios.get('/api/auth-ticket/list', { params }),
  approve: (id: string, data: AuthTicketApprovalRequest) => axios.post(`/api/auth-ticket/${id}/approve`, data),
  // ...
};
```

### Step 2: 创建 React Hooks
```typescript
// src/hooks/useAuthTicketApi.ts
export const useAuthTicketList = () => {
  return useQuery({
    queryKey: ['authTickets'],
    queryFn: () => authTicketApi.list({}),
  });
};
```

### Step 3: 创建页面组件
```typescript
// src/pages/security/AuthTicketApplyPage.tsx
export const AuthTicketApplyPage = () => {
  // 实现申请表单
};
```

---

## 📊 工作量估算

| 功能模块 | 页面数 | 预计工作量 | 优先级 |
|---------|--------|-----------|--------|
| 跨全宗访问授权票据 | 4 | 3-5 天 | P0 |
| 全宗沿革管理 | 2 | 2-3 天 | P0 |
| 审计证据链验真 | 2 | 2-3 天 | P0 |
| 档案销毁流程增强 | 4 | 3-4 天 | P0 |
| MFA 多因素认证 | 2 | 2-3 天 | P1 |
| 用户生命周期管理 | 2 | 2-3 天 | P1 |
| 冻结/保全管理 | 2 | 2-3 天 | P1 |
| 文件存储策略配置 | 2 | 2-3 天 | P2 |
| 性能指标监控 | 1 | 2-3 天 | P2 |
| 服务端渲染水印预览 | 1 | 1-2 天 | P2 |

**总计**: 约 22-30 个工作日

---

## ✅ 总结

**前端功能缺失情况**:
- **P0 功能**: 4 个模块，12 个页面，约 10-15 个工作日
- **P1 功能**: 3 个模块，6 个页面，约 6-9 个工作日
- **P2 功能**: 3 个模块，4 个页面，约 5-8 个工作日

**建议**:
1. **立即开始 P0 功能的实现**，确保核心业务功能完整
2. **逐步实现 P1 功能**，提升安全性和合规性
3. **最后实现 P2 功能**，完善运维和监控能力

---

**分析人**: AI Agent  
**分析日期**: 2025-01  
**下次更新**: 建议在实现 P0 功能后更新此报告




