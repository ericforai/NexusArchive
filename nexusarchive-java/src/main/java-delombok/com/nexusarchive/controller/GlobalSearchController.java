// Input: Lombok、Spring Security、Spring Framework、Java 标准库、Swagger OpenAPI
// Output: GlobalSearchController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.dto.GlobalSearchDTO;
import com.nexusarchive.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 全局搜索控制器
 *
 * <p>提供跨模块的全局搜索功能，支持档案、全宗、用户等多维度搜索。
 * 搜索结果按相关性排序，返回最多 50 条记录。</p>
 */
@Tag(name = "全局搜索", description = """
    跨模块全局搜索接口。

    **功能说明:**
    - 跨模块统一搜索入口
    - 支持档案、全宗、用户、案卷等多维度搜索
    - 搜索结果按相关性排序
    - 最多返回 50 条记录

    **搜索范围:**
    - 档案 (Archive): 档案号、标题、摘要、金额
    - 全宗 (Fonds): 全宗号、全宗名称、描述
    - 用户 (User): 用户名、姓名、邮箱
    - 案卷 (Volume): 案卷号、案卷标题
    - 凭证池 (Pool): 凭证号、摘要

    **搜索规则:**
    - 支持模糊匹配
    - 按相关性评分排序
    - 自动过滤无权限数据
    - 高亮匹配关键词

    **返回数据包括:**
    - type: 结果类型（Archive、Fonds、User、Volume、Pool）
    - id: 记录 ID
    - title: 标题
    - summary: 摘要
    - url: 详情链接
    - relevance: 相关性评分

    **使用场景:**
    - 顶部全局搜索框
    - 快速数据定位
    - 跨模块数据检索

    **权限要求:**
    - archive:read: 档案读取权限
    - archive:manage: 档案管理权限
    - nav:all: 超级管理员权限
    - SYSTEM_ADMIN: 系统管理员角色
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class GlobalSearchController {

    private final GlobalSearchService globalSearchService;

    /**
     * 全局搜索
     * <p>
     * 根据关键词在档案、全宗、用户等多个模块中执行搜索。
     * 搜索结果按相关性排序，返回匹配度最高的前 50 条记录。
     * </p>
     *
     * @param query 搜索关键词，支持档案号、标题、全宗名称、用户名等
     * @return 搜索结果列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "全局搜索",
        description = """
            根据关键词在多个模块中执行搜索，包括档案、全宗、用户、案卷等。搜索结果按相关性排序，最多返回 50 条记录。

            **查询参数:**
            - q: 搜索关键词

            **支持的搜索字段:**
            - 档案: 档案号、标题、摘要、金额
            - 全宗: 全宗号、全宗名称、描述
            - 用户: 用户名、姓名、邮箱
            - 案卷: 案卷号、案卷标题
            - 凭证池: 凭证号、摘要

            **搜索特性:**
            - 模糊匹配: 支持部分关键词匹配
            - 相关性排序: 按匹配度和权重排序
            - 权限过滤: 自动过滤无权限数据
            - 结果限制: 最多返回 50 条

            **使用场景:**
            - 顶部搜索框输入
            - 快速定位档案
            - 跨模块数据检索
            """,
        operationId = "globalSearch",
        tags = {"全局搜索"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "搜索成功",
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = GlobalSearchDTO.class))
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未授权 - 未登录或 token 过期"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "无权限 - 当前用户无搜索权限"
        )
    })
    public List<GlobalSearchDTO> search(
            @Parameter(
                description = "搜索关键词，支持档案号、标题、全宗名称、用户名等",
                required = true,
                example = "2024年度"
            )
            @RequestParam("q") String query) {
        return globalSearchService.search(query);
    }
}
