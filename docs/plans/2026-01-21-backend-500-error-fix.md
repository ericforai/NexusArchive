# 实现计划 - 修复批量上传 500 错误与数据一致性问题

## 目标描述
解决在批量上传过程中出现的 500 内部服务器错误（主要由并发冲突引起的 race condition），并修复系统启动时的数据一致性警告。

## 用户审核事项
> [!IMPORTANT]
> 并发冲突的处理将通过捕获数据库唯一索引异常来实现，这可能导致极少数情况下返回 409 或 400 错误而非 200 SUCCESS，但能有效防止 500 崩溃。

## 拟议变更

### 后端服务 (nexusarchive-java)

#### [MODIFY] [CollectionBatchServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java)
- 在 `uploadFile` 方法中，为 `batchFileMapper.insert(batchFile)` 添加 `try-catch` 块。
- 捕获 `org.springframework.dao.DuplicateKeyException`。
- 如果发生冲突，返回 `FileUploadResult`，状态为 `FAILED`，消息提示“同名文件已在当前批次中处理中或已存在”。

#### [MODIFY] [GlobalExceptionHandler.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/config/GlobalExceptionHandler.java)
- 优化对 `DataIntegrityViolationException`（包含唯一键冲突）的处理，确保不返回空的 500 响应，而是返回带有具体业务错误含义的 JSON。

### 数据配置 (nexusarchive-java/src/main/resources)

#### [MODIFY] Seed Data / Migration
- 修复 `BRJT` 孤儿全宗代码问题：在 `sys_entity` 或 `bas_fonds` 的初始数据中添加对应记录。
- 修复 `TRANSFER_VOUCHER` 未知类型代码问题：在 `sys_original_voucher_type` 中注册该类型。

### 脚本优化 (scripts)

#### [MODIFY] [dev.sh](file:///Users/user/nexusarchive/scripts/dev.sh)
- 在启动前增加磁盘空间检查。
- 在后端健康检查失败时，尝试获取并显示 `backend.log` 的最后几行错误信息。

## 验证计划

### 自动化测试
- 执行现有单元测试：`mvn test -Dtest=CollectionBatchServiceTest`
- 编写并发上传模拟测试（或通过 Mock 模拟唯一键冲突异常）。

### 手动验证
- **浏览器测试**：使用 Browser Subagent 模拟在同一批次中快速多次点击“上传”同一文件，验证系统是否返回友好的“文件已存在”提示而非 500 错误。
- **启动验证**：重新运行 `npm run dev`，确认日志中不再出现 `DataConsistencyValidator` 的 ERROR 指标。
