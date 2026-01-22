# 批量上传问题修复实施方案

## 目标
修复批量上传模块中发现的前端警告及后端 500 错误。

## 变更记录

### 前端模块
#### [MODIFY] [BatchUploadView.tsx](file:///Users/user/nexusarchive/src/pages/collection/BatchUploadView.tsx)
- 替换 `Alert` 组件的 `message` 属性为 `title`。

### 后端模块
#### [MODIFY] [CollectionBatchServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java)
- 注入 `BasFondsMapper`。
- 逻辑：`createBatch` 时通过 `fondsCode` 获取 `fondsId` 并存入 `CollectionBatch`。

#### [MODIFY] [ArchivalAuditAspect.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/aspect/ArchivalAuditAspect.java)
- 优化 `username` 提取逻辑：`UserDetails.getUsername()`。
- 增加长度限制（255），防止数据库溢出。

## 验证结论
- API `/api/collection/batch/create` 调用正常。
- 审计日志记录正常，无溢出报错。
- 前端控制台无已弃用属性告警。
