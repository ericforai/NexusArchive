# 档案附件检索一致性全局修复总结

## 修复概述
针对档案项关联附件缺失的问题，我们不仅修复了当下的 Bug，还进一步优化了系统架构，解决了档案附件在不同存储表（`arc_file_content` 与 `arc_original_voucher_file`）之间检索不一致的隐患。

## 根因分析
- **数据多态性**：系统允许附件源自预归档池（`arc_file_content`）或手工上传/ERP采集（`arc_original_voucher_file`）。
- **检索逻辑割裂**：
    - `AttachmentService` 之前仅查询 `arc_file_content`。
    - `AutoAssociationService` 之前仅查询 `arc_original_voucher_file`。
    - `ArchiveService` 虽然有双查倾向，但底层 Mapper 仅支持单表。

## 修复方案：架构级统一
我们采取了“逻辑下沉，接口统一”的策略：

1. **Mapper 层增强**：
   在 `ArcFileContentMapper.selectAttachmentsByArchiveId` 中引入 `UNION ALL` 联合 SQL。
   ```sql
   SELECT ... FROM arc_original_voucher_file JOIN ...
   UNION ALL
   SELECT ... FROM arc_file_content JOIN ...
   ```

2. **Service 层清理**：
   - 重构 `AttachmentServiceImpl`：移除手动循环查询，直接调用增强后的 Mapper。
   - 简化 `AutoAssociationService`：移除冗余的表查询代码，统一依赖 Mapper 的联合检索能力。

## 验证结果
- **API 验证**：`/api/relations/arc-2024-004/files` 正确返回了位于 `arc_file_content` 中的附件。
- **一致性验证**：经审计，系统内三大核心服务（Archive/Attachment/AutoAssociation）现在均采用统一的检索路径。

## 修改文件

| 模块 | 文件 | 说明 |
|------|------|------|
| Mapper | [ArcFileContentMapper.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcFileContentMapper.java) | 实现 UNION 联合查询逻辑 |
| Service | [AttachmentServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AttachmentServiceImpl.java) | 使用统一 Mapper 方法，提升检索完整性 |
| Service | [AutoAssociationService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/AutoAssociationService.java) | 简化逻辑，移除冗余 Mapper 依赖 |
| Test | [AutoAssociationServiceTest.java](file:///Users/user/nexusarchive/nexusarchive-java/src/test/java/com/nexusarchive/service/AutoAssociationServiceTest.java) | 同步更新 Mock 依赖 |
