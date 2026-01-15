// Input: Spring Web、Lombok、LoginAttemptService、ArchiveService
// Output: DebugController 类 (REST Endpoints)
// Pos: 调试接口层 (仅限内部/开发使用)
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.service.LoginAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 调试控制器
 *
 * PRD 来源: 开发工具（非产品功能）
 * 提供内部调试接口
 *
 * ⚠️ 调试接口 - 仅限内部/开发使用
 * 此控制器包含调试功能，仅供开发团队内部使用。
 * 生产环境部署时应禁用或删除。
 */
@Tag(name = "调试工具", description = """
    内部调试接口（开发工具）。

    **功能说明:**
    - 解锁被锁定的用户账户

    **登录锁定机制:**
    - 连续 5 次密码错误后锁定账户
    - 锁定时长: 15 分钟
    - 使用 Redis 分布式锁

    **解锁方式:**
    - 等待锁定超时（15 分钟）
    - 调用此接口强制解锁
    - 成功登录后自动清除

    **使用场景:**
    - 开发环境测试
    - 用户解锁
    - 账户恢复

    **权限要求:**
    - 仅限开发环境
    - 生产环境应禁用
    - """)
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final LoginAttemptService loginAttemptService;

    /**
     * 解锁用户账户
     */
    @PostMapping("/unlock/{username}")
    @Operation(
        summary = "解锁用户账户",
        description = """
            清除指定用户的登录失败记录，解锁被锁定的账户。

            **路径参数:**
            - username: 用户名

            **业务规则:**
            - 清除 Redis 中的登录失败计数
            - 重置锁定状态
            - 立即生效

            **使用场景:**
            - 开发环境测试
            - 用户忘记密码后解锁
            - 演示环境快速恢复

            **注意事项:**
            - 此接口仅供内部使用
            - 生产环境应禁用
            """,
        operationId = "unlockUserAccount",
        tags = {"调试工具"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "解锁成功"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public String unlockUser(
            @Parameter(description = "用户名", required = true, example = "admin")
            @PathVariable String username) {
        loginAttemptService.recordSuccess(username);
        return "User " + username + " unlocked (login attempts cleared).";
    }

}
