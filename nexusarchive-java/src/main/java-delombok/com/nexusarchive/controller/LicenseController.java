// Input: Lombok、Spring Security、Spring Framework、Swagger/OpenAPI、Java 标准库、本地模块
// Output: LicenseController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * License 管理控制器
 *
 * PRD 来源: 系统管理模块
 * 提供 License 加载和查询功能
 */
@Tag(name = "License 管理", description = """
    License 许可证管理接口。

    **功能说明:**
    - 加载/激活 License
    - 查询当前 License 信息
    - License 有效性验证

    **License 类型:**
    - TRIAL: 试用版（90天）
    - STANDARD: 标准版
    - PROFESSIONAL: 专业版
    - ENTERPRISE: 企业版

    **授权限制:**
    - maxNodes: 最大节点数
    - maxUsers: 最大用户数
    - maxFonds: 最大全宗数
    - maxStorage: 最大存储容量（GB）

    **License 加密:**
    - 使用非对称加密（RSA 2048）
    - 签名防篡改验证
    - 绑定机器指纹（可选）

    **验证规则:**
    - 过期日期检查
    - 节点数限制检查
    - 签名有效性验证

    **使用场景:**
    - 系统激活
    - License 更新/续费
    - 授权信息查询

    **权限要求:**
    - 加载接口匿名可访问（系统激活时）
    - 查询接口需要认证
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    /**
     * 加载 License
     */
    @PostMapping("/load")
    // 注意：此接口需对匿名用户开放，因为系统激活时尚未登录
    @ArchivalAudit(operationType = "LOAD_LICENSE", resourceType = "LICENSE", description = "加载/切换 License")
    @Operation(
        summary = "加载 License",
        description = """
            加载并验证新的 License 文本。

            **请求参数:**
            - licenseText: License 文本内容（Base64 编码的 JSON）

            **返回数据包括:**
            - valid: 是否有效
            - productId: 产品 ID
            - licenseType: License 类型（TRIAL/STANDARD/PROFESSIONAL/ENTERPRISE）
            - expiryDate: 过期日期
            - maxNodes: 最大节点数
            - maxUsers: 最大用户数
            - maxFonds: 最大全宗数
            - maxStorage: 最大存储容量（GB）
            - features: 授权功能列表

            **验证流程:**
            1. 解码 Base64 文本
            2. 验证签名完整性
            3. 检查过期日期
            4. 验证授权范围

            **错误处理:**
            - 签名无效: 返回 400
            - 已过期: 返回 400
            - 格式错误: 返回 400

            **使用场景:**
            - 系统首次激活
            - License 续费更新
            - 版本升级
            """,
        operationId = "loadLicense",
        tags = {"License 管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "License 加载成功"),
        @ApiResponse(responseCode = "400", description = "License 无效或已过期"),
        @ApiResponse(responseCode = "403", description = "超出授权限制（节点数/用户数）")
    })
    public Result<LicenseService.LicenseInfo> load(
            @Parameter(description = "License 文本内容（Base64 编码）", required = true,
                    schema = @Schema(type = "string", format = "base64",
                    example = "eyJwcm9kdWN0SWQiOiJuZXh1cy1hcmNoaXZlIiwiZXhwaXJ5RGF0ZSI6IjIwMjYtMTItMzEifQ=="))
            @RequestBody String licenseText) {
        return Result.success("License 加载成功", licenseService.validate(licenseText));
    }

    /**
     * 查询当前 License
     */
    @GetMapping
    @Operation(
        summary = "查询当前 License",
        description = """
            获取当前系统使用的 License 信息。

            **返回数据包括:**
            - valid: 是否有效
            - productId: 产品 ID
            - licenseType: License 类型
            - expiryDate: 过期日期
            - daysRemaining: 剩余天数
            - maxNodes: 最大节点数
            - currentNodes: 当前节点数
            - maxUsers: 最大用户数
            - maxFonds: 最大全宗数
            - maxStorage: 最大存储容量（GB）
            - features: 授权功能列表
              - ADVANCED_SEARCH: 高级检索
              - AI_ANALYSIS: AI 分析
              - MULTI_TENANT: 多租户
              - ERP_INTEGRATION: ERP 集成

            **业务规则:**
            - 返回当前生效的 License
            - 无 License 时返回默认值
            - 过期后显示 expired 状态

            **使用场景:**
            - 系统信息展示
            - License 到期提醒
            - 功能权限判断
            """,
        operationId = "getCurrentLicense",
        tags = {"License 管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<LicenseService.LicenseInfo> current() {
        return Result.success(licenseService.current());
    }
}
