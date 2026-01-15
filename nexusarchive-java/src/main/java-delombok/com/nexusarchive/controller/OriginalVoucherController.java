// Input: Spring Web、io.swagger、Jakarta EE、Lombok
// Output: OriginalVoucherController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.service.OriginalVoucherService;
import com.nexusarchive.service.OriginalVoucherService.OriginalVoucherStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 原始凭证管理控制器
 *
 * <p>提供原始凭证的 CRUD、版本控制、关联管理、归档流程等 REST API</p>
 *
 * <p>Reference: DA/T 94-2022, 《原始凭证模块设计规范 V1.0》</p>
 */
@Tag(name = "原始凭证", description = """
    原始凭证管理接口。

    **功能说明:**
    - 原始凭证的增删改查
    - 版本历史管理
    - 关联文件管理
    - 记账凭证关联
    - 归档流程控制
    - 凭证类型字典
    - 统计分析

    **凭证类型:**
    - **发票类**: 增值税发票、普通发票、电子发票
    - **收据类**: 收款收据、付款凭证
    - **单据类**: 入库单、出库单、领料单
    - **合同类**: 采购合同、销售合同
    - **其他**: 银行回单、对账单

    **归档状态:**
    - DRAFT: 草稿
    - PENDING: 待归档
    - ARCHIVED: 已归档
    - RETURNED: 已退回

    **业务规则:**
    - 已归档凭证不可删除，只能创建新版本
    - 支持多文件关联（发票、附件等）
    - 记账凭证关联后同步状态
    - 归档需经过审批流程

    **版本控制:**
    - 更新已归档凭证自动创建新版本
    - 版本历史完整可追溯
    - 最新版本为生效版本

    **使用场景:**
    - 原始凭证采集
    - 凭证整理归档
    - 凭证查询检索
    - 关联关系管理

    **权限要求:**
    - 查看: archive:read 权限
    - 管理: archive:manage 权限
    - SYSTEM_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/original-vouchers")
@RequiredArgsConstructor
@Validated
public class OriginalVoucherController {

    private final OriginalVoucherService voucherService;

    // ===== 查询接口 =====

    /**
     * 分页查询原始凭证
     */
    @GetMapping
    @Operation(
        summary = "分页查询原始凭证",
        description = """
            根据条件分页检索原始凭证列表。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 10，最大 100）
            - search: 搜索关键词（模糊匹配凭证号/标题）
            - category: 一级类型（发票/收据/单据/合同/其他）
            - type: 二级类型（具体类型代码）
            - status: 归档状态（DRAFT/PENDING/ARCHIVED/RETURNED）
            - fondsCode: 全宗号
            - fiscalYear: 会计年度

            **返回数据包括:**
            - records: 凭证记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **使用场景:**
            - 凭证列表查询
            - 凭证检索
            """,
        operationId = "listOriginalVouchers",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<OriginalVoucher>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") @Max(100) int limit,
            @Parameter(description = "搜索关键词", example = "发票")
            @RequestParam(required = false) String search,
            @Parameter(description = "一级类型", example = "发票")
            @RequestParam(required = false) String category,
            @Parameter(description = "二级类型", example = "增值税专用发票")
            @RequestParam(required = false) String type,
            @Parameter(description = "归档状态", example = "ARCHIVED")
            @RequestParam(required = false) String status,
            @Parameter(description = "全宗号", example = "F001")
            @RequestParam(required = false) String fondsCode,
            @Parameter(description = "会计年度", example = "2024")
            @RequestParam(required = false) String fiscalYear) {
        return Result.success(
                voucherService.getVouchers(page, limit, search, category, type, status, fondsCode, fiscalYear));
    }

    /**
     * 获取原始凭证详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取原始凭证详情",
        description = """
            查询指定原始凭证的详细信息。

            **路径参数:**
            - id: 凭证ID

            **返回数据包括:**
            - 基本信息（凭证号、类型、金额、日期等）
            - 关联文件列表
            - 版本信息
            - 归档状态
            - 创建人/创建时间
            - 更新人/更新时间

            **使用场景:**
            - 凭证详情查看
            - 凭证编辑前加载
            """,
        operationId = "getOriginalVoucher",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> get(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id) {
        return Result.success(voucherService.getById(id));
    }

    /**
     * 获取关联的文件列表
     */
    @GetMapping("/{id}/files")
    @Operation(
        summary = "获取原始凭证关联的文件列表",
        description = """
            查询指定原始凭证关联的所有文件。

            **路径参数:**
            - id: 凭证ID

            **返回数据包括:**
            - fileId: 文件ID
            - fileName: 文件名
            - fileType: 文件类型
            - fileSize: 文件大小
            - fileRole: 文件角色（PRIMARY/ATTACHMENT）
            - uploadTime: 上传时间

            **使用场景:**
            - 凭证附件查看
            - 文件下载列表
            """,
        operationId = "getOriginalVoucherFiles",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherFile>> getFiles(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id) {
        return Result.success(voucherService.getFiles(id));
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/{id}/versions")
    @Operation(
        summary = "获取版本历史",
        description = """
            查询指定原始凭证的版本历史记录。

            **路径参数:**
            - id: 凭证ID

            **返回数据包括:**
            - 所有历史版本
            - 按时间倒序排列
            - 包含版本号、修改人、修改时间

            **使用场景:**
            - 版本追溯
            - 变更历史查看
            """,
        operationId = "getOriginalVoucherVersions",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucher>> getVersionHistory(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id) {
        return Result.success(voucherService.getVersionHistory(id));
    }

    /**
     * 下载原始凭证文件
     */
    @GetMapping("/files/download/{fileId}")
    @Operation(
        summary = "下载原始凭证文件内容",
        description = """
            下载指定原始凭证关联的文件。

            **路径参数:**
            - fileId: 文件ID

            **返回数据:**
            - 文件流下载

            **使用场景:**
            - 凭证文件下载
            - 原件查看
            """,
        operationId = "downloadOriginalVoucherFile",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件下载"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID", required = true, example = "file-001")
            @PathVariable String fileId) {
        return voucherService.downloadFile(fileId);
    }

    /**
     * 获取关联的记账凭证
     */
    @GetMapping("/{id}/relations")
    @Operation(
        summary = "获取关联的记账凭证",
        description = """
            查询与原始凭证关联的记账凭证列表。

            **路径参数:**
            - id: 原始凭证ID

            **返回数据包括:**
            - accountingVoucherId: 记账凭证ID
            - voucherNo: 凭证号
            - relationDesc: 关联描述
            - relationTime: 关联时间

            **使用场景:**
            - 凭证关联查看
            - 勾稽关系核对
            """,
        operationId = "getOriginalVoucherRelations",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<VoucherRelation>> getRelations(
            @Parameter(description = "原始凭证ID", required = true, example = "voucher-001")
            @PathVariable String id) {
        return Result.success(voucherService.getAccountingRelations(id));
    }

    // ===== 创建接口 =====

    /**
     * 创建原始凭证
     */
    @PostMapping
    @Operation(
        summary = "创建原始凭证",
        description = """
            创建新的原始凭证记录。

            **请求参数:**
            - voucherNo: 凭证号（必填）
            - voucherType: 凭证类型（必填）
            - amount: 金额（必填）
            - voucherDate: 凭证日期（必填）
            - title: 标题（必填）
            - category: 一级分类（必填）
            - type: 二级类型（必填）

            **业务规则:**
            - 凭证号全局唯一
            - 自动记录创建人
            - 初始状态为 DRAFT

            **使用场景:**
            - 手动录入凭证
            - 扫描识别后创建
            """,
        operationId = "createOriginalVoucher",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或凭证号重复"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "CREATE", resourceType = "ORIGINAL_VOUCHER", description = "创建原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> create(
            @Parameter(description = "原始凭证信息", required = true)
            @Valid @RequestBody OriginalVoucher voucher,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.create(voucher, userId));
    }

    // ===== 更新接口 =====

    /**
     * 更新原始凭证
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新原始凭证",
        description = """
            更新原始凭证信息。

            **路径参数:**
            - id: 凭证ID

            **请求参数:**
            - voucher: 更新的凭证内容
            - reason: 更新原因（必填）

            **业务规则:**
            - 已归档凭证会创建新版本
            - 草稿状态直接更新
            - 更新原因记录审计日志

            **使用场景:**
            - 凭证信息修正
            - 补充凭证数据
            """,
        operationId = "updateOriginalVoucher",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ORIGINAL_VOUCHER", description = "更新原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> update(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            @Parameter(description = "更新请求参数", required = true)
            @Valid @RequestBody UpdateVoucherRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return Result.success(voucherService.update(id, request.voucher(), request.reason(), userId));
    }

    // ===== 删除接口 =====

    /**
     * 删除原始凭证
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除原始凭证",
        description = """
            逻辑删除指定的原始凭证。

            **路径参数:**
            - id: 凭证ID

            **业务规则:**
            - 已归档的凭证不允许删除
            - 执行逻辑删除（标记删除）
            - 删除操作记录审计日志

            **使用场景:**
            - 草稿凭证删除
            - 错误数据清理
            """,
        operationId = "deleteOriginalVoucher",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "已归档凭证不允许删除"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "ORIGINAL_VOUCHER", description = "删除原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.delete(id, userId);
        return Result.success();
    }

    // ===== 文件管理 =====

    /**
     * 添加文件到原始凭证
     */
    @PostMapping("/{id}/files")
    @Operation(
        summary = "添加文件到原始凭证",
        description = """
            上传文件并关联到指定的原始凭证。

            **路径参数:**
            - id: 凭证ID

            **请求参数:**
            - file: 文件（multipart/form-data）
            - fileRole: 文件角色（PRIMARY=主文件，ATTACHMENT=附件，默认 PRIMARY）

            **业务规则:**
            - 支持的格式：PDF/JPEG/PNG/TIFF
            - 单文件大小限制：50MB
            - 主文件自动设为封面

            **使用场景:**
            - 上传凭证扫描件
            - 添加附件材料
            """,
        operationId = "addOriginalVoucherFile",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件添加成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误或过大"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "ADD_FILE", resourceType = "ORIGINAL_VOUCHER", description = "添加文件")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucherFile> addFile(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件角色", example = "PRIMARY")
            @RequestParam(value = "fileRole", defaultValue = "PRIMARY") String fileRole,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.addFile(id, file, fileRole, userId));
    }

    // ===== 关联管理 =====

    /**
     * 建立与记账凭证的关联
     */
    @PostMapping("/{id}/relations")
    @Operation(
        summary = "建立与记账凭证的关联",
        description = """
            建立原始凭证与记账凭证的关联关系。

            **路径参数:**
            - id: 原始凭证ID

            **请求参数:**
            - accountingVoucherId: 记账凭证ID（必填）
            - description: 关联描述（可选）

            **业务规则:**
            - 关联关系自动双向同步
            - 记录关联时间和操作人
            - 关联后可追溯勾稽关系

            **使用场景:**
            - 凭证关联建立
            - 勾稽关系维护
            """,
        operationId = "createVoucherRelation",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "关联创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "CREATE_RELATION", resourceType = "ORIGINAL_VOUCHER", description = "建立关联")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<VoucherRelation> createRelation(
            @Parameter(description = "原始凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            @Parameter(description = "关联请求参数", required = true)
            @Valid @RequestBody CreateRelationRequest req,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.createRelation(id, req.accountingVoucherId(), req.description(), userId));
    }

    /**
     * 删除关联关系
     */
    @DeleteMapping("/relations/{relationId}")
    @Operation(
        summary = "删除关联关系",
        description = """
            删除原始凭证与记账凭证的关联关系。

            **路径参数:**
            - relationId: 关联ID

            **使用场景:**
            - 解除凭证关联
            - 错误关联修正
            """,
        operationId = "deleteVoucherRelation",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "关联删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "关联不存在")
    })
    @ArchivalAudit(operationType = "DELETE_RELATION", resourceType = "ORIGINAL_VOUCHER", description = "删除关联")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> deleteRelation(
            @Parameter(description = "关联ID", required = true, example = "relation-001")
            @PathVariable String relationId) {
        voucherService.deleteRelation(relationId);
        return Result.success();
    }

    // ===== 归档流程 =====

    /**
     * 提交归档
     */
    @PostMapping("/{id}/submit")
    @Operation(
        summary = "提交归档",
        description = """
            提交原始凭证进入归档审批流程。

            **路径参数:**
            - id: 凭证ID

            **业务规则:**
            - 状态变为 PENDING
            - 触发归档审批流程
            - 记录提交人和提交时间

            **使用场景:**
            - 凭证归档提交
            - 批量归档操作
            """,
        operationId = "submitOriginalVoucherForArchive",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许提交"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE", resourceType = "ORIGINAL_VOUCHER", description = "提交归档")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> submitForArchive(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.submitForArchive(id, userId);
        return Result.success();
    }

    /**
     * 确认归档
     */
    @PostMapping("/{id}/confirm")
    @Operation(
        summary = "确认归档",
        description = """
            审批通过后确认原始凭证归档。

            **路径参数:**
            - id: 凭证ID

            **业务规则:**
            - 状态变为 ARCHIVED
            - 记录归档时间和操作人
            - 生成归档编号

            **使用场景:**
            - 归档审批通过
            - 批量归档确认
            """,
        operationId = "confirmOriginalVoucherArchive",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "确认成功"),
        @ApiResponse(responseCode = "400", description = "状态不允许确认"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ArchivalAudit(operationType = "CONFIRM_ARCHIVE", resourceType = "ORIGINAL_VOUCHER", description = "确认归档")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> confirmArchive(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String id,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.confirmArchive(id, userId);
        return Result.success();
    }

    // ===== 类型字典 =====

    /**
     * 获取所有原始凭证类型
     */
    @GetMapping("/types")
    @Operation(
        summary = "获取所有原始凭证类型",
        description = """
            查询系统中配置的所有原始凭证类型。

            **返回数据包括:**
            - typeCode: 类型代码
            - typeName: 类型名称
            - categoryCode: 所属分类
            - categoryName: 分类名称
            - sortOrder: 排序号

            **使用场景:**
            - 类型选择器
            - 凭证分类统计
            """,
        operationId = "getAllOriginalVoucherTypes",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherType>> getAllTypes() {
        return Result.success(voucherService.getAllTypes());
    }

    /**
     * 按类别获取凭证类型
     */
    @GetMapping("/types/{categoryCode}")
    @Operation(
        summary = "按类别获取凭证类型",
        description = """
            查询指定分类下的原始凭证类型。

            **路径参数:**
            - categoryCode: 分类代码（INVOICE/RECEIPT/DOCUMENT/CONTRACT/OTHER）

            **返回数据包括:**
            - 该分类下的所有类型

            **使用场景:**
            - 级联类型选择
            - 分类类型筛选
            """,
        operationId = "getOriginalVoucherTypesByCategory",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "分类不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherType>> getTypesByCategory(
            @Parameter(description = "分类代码", required = true, example = "INVOICE")
            @PathVariable String categoryCode) {
        return Result.success(voucherService.getTypesByCategory(categoryCode));
    }

    // ===== 统计 =====

    /**
     * 获取原始凭证统计
     */
    @GetMapping("/stats")
    @Operation(
        summary = "获取原始凭证统计",
        description = """
            查询原始凭证的统计数据。

            **查询参数:**
            - fondsCode: 全宗号（可选）
            - fiscalYear: 会计年度（可选）

            **返回数据包括:**
            - totalCount: 总凭证数
            - archivedCount: 已归档数
            - pendingCount: 待归档数
            - draftCount: 草稿数
            - totalAmount: 总金额
            - typeDistribution: 类型分布

            **使用场景:**
            - 统计仪表盘
            - 归档进度监控
            """,
        operationId = "getOriginalVoucherStats",
        tags = {"原始凭证"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucherStats> getStats(
            @Parameter(description = "全宗号", example = "F001")
            @RequestParam(required = false) String fondsCode,
            @Parameter(description = "会计年度", example = "2024")
            @RequestParam(required = false) String fiscalYear) {
        return Result.success(voucherService.getStats(fondsCode, fiscalYear));
    }

    // ===== DTO Records =====

    public record UpdateVoucherRequest(OriginalVoucher voucher, String reason) {
    }

    public record CreateRelationRequest(String accountingVoucherId, String description) {
    }
}
