# 修复全景视图附件预览 404 故障计划

## 问题描述

在全景视图中预览附件时，前端通过 URL 直接请求下载接口。由于浏览器加载资源时无法携带自定义标头 `X-Fonds-No`，后端过滤器 `FondsContextFilter` 默认将上下文设为用户的第一个全宗（如 `BR-GROUP`）。如果附件所属档案属于另一个全宗（如 `BRJT`），`ArchiveFileContentService` 会因为全宗号不匹配而拒绝返回数据，导致前端收到 404 错误。

## 方案设计

### 1. 技术背景
- **后端下载接口**: `/api/archive/files/download/{fileId}`
- **安全校验逻辑**: `ArchiveFileController` 在调用服务获取记录后，会调用 `authorizeArchiveAccess` 进行完整的数据范围权限校验（基于 `DataScopeService`）。
- **瓶颈点**: `ArchiveFileContentService.getFileContentById` 内部包含了一个基于 `FondsContext` 的硬编码校验，该校验过于严格且依赖于请求标头。

### 2. 拟定修改计划

#### 方案 A: 优化 `FondsContextFilter` (推荐)

修改 `FondsContextFilter.java` 的 `resolveCurrentFonds` 方法。对于下载/预览接口，如果缺失标头，不应用默认的“第一个全宗”逻辑。这样 `FondsContext` 将为 null，安全校验将自动回退到 `allowedFonds` 列表检查。

#### [MODIFY] [FondsContextFilter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/FondsContextFilter.java)

```java
    private String resolveCurrentFonds(HttpServletRequest request, List<String> allowedFonds) {
        // ... (existing header check)
        
        // 如果是下载接口且没有标头，不走默认逻辑
        String path = request.getRequestURI();
        if (path.contains("/files/download/") || path.contains("/content")) {
            return null; 
        }

        // 如果没有指定全宗号，默认使用第一个全宗（避免登录后跳转失败）
        if (!allowedFonds.isEmpty()) {
            return allowedFonds.get(0);
        }
        return null;
    }
```

#### 方案 B: 移除 Service 层冗余校验 (备选)

移除 `ArchiveFileContentService.java` 的内部校验，完全依赖 Controller 层的安全检查。内容参见前一版本。


## 验证计划

### 自动化验证
- 使用 `curl` 进行不带 `X-Fonds-No` 标头的下载测试，验证是否返回 200。

### 手动验证
- 访问全景视图，在 `BRJT` 全宗下选择档案。
- 点击附件预览，确认 PDF 能够显示。

## 风险评估
- **安全性**: 无风险。`ArchiveFileController` 的 `authorizeArchiveAccess` 仍然会拦截没有权限的用户。此修改仅移除了对“当前活动全宗上下文”的限制，不改变用户的总体权限范围。
