# 批量上传失败处理优化计划 - 解决文件“消失”与检测状态异常

## 现状分析
通过对后端代码和数据库记录的深入审计，发现了导致用户在批量上传后“看不到文件”以及“检测通过后仍显示异常”的关键原因：

1. **后端状态逻辑冲突 (Discrepancy Whammy)**:
   - `PreArchiveCheckService` 认为 `WARNING` 级别的检测结果（如全宗号未匹配等非致命告警）仍可标记为 `READY_TO_ARCHIVE`（可归档）。
   - `CollectionBatchServiceImpl` 会在检测后再次进行粗暴的状态同步，将 **非 PASS** (包括 `WARNING`) 的所有记录强制覆盖为 `NEEDS_ACTION`（待处理）。
   - 由于 `CollectionBatchServiceImpl` 的更新发生在后，最终导致原本可以归档的文件被踢回了“待处理”状态。

2. **前端默认筛选遮蔽 (Frontend Filter Whammy)**:
   - “预归档库” (`PoolPage`) 的默认筛选器设置为 `status = PENDING_CHECK` (待检测) 且 `category = VOUCHER` (记账凭证)。
   - 用户新上传的文件（如财务报告 `AC03`）在进入 `NEEDS_ACTION` 状态后，在默认筛选下是不可见的，导致用户产生“文件消失”的错觉。

3. **导航上下文丢失 (Navigation Whammy)**:
   - 批量上传完成后的“前往预归档库”按钮仅执行了简单的路径跳转，未携带当前上传批次的类别或状态参数。

---

## 拟更改的内容

### 核心后端服务 (Backend)

#### [MODIFY] [CollectionBatchServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java)
- **移除冗余状态更新**：在 `executeBatchCheck` 方法中，移除检测后的冗余 `arcFileContentMapper.updateById(file)` 调用。
- **状态统计逻辑修正**：更新统计逻辑，将 `PreArchiveStatus.READY_TO_ARCHIVE` 视为成功，即使其 `report.status` 为 `WARNING`。
- **代码清理**：删除 3.5 节中不再需要的手动持久化逻辑（已在 `checkSingleFile` 前通过 inheritance 处理）。

### 前端导航与视图 (Frontend)

#### [MODIFY] [BatchUploadView.tsx](file:///Users/user/nexusarchive/src/pages/collection/BatchUploadView.tsx)
- **增强导航逻辑**：将“前往预归档库”按钮改为携带参数跳转。
  - 根据检测结果动态生成参数：`?view=list&status=${targetStatus}&category=${targetCategory}`。
- **UI 提示优化**：在成功/部分成功界面明确告知用户文件所在的分类（如：已存入财务报告分类）。

#### [MODIFY] [PoolPage.tsx](file:///Users/user/nexusarchive/src/pages/pre-archive/PoolPage.tsx)
- **URL 参数解析**：增加对 `status` 参数的支持，允许外部链接指定初始筛选状态。
- **状态卡片联动**：确保从 URL 加载的 `status` 参数能正确反映在 `dashboardFilter` 状态中。

---

## 验证计划

### 自动化验证
1. **单元测试回归**：
   - 运行 `CollectionBatchServiceTest.java` 确保重构后的逻辑不影响现有批次统计功能。
   - 命令：`mvn test -Dtest=CollectionBatchServiceTest`

### 手动验证步骤
1. **环境准备**：使用 `DEMO` 全宗，模拟上传一个类别为 `AC03` (财务报告) 的文件。
2. **流程执行**：在“批量上传”页面完成流程，通过智能继承补全元数据。
3. **跳转验证**：点击跳转按钮后，验证：
   - 浏览器地址栏包含正确的参数。
   - 页面直接展示刚上传的财务报告，且状态卡片正确高亮。
4. **数据核对**：通过 `docker exec` 检查数据库记录，确保 `pre_archive_status` 与 UI 一致。
