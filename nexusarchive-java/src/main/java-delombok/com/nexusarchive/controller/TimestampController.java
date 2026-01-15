// Input: io.swagger、Lombok、Spring Security、Spring Framework、等
// Output: TimestampController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.service.TimestampService;
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

import java.util.Base64;

/**
 * 时间戳控制器
 *
 * PRD 来源: 电子签名模块
 * 提供时间戳请求和验证功能
 *
 * <p>合规要求：DA/T 94-2022 电子会计档案管理规范</p>
 * <p>支持：RFC 3161 Time-Stamp Protocol (TSP)</p>
 *
 * @author Agent B - 合规开发工程师
 */
@Tag(name = "时间戳服务", description = """
    时间戳请求和验证接口。

    **功能说明:**
    - 请求数字时间戳
    - 验证时间戳令牌
    - 检查服务状态

    **时间戳协议:**
    - RFC 3161 Time-Stamp Protocol (TSP)
    - 支持可信时间戳服务（TSA）
    - 符合 DA/T 94-2022 规范

    **请求参数:**
    - data: 待加时间戳的数据（Base64 编码）
    - archiveId: 关联的档案 ID（可选）

    **返回数据包括:**
    - success: 请求是否成功
    - timestamp: 时间戳值
    - timestampToken: 时间戳令牌（Base64）
    - errorMessage: 错误信息

    **验证结果包括:**
    - valid: 时间戳是否有效
    - timestamp: 时间戳值
    - serialNumber: 序列号
    - errorMessage: 错误信息

    **服务状态:**
    - available: 服务是否可用
    - message: 状态描述

    **使用场景:**
    - 档案时间戳固化
    - 电子凭证防篡改
    - 合规性验证

    **权限要求:**
    - SYSTEM_ADMIN 角色
    - SECURITY_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/timestamp")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','SECURITY_ADMIN')")
public class TimestampController {

    private final TimestampService timestampService;

    /**
     * 请求时间戳
     */
    @PostMapping("/request")
    @Operation(
        summary = "请求时间戳",
        description = """
            为数据请求可信时间戳。

            **请求参数:**
            - data: 待加时间戳的数据（Base64 编码，必填）
            - archiveId: 关联的档案 ID（可选）

            **返回数据包括:**
            - success: 请求是否成功
            - timestamp: 时间戳值（ISO 8601 格式）
            - timestampToken: 时间戳令牌（Base64 编码）
            - serialNumber: 时间戳序列号
            - errorMessage: 错误信息（失败时）

            **业务规则:**
            - 数据需 Base64 编码
            - 调用 RFC 3161 TSA
            - 记录关联档案 ID

            **使用场景:**
            - 档案固化
            - 凭证时间戳
            """,
        operationId = "requestTimestamp",
        tags = {"时间戳服务"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "请求成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "时间戳服务不可用")
    })
    public Result<TimestampService.TimestampResult> requestTimestamp(
            @Parameter(description = "待加时间戳的数据（Base64编码）", required = true, example = "SGVsbG8gV29ybGQ=")
            @RequestParam String data,
            @Parameter(description = "关联的档案ID", example = "arc-001")
            @RequestParam(required = false) String archiveId) {

        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            TimestampService.TimestampResult result = timestampService.requestTimestamp(dataBytes);

            if (result.isSuccess()) {
                log.info("时间戳请求成功: 档案ID={}, 时间={}", archiveId, result.getTimestamp());
            } else {
                log.warn("时间戳请求失败: 档案ID={}, 错误={}", archiveId, result.getErrorMessage());
            }

            return Result.success(result);
        } catch (Exception e) {
            log.error("时间戳请求异常: {}", e.getMessage(), e);
            return Result.fail("时间戳请求失败: " + e.getMessage());
        }
    }

    /**
     * 验证时间戳
     */
    @PostMapping("/verify")
    @Operation(
        summary = "验证时间戳",
        description = """
            验证时间戳令牌的有效性。

            **请求参数:**
            - data: 原始数据（Base64 编码，必填）
            - timestampToken: 时间戳令牌（Base64 编码，必填）

            **返回数据包括:**
            - valid: 时间戳是否有效
            - timestamp: 时间戳值
            - serialNumber: 时间戳序列号
            - errorMessage: 错误信息（验证失败时）

            **验证内容:**
            - 签名验证
            - 时间戳有效期
            - 数据完整性

            **使用场景:**
            - 档案合规验证
            - 时间戳校验
            """,
        operationId = "verifyTimestamp",
        tags = {"时间戳服务"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<TimestampService.TimestampVerifyResult> verifyTimestamp(
            @Parameter(description = "原始数据（Base64编码）", required = true, example = "SGVsbG8gV29ybGQ=")
            @RequestParam String data,
            @Parameter(description = "时间戳令牌（Base64编码）", required = true, example = "MIIB9gYJK...")
            @RequestParam String timestampToken) {

        try {
            byte[] dataBytes = Base64.getDecoder().decode(data);
            TimestampService.TimestampVerifyResult result = timestampService.verifyTimestamp(dataBytes, timestampToken);

            return Result.success(result);
        } catch (Exception e) {
            log.error("时间戳验证异常: {}", e.getMessage(), e);
            return Result.fail("时间戳验证失败: " + e.getMessage());
        }
    }

    /**
     * 检查时间戳服务状态
     */
    @GetMapping("/status")
    @Operation(
        summary = "检查时间戳服务状态",
        description = """
            检查时间戳服务是否可用。

            **返回数据包括:**
            - available: 服务是否可用
            - message: 状态描述

            **检查项:**
            - TSA 服务连通性
            - 配置完整性

            **使用场景:**
            - 服务健康检查
            - 配置验证
            """,
        operationId = "getTimestampServiceStatus",
        tags = {"时间戳服务"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检查完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ServiceStatus> getServiceStatus() {
        boolean available = timestampService.isAvailable();

        ServiceStatus status = new ServiceStatus();
        status.setAvailable(available);
        status.setMessage(available ? "时间戳服务可用" : "时间戳服务不可用，请检查配置");

        return Result.success(status);
    }

    /**
     * 服务状态 DTO
     */
    public static class ServiceStatus {
        private boolean available;
        private String message;

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
