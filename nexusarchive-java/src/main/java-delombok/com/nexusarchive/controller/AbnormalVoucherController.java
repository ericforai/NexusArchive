// Input: Lombok、Spring Framework、Jakarta EE、Spring Security、等
// Output: AbnormalVoucherController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.entity.AbnormalVoucher;
import com.nexusarchive.service.AbnormalVoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 异常凭证处理控制器
 *
 * 提供异常凭证的查询、重试、更新功能
 */
@Tag(name = "异常凭证处理", description = """
    异常凭证处理接口。

    **功能说明:**
    - 查询待处理的异常凭证列表
    - 重新处理失败的凭证
    - 更新凭证的 SIP 数据

    **异常类型:**
    - PARSING_ERROR: 解析失败（SIP 格式错误）
    - VALIDATION_ERROR: 校验失败（四性检测不通过）
    - STORAGE_ERROR: 存储失败（文件写入错误）
    - BUSINESS_ERROR: 业务错误（数据冲突等）

    **凭证状态:**
    - PENDING: 待处理
    - RETRYING: 重试中
    - RESOLVED: 已解决
    - FAILED: 失败（放弃处理）

    **处理流程:**
    1. 凭证导入失败时创建异常记录
    2. 管理员查看异常列表
    3. 修正数据后重试处理
    4. 或手动更新 SIP 数据后重试

    **使用场景:**
    - 批量导入失败处理
    - 数据质量异常修复
    - 手动干预处理流程
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/v1/abnormal")
@RequiredArgsConstructor
@Validated
public class AbnormalVoucherController {

    private final AbnormalVoucherService abnormalVoucherService;

    @GetMapping
    @Operation(
        summary = "查询待处理异常凭证列表",
        description = """
            获取所有待处理的异常凭证记录。

            **返回数据包括:**
            - id: 异常记录 ID
            - voucherId: 凭证 ID
            - abnormalType: 异常类型
            - errorMessage: 错误消息
            - retryCount: 重试次数
            - status: 处理状态
            - createdAt: 创建时间

            **使用场景:**
            - 异常凭证管理页面
            - 批量导入错误查看
            - 数据质量监控
            """,
        operationId = "listPendingAbnormals",
        tags = {"异常凭证处理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<List<AbnormalVoucher>> listPending() {
        return Result.success(abnormalVoucherService.getPendingAbnormals());
    }

    @PostMapping("/{id}/retry")
    @Operation(
        summary = "重新处理异常凭证",
        description = """
            对失败的凭证进行重新处理。

            **路径参数:**
            - id: 异常记录 ID

            **业务规则:**
            - 仅对待处理状态的凭证可重试
            - 重试次数限制：最多 5 次
            - 重试后状态更新为 RETRYING
            - 重试成功后状态变更为 RESOLVED

            **使用场景:**
            - 修正数据后重新导入
            - 临时故障恢复后重试
            - 手动触发处理流程
            """,
        operationId = "retryAbnormalVoucher",
        tags = {"异常凭证处理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "重试请求已提交"),
        @ApiResponse(responseCode = "400", description = "参数错误或超过重试次数"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "异常记录不存在")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<Void> retry(
            @Parameter(description = "异常记录ID", required = true, example = "1")
            @PathVariable String id) {
        if (id == null || id.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "id 不能为空");
        }
        abnormalVoucherService.retry(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新异常凭证的 SIP 数据",
        description = """
            更新异常凭证的 SIP（Submission Information Package）数据。

            **路径参数:**
            - id: 异常记录 ID

            **请求体:**
            - AccountingSipDto: 会计凭证 SIP 数据

            **业务规则:**
            - 更新后可配合 retry 接口重新处理
            - SIP 数据需符合 DA/T 94-2022 规范

            **使用场景:**
            - 修正解析错误的原始数据
            - 补充缺失的元数据
            - 更新文件路径信息
            """,
        operationId = "updateAbnormalVoucherSip",
        tags = {"异常凭证处理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "异常记录不存在")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<Void> updateSip(
            @Parameter(description = "异常记录ID", required = true, example = "1")
            @PathVariable String id,
            @Parameter(description = "SIP数据", required = true)
            @Valid @RequestBody AccountingSipDto sipDto) {
        if (id == null || id.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "id 不能为空");
        }
        abnormalVoucherService.updateSipData(id, sipDto);
        return Result.success();
    }
}
