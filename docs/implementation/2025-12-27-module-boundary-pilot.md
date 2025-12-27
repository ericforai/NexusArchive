# 模块边界试点开发成果（SYS Settings + Borrowing）

> 时间：2025-12-27  
> 目标：完成前端 SYS(Settings) 与后端 Borrowing 试点的模块化落地，并建立强制 guardrails。

---

## 1. 目标与范围

- **前端**：SYS(Settings) 试点，强制模块入口与 DDD-ish 分层（domain/application/infrastructure）。
- **后端**：Borrowing 模块重构为 `api/app/domain/infra`，对外只暴露 `BorrowingFacade` + DTO。
- **Guardrails**：ESLint (boundaries) + ArchUnit，确保边界长期稳定。
- **文档**：补全边界文档、模块说明、目录 README 与导航更新。

---

## 2. 已完成事项

### 2.1 前端（Settings 试点）

- 新结构：`src/features/settings/{domain,application,infrastructure}`。
- 入口统一：`src/features/settings/index.ts` 只导出 application + domain。
- 新增页面容器：`src/pages/settings/*Page.tsx` 与 `SettingsLayoutPage.tsx`。
- Settings 组件改造为“纯展示 + API 注入”：
  - `src/components/settings/*.tsx` 接收 `adminApi/erpApi/licenseApi` 等接口。
  - 接口契约集中：`src/components/settings/types.ts`。
- Fonds 从 SYS 范围移除（Settings Tab/Route 取消）。

### 2.2 后端（Borrowing 模块）

- 完整模块化结构：
  - `com.nexusarchive.modules.borrowing.api`（Controller + DTO）
  - `com.nexusarchive.modules.borrowing.app`（Facade + Application Service）
  - `com.nexusarchive.modules.borrowing.domain`（Entity + Status）
  - `com.nexusarchive.modules.borrowing.infra`（Mapper + Policy 实现）
- 引入 `BorrowingFacade`，Workflow 仅通过 Facade 调用。
- `DataScopeService` 仅保留通用能力；借阅业务规则迁至 `BorrowingScopePolicy`。

### 2.3 Guardrails

- ESLint 使用 `eslint-plugin-boundaries`：
  - pages 只能从 `features/<module>/index.ts` 引入（禁止深路径）。
  - components 禁止依赖 `features/pages/api/store`。
  - routes 仅允许引入 pages + layouts/common/auth。
- ArchUnit 新增 Borrowing 边界测试，限制外部依赖范围。

### 2.4 文档

- `docs/architecture/module-boundaries.md`：边界图 + 合约 + Allowed Imports。
- 前后端 README / 目录说明补齐。
- 新增本成果记录与新人接手指南（见文末链接）。
- 新增自审材料（Module Manifest / Data Ownership Map / Contract Catalog / Variability Registry / SOP）。

---

## 3. 关键结构变化（路径级别）

### 前端

- Settings 模块入口：`src/features/settings/index.ts`
- Settings 领域层：
  - `src/features/settings/domain/`
  - `src/features/settings/application/`
  - `src/features/settings/infrastructure/`
- Settings 页面容器：`src/pages/settings/`
- Settings UI 组件：`src/components/settings/`

### 后端

- Borrowing 模块根：`nexusarchive-java/src/main/java/com/nexusarchive/modules/borrowing/`
  - `api/`（Controller + DTO）
  - `app/`（Facade + Application Service）
  - `domain/`（Entity / Status）
  - `infra/`（Mapper / ScopePolicy 实现）

---

## 4. 关键引用替换点

- Workflow 侧：`WorkflowServiceImpl` -> `BorrowingFacade.approveBorrowing(...)`
- API 侧：`BorrowingController` 仅依赖 `BorrowingFacade` + DTO
- Settings 页面：仅从 `src/features/settings/index.ts` 引入业务 hooks

---

## 5. 验证与运行记录

- `npm install`：成功，但有 Node 版本兼容警告（jsdom/vitest）。
- `npm run lint`：✅ 通过（已补齐 `eslint.config.cjs`）。
- `npm run typecheck`：✅ 通过（新增脚本，覆盖 `src` 与 `tests`）。
- Playwright 冒烟：✅ 通过（Settings/Borrowing/Archive 详情弹窗）。
  - 运行时需设置 `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=mac15-arm64`（macOS Headless 环境）。
- 后端：`mvn -Dtest=... clean test` ✅ 通过（ModuleBoundaryTest / BorrowingApplicationServiceTest / BorrowingControllerTest）。

---

## 6. 待处理 / 风险项

1. **Playwright 运行环境**  
   - CI 需提前执行 `npx playwright install chromium`。  
   - macOS Headless 环境请设置 `PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=mac15-arm64`。

2. **Typecheck 作用域**  
   - 当前 `tsc --noEmit` 覆盖 `src` + `tests`。  
   - 若 CI 仅需生产代码，可单独拆分 `tsconfig` 或脚本。

---

## 7. 相关文档

- 模块边界：`docs/architecture/module-boundaries.md`
- 前端边界：`docs/architecture/frontend-boundaries.md`
- 新人接手指南：`docs/guides/新人接手指南.md`
