# 报销单与原始凭证数字关联设计方案 (Reimbursement & Voucher Association Design)

本方案旨在解决报销单与大量原始凭证在数字化环境下的关联问题，替换传统的线下人工装订模式。

## 用户审查需求 (User Review Required)

> [!IMPORTANT]
> **关于纸质原件保存的合规说明（依据 2024 修订版《会计法》及财会〔2020〕6号）**：
> 1. **电子原件**（如 OFD/XML 电子发票）：若系统满足“四性检测”及防重复入账要求，可**仅保存电子档**，无需打印保存纸质件。
> 2. **纸质原件**（如纸质发票、手写收据）：数字化扫描件仅视为“电子副本”，**必须继续妥善保存纸质原件**。
> 3. **双轨并行**：在 2026 年全面电子化目标达成前，系统需支持“电子原件归档”与“纸质原件扫描挂接”两种模式并行。

## 拟议变更 (Proposed Changes)

### 后端模块 (Backend)

#### [MODIFY] [OriginalVoucherController.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/controller/OriginalVoucherController.java)
- 利用现有的 `POST /{id}/relations` 接口支持单条关联。
- **新增**：`POST /batch-bind` 接口，支持将多个原始凭证 ID 一次性绑定到指定的报销单（记账凭证）。

#### [MODIFY] [OriginalVoucherService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/OriginalVoucherService.java)
- 实现批量绑定逻辑，包含幂等性检查和权限校验。
- 强化 OCR 自动填充逻辑，记录 OCR 识别出的发票号作为关键关联维度。

### 前端模块 (Frontend)

#### [NEW] [VoucherBindingModule.tsx](file:///Users/user/nexusarchive/src/pages/matching/VoucherBindingModule.tsx)
- 提供一个“关联工作台”，左侧显示报销单（记账凭证），右侧显示原始凭证池。
- 支持拖拽或多选确认进行绑定。

#### [MODIFY] [BatchUploadView.tsx](file:///Users/user/nexusarchive/src/pages/collection/BatchUploadView.tsx)
- 在上传成功后的确认页面，增加“立即关联到单据”的快捷入口。

## 验证计划 (Verification Plan)

### 自动化测试
- 运行 `OriginalVoucherServiceTest` 验证批量绑定逻辑。
- 执行 `FourNatureCheckServiceTest` 确保关联后原始凭证的合规性检测通过。

### 手动验证
1. 进入“采集模块”，上传一张 PDF 电子发票，观察 OCR 是否自动识别金额。
2. 进入“关联模块”，选择一张已归档的报销单。
3. 从待关联池中选择该发票，点击“确认关联”。
4. 在报销单详情面，点击“查看关联原件”，确保能正确预览该发票。
