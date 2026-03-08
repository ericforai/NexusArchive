# OFD Preview Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在真实档案预览入口中接入可用的 OFD 在线预览，并让 OFD 与 PDF/图片文件走统一的前端预览与错误回退流程。

**Architecture:** 先在 `src/components/preview` 内补一个轻量 OFD 解析/渲染层，负责读取 OFD Zip、解析页面与资源并渲染图片/文本页。然后把 `SmartFilePreview` 接入这个查看器，并将 `src/pages/panorama/EvidencePreview.tsx` 切换到统一预览链路，避免继续走手写 `iframe`/`fetch` 分叉。浏览器不支持解压或 OFD 结构异常时，展示明确错误与下载回退。

**Tech Stack:** React 18, TypeScript, Vite, Vitest, Testing Library, 原生 `DecompressionStream`, DOMParser

---

### Task 1: 锁定 OFD 解析能力

**Files:**
- Create: `src/components/preview/ofdParser.ts`
- Create: `src/components/preview/__tests__/ofdParser.test.ts`

**Step 1: Write the failing test**

```typescript
it('parses image-based OFD pages from fixture', async () => {
  const buffer = await readFixture('nexusarchive-java/data/temp/uploads/test_upload.ofd');
  const doc = await parseOfdDocument(buffer);

  expect(doc.pages).toHaveLength(1);
  expect(doc.pages[0].images).toHaveLength(1);
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test:run -- src/components/preview/__tests__/ofdParser.test.ts`
Expected: FAIL with missing parser module/export.

**Step 3: Write minimal implementation**

```typescript
export async function parseOfdDocument(buffer: ArrayBuffer) {
  // 1. 解析 zip central directory
  // 2. 解压 OFD.xml / Document.xml / Content.xml / 资源文件
  // 3. 返回页面尺寸、图片对象、文本对象
}
```

**Step 4: Run test to verify it passes**

Run: `npm run test:run -- src/components/preview/__tests__/ofdParser.test.ts`
Expected: PASS

**Step 5: Commit**

```bash
git add src/components/preview/ofdParser.ts src/components/preview/__tests__/ofdParser.test.ts
git commit -m "feat: add lightweight ofd parser"
```

### Task 2: 锁定预览组件对 OFD 的分流

**Files:**
- Create: `src/components/preview/OfdViewer.tsx`
- Create: `src/components/preview/__tests__/SmartFilePreview.test.tsx`
- Modify: `src/components/preview/SmartFilePreview.tsx`
- Modify: `src/components/preview/FilePreview.tsx`
- Modify: `src/components/preview/index.ts`

**Step 1: Write the failing test**

```typescript
it('renders OfdViewer when current file is ofd', () => {
  render(
    <SmartFilePreview
      isPool
      fileId="file-ofd"
      fileName="invoice.ofd"
      files={[{ id: 'file-ofd', fileName: 'invoice.ofd', fileType: 'ofd' }]}
      currentFileId="file-ofd"
    />
  );

  expect(screen.getByTestId('ofd-viewer')).toBeInTheDocument();
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test:run -- src/components/preview/__tests__/SmartFilePreview.test.tsx`
Expected: FAIL because OFD still走下载占位 UI。

**Step 3: Write minimal implementation**

```typescript
if (fileType === 'ofd') {
  return <OfdViewer data-testid="ofd-viewer" url={url} fileName={fileName} scale={scale} />;
}
```

**Step 4: Run test to verify it passes**

Run: `npm run test:run -- src/components/preview/__tests__/SmartFilePreview.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add src/components/preview/OfdViewer.tsx src/components/preview/SmartFilePreview.tsx src/components/preview/FilePreview.tsx src/components/preview/index.ts src/components/preview/__tests__/SmartFilePreview.test.tsx
git commit -m "feat: add ofd viewer to shared preview flow"
```

### Task 3: 接入真实页面入口并保留错误/下载回退

**Files:**
- Create: `src/pages/panorama/__tests__/EvidencePreview.test.tsx`
- Modify: `src/pages/panorama/EvidencePreview.tsx`
- Modify: `src/pages/panorama/README.md`
- Modify: `src/components/preview/README.md`

**Step 1: Write the failing test**

```typescript
it('routes selected ofd attachment to shared preview component', async () => {
  render(<EvidencePreview voucherId="archive-1" sourceType="ARCHIVE" />);

  await screen.findByText('invoice.ofd');
  expect(screen.getByTestId('smart-file-preview')).toBeInTheDocument();
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test:run -- src/pages/panorama/__tests__/EvidencePreview.test.tsx`
Expected: FAIL because page still renders legacy iframe/FileViewer path.

**Step 3: Write minimal implementation**

```typescript
<SmartFilePreview
  isPool={true}
  fileId={selectedFile.id}
  fileName={selectedFile.fileName}
  files={currentTabFiles.map(...)}
  currentFileId={selectedFile.id}
  onFileChange={...}
/>
```

**Step 4: Run test to verify it passes**

Run: `npm run test:run -- src/pages/panorama/__tests__/EvidencePreview.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add src/pages/panorama/EvidencePreview.tsx src/pages/panorama/README.md src/components/preview/README.md src/pages/panorama/__tests__/EvidencePreview.test.tsx
git commit -m "feat: wire ofd preview into panorama evidence flow"
```

### Task 4: 完整验证与工单收尾

**Files:**
- Modify: `docs/plans/2026-03-06-ofd-preview-integration.md`

**Step 1: Run focused tests**

Run: `npm run test:run -- src/components/preview/__tests__/ofdParser.test.ts src/components/preview/__tests__/SmartFilePreview.test.tsx src/pages/panorama/__tests__/EvidencePreview.test.tsx`
Expected: PASS

**Step 2: Run broader safety checks**

Run: `npm run test:run -- src/__tests__/api/preview.test.ts`
Expected: PASS

Run: `npm run build`
Expected: exit 0

**Step 3: Update plan/workpad with actual verification commands and results**

```markdown
- [x] OFD parser tests
- [x] Shared preview tests
- [x] Panorama entry test
- [x] Build
```

**Step 4: Commit**

```bash
git add docs/plans/2026-03-06-ofd-preview-integration.md
git commit -m "docs: finalize ofd preview implementation plan"
```
