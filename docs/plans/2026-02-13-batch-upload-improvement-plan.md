# 改进计划：在线上传元数据录入与四性检测优化

## 1. 目标
解决批量上传时因单个文件元数据缺失导致的“四性检测”不通过问题，平衡“采集效率”与“合规标准”。

## 2. 核心方案

### 阶段一：元数据“智能继承”（短期 - 本次实施重点）
*   **逻辑优化**：在 `CollectionBatchServiceImpl.uploadFile` 阶段，将原本固定的“系统管理员”/“1月1日”逻辑替换为**批次元数据动态继承**。
*   **具体映射**：
    *   `creator` (责任者) -> 继承当前操作用户名称。
    *   `docDate` (凭证日期) -> 继承批次的 `fiscalYear` + `fiscalPeriod` (默认为该月1日)。
    *   `summary` (摘要) -> 继承批次名称 + 顺序号，或通过文件名语义提取。
*   **合规性保障**：虽然是自动填充，但相比固定默认值更具业务相关性。

### 阶段二：支持“带清单上传”（中期）
*   **新增能力**：支持上传 Zip 包时包含一个 `manifest.csv`。
*   **流程**：后端解析清单，将文件哈希与清单中的元数据（真实摘要、真实金额等）精准关联。

### 阶段三：预归档库“批量补录”视图（中期）
*   **前端改进**：在预归档库界面增加“批量编辑”模式。
*   **功能**：对处于 `NEEDS_ACTION` 状态且原因缺失元数据的文件，支持像 Excel 一样批量修改责任者、日期等。

---

## 3. 拟修改代码清单

### 后端 (Java)
*   #### [MODIFY] [CollectionBatchServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/CollectionBatchServiceImpl.java)
    *   优化 `executeBatchCheck` 方法中的默认值设置逻辑。
    *   在 `createArcFileContent` 中增加更多字段的初步推断。

### 前端 (TypeScript)
*   #### [MODIFY] [BatchUploadView.tsx](file:///Users/user/nexusarchive/src/pages/collection/BatchUploadView.tsx)
    *   在上传成功后的摘要信息中，明确告知用户：“已根据批次信息初步生成元数据，如有异常请在预归档库中修正”。

---

## 4. 验证计划
1. **录入验证**：使用批量上传功能，上传 10 个 PDF。
2. **状态验证**：检查 `arc_file_content` 表中的 `creator`, `doc_date`, `summary` 是否正确继承批次信息。
3. **闭环验证**：执行四性检测，验证其状态是否自动转为 `READY_TO_ARCHIVE` (就绪) 而非 `NEEDS_ACTION` (需处理)。
