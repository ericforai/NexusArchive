// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: AdminOrgController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.OrgImportResult;
import com.nexusarchive.entity.Org;
import com.nexusarchive.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/org")
@PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class AdminOrgController {

    private final OrgService orgService;

    @GetMapping("/tree")
    public Result<List<OrgService.OrgTreeNode>> getOrgTree() {
        return Result.success(orgService.getTree());
    }

    @GetMapping
    public Result<List<Org>> listAll() {
        return Result.success(orgService.listAll());
    }

    @PostMapping
    public Result<Org> create(@RequestBody Org org) {
        return Result.success("创建成功", orgService.create(org));
    }

    @PostMapping("/bulk")
    public Result<Void> bulkCreate(@RequestBody List<Org> orgs) {
        orgService.createBatch(orgs);
        return Result.success("批量创建成功", null);
    }

    @PostMapping("/import")
    public Result<OrgImportResult> importOrg(@RequestParam("file") MultipartFile file) {
        return Result.success(orgService.importFromFile(file));
    }

    @GetMapping("/import/template")
    public Result<Map<String, String>> getTemplate() {
        return Result.success(Map.of(
                "csvHeader", "name,code,parentId,type,orderNum",
                "example", "财务部,FIN,,DEPARTMENT,1"
        ));
    }

    @PutMapping("/{id}")
    public Result<Org> update(@PathVariable String id, @RequestBody Org org) {
        return Result.success("更新成功", orgService.update(id, org));
    }

    @PutMapping("/{id}/order")
    public Result<Void> updateOrder(@PathVariable String id, @RequestParam Integer orderNum) {
        orgService.updateOrder(id, orderNum);
        return Result.success("排序已更新", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        orgService.delete(id);
        return Result.success("删除成功", null);
    }
}
