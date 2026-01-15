// Input: Lombok、Spring Framework、Java 标准库、本地模块、DtoMapper、IngestRequestStatusResponse
// Output: IngestController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.IngestRequestStatusResponse;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.service.IngestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * SIP 接收网关控制器
 * Reference: DA/T 104-2024 接口规范
 * 所有返回值使用 DTO，避免直接暴露 Entity
 */
@Tag(name = "SIP 接收网关", description = """
    SIP（Submission Information Package）接收网关接口。

    **功能说明:**
    - 接收会计凭证 SIP 包
    - 查询接收处理状态
    - 正式归档（凭证池转 AIP）

    **规范参考:**
    - DA/T 104-2024: 电子档案接收接口规范
    - DA/T 94-2022: 电子会计档案管理规范

    **SIP 包结构:**
    - requestId: 请求 ID（唯一标识）
    - sourceSystem: 来源系统
    - businessDate: 业务日期
    - vouchers: 凭证列表
    - attachments: 附件列表
    - metadata: 元数据

    **处理流程:**
    1. 接收 SIP 包
    2. 验证数据格式
    3. 存储到凭证池
    4. 返回处理结果

    **状态类型:**
    - PENDING: 待处理
    - PROCESSING: 处理中
    - SUCCESS: 处理成功
    - FAILED: 处理失败

    **归档流程:**
    1. 凭证池数据验证
    2. 四性检测（真实性、完整性、可用性、安全性）
    3. 生成 AIP 包
    4. 存储到正式档案库

    **使用场景:**
    - ERP 系统凭证推送
    - 第三方系统集成
    - 批量凭证导入
    - 手动归档操作

    **注意事项:**
    - requestId 必须全局唯一
    - 归档操作不可逆
    - 建议先查询状态再归档
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/v1/archive/sip")

@RequiredArgsConstructor
public class IngestController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IngestController.class);

    private final IngestService ingestService;
    private final com.nexusarchive.mapper.IngestRequestStatusMapper statusMapper;
    private final DtoMapper dtoMapper;

    /**
     * 接收会计凭证 SIP 包
     *
     * @param sipDto SIP 数据包
     * @return 处理结果
     */
    @PostMapping("/ingest")
    @Operation(
        summary = "接收会计凭证 SIP 包",
        description = """
            接收并处理会计凭证 SIP（Submission Information Package）包。

            **请求体 (AccountingSipDto):**
            - requestId: 请求 ID（必填，全局唯一）
            - sourceSystem: 来源系统（必填）
            - businessDate: 业务日期（必填）
            - vouchers: 凭证列表
            - attachments: 附件列表
            - metadata: 元数据

            **返回数据 (IngestResponse):**
            - requestId: 请求 ID
            - status: 处理状态（PENDING/PROCESSING/SUCCESS/FAILED）
            - message: 处理消息
            - poolItemCount: 凭证池项目数量

            **处理规则:**
            - requestId 必须全局唯一
            - 重复 requestId 将返回已存在的状态
            - 数据验证失败返回 FAILED 状态
            - 验证通过后存储到凭证池

            **使用场景:**
            - ERP 系统凭证推送
            - 第三方系统集成
            - 批量凭证导入
            """,
        operationId = "ingestSip",
        tags = {"SIP 接收网关"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "接收成功",
            content = @Content(schema = @Schema(implementation = IngestResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "数据格式错误或验证失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "409", description = " requestId 已存在")
    })
    public Result<IngestResponse> ingestSip(
            @Parameter(description = "SIP数据包", required = true) @RequestBody @Validated AccountingSipDto sipDto) {
        log.info("收到 SIP 接收请求: requestId={}, source={}", sipDto.getRequestId(), sipDto.getSourceSystem());

        IngestResponse response = ingestService.ingestSip(sipDto);

        return Result.success("接收成功", response);
    }

    /**
     * 查询接收处理状态
     *
     * @param requestId 请求ID
     * @return 处理状态
     */
    @GetMapping("/status/{requestId}")
    @Operation(
        summary = "查询 SIP 处理状态",
        description = """
            查询指定请求 ID 的 SIP 处理状态。

            **路径参数:**
            - requestId: 请求 ID

            **返回数据 (IngestRequestStatusResponse):**
            - requestId: 请求 ID
            - status: 处理状态
            - message: 处理消息
            - createdAt: 创建时间
            - updatedAt: 更新时间

            **状态说明:**
            - PENDING: 待处理
            - PROCESSING: 处理中
            - SUCCESS: 处理成功
            - FAILED: 处理失败

            **使用场景:**
            - 轮询处理状态
            - 查询历史请求
            - 故障排查
            """,
        operationId = "getIngestStatus",
        tags = {"SIP 接收网关"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "请求 ID 不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<IngestRequestStatusResponse> getStatus(
            @Parameter(description = "请求ID", required = true, example = "req-20240101-001") @PathVariable String requestId) {
        com.nexusarchive.entity.IngestRequestStatus status = statusMapper.selectById(requestId);
        if (status == null) {
            return Result.error(404, "Request ID not found");
        }
        return Result.success(dtoMapper.toIngestRequestStatusResponse(status));
    }

    /**
     * 正式归档
     * 将凭证池中的记录转换为正式的 AIP 档案包
     *
     * @param request 归档请求（包含凭证池 ID 列表）
     * @return 归档结果
     */
    @PostMapping("/archive")
    @Operation(
        summary = "正式归档",
        description = """
            将凭证池中的记录转换为正式的 AIP（Archival Information Package）档案包。

            **请求体 (ArchiveRequest):**
            - poolItemIds: 凭证池项目 ID 列表

            **处理流程:**
            1. 验证凭证池数据
            2. 执行四性检测（真实性、完整性、可用性、安全性）
            3. 生成符合 DA/T 94-2022 的 AIP 包
            4. 存储到正式档案库
            5. 更新凭证池状态

            **注意事项:**
            - 归档操作不可逆
            - 已归档的记录无法修改
            - 建议先查询状态再归档
            - 批量归档建议分批处理

            **使用场景:**
            - 手动触发归档
            - 批量归档操作
            - 定时自动归档
            """,
        operationId = "archivePoolItems",
        tags = {"SIP 接收网关"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "归档成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或归档失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "凭证池项目不存在")
    })
    public Result<String> archivePoolItems(
            @Parameter(description = "归档请求", required = true) @RequestBody @Validated com.nexusarchive.dto.ArchiveRequest request) {
        log.info("收到正式归档请求: poolItemIds={}", request.getPoolItemIds());

        try {
            String userId = (String) ((jakarta.servlet.http.HttpServletRequest) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes().resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST)).getAttribute("userId");
            ingestService.archivePoolItems(request.getPoolItemIds(), userId);
            return Result.success("归档成功");
        } catch (Exception e) {
            log.error("归档失败", e);
            return Result.error(500, "归档失败: " + e.getMessage());
        }
    }
}
