// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: WebIngestController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Web 端归档控制器
 *
 * <p>处理前端直接上传的文件，提供简单易用的归档入口</p>
 */
@Tag(name = "Web 文件上传", description = """
    Web 端文件上传与归档接口。

    **功能说明:**
    - 单文件上传
    - 批量文件上传
    - 自动创建 SIP 包
    - 自动调用归档接收服务

    **支持格式:**
    - PDF: 便携式文档格式
    - JPEG/PNG: 图片格式
    - TIFF: 标签图像文件格式
    - OFD: 国家标准版式文档
    - DOCX/DOC: Word 文档（需转换）

    **文件限制:**
    - 单文件大小: ≤ 100MB
    - 批量上传: ≤ 20 个文件
    - 总大小: ≤ 500MB

    **处理流程:**
    1. 接收上传文件
    2. 文件格式验证
    3. 病毒扫描
    4. 创建 SIP 包
    5. 调用 IngestService
    6. 返回文件 ID

    **安全检查:**
    - 文件类型验证（Magic Number）
    - ClamAV 病毒扫描
    - 文件大小限制
    - 恶意文件检测

    **使用场景:**
    - 用户手动上传档案
    - 批量文件导入
    - 临时文件归档
    - 扫描文件上传

    **权限要求:**
    - archive:upload: 档案上传权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@Slf4j
public class WebIngestController {

    private final IngestService ingestService;

    /**
     * 文件上传接口
     *
     * @param file 上传的文件
     * @return 上传后的文件路径或ID
     */
    @PostMapping("/upload")
    @Operation(
        summary = "上传文件并归档",
        description = """
            接收前端上传的文件，自动创建 SIP 并调用归档接收服务。

            **请求参数:**
            - file: 上传的文件（MultipartFile）

            **返回数据 (FileUploadResponse):**
            - fileId: 文件 ID
            - fileName: 文件名
            - fileSize: 文件大小（字节）
            - contentType: 内容类型
            - uploadTime: 上传时间
            - sipId: SIP 包 ID（已创建）

            **业务规则:**
            - 单文件最大 100MB
            - 自动生成文件 ID
            - 自动创建 SIP 包
            - 自动进行病毒扫描
            - 扫描通过后才归档

            **错误处理:**
            - 文件过大: 413 Payload Too Large
            - 格式不支持: 415 Unsupported Media Type
            - 病毒检测: 403 Forbidden（含病毒）
            - 存储失败: 500 Internal Server Error

            **使用场景:**
            - 用户手动上传档案
            - 批量文件导入
            - 扫描文件上传
            """,
        operationId = "uploadFile",
        tags = {"Web 文件上传"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限或文件含病毒"),
        @ApiResponse(responseCode = "413", description = "文件过大"),
        @ApiResponse(responseCode = "415", description = "不支持的文件格式"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public Result<com.nexusarchive.dto.FileUploadResponse> uploadFile(
            @Parameter(description = "上传的文件", required = true)
            @RequestParam("file") MultipartFile file) {
        log.info("收到文件上传请求: {}", file.getOriginalFilename());
        com.nexusarchive.dto.FileUploadResponse response = ingestService.handleFileUpload(file);
        return Result.success("上传成功", response);
    }
}
