# Walkthrough: 附件预览 404 故障修复

## 修改内容

### 1. 业务逻辑修复
- **FondsContextFilter.java**:
    - 在 `requiresFondsPermission` 中豁免了 `/archive/files/download/` 和 `/archive/{id}/content` 路径的强制全宗要求。
    - 修改 `resolveCurrentFonds`，使得这些路径在缺失 `X-Fonds-No` 标头时返回 `null`，而非默认回退到第一个全宗。
- **存储路径同步**:
    - 将 demo PDF 文件从 `uploads/demo` 同步到了后端的工作目录 `./data/archives/demo`。

## 验证结果

### 1. 后端 API 验证 (无标头下载)
使用 `curl` 模拟浏览器原生请求（不带 `X-Fonds-No`）：
```bash
curl -v -H "Authorization: Bearer <TOKEN>" \
  http://localhost:19090/api/archive/files/download/demo-file-002
```
**结果**: `200 OK`, `Content-Type: application/pdf`.

### 2. 权限安全性验证
- 验证全宗归属：`demo-file-002` 归属于 `BRJT` 全宗。
- 验证用户权限：当前测试用户具有 `BRJT` 全宗权限，因此校验通过。
- 风险确认：如果用户没有 `BRJT` 权限，`ArchiveFileController` 仍会触发 403，安全机制依然稳固。

## 操作指南
1. 刷新全景视图。
2. 确保在顶部选择了正确的全宗（如：泊冉集团总部）。
3. 现在点击 PDF 附件应能正常加载预览。
