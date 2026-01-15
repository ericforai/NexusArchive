// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: PositionController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.CreatePositionRequest;
import com.nexusarchive.dto.request.UpdatePositionRequest;
import com.nexusarchive.entity.Position;
import com.nexusarchive.service.PositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 岗位管理控制器
 *
 * <p>提供组织架构中岗位的 CRUD 操作</p>
 */
@Tag(name = "岗位管理", description = """
    岗位管理接口。

    **功能说明:**
    - 岗位的增删改查操作
    - 分页查询岗位列表
    - 按关键词和状态过滤

    **岗位属性:**
    - id: 岗位 ID
    - name: 岗位名称
    - code: 岗位编码
    - description: 岗位描述
    - status: 状态（ACTIVE/INACTIVE）
    - level: 岗位级别
    - permissions: 岗位权限列表

    **岗位状态:**
    - ACTIVE: 启用
    - INACTIVE: 停用

    **业务规则:**
    - 岗位编码必须唯一
    - 有用户的岗位不可直接删除
    - 删除操作需检查关联关系

    **使用场景:**
    - 组织架构管理
    - 岗位权限配置
    - 人员岗位分配

    **权限要求:**
    - manage_positions: 岗位管理权限
    - SYSTEM_ADMIN: 系统管理员角色
    - nav:all: 超级管理员权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/admin/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    @Operation(
        summary = "创建岗位",
        description = """
            创建新的岗位记录。

            **请求体 (CreatePositionRequest):**
            - name: 岗位名称（必填）
            - code: 岗位编码（必填，唯一）
            - description: 岗位描述（可选）
            - level: 岗位级别（可选）
            - status: 状态（默认 ACTIVE）

            **业务规则:**
            - 岗位编码必须唯一
            - 岗位名称不能为空

            **使用场景:**
            - 新增岗位
            - 组织架构调整
            """,
        operationId = "createPosition",
        tags = {"岗位管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或编码已存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Position> create(
            @Parameter(description = "创建请求", required = true) @Validated @RequestBody CreatePositionRequest request) {
        return Result.success(positionService.create(request));
    }

    @PutMapping
    @Operation(
        summary = "更新岗位",
        description = """
            更新已有岗位的信息。

            **请求体 (UpdatePositionRequest):**
            - id: 岗位 ID（必填）
            - name: 岗位名称
            - code: 岗位编码
            - description: 岗位描述
            - level: 岗位级别
            - status: 状态

            **业务规则:**
            - 岗位编码修改需检查唯一性
            - 已分配用户的岗位修改需谨慎

            **使用场景:**
            - 岗位信息更新
            - 岗位状态调整
            """,
        operationId = "updatePosition",
        tags = {"岗位管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "岗位不存在")
    })
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Position> update(
            @Parameter(description = "更新请求", required = true) @Validated @RequestBody UpdatePositionRequest request) {
        return Result.success(positionService.update(request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除岗位",
        description = """
            删除指定的岗位记录。

            **路径参数:**
            - id: 岗位 ID

            **业务规则:**
            - 有用户关联的岗位不可删除
            - 删除操作会检查关联关系

            **使用场景:**
            - 岗位注销
            - 组织架构调整
            """,
        operationId = "deletePosition",
        tags = {"岗位管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "岗位有关联用户，无法删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "岗位不存在")
    })
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Void> delete(
            @Parameter(description = "岗位ID", required = true, example = "1") @PathVariable String id) {
        positionService.delete(id);
        return Result.success();
    }

    @GetMapping
    @Operation(
        summary = "分页查询岗位列表",
        description = """
            分页查询岗位列表，支持关键词和状态过滤。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 10）
            - search: 搜索关键词（可选，模糊匹配名称/编码）
            - status: 状态过滤（可选，ACTIVE/INACTIVE）

            **返回数据包括:**
            - records: 岗位记录列表
            - total: 总记录数
            - page: 当前页码
            - size: 每页大小

            **使用场景:**
            - 岗位列表展示
            - 岗位搜索
            - 岗位选择器
            """,
        operationId = "listPositions",
        tags = {"岗位管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Page<Position>> list(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "搜索关键词", example = "会计") @RequestParam(required = false) String search,
            @Parameter(description = "状态过滤", example = "ACTIVE") @RequestParam(required = false) String status) {
        return Result.success(positionService.list(page, limit, search, status));
    }
}
