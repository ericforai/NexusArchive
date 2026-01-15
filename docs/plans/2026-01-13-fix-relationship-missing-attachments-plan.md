# 修复档案关联查询缺失附件逻辑实现计划

档案项 `BRJT-2024-30Y-FIN-AC01-0004` 虽然在 `acc_archive_attachment` 中有关联附件，但由于后端 `RelationController` 的接口逻辑仅通过 `acc_archive_relation` 查询，导致穿透联查页面显示不全。本计划旨在对齐查询逻辑，确保附件数据正确返回。

## 用户评审建议
> [!IMPORTANT]
> 本次修改涉及后端接口逻辑调整，将合并来自 `acc_archive_relation`（档案间关系）和 `acc_archive_attachment`（档案与原始文件关系）的数据。

## 拟定变更

### 后端服务 (nexusarchive-java)

#### [MODIFY] [RelationController.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/controller/RelationController.java)
- 修改 `getLinkedFiles` 接口，调用 `ArchiveService.getFilesByArchiveId` 而不是 `autoAssociationService.getLinkedFiles`，或者在 `autoAssociationService` 中整合逻辑。
- 考虑到 `ArchiveService` 已经有完善的“双源查询”逻辑，建议优先使用。

#### [MODIFY] [AutoAssociationService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/AutoAssociationService.java)
- 更新 `getLinkedFiles` 方法，使其不仅查询 `ArchiveRelation`，也查询 `acc_archive_attachment` 关联的文件，并转换为 DTO 返回。

### 验证计划

#### 自动化测试
- 使用 `curl` 模拟前端请求 `/api/relations/arc-2024-004/files`。
- 预期结果：返回数据中应包含 `demo-file-003` (报销申请单)。

#### 手动验证
- 登录系统，访问 `/system/utilization/relationship`。
- 搜索 `BRJT-2024-30Y-FIN-AC01-0004`。
- 检查右侧详情抽屉或关联列表（如果存在）是否显示关联附件。
