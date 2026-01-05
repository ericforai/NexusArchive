<!-- 一旦我所属的文件夹有所变化，请更新我。-->
<!-- 本目录存放开发辅助组件。-->
<!-- 仅在开发环境使用，生产环境无副作用。-->
<!-- 最后更新: 2025-01-05 -->

## 用途

本目录存放开发环境专用的辅助组件，包括：
- 文档守卫（代码变更后提醒更新文档）
- 其他开发调试工具

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `DocumentationGuardProvider.tsx` | 组件 | 文档守卫 Provider，自动监控代码变更并提醒更新文档 |

## 使用方式

在 `App.tsx` 中引入即可，开发环境自动生效：

```tsx
import { DocumentationGuardProvider } from './components/dev/DocumentationGuardProvider';

const App = () => (
  <>
    <DocumentationGuardProvider />
    {/* 其他内容 */}
  </>
);
```
