# 端到端测试执行指南

> **创建日期**: 2025-01  
> **适用范围**: P0 + P1 功能完整测试

---

## 📋 快速开始

### 1. 环境准备

确保以下服务正在运行：

```bash
# 前端服务
npm run dev
# 访问地址: http://localhost:15175

# 后端服务
cd nexusarchive-java && mvn spring-boot:run
# 访问地址: http://localhost:19090/api
```

### 2. 安装测试依赖

```bash
# 安装Node.js依赖（包含Playwright）
npm install

# 安装Playwright浏览器
npx playwright install
```

### 3. 运行测试

#### 方式一：使用测试脚本（推荐）

```bash
./scripts/run-e2e-tests.sh
```

脚本会提示选择测试模式：
- **快速冒烟测试** - 验证关键页面可访问
- **P0功能测试** - 核心功能完整测试
- **P1功能测试** - 重要功能完整测试
- **完整测试套件** - 执行所有测试
- **API集成测试** - 仅API测试
- **业务流程测试** - 端到端流程测试

#### 方式二：直接使用Playwright命令

```bash
# 快速冒烟测试
npm run test:smoke

# 所有UI测试
npx playwright test tests/playwright/ui/

# 所有API测试
npx playwright test tests/playwright/api/

# 特定测试文件
npx playwright test tests/playwright/ui/legacy_import.spec.ts

# 带标签的测试
npx playwright test --grep "@P0"

# 查看测试报告
npx playwright show-report
```

---

## 🎯 测试执行顺序建议

### 第一阶段：快速验证（30分钟）

1. **快速冒烟测试**
   ```bash
   npm run test:smoke
   ```
   - 验证关键页面可访问
   - 验证登录功能正常
   - 验证基础交互正常

### 第二阶段：API测试（1-2小时）

2. **API集成测试**
   ```bash
   npx playwright test tests/playwright/api/
   ```
   - 验证所有API端点
   - 验证请求/响应格式
   - 验证错误处理

### 第三阶段：P0功能测试（2-3小时）

3. **P0核心功能测试**
   ```bash
   npx playwright test tests/playwright/ui/ --grep "@P0"
   ```
   - 历史数据导入
   - 跨全宗访问授权票据
   - 审计证据链验真
   - 档案销毁流程

### 第四阶段：P1功能测试（1-2小时）

4. **P1重要功能测试**
   ```bash
   npx playwright test tests/playwright/ui/ --grep "@P1"
   ```
   - MFA多因素认证
   - 用户生命周期管理
   - 冻结/保全管理

### 第五阶段：业务流程测试（2-3小时）

5. **端到端业务流程测试**
   ```bash
   npx playwright test tests/playwright/workflows/
   ```
   - 数据迁移完整流程
   - 跨全宗访问完整流程
   - 档案销毁完整流程
   - 审计验真完整流程

---

## 🔧 测试配置

### 环境变量

创建 `.env.test` 文件（可选）：

```bash
BASE_URL=http://localhost:15175
PW_USER=admin
PW_PASS=admin123
```

或使用环境变量：

```bash
export BASE_URL=http://localhost:15175
export PW_USER=admin
export PW_PASS=admin123
```

### Playwright配置

配置文件: `playwright.config.ts`

主要配置项：
- `baseURL`: 前端服务地址（默认: http://localhost:3000，需要修改为 15175）
- `headless`: 是否无头模式（默认: true）
- `timeout`: 超时时间（默认: 30s）

---

## 📊 测试报告

### 查看测试报告

```bash
# 自动打开HTML报告
npx playwright show-report

# 生成JSON报告
npx playwright test --reporter=json

# 生成JUnit XML报告（CI/CD）
npx playwright test --reporter=junit
```

### 报告内容

测试报告包含：
- **测试概览**: 执行时间、通过/失败统计
- **详细结果**: 每个测试用例的执行结果
- **错误信息**: 失败测试的详细错误和堆栈
- **截图/视频**: 失败测试的截图和视频（如果启用）
- **执行时间**: 每个测试的执行时间

---

## 🐛 调试测试

### 调试模式运行

```bash
# UI模式运行（有浏览器界面）
npx playwright test --headed

# 调试模式（暂停执行）
npx playwright test --debug

# 追踪模式（记录操作）
npx playwright test --trace on
```

### 查看追踪

```bash
# 打开追踪查看器
npx playwright show-trace trace.zip
```

### 常用调试技巧

1. **添加断点**
   ```typescript
   await page.pause(); // 暂停执行
   ```

2. **查看页面状态**
   ```typescript
   await page.screenshot({ path: 'debug.png' });
   console.log(await page.content());
   ```

3. **慢速执行**
   ```bash
   npx playwright test --slow-mo=1000
   ```

---

## ✅ 测试检查清单

执行测试前，请确认：

- [ ] 前端服务正在运行 (http://localhost:15175)
- [ ] 后端服务正在运行 (http://localhost:19090/api)
- [ ] 数据库连接正常
- [ ] 测试用户账号可用（admin/admin123）
- [ ] 测试数据已准备（如有需要）

测试执行后，检查：

- [ ] 所有P0测试通过
- [ ] 所有P1测试通过
- [ ] 关键业务流程测试通过
- [ ] 没有阻塞性问题
- [ ] 测试报告已生成

---

## 🔗 相关文档

- **测试计划**: `docs/testing/e2e-test-plan.md`
- **测试框架文档**: `tests/README.md`
- **Playwright文档**: https://playwright.dev/

---

**文档版本**: v1.0  
**最后更新**: 2025-01


