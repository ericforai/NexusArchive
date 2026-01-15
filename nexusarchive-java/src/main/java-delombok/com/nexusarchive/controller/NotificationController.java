// Input: Lombok、Spring Framework、Swagger/OpenAPI、Java 标准库、本地模块
// Output: NotificationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.notification.NotificationDto;
import com.nexusarchive.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知消息控制器
 *
 * PRD 来源: 消息通知模块
 * 提供系统通知的查询功能
 */
@Tag(name = "消息通知", description = """
    系统通知消息接口。

    **功能说明:**
    - 获取当前用户的最新通知列表
    - 支持已读/未读状态过滤
    - 自动按时间倒序排列

    **通知类型:**
    - SYSTEM: 系统通知
    - APPROVAL: 审批通知
    - TASK: 任务通知
    - ALERT: 告警通知

    **通知状态:**
    - UNREAD: 未读
    - READ: 已读
    - ARCHIVED: 已归档

    **业务规则:**
    - 只返回当前用户的通知
    - 默认返回最新 50 条
    - 自动过滤过期通知
    - 读取后自动标记为已读

    **使用场景:**
    - 首页通知展示
    - 消息中心
    - 待办提醒

    **权限要求:**
    - 所有认证用户可访问
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping
    @Operation(
        summary = "获取通知列表",
        description = """
            获取当前用户的最新通知列表。

            **返回数据包括:**
            - id: 通知 ID
            - type: 通知类型（SYSTEM/APPROVAL/TASK/ALERT）
            - title: 通知标题
            - content: 通知内容
            - status: 状态（UNREAD/READ/ARCHIVED）
            - createTime: 创建时间
            - link: 关联链接（可选）

            **业务规则:**
            - 只返回当前用户的通知
            - 按创建时间倒序排列
            - 默认返回最新 50 条
            - 自动过滤已过期通知

            **使用场景:**
            - 首页通知栏
            - 消息中心列表
            - 未读消息提示
            """,
        operationId = "listNotifications",
        tags = {"消息通知"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<List<NotificationDto>> list() {
        return Result.success(notificationService.listLatest());
    }
}
