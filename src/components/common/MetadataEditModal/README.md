一旦我所属的文件夹有所变化，请更新我。

# MetadataEditModal 元数据编辑模态框模块

**作用**：文件元数据补录弹窗，符合《会计档案管理办法》财政部79号令规范。

## 文件清单

| 文件 | 类型 | 功能 |
|------|------|------|
| `MetadataEditModal.tsx` | 主组件 | 模态框入口 |
| `ModalHeader.tsx` | 头部组件 | 模态框头部 UI |
| `useFileDetailLoader.ts` | Hook | 文件详情加载与表单初始化 |
| `useMetadataSubmit.ts` | Hook | 表单验证与提交逻辑 |
| `constants.ts` | 常量 | 单据类型选项、默认字段配置 |
| `types.ts` | 类型定义 | FileDetail、MetadataUpdatePayload 等 |
| `index.ts` | 模块导出 | 统一导出入口 |

## 依赖

- `lucide-react` - 图标
- `../modals/FormModal.tsx` - 表单模态框容器
- `../MetadataForm.tsx` - 元数据表单组件
