// Input: Lombok、Spring Framework、Spring Security、Java 标准库、等
// Output: ArchiveExportController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.service.ArchiveExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;

/**
 * 档案导出控制器
 *
 * PRD 来源: 档案导出模块
 * 提供档案 AIP 包导出功能
 *
 * <p>符合 GB/T 39674 AIP 归档信息包导出格式</p>
 */
@Tag(name = "档案导出", description = """
    档案 AIP 包导出接口。

    **功能说明:**
    - 导出单个档案的 AIP 包

    **AIP 包格式 (GB/T 39674):**
    - ZIP 压缩格式
    - 包含 index.xml（元数据）
    - 包含原始文件内容
    - 包含签名信息
    - 包含时间戳令牌

    **AIP 包结构:**
    ```
    {archivalCode}.zip
    ├── index.xml          # 归档信息包元数据
    ├── content/            # 原始文件内容
    │   ├── original.pdf    # 原始 PDF/OFD
    │   └── attachments/    # 附件文件
    ├── signature/          # 签名信息
    │   └── sign.p7s        # 数字签名
    └── timestamp/          # 时间戳
        └── token.tsr       # 时间戳令牌
    ```

    **导出规则:**
    - 文件名使用档号
    - 自动计算文件哈希
    - 包含完整的元数据

    **使用场景:**
    - 档案移交
    - 馆际交换
    - 长期保存备份

    **权限要求:**
    - archive:read 权限
    - archive:export 权限
    - nav:all 全部导航权限
    - SYSTEM_ADMIN 系统管理员
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ArchiveExportController {

    private final ArchiveExportService exportService;

    /**
     * 导出 AIP 包
     */
    @GetMapping("/aip/{archivalCode}")
    @Operation(
        summary = "导出 AIP 包",
        description = """
            导出单个档案的归档信息包（AIP）为 ZIP 文件。

            **路径参数:**
            - archivalCode: 档号

            **返回数据:**
            - ZIP 文件流
            - Content-Type: application/octet-stream
            - Content-Disposition: attachment

            **文件名格式:**
            - {档号}.zip
            - 例如: BR-GROUP-2024-30Y-FIN-AC01-0001.zip

            **AIP 包包含:**
            - index.xml: 归档信息包元数据
            - content/: 原始文件内容
            - signature/: 数字签名
            - timestamp/: 时间戳令牌

            **业务规则:**
            - 实时生成 ZIP 文件
            - 自动计算文件哈希值
            - 包含完整的四性检测报告

            **使用场景:**
            - 档案移交导出
            - 馆际交换
            - 长期保存备份
            """,
        operationId = "exportAipPackage",
        tags = {"档案导出"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导出成功，返回 ZIP 文件"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "500", description = "导出失败")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:export','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "EXPORT", resourceType = "ARCHIVE", description = "导出 AIP 包")
    public ResponseEntity<Resource> downloadAipPackage(
            @Parameter(description = "档号", required = true, example = "BR-GROUP-2024-30Y-FIN-AC01-0001")
            @PathVariable String archivalCode) throws IOException {
        log.info("收到 AIP 包导出请求: {}", archivalCode);

        // 生成 ZIP 文件
        File zipFile = exportService.exportAipPackage(archivalCode);

        // 使用 FileSystemResource 避免流被多次读取的问题
        FileSystemResource resource = new FileSystemResource(zipFile);

        // 设置响应头
        String filename = archivalCode + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipFile.length())
                .body(resource);
    }
}
