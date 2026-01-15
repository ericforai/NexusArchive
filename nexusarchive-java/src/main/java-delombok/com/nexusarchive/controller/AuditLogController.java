// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、DtoMapper、AuditLogResponse
// Output: AuditLogController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.AuditLogResponse;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志查询控制器
 *
 * <p>提供审计日志的查询功能，支持分页和多条件过滤</p>
 */
@Tag(name = "审计日志查询", description = """
    审计日志查询接口。

    **功能说明:**
    - 分页查询审计日志
    - 按用户 ID 过滤
    - 按资源类型过滤
    - 按操作类型过滤

    **日志字段:**
    - id: 日志 ID
    - userId: 操作用户 ID
    - username: 操作用户名
    - action: 操作类型（CREATE、UPDATE、DELETE、QUERY等）
    - resourceType: 资源类型（Archive、Fonds、User等）
    - resourceId: 资源 ID
    - dataBefore: 操作前数据（JSON）
    - dataAfter: 操作后数据（JSON）
    - ipAddress: 操作 IP 地址
    - userAgent: 用户代理
    - logHash: SM3 日志哈希（防篡改）
    - prevLogHash: 前一条日志哈希（链式验证）
    - createdAt: 操作时间

    **防篡改机制:**
    - SM3 哈希链：每条日志包含前一条日志的哈希值
    - 链式验证：通过 `verifyLogChain` 方法验证日志完整性
    - 不可修改：已写入的日志不允许修改或删除

    **使用场景:**
    - 安全审计员查看操作记录
    - 三员分立合规检查（GB/T 39784-2021）
    - 异常操作追溯
    - 数据变更追踪

    **权限要求:**
    - audit:view: 审计查看权限
    - audit_logs: 审计日志权限
    - AUDIT_ADMIN: 安全审计员角色
    - SYSTEM_ADMIN: 系统管理员角色
    - nav:all: 超级管理员权限
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('audit:view') or hasAuthority('audit_logs') or hasRole('AUDIT_ADMIN') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;
    private final DtoMapper dtoMapper;

    @GetMapping
    @Operation(
        summary = "分页查询审计日志",
        description = """
            分页查询审计日志，支持按用户、资源类型、操作类型过滤。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 20）
            - userId: 用户 ID（可选，精确匹配）
            - resourceType: 资源类型（可选，精确匹配）
            - action: 操作类型（可选，精确匹配）

            **返回数据包括:**
            - records: 日志记录列表
            - total: 总记录数
            - page: 当前页码
            - size: 每页大小

            **使用场景:**
            - 审计员查看特定用户的操作记录
            - 查询特定资源的变更历史
            - 按操作类型筛选日志

            **排序规则:**
            - 默认按操作时间倒序排列（最新在前）
            """,
        operationId = "listAuditLogs",
        tags = {"审计日志查询"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    public Result<Page<AuditLogResponse>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "用户ID（可选，精确匹配）", example = "user123")
            @RequestParam(required = false) String userId,
            @Parameter(description = "资源类型（可选，如 Archive、Fonds、User）", example = "Archive")
            @RequestParam(required = false) String resourceType,
            @Parameter(description = "操作类型（可选，如 CREATE、UPDATE、DELETE）", example = "UPDATE")
            @RequestParam(required = false) String action
    ) {
        Page<SysAuditLog> entityPage = auditLogQueryService.query(page, limit, userId, resourceType, action);
        return Result.success(dtoMapper.toAuditLogResponsePage(entityPage));
    }
}
