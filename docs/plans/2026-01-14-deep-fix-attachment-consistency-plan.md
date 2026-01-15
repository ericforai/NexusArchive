# 深入修复：档案附件检索一致性全局方案

经过深度分析，发现系统中存在多处针对 `acc_archive_attachment` 的不一致查询逻辑。本方案旨在通过 Mapper 层的联合查询实现全局一致性修复。

## 用户评审建议
> [!IMPORTANT]
> 本次修改采用了更彻底的架构级修复，将附件查询逻辑下沉到 Mapper 层，并支持多表联合检索。

## 拟定变更

### 1. Mapper 层 (nexusarchive-java)

#### [MODIFY] [ArcFileContentMapper.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/mapper/ArcFileContentMapper.java)
- 更新 `selectAttachmentsByArchiveId` 的 `@Select` 注解。
- 使用 `UNION ALL` 关联 `arc_original_voucher_file` 和 `arc_file_content` 两张表，确保不论附件源自何处都能被检索到。

### 2. Service 层 (nexusarchive-java)

#### [MODIFY] [AttachmentServiceImpl.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/impl/AttachmentServiceImpl.java)
- 重构 `getAttachmentsByArchive` 方法，直接调用 `arcFileContentMapper.selectAttachmentsByArchiveId`，消除原有的单表限制。

#### [REVERT/CLEAN] [AutoAssociationService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/AutoAssociationService.java)
- 保持我之前的修复逻辑（作为 DTO 转换的防线），但底层将受益于 Mapper 的增强。

### 3. 验证计划

#### 自动化测试
- 编写集成测试，在一个档案下关联两个文件：一个在 `arc_original_voucher_file`，另一个在 `arc_file_content`。
- 验证 `ArchiveService.getFilesByArchiveId` 和 `AttachmentService.getAttachmentsByArchive` 是否都能返回这两个文件。

#### 手动验证
- 在穿透联查页面搜索凭证，确认详情和关联列表均显示完整附件。
