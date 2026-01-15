// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BasFondsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.service.BasFondsService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FondsScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;

/**
 * 基础全宗管理控制器
 *
 * 提供全宗的 CRUD 操作和权限过滤查询
 */
@Tag(name = "基础全宗管理", description = """
    全宗基础信息的增删改查接口。

    **功能说明:**
    - 全宗信息的分页查询
    - 全宗列表查询（带权限过滤）
    - 全宗的增删改操作
    - 全宗修改能力检查

    **全宗数据结构:**
    - id: 全宗 ID
    - fondsCode: 全宗号（唯一标识）
    - fondsName: 全宗名称
    - description: 全宗描述
    - createdAt: 创建时间
    - updatedAt: 更新时间

    **权限控制:**
    - 列表查询自动过滤用户有权限的全宗
    - 未认证用户返回空列表
    - 无全宗权限用户返回空列表
    - 超级管理员可查看所有全宗

    **修改约束:**
    - 已有档案关联的全宗不可修改全宗号
    - 使用 canModify 接口检查修改能力

    **使用场景:**
    - 全宗下拉列表填充
    - 全宗管理页面
    - 全宗切换器数据源
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/bas/fonds")
@RequiredArgsConstructor
public class BasFondsController {

    private final BasFondsService basFondsService;
    private final FondsScopeService fondsScopeService;

    @GetMapping("/page")
    @Operation(
        summary = "分页查询全宗",
        description = """
            分页查询全宗列表（不带权限过滤，需管理员权限）。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 10）

            **返回数据包括:**
            - records: 全宗记录列表
            - total: 总记录数
            - page: 当前页码
            - size: 每页大小

            **使用场景:**
            - 全宗管理页面
            - 全宗列表分页展示
            """,
        operationId = "getFondsPage",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Page<BasFonds>> getPage(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        Page<BasFonds> pageParam = new Page<>(page, limit);
        return Result.success(basFondsService.page(pageParam));
    }

    @GetMapping("/list")
    @Operation(
        summary = "查询全宗列表（带权限过滤）",
        description = """
            获取当前用户有权限访问的全宗列表。

            **权限过滤规则:**
            - 未认证用户: 返回空列表
            - 无全宗权限用户: 返回空列表
            - 有权限用户: 仅返回有权限的全宗
            - 超级管理员: 返回所有全宗

            **使用场景:**
            - 全宗下拉列表
            - 全宗切换器
            - 数据范围限定
            """,
        operationId = "getFondsList",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<List<BasFonds>> list() {
        // 从当前认证用户获取允许访问的全宗号列表
        List<String> allowedFonds = resolveAllowedFonds();

        // null 表示未认证或无法解析用户，返回空列表
        if (allowedFonds == null) {
            return Result.success(Collections.emptyList());
        }

        // 空列表表示用户没有任何全宗权限，返回空列表
        if (allowedFonds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 有权限：只返回用户有权限的全宗
        LambdaQueryWrapper<BasFonds> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BasFonds::getFondsCode, allowedFonds);
        return Result.success(basFondsService.list(wrapper));
    }

    /**
     * 解析当前用户允许访问的全宗号列表
     */
    private List<String> resolveAllowedFonds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            String userId = null;
            if (principal instanceof String) {
                userId = (String) principal;
            } else if (principal instanceof com.nexusarchive.security.CustomUserDetails userDetails) {
                userId = userDetails.getId();
            }
            if (userId != null) {
                return fondsScopeService.getAllowedFonds(userId);
            }
        }
        return null;
    }

    @GetMapping("/{id}/can-modify")
    @Operation(
        summary = "检查全宗是否可以修改",
        description = """
            检查指定全宗的全宗号是否可以修改。

            **路径参数:**
            - id: 全宗 ID

            **返回数据包括:**
            - canModify: true 表示可以修改，false 表示不可修改

            **不可修改的情况:**
            - 全宗已关联档案数据
            - 全宗有正在进行的业务流程
            - 全宗已参与账套映射

            **使用场景:**
            - 全宗编辑前检查
            - 修改按钮禁用判断
            """,
        operationId = "checkFondsCanModify",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检查成功"),
        @ApiResponse(responseCode = "404", description = "全宗不存在")
    })
    public Result<Boolean> canModify(
            @Parameter(description = "全宗ID", required = true, example = "1")
            @PathVariable String id) {
        BasFonds fonds = basFondsService.getById(id);
        if (fonds == null) {
            return Result.error("全宗不存在");
        }
        return Result.success(basFondsService.canModifyFondsCode(fonds.getFondsCode()));
    }

    @PostMapping
    @Operation(
        summary = "创建全宗",
        description = """
            创建新的全宗记录。

            **请求体:**
            - fondsCode: 全宗号（必填，唯一）
            - fondsName: 全宗名称（必填）
            - description: 全宗描述（可选）

            **业务规则:**
            - 全宗号必须唯一
            - 全宗号不能为空
            - 全宗名称不能为空

            **使用场景:**
            - 新增全宗
            - 法人档案初始化
            """,
        operationId = "createFonds",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或全宗号已存在"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Boolean> save(
            @Parameter(description = "全宗对象", required = true)
            @Valid @RequestBody BasFonds fonds) {
        if (fonds.getFondsCode() == null || fonds.getFondsName() == null) {
            return Result.error("Fonds Code and Name are required");
        }
        return Result.success(basFondsService.save(fonds));
    }

    @PutMapping
    @Operation(
        summary = "更新全宗",
        description = """
            更新已有全宗的信息。

            **请求体:**
            - id: 全宗 ID（必填）
            - fondsCode: 全宗号（需满足修改约束）
            - fondsName: 全宗名称
            - description: 全宗描述

            **业务规则:**
            - 使用带约束的更新方法
            - 全宗号修改需满足 canModify 检查

            **使用场景:**
            - 全宗信息更新
            - 全宗名称变更
            """,
        operationId = "updateFonds",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或不满足修改约束"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "全宗不存在")
    })
    public Result<Boolean> update(
            @Parameter(description = "全宗对象", required = true)
            @Valid @RequestBody BasFonds fonds) {
        // 使用带约束的更新方法
        return Result.success(basFondsService.updateFonds(fonds));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除全宗",
        description = """
            删除指定的全宗记录。

            **路径参数:**
            - id: 全宗 ID

            **业务规则:**
            - 已有关联数据的全宗不可删除
            - 删除操作会级联处理相关数据

            **使用场景:**
            - 全宗注销
            - 测试数据清理
            """,
        operationId = "deleteFonds",
        tags = {"基础全宗管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "全宗有关联数据，无法删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "全宗不存在")
    })
    public Result<Boolean> remove(
            @Parameter(description = "全宗ID", required = true, example = "1")
            @PathVariable String id) {
        return Result.success(basFondsService.removeById(id));
    }
}
