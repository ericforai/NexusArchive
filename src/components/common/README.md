一旦我所属的文件夹有所变化，请更新我。
本目录存放通用可复用组件。
用于通用展示、错误处理与报表查看。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `ComplianceRadar.tsx` | 通用组件 | 合规雷达图展示 |
| `DemoBadge.tsx` | 通用组件 | 演示标识提示 |
| `ErrorBoundary.tsx` | 通用组件 | 错误边界与兜底 |
| `FondsSwitcher.tsx` | 通用组件 | 全宗切换（自适应显示） |
| `MetadataEditModal.tsx` | 通用组件 | 元数据编辑弹窗 |
| `MetadataForm.tsx` | 通用组件 | 元数据表单 |
| `MetadataFormField.tsx` | 通用组件 | 元数据表单字段 |
| `OfdViewer.tsx` | 通用组件 | OFD 文件预览 |
| `ReconciliationReport.tsx` | 通用组件 | 对账报告展示 |
| `ToastContainer.tsx` | 通用组件 | 全局提示容器 |
| `index.ts` | 聚合入口 | 通用组件导出 |

---

## FondsSwitcher 全宗切换组件

根据用户权限显示全宗信息，支持自适应显示。

### 行为模式

| 场景 | 显示样式 |
| --- | --- |
| **单个全宗** | 纯文本显示（全宗名称 + 全宗代码），无图标，无下拉 |
| **多个全宗** | 下拉按钮（Building2 图标 + ChevronDown），可点击切换 |
| **无全宗** | 显示 "暂无全宗权限" 灰色提示 |
| **加载中** | 显示旋转的 Loader2 图标 |

### Props

```typescript
interface FondsSwitcherProps {
    currentFonds: Fonds | null;      // 当前选中的全宗
    fondsList: Fonds[];              // 全宗列表
    isLoading: boolean;              // 是否正在加载
    hasHydrated: boolean;            // 是否已从持久化存储中恢复
    onLoadFondsList: () => void;     // 加载全宗列表的回调
    onSetCurrentFonds: (fonds: Fonds) => void; // 设置当前全宗的回调
}
```
