# components/matching/

智能凭证关联前端组件。

一旦我所属的文件夹有所变化，请更新我。

## 组件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `VoucherMatchingView.tsx` | 主视图 | 智能关联主界面，支持异步匹配、结果展示、候选选择 |
| `VoucherMatchingView.css` | 样式 | 主视图样式 |
| `OnboardingWizard.tsx` | 向导组件 | 初始化配置向导（扫描→应用规则→确认） |
| `ComplianceReport.tsx` | 报告组件 | 合规报告（统计+待补证清单） |

## 功能说明

### VoucherMatchingView
- 异步执行匹配（轮询 taskId）
- 展示匹配结果（场景、状态、关联详情）
- 候选选择弹窗（NEED_CONFIRM 状态）
- 缺失警告（PENDING 状态）

### OnboardingWizard
- Step 1: 扫描客户数据
- Step 2: 应用预置规则
- Step 3: 确认映射
- Step 4: 完成

### ComplianceReport
- 统计卡片（总数、已匹配、待补证、合规率）
- 待补证清单（支持筛选和导出）
