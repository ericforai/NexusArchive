# 穿透联查页面关联附件缺失问题修复总结

## 问题描述
档案项 `BRJT-2024-30Y-FIN-AC01-0004` 在穿透联查页面（`/system/utilization/relationship`）无法显示其关联的附件。

## 根因分析
1. **数据库结构**：档案附件关联存储在 `acc_archive_attachment` 表中（`archive_id` → `file_id`）
2. **原有逻辑缺陷**：`AutoAssociationService.getLinkedFiles` 方法在查询 `acc_archive_attachment` 后，仅尝试从 `arc_original_voucher_file` 表匹配 `file_id`
3. **实际数据位置**：`demo-file-003` 存在于 `arc_file_content` 表，而非 `arc_original_voucher_file`

## 修复方案
扩展 `getLinkedFiles` 方法，增加回退查询逻辑：
1. 优先从 `arc_original_voucher_file` 查询
2. 若无结果，回退到 `arc_file_content` 表查询
3. 添加 `ArcFileContentMapper` 依赖注入

## 验证结果
API `/api/relations/arc-2024-004/files` 成功返回：
```json
{
  "code": 200,
  "data": [{
    "id": "demo-file-003",
    "name": "报销.pdf",
    "type": "other",
    "url": "/api/files/demo-file-003/download",
    "uploadDate": "2026-01-13",
    "size": "105.3KB"
  }]
}
```

## 修改文件

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| [AutoAssociationService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/AutoAssociationService.java) | 修改 | 添加 `ArcFileContentMapper` 依赖，扩展 `getLinkedFiles` 回退查询逻辑 |
| [AutoAssociationServiceTest.java](file:///Users/user/nexusarchive/nexusarchive-java/src/test/java/com/nexusarchive/service/AutoAssociationServiceTest.java) | 修改 | 更新测试类构造函数以匹配新依赖 |
