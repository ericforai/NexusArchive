一旦我所属的文件夹有所变化，请更新我。
本目录包含测试说明与测试脚本文档入口。
用于汇总 Playwright、k6 与测试报告。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `ADVANCED_TEST_RESULTS.md` | 报告文档 | 高级测试结果汇总 |
| `BUG_ANALYSIS.md` | 报告文档 | 缺陷分析与原因梳理 |
| `BUG_REPORT.md` | 报告文档 | 缺陷报告与记录 |
| `FINAL_TEST_REPORT.md` | 报告文档 | 最终测试报告 |
| `README.md` | 说明文档 | 测试使用说明 |
| `TEST_RESULTS.md` | 报告文档 | 测试结果记录 |
| `TEST_SUMMARY.md` | 报告文档 | 测试摘要概览 |
| `fixtures/` | 目录入口 | 测试数据与夹具 |
| `playwright/` | 目录入口 | Playwright 测试集合 |

# 测试脚本说明

本文档说明如何运行电子会计档案系统的测试用例。

## 目录结构

```
tests/
├── playwright/
│   ├── api/                    # API 测试脚本
│   │   ├── archive_concurrency.spec.ts      # 归档并发与存储保护
│   │   ├── storage_quota_guard_spec.ts      # 存储空间耗尽保护
│   │   ├── four_integrities.spec.ts         # 四性校验与 TSA
│   │   ├── sign_timestamp_spec.ts            # 签章与时间戳
│   │   ├── authz_audit.spec.ts              # 权限与审计
│   │   ├── search_index_spec.ts             # 检索性能与索引一致性
│   │   ├── backup_restore_spec.ts           # 备份恢复
│   │   ├── erp_sync.spec.ts                 # ERP 对接
│   │   ├── batch_import_export_spec.ts       # 批量导入导出
│   │   ├── workflow_borrow_destroy_spec.ts   # 借阅销毁流程
│   │   ├── ocr_metadata_spec.ts            # OCR 与元数据
│   │   └── report_version_spec.ts           # 报表与版本管理
│   ├── ui/                      # UI 测试脚本
│   │   └── search_preview.spec.ts           # 搜索预览
│   └── utils/                   # 测试工具
│       └── auth.ts                          # 认证工具
└── README.md                    # 本文档

perf/
├── search_peak.k6.js            # 检索压测（50 并发，2h）
├── archive_soak.k6.js          # 归档压测（72h 持续）
├── upload_1gb.k6.js            # 大文件上传压测
├── import_100k.k6.js           # 批量导入压测（10 万条）
├── erp_flaky_network.k6.js     # ERP 网络抖动压测
├── backup_restore_cycle.sh     # 备份恢复链路脚本
└── upgrade_rollback.sh         # 升级回滚脚本
```

## 环境准备

### 1. 安装依赖

```bash
# 安装 Node.js 依赖（包含 Playwright）
npm install

# 安装 Playwright 浏览器
npx playwright install

# 安装 k6（压测工具）
# macOS
brew install k6
# 或下载二进制：https://k6.io/docs/getting-started/installation/
```

### 2. 环境变量

创建 `.env.test` 文件（或使用环境变量）：

```bash
BASE_URL=http://localhost:8080  # 后端 API 地址
PW_USER=admin                    # 测试用户名
PW_PASS=admin123                 # 测试密码
TOKEN=                           # 可选：直接使用 token（跳过登录）
```

## 运行测试

### Playwright API 测试

```bash
# 运行所有 API 测试
npx playwright test tests/playwright/api

# 运行特定测试文件
npx playwright test tests/playwright/api/archive_concurrency.spec.ts

# 运行 P0 优先级测试（通过标签筛选）
npx playwright test --grep "@P0"

# 查看测试报告
npx playwright show-report
```

### Playwright UI 测试

```bash
# 运行 UI 测试
npx playwright test tests/playwright/ui

# 以有头模式运行（调试用）
npx playwright test --headed
```

### k6 压测

