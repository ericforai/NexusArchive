# 附件预览 404 故障排查与全宗过滤器优化

> **日期**: 2026-01-13  
> **问题**: 全景视图中附件预览无法加载（报 404），即便文件物理存在且数据库记录正确  
> **状态**: ✅ 已解决

---

## 问题现象

在档案全景视图展示时：
1. 附件列表显示正常（API 返回了附件元数据）。
2. 点击 PDF 预览时，浏览器请求 `/api/archive/files/download/{fileId}` 返回 **404 Not Found**。
3. 观察后端日志，发现错误信息：`全宗权限不匹配: fileId=xxx, fondsCode=BRJT, currentFonds=BR-GROUP`。

---

## 根因分析

### 1. 浏览器原生请求的局限性
前端渲染 PDF 预览通常使用 `<embed>`、`<iframe>` 或 `window.open`。这些原生浏览器行为**无法在 HTTP Header 中携带自定义标头**（如项目通用的 `X-Fonds-No`）。

### 2. 过滤器的默认行为冲突
`FondsContextFilter` 之前的逻辑是：
- 如果请求没有携带 `X-Fonds-No` 标头，且用户有多个全宗权限。
- 过滤器会**默认将当前上下文（FondsContext）回落到用户的第一个全宗**（例如 `BR-GROUP`）。

### 3. Service 层的超范围校验
`ArchiveFileContentService.getFileContentById()` 内部会检查 `FondsContext.getCurrentFondsNo()` 是否与文件所属的 `fonds_code` 一致。
- 当预览一个属于 `BRJT` 全宗的文件时，上下文却是 `BR-GROUP`。
- 业务校验失败，返回 `null`，导致 Controller 抛出 404 异常。

---

## 解决方案

### 核心原则：安全校验应由授权中心统一管理，过滤器不应强制施加“当前活动全宗”限制于资源下载。

### 1. 过滤器逻辑优化
修改 `FondsContextFilter.java`：
- **排除特定路径**：将附件下载 (`/files/download/`) 和内容预览 (`/content`) 路径设为“非强制全宗”路径。
- **取消默认回落**：在这些路径下，如果缺失标头，不再回落到第一个全宗，而是允许 `FondsContext` 保持为空。

### 2. 授权机制回退
当 `FondsContext.getCurrentFondsNo()` 为空时：
- `ArchiveFileController` 通过 `DataScopeService` 进行校验。
- `DataScopeService` 会自动退回到二级校验：检查档案所属全宗是否在用户的 **Allowed Fonds 列表**中。
- 由于管理员具有所有全宗权限，校验通过，预览成功。

---

## 经验教训

### ❌ 陷阱：在业务 Service 中硬编码 FondsContext 校验
业务服务不应对“当前正处于哪个全宗界面”这种 UI 状态感知过深，除非是写入操作。对于资源查询，应优先考虑用户的**总体权限范围**。

### ✅ 最佳实践
1. **区分标头依赖**：识别哪些 API 是通过 `axios` 调用的（可以带标头），哪些是由浏览器原生触发的（如下载、预览、图片 SRC），并对后者做兼容处理。
2. **安全层级设计**：
    - **一级校验**：显式指定的活动全宗。
    - **二级校验**：用户拥有的所有全宗权限。
    - **三级校验**：数据创建者权限。
3. **物理存储一致性**：使用相对路径时（如 `./data/archives`），务必确保后端服务的启动目录（CWD）与该路径匹配。

---

## 相关文件
- [FondsContextFilter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java) - 修复位置
- [ArchiveFileContentService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveFileContentService.java) - 涉及校验逻辑
- [2026-01-13-fix-attachment-preview-404.md](file:///Users/user/nexusarchive/docs/plans/2026-01-13-fix-attachment-preview-404.md) - 对应计划

