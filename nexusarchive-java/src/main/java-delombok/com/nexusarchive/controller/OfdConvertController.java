// Input: io.swagger、Lombok、Spring Security、Spring Framework、等
// Output: OfdConvertController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.OfdConvertService;
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

import java.util.List;
import java.util.Map;

/**
 * 电子档案格式转换控制器
 *
 * <p>提供将档案转换为 OFD 格式的功能</p>
 */
@Tag(name = "电子档案格式转换", description = """
    档案格式转换为 OFD 接口。

    **功能说明:**
    - 单个档案转换为 OFD 格式
    - 批量档案转换为 OFD 格式
    - 转换状态查询
    - 转换结果下载

    **OFD 格式:**
    - GB/T 33190-2016《电子文件存储与交换格式 版式文档》
    - 国家标准版式文档格式
    - 支持数字签名验证
    - 支持长期保存

    **转换流程:**
    1. 获取源文件（PDF/图片等）
    2. 转换为 OFD 格式
    3. 嵌入数字签名（可选）
    4. 存储 OFD 文件
    5. 返回转换结果

    **支持源格式:**
    - PDF: 便携式文档格式
    - JPEG/PNG: 图片格式
    - TIFF: 标签图像文件格式
    - DOCX: Word 文档（需预转换）

    **转换限制:**
    - 单个文件大小: ≤ 100MB
    - 批量转换数量: ≤ 50 个
    - 超时时间: 10 分钟

    **使用场景:**
    - 档案长期保存格式转换
    - 标准化格式输出
    - 数字签名嵌入

    **权限要求:**
    - archive:edit: 档案编辑权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
public class OfdConvertController {

    private final OfdConvertService ofdConvertService;

    @PostMapping("/{id}/convert-to-ofd")
    @Operation(
        summary = "单档案转换为 OFD",
        description = """
            将指定档案的文件转换为 OFD 格式。

            **路径参数:**
            - id: 档案 ID

            **返回数据包括:**
            - success: 转换是否成功
            - ofdFileId: OFD 文件 ID
            - downloadUrl: 下载链接
            - fileSize: 文件大小（字节）
            - convertTime: 转换耗时（毫秒）

            **业务规则:**
            - 转换后生成新的文件记录
            - 原文件保留不变
            - OFD 文件关联到原档案
            - 支持断点续传

            **错误处理:**
            - 档案不存在: 404
            - 不支持的格式: 400
            - 文件过大: 413
            - 转换超时: 504

            **使用场景:**
            - 单个档案格式转换
            - 归档前格式标准化
            """,
        operationId = "convertToOfd",
        tags = {"电子档案格式转换"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "转换成功"),
        @ApiResponse(responseCode = "400", description = "不支持的格式或参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "413", description = "文件过大"),
        @ApiResponse(responseCode = "504", description = "转换超时")
    })
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Map<String, Object>> convertToOfd(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String id) {
        return Result.success(ofdConvertService.convertToOfd(id));
    }

    @PostMapping("/batch-convert-to-ofd")
    @Operation(
        summary = "批量转换为 OFD",
        description = """
            批量将多个档案的文件转换为 OFD 格式。

            **请求体:**
            - 档案 ID 列表（最多 50 个）

            **返回数据包括:**
            - totalCount: 总数
            - successCount: 成功数
            - failedCount: 失败数
            - results: 转换结果列表
              - archiveId: 档案 ID
              - success: 是否成功
              - ofdFileId: OFD 文件 ID（成功时）
              - error: 错误信息（失败时）

            **业务规则:**
            - 异步执行转换
            - 返回任务 ID
            - 支持进度查询
            - 失败不影响其他文件

            **使用场景:**
            - 批量档案格式转换
            - 定时格式标准化
            - 历史数据处理
            """,
        operationId = "batchConvertToOfd",
        tags = {"电子档案格式转换"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量转换任务已提交"),
        @ApiResponse(responseCode = "400", description = "参数错误或超过批量限制"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAuthority('archive:edit')")
    public Result<Integer> batchConvertToOfd(
            @Parameter(description = "档案ID列表", required = true) @RequestBody List<String> ids) {
        return Result.success(ofdConvertService.batchConvert(ids));
    }
}
