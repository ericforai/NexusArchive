# 预览组件库

统一的文件预览组件，支持多种文件格式的预览。

## 问题修复

**修复了 "Not Found" 预览错误：**
- 之前：各页面自己拼接 URL (`/api/pool/preview/${id}`)，导致错误
- 现在：统一通过 `previewApi.getPreview()`，自动处理权限和水印

## 组件列表

### SmartFilePreview ⭐ 推荐

**智能文件预览器**，自动调用 previewApi 获取文件内容。

**Features:**
- ✅ 统一调用 previewApi（权限 + 水印）
- ✅ 支持多文件切换
- ✅ 缩放、旋转
- ✅ 友好错误处理 + 重试按钮
- ✅ 自动下载功能

**Usage:**
```tsx
import { SmartFilePreview } from '@/components/preview';

// 单文件预览
<SmartFilePreview
  archiveId="archive-123"
  fileId="file-456"
  fileName="凭证.pdf"
/>

// 多文件预览（支持切换）
<SmartFilePreview
  archiveId="archive-123"
  files={[
    { id: 'file-1', fileName: '主文件.pdf' },
    { id: 'file-2', fileName: '附件.jpg' },
  ]}
  currentFileId="file-1"
  onFileChange={(id) => console.log('切换到:', id)}
/>
```

---

### FilePreviewModal

**预览 Modal**（基于 BaseModal），用于弹出式预览。

**Usage:**
```tsx
import { FilePreviewModal } from '@/components/preview';

const [showPreview, setShowPreview] = useState(false);
const [selectedFileId, setSelectedFileId] = useState('file-1');

<FilePreviewModal
  isOpen={showPreview}
  onClose={() => setShowPreview(false)}
  archiveId="archive-123"
  files={files}
  currentFileId={selectedFileId}
  onFileChange={setSelectedFileId}
  maxWidth="4xl"
/>
```

---

### useFilePreview Hook

底层 Hook，用于自定义预览场景。

**Usage:**
```tsx
import { useFilePreview } from '@/components/preview';

const { blobUrl, watermark, loading, error, retry } = useFilePreview({
  archiveId: 'archive-123',
  fileId: 'file-456',
  mode: 'stream',  // stream | presigned | rendered
});

if (loading) return <Spinner />;
if (error) return <Error message={error} retry={retry} />;
return <iframe src={blobUrl} />;
```

---

### FilePreview (传统)

**基础预览器**，需要自己传入 URL（不推荐用于档案预览）。

```tsx
// ❌ 不推荐：需要自己构建 URL
<FilePreview url={constructUrl(...)} />

// ✅ 推荐：让组件自己获取
<SmartFilePreview archiveId="xxx" />
```

---

### PdfViewer & ImageViewer

底层查看器，通常不需要直接使用，由 SmartFilePreview 自动选择。

---

## 迁移指南

### 从 ArchiveDetailModal 的 OfdViewer 迁移

**之前（有问题）：**
```tsx
// ❌ 直接拼接 URL，会报 Not Found
<OfdViewer
  fileUrl={getPreviewUrl(activePreviewId, isPoolView)}  // 拼接的 URL
  fileName={fileName}
  fileType={type}
/>
```

**现在（修复）：**
```tsx
// ✅ 使用 SmartFilePreview，自动调用 previewApi
<SmartFilePreview
  archiveId={row.archiveId || row.id}
  fileId={activePreviewId}
  fileName={fileName}
/>
```

---

### 从 ArchivePreviewModal 迁移

**之前：**
```tsx
import { ArchivePreviewModal } from '@/pages/preview/ArchivePreviewModal';

<ArchivePreviewModal
  visible={show}
  onCancel={() => setShow(false)}
  archiveId="xxx"
/>
```

**现在：**
```tsx
import { FilePreviewModal } from '@/components/preview';

<FilePreviewModal
  isOpen={show}
  onClose={() => setShow(false)}
  archiveId="xxx"
/>
```

---

## 待办页面迁移

| 页面 | 当前状态 | 迁移目标 |
|------|---------|---------|
| 预归档库-凭证关联 | ❌ Not Found | ✅ 使用 SmartFilePreview |
| 档案列表 → 详情 | ❌ Not Found | ✅ 使用 SmartFilePreview |
| 电子凭证池 | ⚠️ 缺少预览按钮 | ✅ 添加 FilePreviewModal |
| 全景视图 | ⚠️ 不稳定 | ✅ 使用 SmartFilePreview |

---

## API 参考

### SmartFilePreviewProps

| 属性 | 类型 | 必填 | 说明 |
|------|------|------|------|
| archiveId | string | ✅ | 档案 ID |
| fileId | string | | 文件 ID |
| fileName | string | | 文件名 |
| files | FileItem[] | | 文件列表（用于切换） |
| currentFileId | string | | 当前选中的文件 ID |
| onFileChange | (id: string) => void | | 文件切换回调 |
| showToolbar | boolean | | 显示工具栏（默认 true） |
| showFileNav | boolean | | 显示文件切换按钮（默认 true） |
| mode | 'stream' \| 'presigned' \| 'rendered' | | 预览模式（默认 stream） |

### useFilePreview 返回值

| 属性 | 类型 | 说明 |
|------|------|------|
| blobUrl | string \| null | Blob URL（用于 iframe/img） |
| presignedUrl | string \| null | 预签名 URL（presigned 模式） |
| watermark | WatermarkMetadata \| null | 水印元数据 |
| traceId | string \| null | 追踪 ID |
| loading | boolean | 加载状态 |
| error | string \| null | 错误信息 |
| retry | () => void | 重试加载 |

---

## 收益

- ✅ **修复 Not Found 错误**：统一 API 调用
- ✅ **统一的预览体验**：所有页面使用相同组件
- ✅ **减少重复代码**：删除 4+ 个重复的预览组件
- ✅ **支持权限和水印**：自动处理合规需求
