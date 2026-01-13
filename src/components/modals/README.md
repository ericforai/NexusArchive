一旦我所属的文件夹有所变化，请更新我。

# Modal 组件库

统一的模态框组件，减少重复代码 70%+。

## 组件列表

### BaseModal
基础模态框组件，提供 backdrop、header、footer 结构。

**Usage:**
```tsx
import { BaseModal } from '@/components/modals/BaseModal';

<BaseModal
  isOpen={open}
  onClose={() => setOpen(false)}
  title="标题"
  footer={<button>操作</button>}
>
  内容
</BaseModal>
```

### ConfirmModal
确认对话框，支持 info/success/warning/danger 四种变体。

**Usage:**
```tsx
import { ConfirmModal } from '@/components/modals/ConfirmModal';

<ConfirmModal
  isOpen={open}
  onClose={() => setOpen(false)}
  onConfirm={handleConfirm}
  title="确认删除？"
  description="此操作不可撤销"
  variant="danger"
/>
```

### FormModal
表单模态框，自动处理表单提交和错误显示。

**Usage:**
```tsx
import { FormModal } from '@/components/modals/FormModal';

<FormModal
  isOpen={open}
  onClose={() => setOpen(false)}
  onSubmit={handleSubmit}
  isSubmitting={loading}
  error={error}
  title="编辑信息"
>
  <input name="field" />
</FormModal>
```

### DetailModal
详情模态框，适用于展示档案、凭证等详情信息。

**Usage:**
```tsx
import { DetailModal } from '@/components/modals/DetailModal';

<DetailModal
  isOpen={open}
  onClose={() => setOpen(false)}
  title="档案详情"
  subtitle={fileName}
  details={<div>详情内容</div>}
/>
```

## 迁移指南

1. 将现有 Modal 组件的 backdrop、header、footer 代码替换为对应的 BaseModal
2. 使用 ConfirmModal 替代简单的确认对话框
3. 使用 FormModal 包装表单内容
4. 使用 DetailModal 展示详情信息

## 收益

- 减少重复代码 70%+
- 统一的模态框样式和行为
- 提高用户体验一致性
