// Input: Spring Web、EnterpriseArchitectureService、Result
// Output: EnterpriseArchitectureController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.EnterpriseArchitectureTree;
import com.nexusarchive.service.EnterpriseArchitectureService;
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

/**
 * 集团架构控制器
 *
 * PRD 来源: 集团架构视图模块
 * 功能: 提供集团架构树视图数据（法人 -> 全宗 -> 档案）
 *
 * <p>实现三层架构树视图，支持法人、全宗、档案层级展示</p>
 */
@Slf4j
@Tag(name = "集团架构", description = """
    集团架构树视图接口。

    **功能说明:**
    - 获取完整的集团架构树
    - 获取指定法人下的架构树

    **架构层级:**
    ```
    集团
    └── 法人 (Entity)
        └── 全宗 (Fonds)
            └── 档案 (Archive)
    ```

    **树节点类型:**
    - ENTITY: 法人节点
    - FONDS: 全宗节点
    - ARCHIVE: 档案节点

    **节点属性:**
    - id: 节点唯一标识
    - name: 节点名称
    - type: 节点类型
    - code: 编码（法人代码/全宗号）
    - children: 子节点列表
    - archiveCount: 档案数量（全宗节点）
    - metadata: 扩展元数据

    **使用场景:**
    - 集团架构可视化
    - 跨法人数据查询
    - 组织架构管理
    - 档案归属展示

    **权限要求:**
    - entity:view 权限
    - fonds:view 权限
    - SYS_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/enterprise-architecture")
@RequiredArgsConstructor
public class EnterpriseArchitectureController {

    private final EnterpriseArchitectureService architectureService;

    /**
     * 获取完整的集团架构树
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAnyAuthority('entity:view', 'fonds:view') or hasRole('SYS_ADMIN')")
    @Operation(
        summary = "获取完整的集团架构树",
        description = """
            返回整个集团的完整架构树，包括所有法人、全宗和档案。

            **返回数据包括:**
            - id: 根节点ID
            - name: 集团名称
            - type: ROOT
            - children: 法人节点列表
              - id: 法人ID
              - name: 法人名称
              - type: ENTITY
              - code: 法人代码
              - children: 全宗节点列表
                - id: 全宗ID
                - name: 全宗名称
                - type: FONDS
                - code: 全宗号
                - archiveCount: 档案数量
                - children: 档案节点列表

            **业务规则:**
            - 按法人 -> 全宗 -> 档案三层组织
            - 仅返回用户有权限访问的数据
            - 空节点可选择性过滤

            **使用场景:**
            - 集团架构全景视图
            - 跨法人档案导航
            """,
        operationId = "getEnterpriseArchitectureTree",
        tags = {"集团架构"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<EnterpriseArchitectureTree> getArchitectureTree() {
        try {
            EnterpriseArchitectureTree tree = architectureService.getArchitectureTree();
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取集团架构树失败", e);
            return Result.error("获取集团架构树失败: " + e.getMessage());
        }
    }

    /**
     * 获取指定法人下的架构树
     */
    @GetMapping("/tree/entity/{entityId}")
    @PreAuthorize("hasAnyAuthority('entity:view', 'fonds:view') or hasRole('SYS_ADMIN')")
    @Operation(
        summary = "获取指定法人下的架构树",
        description = """
            返回指定法人及其下属全宗、档案的架构树。

            **路径参数:**
            - entityId: 法人ID

            **返回数据包括:**
            - id: 法人节点ID
            - name: 法人名称
            - type: ENTITY
            - code: 法人代码
            - children: 全宗节点列表
              - id: 全宗ID
              - name: 全宗名称
              - type: FONDS
              - code: 全宗号
              - archiveCount: 档案数量
              - children: 档案节点列表

            **业务规则:**
            - 仅返回指定法人下的数据
            - 按全宗 -> 档案两层组织
            - 法人不存在时返回空树

            **使用场景:**
            - 单法人视图
            - 法人下属档案查询
            """,
        operationId = "getEntityArchitectureTree",
        tags = {"集团架构"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "法人不存在")
    })
    public Result<EnterpriseArchitectureTree> getArchitectureTreeByEntity(
            @Parameter(description = "法人ID", required = true, example = "entity-001")
            @PathVariable String entityId) {
        try {
            EnterpriseArchitectureTree tree = architectureService.getArchitectureTreeByEntity(entityId);
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取法人架构树失败: entityId={}", entityId, e);
            return Result.error("获取法人架构树失败: " + e.getMessage());
        }
    }
}
