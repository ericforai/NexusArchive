// Input: Spring Web、JDBC、本地模块
// Output: DatabaseFixController 类
// Pos: Controller 层 - 数据库修复
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。
//
// ⚠️ 临时修复工具 - 不属于产品功能
// 此控制器用于数据库临时修复，仅供内部使用。
// 生产环境部署时应删除。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 临时数据库修复控制器
 *
 * PRD 来源: 临时工具（非产品功能）
 * 提供数据库表结构临时修复功能
 *
 * ⚠️ 临时修复工具 - 不属于产品功能
 * 此控制器用于数据库临时修复，仅供内部使用。
 * 生产环境部署时应删除。
 */
@Tag(name = "数据库修复", description = """
    临时数据库修复接口（开发工具）。

    **功能说明:**
    - 修复表结构缺失字段
    - 添加缺失的列

    **支持的操作:**
    - 修复 biz_borrowing.updated_at 字段

    **修复规则:**
    - 使用 IF NOT EXISTS 避免重复
    - 设置默认值
    - 不影响现有数据

    **使用场景:**
    - 数据迁移修复
    - 表结构补丁

    **注意事项:**
    - 此接口仅供内部使用
    - 生产环境应禁用
    - """)
@RestController
@RequestMapping("/db-fix")
@RequiredArgsConstructor
public class DatabaseFixController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 修复借阅表更新时间字段
     */
    @PostMapping("/fix-borrowing-updated-at")
    @Operation(
        summary = "修复借阅表更新时间字段",
        description = """
            为 biz_borrowing 表添加 updated_at 字段。

            **业务规则:**
            - 使用 IF NOT EXISTS 避免重复添加
            - 设置默认值为 CURRENT_TIMESTAMP
            - 操作幂等可重复执行

            **SQL:**
            ALTER TABLE biz_borrowing
            ADD COLUMN IF NOT EXISTS updated_at
            TIMESTAMP WITHOUT TIME ZONE
            DEFAULT CURRENT_TIMESTAMP

            **使用场景:**
            - 数据迁移后修复
            - 表结构补丁
            """,
        operationId = "fixBorrowingUpdatedAt",
        tags = {"数据库修复"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "修复成功"),
        @ApiResponse(responseCode = "500", description = "修复失败")
    })
    public Result<String> fixBorrowingUpdatedAt(
            @Parameter(description = "请求负载（预留）", required = false)
            @RequestBody Map<String, String> payload) {
        String sql = "ALTER TABLE biz_borrowing ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP";
        try {
            jdbcTemplate.execute(sql);
            return Result.success("修复成功：biz_borrowing.updated_at 列已添加");
        } catch (Exception e) {
            return Result.error("修复失败: " + e.getMessage());
        }
    }
}
