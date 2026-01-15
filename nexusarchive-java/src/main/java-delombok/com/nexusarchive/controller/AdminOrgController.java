// Input: Spring Web, EntityService, ErpOrgSyncService, Result
// Output: AdminOrgController 类
// Pos: 接口层 Controller - 组织管理 API（兼容前端）
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.OrgImportResult;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.service.ErpOrgSyncService;
import com.nexusarchive.service.EntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * 组织管理控制器（兼容前端 API）
 *
 * PRD 来源: 组织架构管理模块
 * 重构后：sys_org 表已合并到 sys_entity，通过 parent_id 字段建立层级关系
 */
@Tag(name = "组织管理", description = """
    组织架构管理接口。

    **功能说明:**
    - 获取组织树形结构
    - 获取组织列表
    - 创建/更新/删除组织
    - 批量创建组织
    - 从 ERP 同步组织架构
    - 导入组织数据
    - 更新组织排序

    **组织类型:**
    - GROUP: 法人集团
    - DEPARTMENT: 部门
    - TEAM: 团队

    **数据结构:**
    - 使用 parent_id 建立层级关系
    - 使用 order_num 控制同级排序

    **使用场景:**
    - 组织架构管理
    - ERP 组织同步
    - 部门层级维护

    **权限要求:**
    - manage_org: 组织管理权限
    - SYSTEM_ADMIN: 系统管理员
    - nav:all: 全部导航权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/org")
@RequiredArgsConstructor
public class AdminOrgController {

    private final EntityService entityService;
    private final ErpOrgSyncService erpOrgSyncService;

    /**
     * 获取组织树
     */
    @GetMapping("/tree")
    @Operation(
        summary = "获取组织树",
        description = """
            获取完整的组织架构树形结构。

            **返回数据包括:**
            - id: 组织ID
            - name: 组织名称
            - code: 组织编码
            - type: 组织类型
            - parentId: 父组织ID
            - orderNum: 排序号
            - children: 子组织列表

            **树形结构:**
            - 根节点: 法人集团 (GROUP)
            - 二级节点: 部门 (DEPARTMENT)
            - 三级节点: 团队 (TEAM)

            **使用场景:**
            - 组织架构树展示
            - 组织选择器
            """,
        operationId = "getOrgTree",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<List<EntityService.EntityTreeNode>> getOrgTree() {
        return Result.success(entityService.getTree());
    }

    /**
     * 获取组织列表
     */
    @GetMapping
    @Operation(
        summary = "获取组织列表",
        description = """
            获取所有组织的平铺列表。

            **返回数据包括:**
            - id: 组织ID
            - name: 组织名称
            - code: 组织编码
            - type: 组织类型
            - parentId: 父组织ID
            - orderNum: 排序号

            **使用场景:**
            - 组织表格展示
            - 组织选择器
            """,
        operationId = "listOrgs",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<List<SysEntity>> listAll() {
        return Result.success(entityService.list());
    }

    /**
     * 创建组织
     */
    @PostMapping
    @Operation(
        summary = "创建组织",
        description = """
            创建新的组织单元。

            **请求体:**
            - name: 组织名称（必填）
            - code: 组织编码（必填）
            - type: 组织类型（必填）
            - parentId: 父组织ID（可选）
            - orderNum: 排序号（可选）

            **组织类型:**
            - GROUP: 法人集团
            - DEPARTMENT: 部门
            - TEAM: 团队

            **业务规则:**
            - 编码必须唯一
            - 父组织必须存在

            **使用场景:**
            - 新增部门
            - 新增团队
            """,
        operationId = "createOrg",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "CREATE", resourceType = "ORG", description = "创建组织")
    public Result<String> create(
            @Parameter(description = "组织信息", required = true)
            @Valid @RequestBody SysEntity org) {
        entityService.save(org);
        return Result.success("创建成功");
    }

    /**
     * 批量创建组织
     */
    @PostMapping("/bulk")
    @Operation(
        summary = "批量创建组织",
        description = """
            批量创建组织单元。

            **请求体:**
            - 组织信息数组

            **业务规则:**
            - 原子操作（全部成功或全部失败）
            - 单次最多 100 条

            **使用场景:**
            - 初始化组织架构
            - 批量导入部门
            """,
        operationId = "bulkCreateOrg",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或超过限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "BULK_CREATE", resourceType = "ORG", description = "批量创建组织")
    public Result<Void> bulkCreate(
            @Parameter(description = "组织信息数组", required = true)
            @RequestBody List<SysEntity> orgs) {
        entityService.saveBatch(orgs);
        return Result.success("批量创建成功", null);
    }

    /**
     * 导入组织
     */
    @PostMapping("/import")
    @Operation(
        summary = "导入组织",
        description = """
            通过 CSV 文件导入组织数据。

            **请求参数 (multipart/form-data):**
            - file: CSV 文件

            **CSV 格式:**
            - 表头: name,code,parentId,type,orderNum
            - 示例: 财务部,FIN,,DEPARTMENT,1

            **使用场景:**
            - 批量导入组织架构
            """,
        operationId = "importOrg",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导入成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "IMPORT", resourceType = "ORG", description = "导入组织数据")
    public Result<OrgImportResult> importOrg(
            @Parameter(description = "CSV 文件", required = true)
            @RequestParam("file") MultipartFile file) {
        return Result.error("导入功能待实现");
    }

    /**
     * 从 ERP 同步组织
     */
    @PostMapping("/sync")
    @Operation(
        summary = "从 ERP 同步组织",
        description = """
            从 YonSuite 等 ERP 系统同步组织架构。

            **同步方式:**
            - 增量同步: 基于 pubts 时间戳
            - 树版本同步: 获取最新组织树版本

            **返回数据包括:**
            - success: 是否成功
            - message: 同步结果消息
            - syncedCount: 同步数量

            **业务规则:**
            - 自动创建不存在的组织
            - 更新已存在的组织
            - 保留本地自定义组织

            **使用场景:**
            - ERP 组织同步
            - 组织架构初始化
            """,
        operationId = "syncOrgFromErp",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "同步完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "同步失败")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "SYNC", resourceType = "ORG", description = "从 ERP 同步组织")
    public Result<ErpOrgSyncService.SyncResult> syncFromErp() {
        ErpOrgSyncService.SyncResult result = erpOrgSyncService.syncFromYonSuite();
        return result.isSuccess()
                ? Result.success(result.getMessage(), result)
                : Result.error(result.getMessage());
    }

    /**
     * 获取导入模板
     */
    @GetMapping("/import/template")
    @Operation(
        summary = "获取导入模板",
        description = """
            获取组织导入的 CSV 模板格式。

            **返回数据包括:**
            - csvHeader: CSV 表头
            - example: 示例数据

            **CSV 格式:**
            - 表头: name,code,parentId,type,orderNum
            - 示例: 财务部,FIN,,DEPARTMENT,1

            **使用场景:**
            - 下载导入模板
            """,
        operationId = "getOrgImportTemplate",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Map<String, String>> getTemplate() {
        return Result.success(Map.of(
                "csvHeader", "name,code,parentId,type,orderNum",
                "example", "财务部,FIN,,DEPARTMENT,1"
        ));
    }

    /**
     * 更新组织
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新组织",
        description = """
            更新组织信息。

            **路径参数:**
            - id: 组织ID

            **请求体:**
            - name: 组织名称
            - code: 组织编码
            - type: 组织类型
            - parentId: 父组织ID
            - orderNum: 排序号

            **业务规则:**
            - 不可修改已有子组织的父组织关系

            **使用场景:**
            - 修改组织信息
            - 调整组织层级
            """,
        operationId = "updateOrg",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "组织不存在")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ORG", description = "更新组织")
    public Result<String> update(
            @Parameter(description = "组织ID", required = true, example = "org-001")
            @PathVariable String id,
            @Parameter(description = "组织信息", required = true)
            @Valid @RequestBody SysEntity org) {
        org.setId(id);
        entityService.updateById(org);
        return Result.success("更新成功");
    }

    /**
     * 更新组织排序
     */
    @PutMapping("/{id}/order")
    @Operation(
        summary = "更新组织排序",
        description = """
            更新组织在同级中的排序位置。

            **路径参数:**
            - id: 组织ID

            **请求参数:**
            - orderNum: 排序号

            **使用场景:**
            - 调整组织显示顺序
            """,
        operationId = "updateOrgOrder",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "组织不存在")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ORG", description = "更新组织排序")
    public Result<Void> updateOrder(
            @Parameter(description = "组织ID", required = true, example = "org-001")
            @PathVariable String id,
            @Parameter(description = "排序号", example = "1")
            @RequestParam Integer orderNum) {
        entityService.updateOrder(id, orderNum);
        return Result.success("排序已更新", null);
    }

    /**
     * 删除组织
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除组织",
        description = """
            删除组织单元。

            **路径参数:**
            - id: 组织ID

            **业务规则:**
            - 存在关联数据的组织不可删除
            - 存在子组织的组织不可删除
            - 删除操作会被审计记录

            **使用场景:**
            - 删除空组织
            """,
        operationId = "deleteOrg",
        tags = {"组织管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "存在关联数据，无法删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "组织不存在")
    })
    @PreAuthorize("hasAuthority('manage_org') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "DELETE", resourceType = "ORG", description = "删除组织")
    public Result<Void> delete(
            @Parameter(description = "组织ID", required = true, example = "org-001")
            @PathVariable String id) {
        if (!entityService.canDelete(id)) {
            return Result.error("该组织下存在关联数据，无法删除");
        }
        entityService.removeById(id);
        return Result.success("删除成功", null);
    }
}
