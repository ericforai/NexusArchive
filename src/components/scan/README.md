一旦我所属的文件夹有所变化，请更新我。

// Input: React 扫描组件
// Output: 扫描集成组件目录说明
// Pos: src/components/scan/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# 扫描集成组件 (Scan)

本目录包含扫描工作区相关的 React 组件。

## 目录结构

```
src/components/scan/
├── index.ts                    # 公共 API 入口
├── manifest.config.ts          # 模块清单
├── README.md                   # 本文件
└── FolderMonitorDialog.tsx     # 监控文件夹设置对话框
```

## 组件说明

### FolderMonitorDialog

**位置**: `FolderMonitorDialog.tsx`  
**类型**: React.FC  
**功能**: 监控文件夹设置对话框

#### Props

| 属性 | 类型 | 说明 |
|------|------|------|
| `open` | `boolean` | 是否显示对话框 |
| `onClose` | `() => void` | 关闭回调 |
| `onSuccess` | `() => void` | 成功回调 |

#### 功能

- 添加监控文件夹
- 删除监控文件夹
- 启用/暂停监控
- 文件类型过滤配置
- 导入后删除源文件选项

## 使用方式

```typescript
import { FolderMonitorDialog } from '@components/scan';

function MyComponent() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button onClick={() => setIsOpen(true)}>打开设置</button>
      <FolderMonitorDialog
        open={isOpen}
        onClose={() => setIsOpen(false)}
        onSuccess={() => console.log('配置已更新')}
      />
    </>
  );
}
```

## 依赖

- `lucide-react`: 图标库
- `../../utils/notificationService`: 通知服务
- 扫描工作区 API（通过父组件传入）

## 架构合规

- ✅ 有 `manifest.config.ts` 模块清单
- ✅ 有 `index.ts` 公共 API 入口
- ✅ 使用路径别名 `@components/scan` 导入
- ✅ 符合架构防御规范
