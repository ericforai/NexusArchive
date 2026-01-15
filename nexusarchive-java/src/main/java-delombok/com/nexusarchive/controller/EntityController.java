// Input: Spring Web、EntityService、Result、DtoMapper、EntityResponse
// Output: EntityController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.EntityResponse;
import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.service.EntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * 法人实体控制器
 *
 * PRD 来源: Section 1.1 - 法人仅管理维度
 * 所有返回值使用 DTO，避免直接暴露 Entity
 */
@Tag(name = "法人管理", description = """
    法人实体管理接口。

    **功能说明:**
    - 法人实体的 CRUD 操作
    - 法人树形结构管理
    - 法人与全宗关联管理
    - 法人层级关系调整

    **法人实体:**
    - 法人是档案管理的基本组织单位
    - 支持树形层级结构（父子关系）
    - 一个法人可关联多个全宗
    - 法人有唯一标识符和名称

    **树形结构:**
    - 通过 parentId 构建父子关系
    - 通过 orderNum 控制同级排序
    - 支持任意层级嵌套

    **使用场景:**
    - 企业集团多层级管理
    - 法人合并分立处理
    - 组织架构调整

    **权限要求:**
    - entity:view: 查看法人权限
    - entity:manage: 管理法人权限
    - SYS_ADMIN: 系统管理员
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/entity")
@RequiredArgsConstructor
public class EntityController {

    private final EntityService entityService;
    private final DtoMapper dtoMapper;

    @GetMapping("/page")
    @Operation(
        summary = "分页查询法人列表",
        description = """
            分页查询法人实体列表。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 10）

            **返回数据包括:**
            - records: 法人记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **使用场景:**
            - 法人列表展示
            - 法人分页查询
            """,
        operationId = "getEntityPage",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Page<EntityResponse>> getPage(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        Page<SysEntity> entityPage = new Page<>(page, limit);
        Page<SysEntity> result = entityService.page(entityPage);
        return Result.success(dtoMapper.toEntityResponsePage(result));
    }

    @GetMapping("/list")
    @Operation(
        summary = "查询法人列表",
        description = """
            获取所有法人实体列表（不分页）。

            **返回数据包括:**
            - id: 法人ID
            - name: 法人名称
            - code: 法人编码
            - status: 状态
            - parentId: 父节点ID
            - orderNum: 排序号

            **使用场景:**
            - 法人选择器
            - 法人下拉列表
            """,
        operationId = "listEntities",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityResponse>> list() {
        List<SysEntity> entities = entityService.list();
        return Result.success(dtoMapper.toEntityResponseList(entities));
    }

    @GetMapping("/list/active")
    @Operation(
        summary = "查询活跃法人列表",
        description = """
            获取所有活跃状态的法人实体列表。

            **返回数据包括:**
            - id: 法人ID
            - name: 法人名称
            - code: 法人编码
            - status: ACTIVE（活跃状态）

            **使用场景:**
            - 活跃法人选择器
            - 新建档案时选择法人
            """,
        operationId = "listActiveEntities",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityResponse>> listActive() {
        List<SysEntity> entities = entityService.listActive();
        return Result.success(dtoMapper.toEntityResponseList(entities));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "查询法人详情",
        description = """
            获取指定法人的详细信息。

            **路径参数:**
            - id: 法人ID

            **返回数据包括:**
            - id: 法人ID
            - name: 法人名称
            - code: 法人编码
            - status: 状态
            - parentId: 父节点ID
            - orderNum: 排序号
            - createdAt: 创建时间
            - updatedAt: 更新时间

            **使用场景:**
            - 法人详情查看
            - 法人编辑预填充
            """,
        operationId = "getEntityById",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> getById(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id) {
        SysEntity entity = entityService.getById(id);
        if (entity == null) {
            return Result.error("法人不存在");
        }
        return Result.success(dtoMapper.toEntityResponse(entity));
    }

    @GetMapping("/{id}/fonds")
    @Operation(
        summary = "查询法人下的全宗列表",
        description = """
            获取指定法人关联的所有全宗编号列表。

            **路径参数:**
            - id: 法人ID

            **返回数据包括:**
            - 全宗编号列表

            **使用场景:**
            - 查看法人关联的全宗
            - 全宗分配参考
            """,
        operationId = "getEntityFonds",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<String>> getFondsIds(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id) {
        List<String> fondsIds = entityService.getFondsIdsByEntityId(id);
        return Result.success(fondsIds);
    }

    @GetMapping("/{id}/can-delete")
    @Operation(
        summary = "检查法人是否可以删除",
        description = """
            检查指定法人是否可以被删除。

            **路径参数:**
            - id: 法人ID

            **返回数据包括:**
            - true: 可以删除
            - false: 不可以删除

            **业务规则:**
            - 有关联全宗的法人不可删除
            - 有下级法人的不可删除
            - 被其他数据引用的不可删除

            **使用场景:**
            - 删除前检查
            - UI 禁用判断
            """,
        operationId = "canDeleteEntity",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检查完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Boolean> canDelete(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id) {
        return Result.success(entityService.canDelete(id));
    }

    @PostMapping
    @Operation(
        summary = "创建法人",
        description = """
            创建新的法人实体。

            **请求体:**
            - name: 法人名称（必填）
            - code: 法人编码（必填）
            - status: 状态（默认 ACTIVE）
            - parentId: 父节点ID
            - orderNum: 排序号

            **业务规则:**
            - 法人名称不能为空
            - 默认状态为 ACTIVE
            - 编码必须唯一

            **使用场景:**
            - 新增法人
            - 创建下级法人
            """,
        operationId = "createEntity",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> save(
            @Parameter(description = "法人信息", required = true)
            @Valid @RequestBody SysEntity entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            return Result.error("法人名称不能为空");
        }
        if (entity.getStatus() == null) {
            entity.setStatus("ACTIVE");
        }
        entityService.save(entity);
        return Result.success(dtoMapper.toEntityResponse(entity));
    }

    @PutMapping
    @Operation(
        summary = "更新法人",
        description = """
            更新法人实体信息。

            **请求体:**
            - id: 法人ID（必填）
            - name: 法人名称
            - code: 法人编码
            - status: 状态
            - parentId: 父节点ID
            - orderNum: 排序号

            **注意:**
            - 法人ID不能为空
            - 法人名称不能为空

            **使用场景:**
            - 修改法人信息
            - 调整法人状态
            """,
        operationId = "updateEntity",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> update(
            @Parameter(description = "法人信息", required = true)
            @Valid @RequestBody SysEntity entity) {
        if (entity.getId() == null) {
            return Result.error("法人ID不能为空");
        }
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            return Result.error("法人名称不能为空");
        }
        entityService.updateById(entity);
        return Result.success(dtoMapper.toEntityResponse(entity));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除法人",
        description = """
            删除指定的法人实体。

            **路径参数:**
            - id: 法人ID

            **业务规则:**
            - 有关联全宗的法人不可删除
            - 有下级法人的不可删除

            **使用场景:**
            - 删除法人
            """,
        operationId = "deleteEntity",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "法人有关联数据，无法删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Boolean> remove(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id) {
        if (!entityService.canDelete(id)) {
            return Result.error("该法人下存在关联全宗或下级法人，无法删除");
        }
        return Result.success(entityService.removeById(id));
    }

    @GetMapping("/tree")
    @Operation(
        summary = "获取法人树形结构",
        description = """
            获取法人实体的完整树形结构。

            **返回数据包括:**
            - id: 法人ID
            - name: 法人名称
            - code: 法人编码
            - children: 子法人列表

            **使用场景:**
            - 法人树展示
            - 组织架构图
            - 层级选择器
            """,
        operationId = "getEntityTree",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityService.EntityTreeNode>> getTree() {
        return Result.success(entityService.getTree());
    }

    @PutMapping("/{id}/parent")
    @Operation(
        summary = "更新法人父节点（调整层级关系）",
        description = """
            调整法人实体的父节点，修改层级关系。

            **路径参数:**
            - id: 法人ID

            **查询参数:**
            - parentId: 新的父节点ID（null 表示移为根节点）

            **业务规则:**
            - 不能将自己设为父节点
            - 不能将子孙节点设为父节点（防止循环）

            **使用场景:**
            - 组织架构调整
            - 法人层级变更
            """,
        operationId = "updateEntityParent",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误（如循环引用）"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> updateParent(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id,
            @Parameter(description = "新的父节点ID（null 表示根节点）")
            @RequestParam(required = false) String parentId) {
        entityService.updateParent(id, parentId);
        return Result.success("层级关系已更新", null);
    }

    @PutMapping("/{id}/order")
    @Operation(
        summary = "更新法人排序",
        description = """
            调整法人在同级节点中的排序位置。

            **路径参数:**
            - id: 法人ID

            **查询参数:**
            - orderNum: 排序号（数字越小越靠前）

            **使用场景:**
            - 法人排序调整
            - 自定义显示顺序
            """,
        operationId = "updateEntityOrder",
        tags = {"法人管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> updateOrder(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String id,
            @Parameter(description = "排序号", example = "1")
            @RequestParam Integer orderNum) {
        entityService.updateOrder(id, orderNum);
        return Result.success("排序已更新", null);
    }
}
