// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、DtoMapper、VolumeResponse
// Output: VolumeController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.ArchiveResponse;
import com.nexusarchive.dto.response.VolumeResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.service.VolumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

/**
 * 组卷与归档审核 Controller
 * 符合 DA/T 104-2024 组卷规范
 * 所有返回值使用 DTO，避免直接暴露 Entity
 */
@Tag(name = "案卷管理", description = """
    组卷与归档审核接口。

    **功能说明:**
    - 按月自动组卷
    - 案卷列表查询
    - 案卷审核流程
    - 案卷导出（AIP 格式）

    **案卷状态:**
    - DRAFT: 草稿（组卷中）
    - PENDING_REVIEW: 待审核
    - APPROVED: 已审核
    - REJECTED: 已驳回
    - ARCHIVED: 已归档
    - HANDED_OVER: 已移交

    **组卷规范 (DA/T 104-2024):**
    - 按会计期间（月度）组卷
    - 同一账簿类型的档案归入同一卷
    - 每卷档案数量建议不超过 200 件
    - 案卷编号规则：{全宗号}-{年度}-{月份}-{流水号}

    **审核流程:**
    1. 组卷完成 → 提交审核
    2. 待审核 → 审核通过/驳回
    3. 审核通过 → 归档
    4. 归档完成 → 移交档案管理部门

    **使用场景:**
    - 月度组卷操作
    - 案卷审核管理
    - 案卷导出移交
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/volumes")
@RequiredArgsConstructor
@Slf4j
public class VolumeController {

    private final VolumeService volumeService;
    private final DtoMapper dtoMapper;

    /**
     * 按月自动组卷
     * POST /api/volumes/assemble
     */
    @PostMapping("/assemble")
    @Operation(
        summary = "按月自动组卷",
        description = """
            按照会计期间（月度）自动组卷。

            **请求体:**
            - fiscalPeriod: 财政期间（格式：YYYY-MM，必填）

            **组卷规则 (DA/T 104-2024):**
            - 同一月份的凭证归入同一卷
            - 按账簿类型分别组卷
            - 自动生成案卷编号和封面信息

            **返回数据包括:**
            - id: 案卷ID
            - volumeCode: 案卷编号
            - fiscalPeriod: 会计期间
            - archiveCount: 卷内档案数量
            - status: 案卷状态

            **使用场景:**
            - 月末组卷操作
            - 批量组卷
            """,
        operationId = "assembleVolumeByMonth",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "组卷成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或组卷失败"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> assembleByMonth(
            @Parameter(description = "组卷请求", required = true)
            @Valid @RequestBody AssembleRequest request) {
        log.info("请求组卷: fiscalPeriod={}", request.getFiscalPeriod());

        Volume volume = volumeService.assembleByMonth(request.getFiscalPeriod());
        VolumeResponse response = dtoMapper.toVolumeResponse(volume);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "组卷成功");
        result.put("data", response);
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 获取案卷列表
     * GET /api/volumes
     */
    @GetMapping
    @Operation(
        summary = "获取案卷列表",
        description = """
            分页查询案卷列表。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 20）
            - status: 状态过滤（可选）

            **返回数据包括:**
            - records: 案卷记录列表
            - total: 总记录数
            - page: 当前页码
            - limit: 每页大小

            **使用场景:**
            - 案卷列表展示
            - 分页查询
            """,
        operationId = "getVolumeList",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> getVolumeList(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "状态过滤")
            @RequestParam(required = false) String status) {

        Page<Volume> pageResult = volumeService.getVolumeList(page, limit, status);
        Page<VolumeResponse> responsePage = dtoMapper.toVolumeResponsePage(pageResult);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", Map.of(
                "records", responsePage.getRecords(),
                "total", responsePage.getTotal(),
                "page", responsePage.getCurrent(),
                "limit", responsePage.getSize()
        ));
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 获取案卷详情
     * GET /api/volumes/{id}
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取案卷详情",
        description = """
            获取指定案卷的详细信息。

            **路径参数:**
            - id: 案卷ID

            **返回数据包括:**
            - id: 案卷ID
            - volumeCode: 案卷编号
            - fiscalPeriod: 会计期间
            - archiveCount: 卷内档案数量
            - status: 案卷状态
            - createdAt: 创建时间

            **使用场景:**
            - 案卷详情查看
            - 案卷编辑预填充
            """,
        operationId = "getVolumeDetail",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> getVolumeDetail(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) {
        Volume volume = volumeService.getVolumeById(id);
        VolumeResponse response = dtoMapper.toVolumeResponse(volume);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", response);
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 获取卷内文件列表
     * GET /api/volumes/{id}/files
     */
    @GetMapping("/{id}/files")
    @Operation(
        summary = "获取卷内文件列表",
        description = """
            获取指定案卷的所有档案文件列表。

            **路径参数:**
            - id: 案卷ID

            **返回数据包括:**
            - id: 档案ID
            - title: 档案标题
            - docDate: 日期
            - amount: 金额
            - voucherNo: 凭证号

            **使用场景:**
            - 卷内档案查看
            - 案卷内容浏览
            """,
        operationId = "getVolumeFiles",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> getVolumeFiles(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) {
        List<Archive> files = volumeService.getVolumeFiles(id);
        List<ArchiveResponse> responseList = dtoMapper.toArchiveResponseList(files);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", responseList);
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 提交案卷审核
     * POST /api/volumes/{id}/submit-review
     */
    @PostMapping("/{id}/submit-review")
    @Operation(
        summary = "提交案卷审核",
        description = """
            将案卷提交到审核流程。

            **路径参数:**
            - id: 案卷ID

            **业务规则:**
            - 只有草稿或驳回状态的案卷可以提交审核
            - 提交后状态变更为 PENDING_REVIEW

            **使用场景:**
            - 组卷完成后提交审核
            - 驳回后重新提交
            """,
        operationId = "submitVolumeForReview",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许提交"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> submitForReview(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) {
        volumeService.submitForReview(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已提交审核");
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 审核通过并归档
     * POST /api/volumes/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @Operation(
        summary = "审核通过并归档",
        description = """
            审核通过案卷并进行归档处理。

            **路径参数:**
            - id: 案卷ID

            **查询参数:**
            - reviewerId: 审核人ID（默认：system）

            **业务规则:**
            - 只有待审核状态的案卷可以审核通过
            - 审核通过后状态变更为 ARCHIVED

            **使用场景:**
            - 案卷审核通过
            - 自动归档处理
            """,
        operationId = "approveVolumeArchival",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "归档成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许归档"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> approveArchival(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id,
            @Parameter(description = "审核人ID")
            @RequestParam(defaultValue = "system") String reviewerId) {

        volumeService.approveArchival(id, reviewerId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "归档成功");
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 审核驳回
     * POST /api/volumes/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @Operation(
        summary = "审核驳回",
        description = """
            驳回案卷审核。

            **路径参数:**
            - id: 案卷ID

            **请求体:**
            - reviewerId: 审核人ID
            - reason: 驳回原因（必填）

            **业务规则:**
            - 只有待审核状态的案卷可以驳回
            - 驳回后状态变更为 REJECTED

            **使用场景:**
            - 案卷审核驳回
            - 需要修改后重新提交
            """,
        operationId = "rejectVolumeArchival",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "驳回成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许驳回"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> rejectArchival(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id,
            @Parameter(description = "驳回请求", required = true)
            @Valid @RequestBody RejectRequest request) {

        volumeService.rejectArchival(id, request.getReviewerId(), request.getReason());

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已驳回");
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 移交档案管理部门
     * POST /api/volumes/{id}/handover
     */
    @PostMapping("/{id}/handover")
    @Operation(
        summary = "移交档案管理部门",
        description = """
            将已归档的案卷移交到档案管理部门。

            **路径参数:**
            - id: 案卷ID

            **业务规则:**
            - 只有已归档状态的案卷可以移交
            - 移交后状态变更为 HANDED_OVER
            - 移交操作会记录审计日志

            **使用场景:**
            - 档案正式移交
            - 档案馆接收
            """,
        operationId = "handoverVolume",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "移交成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许移交"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> handoverToArchives(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) {

        volumeService.handoverToArchives(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "移交成功");
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 获取归档登记表
     * GET /api/volumes/{id}/registration-form
     */
    @GetMapping("/{id}/registration-form")
    @Operation(
        summary = "获取归档登记表",
        description = """
            生成案卷的归档登记表。

            **路径参数:**
            - id: 案卷ID

            **返回数据包括:**
            - volumeCode: 案卷编号
            - archiveCount: 卷内档案数量
            - retentionPeriod: 保管期限
            - confidentiality: 密级
            - archivist: 立卷人
            - archivistDate: 立卷日期

            **使用场景:**
            - 归档登记表打印
            - 档案移交清单
            """,
        operationId = "getRegistrationForm",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在")
    })
    public org.springframework.http.ResponseEntity<Map<String, Object>> getRegistrationForm(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) {
        Map<String, Object> form = volumeService.generateRegistrationForm(id);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "操作成功");
        result.put("data", form);
        return org.springframework.http.ResponseEntity.ok(result);
    }

    /**
     * 导出案卷 AIP 包
     * GET /api/volumes/{id}/export-aip
     */
    @GetMapping("/{id}/export-aip")
    @Operation(
        summary = "导出案卷 AIP 包",
        description = """
            导出案卷的 AIP（Archival Information Package）归档信息包。

            **路径参数:**
            - id: 案卷ID

            **AIP 包结构 (GB/T 39674):**
            - index.xml: 元数据描述文件
            - data/: 档案数据目录
            - files/: 原始文件目录

            **响应:**
            - Content-Type: application/octet-stream
            - Content-Disposition: attachment; filename="{案卷编号}.zip"

            **使用场景:**
            - 档案移交导出
            - 馆际交换
            - 长期保存
            """,
        operationId = "exportAipPackage",
        tags = {"案卷管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导出成功，返回 ZIP 文件"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "案卷不存在"),
        @ApiResponse(responseCode = "500", description = "导出失败")
    })
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> exportAipPackage(
            @Parameter(description = "案卷ID", required = true, example = "vol-001")
            @PathVariable String id) throws java.io.IOException {
        java.io.File zipFile = volumeService.exportAipPackage(id);

        org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(new java.io.FileInputStream(zipFile));

        String filename = zipFile.getName();

        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipFile.length())
                .body(resource);
    }

    @Data
    public static class AssembleRequest {
        private String fiscalPeriod; // YYYY-MM
    }

    @Data
    public static class RejectRequest {
        private String reviewerId;
        private String reason;
    }
}
