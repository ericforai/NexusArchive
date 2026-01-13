// Input: Spring Web, EntityService, ErpOrgSyncService, Result
// Output: AdminOrgController 类
// Pos: 接口层 Controller - 组织管理 API（兼容前端）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.OrgImportResult;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.service.ErpOrgSyncService;
import com.nexusarchive.service.EntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * 组织管理控制器（兼容前端 API）
 * 重构后：sys_org 表已合并到 sys_entity，通过 parent_id 字段建立层级关系
 */
@RestController
@RequestMapping("/admin/org")
@RequiredArgsConstructor
public class AdminOrgController {

    private final EntityService entityService;
    private final ErpOrgSyncService erpOrgSyncService;

    @GetMapping("/tree")
    public Result<List<EntityService.EntityTreeNode>> getOrgTree() {
        return Result.success(entityService.getTree());
    }

    @GetMapping
    public Result<List<SysEntity>> listAll() {
        return Result.success(entityService.list());
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody SysEntity org) {
        entityService.save(org);
        return Result.success("创建成功");
    }

    @PostMapping("/bulk")
    public Result<Void> bulkCreate(@RequestBody List<SysEntity> orgs) {
        entityService.saveBatch(orgs);
        return Result.success("批量创建成功", null);
    }

    @PostMapping("/import")
    public Result<OrgImportResult> importOrg(@RequestParam("file") MultipartFile file) {
        return Result.error("导入功能待实现");
    }

    @PostMapping("/sync")
    public Result<ErpOrgSyncService.SyncResult> syncFromErp() {
        ErpOrgSyncService.SyncResult result = erpOrgSyncService.syncFromYonSuite();
        return result.isSuccess()
                ? Result.success(result.getMessage(), result)
                : Result.error(result.getMessage());
    }

    @GetMapping("/import/template")
    public Result<Map<String, String>> getTemplate() {
        return Result.success(Map.of(
                "csvHeader", "name,code,parentId,type,orderNum",
                "example", "财务部,FIN,,DEPARTMENT,1"
        ));
    }

    @PutMapping("/{id}")
    public Result<String> update(@PathVariable String id, @Valid @RequestBody SysEntity org) {
        org.setId(id);
        entityService.updateById(org);
        return Result.success("更新成功");
    }

    @PutMapping("/{id}/order")
    public Result<Void> updateOrder(@PathVariable String id, @RequestParam Integer orderNum) {
        entityService.updateOrder(id, orderNum);
        return Result.success("排序已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        if (!entityService.canDelete(id)) {
            return Result.error("该组织下存在关联数据，无法删除");
        }
        entityService.removeById(id);
        return Result.success("删除成功", null);
    }
}
