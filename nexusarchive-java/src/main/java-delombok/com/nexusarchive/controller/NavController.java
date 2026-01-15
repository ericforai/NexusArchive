// Input: io.swagger、Lombok、Spring Framework、Java 标准库、等
// Output: NavController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.mapper.ArchiveMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 动态导航菜单控制器
 *
 * PRD 来源: 导航菜单模块
 * 负责提供基于数据的动态菜单结构
 */
@Tag(name = "导航接口", description = """
    动态菜单与路由接口。

    **功能说明:**
    - 根据全宗号获取存在的账簿类型
    - 提供基于数据的动态菜单

    **账簿类型:**
    - 总账
    - 明细账
    - 现金日记账
    - 银行存款日记账
    - 其他辅助账簿

    **动态菜单原理:**
    - 根据当前全宗的实际数据生成菜单
    - 无数据的账簿类型不显示
    - 支持前端动态渲染菜单

    **使用场景:**
    - 动态菜单生成
    - 账簿类型筛选
    - 全宗切换后菜单更新

    **权限要求:**
    - archive:read: 档案查看权限
    - nav:all: 全部导航权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/nav")
@RequiredArgsConstructor
@Slf4j
public class NavController {

    private final ArchiveMapper archiveMapper;

    /**
     * 获取账簿类型列表
     */
    @GetMapping("/books")
    @Operation(
        summary = "获取账簿类型列表",
        description = """
            根据全宗号获取系统中存在的会计账簿类型。

            **查询参数:**
            - fondsNo: 全宗号（默认: DEMO）

            **返回数据包括:**
            - 账簿类型列表（如：总账、明细账、现金日记账等）

            **账簿类型来源:**
            - 从 acc_archive 表的 sub_type 字段聚合获取
            - 只返回当前全宗有数据的账簿类型

            **业务规则:**
            - 过滤空值和空字符串
            - 返回结果去重

            **使用场景:**
            - 动态生成账簿菜单
            - 账簿类型筛选器
            - 全宗切换后菜单更新
            """,
        operationId = "getBookTypes",
        tags = {"导航接口"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAuthority('archive:read') or hasAuthority('nav:all')")
    public List<String> getBookTypes(
            @Parameter(description = "全宗号", example = "DEMO")
            @RequestParam(defaultValue = "DEMO") String fondsNo) {
        log.info("Fetching dynamic book types for fonds: {}", fondsNo);
        List<String> types = archiveMapper.selectDistinctBookTypes(fondsNo);

        // 过滤空值
        if (types != null) {
            return types.stream()
                .filter(t -> t != null && !t.isEmpty())
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
