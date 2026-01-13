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
@Slf4j
@RestController
@RequestMapping("/entity")
@RequiredArgsConstructor
@Tag(name = "法人管理", description = "法人实体管理接口")
public class EntityController {

    private final EntityService entityService;
    private final DtoMapper dtoMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询法人列表")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Page<EntityResponse>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        Page<SysEntity> entityPage = new Page<>(page, limit);
        Page<SysEntity> result = entityService.page(entityPage);
        return Result.success(dtoMapper.toEntityResponsePage(result));
    }

    @GetMapping("/list")
    @Operation(summary = "查询法人列表")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityResponse>> list() {
        List<SysEntity> entities = entityService.list();
        return Result.success(dtoMapper.toEntityResponseList(entities));
    }

    @GetMapping("/list/active")
    @Operation(summary = "查询活跃法人列表")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityResponse>> listActive() {
        List<SysEntity> entities = entityService.listActive();
        return Result.success(dtoMapper.toEntityResponseList(entities));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询法人详情")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> getById(@PathVariable String id) {
        SysEntity entity = entityService.getById(id);
        if (entity == null) {
            return Result.error("法人不存在");
        }
        return Result.success(dtoMapper.toEntityResponse(entity));
    }

    @GetMapping("/{id}/fonds")
    @Operation(summary = "查询法人下的全宗列表")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<String>> getFondsIds(@PathVariable String id) {
        List<String> fondsIds = entityService.getFondsIdsByEntityId(id);
        return Result.success(fondsIds);
    }

    @GetMapping("/{id}/can-delete")
    @Operation(summary = "检查法人是否可以删除")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Boolean> canDelete(@PathVariable String id) {
        return Result.success(entityService.canDelete(id));
    }

    @PostMapping
    @Operation(summary = "创建法人")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> save(@Valid @RequestBody SysEntity entity) {
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
    @Operation(summary = "更新法人")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<EntityResponse> update(@Valid @RequestBody SysEntity entity) {
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
    @Operation(summary = "删除法人")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Boolean> remove(@PathVariable String id) {
        if (!entityService.canDelete(id)) {
            return Result.error("该法人下存在关联全宗或下级法人，无法删除");
        }
        return Result.success(entityService.removeById(id));
    }

    @GetMapping("/tree")
    @Operation(summary = "获取法人树形结构")
    @PreAuthorize("hasAnyAuthority('entity:view', 'entity:manage') or hasRole('SYS_ADMIN')")
    public Result<List<EntityService.EntityTreeNode>> getTree() {
        return Result.success(entityService.getTree());
    }

    @PutMapping("/{id}/parent")
    @Operation(summary = "更新法人父节点（调整层级关系）")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> updateParent(
            @PathVariable String id,
            @RequestParam(required = false) String parentId) {
        entityService.updateParent(id, parentId);
        return Result.success("层级关系已更新", null);
    }

    @PutMapping("/{id}/order")
    @Operation(summary = "更新法人排序")
    @PreAuthorize("hasAnyAuthority('entity:manage') or hasRole('SYS_ADMIN')")
    public Result<Void> updateOrder(
            @PathVariable String id,
            @RequestParam Integer orderNum) {
        entityService.updateOrder(id, orderNum);
        return Result.success("排序已更新", null);
    }
}