```bash
# 检索压测（50 并发，2 小时）
k6 run perf/search_peak.k6.js

# 归档压测（72 小时持续）
k6 run perf/archive_soak.k6.js

# 批量导入压测（10 万条数据）
k6 run perf/import_100k.k6.js

# ERP 网络抖动压测
k6 run perf/erp_flaky_network.k6.js --env SCENARIO_ID=1

# 自定义参数
BASE_URL=http://localhost:8080 TOKEN=xxx k6 run perf/search_peak.k6.js
```

### Shell 脚本

```bash
# 备份恢复链路测试
BASE_URL=http://localhost:8080 TOKEN=xxx ./perf/backup_restore_cycle.sh

# 升级回滚测试
BASE_URL=http://localhost:8080 OLD_VERSION=1.0.0 NEW_VERSION=2.0.0 ./perf/upgrade_rollback.sh
```

## 测试用例说明

### P0 优先级测试（核心功能）

| 测试文件 | 测试内容 | 状态 |
|---------|---------|------|
| `archive_concurrency.spec.ts` | 并发归档、防覆盖 | ✅ 已实现 |
| `storage_quota_guard_spec.ts` | 存储耗尽保护、特殊字符 | ✅ 已实现 |
| `four_integrities.spec.ts` | 四性检测、TSA 超时 | ✅ 已实现（部分跳过） |
| `sign_timestamp_spec.ts` | 签章、时间戳 | ⏸️ 需后端支持 |
| `authz_audit.spec.ts` | 权限、审计日志 | ✅ 已实现 |
| `search_index_spec.ts` | 检索性能、索引一致性 | ✅ 已实现 |
| `backup_restore_spec.ts` | 备份恢复 | ⏸️ 需后端支持 |
| `erp_sync.spec.ts` | ERP 对接、幂等 | ✅ 已实现（部分跳过） |

### P1 优先级测试（次核心功能）

| 测试文件 | 测试内容 | 状态 |
|---------|---------|------|
| `batch_import_export_spec.ts` | 批量导入导出 | ⏸️ 需后端支持 |
| `workflow_borrow_destroy_spec.ts` | 借阅销毁流程 | ✅ 已实现 |
| `ocr_metadata_spec.ts` | OCR 识别 | ⏸️ 需后端支持 |
| `report_version_spec.ts` | 报表、版本管理 | ✅ 已实现（部分跳过） |

## 注意事项

1. **跳过测试**：部分测试用例使用 `test.skip()` 标记，因为需要后端提供特定接口（如存储模拟、TSA 开关等）。当后端实现这些功能后，可以移除 `skip` 标记。

2. **数据准备**：部分测试需要预置数据（如档案 ID、ERP 场景 ID）。如果测试跳过，请检查是否需要准备测试数据。

3. **压测环境**：k6 压测脚本应在隔离环境中运行，避免影响生产环境。建议使用独立的测试服务器。

4. **权限要求**：部分测试需要管理员权限。确保测试账号具有相应权限。

5. **网络延迟**：压测脚本中的延迟和阈值可能需要根据实际环境调整。

## CI/CD 集成

### GitHub Actions 示例

```yaml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm install
      - run: npx playwright install --with-deps
      - run: npx playwright test tests/playwright/api
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

## 故障排除

### 测试失败常见原因

1. **登录失败**：检查 `PW_USER` 和 `PW_PASS` 环境变量
2. **API 不可用**：确认后端服务已启动，`BASE_URL` 配置正确
3. **权限不足**：检查测试账号权限
4. **数据不存在**：部分测试需要预置数据，检查测试数据准备

### 调试技巧

```bash
# 以调试模式运行单个测试
npx playwright test tests/playwright/api/archive_concurrency.spec.ts --debug

# 查看详细日志
DEBUG=pw:api npx playwright test

# 生成追踪文件
npx playwright test --trace on
```

## 参考文档

- [测试用例文档](../../docs/guides/电子会计档案系统测试用例.md)
- [Playwright 文档](https://playwright.dev/)
- [k6 文档](https://k6.io/docs/)











