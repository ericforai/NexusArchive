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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.nexusarchive.common.constants.HttpConstants;

import java.util.List;

/**
 * 全局搜索控制器
 * <p>
 * 提供跨模块的全局搜索功能，支持档案、全宗、用户等多维度搜索。
 * 搜索结果按相关性排序，返回最多 50 条记录。
 * </p>
 */
@Tag(name = "全局搜索", description = "跨模块全局搜索接口，支持档案、全宗、用户等多维度搜索")
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
            description = "根据关键词在多个模块中执行搜索，包括档案、全宗、用户、案卷等。搜索结果按相关性排序，最多返回 50 条记录。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "搜索成功",
                    content = @Content(
                            mediaType = HttpConstants.APPLICATION_JSON,
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
