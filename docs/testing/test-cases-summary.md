# 测试用例创建总结

> **创建日期**: 2025-01  
> **状态**: 测试用例已创建，待执行

---

## ✅ 已创建的测试用例

### P0 功能测试用例（5个文件）

1. **`tests/playwright/ui/legacy_import.spec.ts`** - 历史数据导入
   - 页面访问测试
   - 标签页切换测试
   - 文件上传测试（需要实际文件）
   - 导入历史列表测试

2. **`tests/playwright/ui/auth_ticket.spec.ts`** - 跨全宗访问授权票据
   - 列表页面访问测试
   - 申请页面访问测试
   - 列表显示和筛选测试
   - 详情查看测试

3. **`tests/playwright/ui/audit_verification.spec.ts`** - 审计证据链验真
   - 页面访问测试
   - 验证方式选项测试
   - 单条验证测试
   - 验证结果展示测试

4. **`tests/playwright/ui/audit_evidence_package.spec.ts`** - 审计证据包导出
   - 页面访问测试
   - 导出条件选择测试
   - 导出历史查询测试
   - 导出按钮测试

5. **`tests/playwright/ui/destruction_workflow.spec.ts`** - 档案销毁流程
   - 到期档案识别页面测试
   - 鉴定清单页面测试
   - 销毁审批页面测试
   - 销毁执行页面测试
   - 筛选选项测试

### P1 功能测试用例（3个文件）

6. **`tests/playwright/ui/mfa.spec.ts`** - MFA多因素认证
   - MFA设置页面访问测试
   - MFA状态显示测试
   - 启用MFA按钮测试
   - 二维码显示测试

7. **`tests/playwright/ui/user_lifecycle.spec.ts`** - 用户生命周期管理
   - 用户生命周期管理页面测试
   - 标签页显示测试（入职、离职、调岗）
   - 权限定期复核页面测试
   - 任务列表和筛选测试

8. **`tests/playwright/ui/freeze_hold.spec.ts`** - 冻结/保全管理
   - 冻结/保全管理页面测试
   - 列表显示测试
   - 申请按钮测试
   - 筛选选项测试
   - 详情页面测试

---

## 📝 测试用例特点

### 1. 健壮性设计
- 所有测试用例都包含登录逻辑
- 使用灵活的选择器策略（支持多种可能的UI结构）
- 包含条件判断，避免因数据为空而失败
- 使用合理的超时时间

### 2. 测试覆盖
- **页面访问**: 验证所有关键页面可以正常访问
- **UI元素**: 验证关键UI元素存在和可见
- **交互功能**: 验证按钮、筛选、列表等基本交互
- **导航流程**: 验证页面间的导航

### 3. 标签标记
- 所有P0测试用例标记为 `@P0`
- 所有P1测试用例标记为 `@P1`
- 便于按优先级执行测试

---

## ⚠️ 注意事项

### 1. 路由配置
部分页面（MFA、用户生命周期、冻结/保全）的路由可能尚未完全配置到路由表中。测试用例使用了灵活的导航策略，但建议：

- 确认所有页面的路由路径已配置
- 如果需要，更新测试用例中的路径

### 2. 测试数据
- 部分测试需要测试数据（如列表数据）
- 建议准备基础的测试数据
- 或者使用测试fixtures提供数据

### 3. 服务依赖
- 确保前端服务运行在 `http://localhost:15175`
- 确保后端服务运行在 `http://localhost:19090/api`
- 确保数据库连接正常

---

## 🚀 执行测试

### 方式一：执行所有P0测试
```bash
npx playwright test tests/playwright/ui/ --grep "@P0"
```

### 方式二：执行所有P1测试
```bash
npx playwright test tests/playwright/ui/ --grep "@P1"
```

### 方式三：执行所有新创建的测试
```bash
npx playwright test tests/playwright/ui/legacy_import.spec.ts \
  tests/playwright/ui/auth_ticket.spec.ts \
  tests/playwright/ui/audit_verification.spec.ts \
  tests/playwright/ui/audit_evidence_package.spec.ts \
  tests/playwright/ui/destruction_workflow.spec.ts \
  tests/playwright/ui/mfa.spec.ts \
  tests/playwright/ui/user_lifecycle.spec.ts \
  tests/playwright/ui/freeze_hold.spec.ts
```

### 方式四：使用测试脚本
```bash
./scripts/run-e2e-tests.sh
```

---

## 📊 测试统计

| 类别 | 测试文件数 | 测试用例数（估计） | 优先级 |
|------|-----------|-----------------|--------|
| P0功能测试 | 5 | ~20-25 | P0 |
| P1功能测试 | 3 | ~12-15 | P1 |
| **总计** | **8** | **~32-40** | - |

---

## 🔄 下一步

1. **添加路由配置**（如需要）
   - 确认MFA设置页面路由
   - 确认用户生命周期管理页面路由
   - 确认冻结/保全管理页面路由

2. **准备测试数据**
   - 创建基础测试数据
   - 准备测试文件（CSV/Excel）

3. **执行测试**
   - 先执行快速冒烟测试验证环境
   - 然后按优先级执行功能测试

4. **完善测试用例**
   - 根据实际UI调整选择器
   - 添加更多交互测试
   - 添加错误场景测试

---

**文档版本**: v1.0  
**最后更新**: 2025-01





