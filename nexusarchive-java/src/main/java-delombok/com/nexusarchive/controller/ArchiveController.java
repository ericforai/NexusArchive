// Input: MyBatis-Plus、io.swagger、Jakarta EE、Lombok、DtoMapper、ArchiveResponse
// Output: ArchiveController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.ArchiveResponse;
import com.nexusarchive.dto.response.ArchiveFileResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

/**
 * 档案管理控制器
 *
 * PRD 来源: 电子会计档案核心模块
 * 提供电子会计档案的 CRUD 操作
 *
 * <p>符合 DA/T 94-2022《电子会计档案管理规范》</p>
 * <p>所有返回值使用 DTO，避免直接暴露 Entity</p>
 */
@Tag(name = "档案管理", description = """
    电子会计档案核心管理接口。

    **功能说明:**
    - 分页查询档案列表
    - 获取档案详情
    - 获取档案关联文件
    - 获取最近创建的档案
    - 创建新档案
    - 更新档案元数据
    - 逻辑删除档案

    **档案类别:**
    - 会计凭证: 记账凭证及附件
    - 会计账簿: 总账、明细账、日记账等
    - 财务报表: 资产负债表、利润表等
    - 其他会计资料: 审计报告、验资报告等

    **档案状态:**
    - DRAFT: 草稿
    - PENDING: 待归档
    - ARCHIVED: 已归档
    - FROZEN: 已冻结
    - DESTROYED: 已销毁

    **保管期限:**
    - 永久: 永久保存
    - 30年: 30年
    - 10年: 10年

    **使用场景:**
    - 档案检索查询
    - 档案元数据管理
    - 档案生命周期管理

    **权限要求:**
    - archive:read: 查看权限
    - archive:manage: 管理权限
    - nav:all: 全部导航权限
    - SYSTEM_ADMIN: 系统管理员
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
@Validated
public class ArchiveController {

    private final ArchiveService archiveService;
    private final DtoMapper dtoMapper;

    /**
     * 分页查询档案
     */
    @GetMapping
    @Operation(
        summary = "分页查询档案",
        description = """
            根据条件分页检索档案列表，支持多维度搜索。

            **查询参数:**
            - page: 页码（从 1 开始）
            - limit: 每页条数（最多 100）
            - search: 搜索关键词（档号、标题模糊搜索）
            - status: 档案状态过滤
            - categoryCode: 类别号过滤
            - orgId: 部门ID过滤
            - subType: 子类型（账簿类型/报表周期）
            - uniqueBizId: 唯一业务ID
            - fondsNo: 全宗号过滤

            **返回数据包括:**
            - records: 档案记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **业务规则:**
            - 按当前用户全宗过滤
            - 支持多条件组合查询
            - 默认按创建时间倒序

            **使用场景:**
            - 档案列表展示
            - 档案检索
            """,
        operationId = "listArchives",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<ArchiveResponse>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") int page,

            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10")
            @Max(value = 100, message = "每页最多显示100条") int limit,
            @Parameter(description = "搜索关键词", example = "发票") @RequestParam(required = false) String search,
            @Parameter(description = "状态", example = "ARCHIVED") @RequestParam(required = false) String status,
            @Parameter(description = "类别号", example = "PZ") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "部门ID") @RequestParam(required = false) String orgId,
            @Parameter(description = "子类型(账簿类型/报表周期/其他类型)") @RequestParam(required = false) String subType,
            @Parameter(description = "唯一业务ID") @RequestParam(required = false) String uniqueBizId,
            @Parameter(description = "全宗号", example = "F001") @RequestParam(required = false) String fondsNo) {
        Page<Archive> entityPage = archiveService.getArchives(page, limit, search, status, categoryCode, orgId, uniqueBizId, subType, fondsNo);
        return Result.success(dtoMapper.toArchiveResponsePage(entityPage));
    }

    /**
     * 获取档案详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取档案详情",
        description = """
            根据ID获取档案详细信息。

            **路径参数:**
            - id: 档案ID

            **返回数据包括:**
            - id: 档案ID
            - archiveCode: 档号
            - title: 题名
            - categoryCode: 类别号
            - docDate: 文件日期
            - retentionPeriod: 保管期限
            - fondsNo: 全宗号
            - status: 状态
            - metadata: 扩展元数据

            **使用场景:**
            - 档案详情页
            - 档案编辑预填充
            """,
        operationId = "getArchiveDetail",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<ArchiveResponse> get(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String id) {
        Archive archive = archiveService.getArchiveById(id);
        return Result.success(dtoMapper.toArchiveResponse(archive));
    }

    /**
     * 获取档案关联文件
     */
    @GetMapping("/{id}/files")
    @Operation(
        summary = "获取档案关联文件",
        description = """
            获取档案关联的所有文件列表。

            **路径参数:**
            - id: 档案ID

            **返回数据包括:**
            - fileId: 文件ID
            - fileName: 文件名
            - fileType: 文件类型
            - fileSize: 文件大小
            - storagePath: 存储路径

            **文件类型:**
            - PDF: 便携式文档格式
            - OFD: 版式文档
            - XML: 元数据文件
            - 附件: 原始凭证附件

            **使用场景:**
            - 档案文件列表
            - 文件下载入口
            """,
        operationId = "getArchiveFiles",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveFileResponse>> getArchiveFiles(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String id) {
        List<com.nexusarchive.entity.ArcFileContent> files = archiveService.getFilesByArchiveId(id);
        return Result.success(dtoMapper.toArchiveFileResponseList(files));
    }

    /**
     * 获取最近档案
     */
    @GetMapping("/recent")
    @Operation(
        summary = "获取最近档案",
        description = """
            获取最近创建的档案列表。

            **查询参数:**
            - limit: 返回数量（默认 5，最多 50）

            **返回数据包括:**
            - 档案基本信息列表
            - 按创建时间倒序

            **使用场景:**
            - 首页最近档案
            - 快速访问入口
            """,
        operationId = "getRecentArchives",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveResponse>> recent(
            @Parameter(description = "返回数量", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        List<Archive> archives = archiveService.getRecentArchives(limit);
        return Result.success(dtoMapper.toArchiveResponseList(archives));
    }

    /**
     * 创建档案
     */
    @PostMapping
    @Operation(
        summary = "创建档案",
        description = """
            创建新的电子会计档案。

            **请求体:**
            - archiveCode: 档号（必填）
            - title: 题名（必填）
            - categoryCode: 类别号（必填）
            - docDate: 文件日期（必填）
            - retentionPeriod: 保管期限
            - fondsNo: 全宗号
            - metadata: 扩展元数据

            **返回数据:**
            - 创建的档案详细信息

            **业务规则:**
            - 档号需唯一
            - 自动记录创建人
            - 初始状态为 DRAFT

            **使用场景:**
            - 手动创建档案
            - 数据导入
            """,
        operationId = "createArchive",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或档号重复"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @ArchivalAudit(operationType = "CREATE", resourceType = "ARCHIVE", description = "创建新档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<ArchiveResponse> create(
            @Valid @RequestBody Archive archive,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Archive created = archiveService.createArchive(archive, userId);
        return Result.success(dtoMapper.toArchiveResponse(created));
    }

    /**
     * 更新档案
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "更新档案",
        description = """
            更新现有档案的元数据。

            **路径参数:**
            - id: 档案ID

            **请求体:**
            - title: 题名
            - docDate: 文件日期
            - retentionPeriod: 保管期限
            - metadata: 扩展元数据

            **注意:**
            - 档号、类别号不可修改
            - 更新操作会被审计记录

            **使用场景:**
            - 档案元数据修正
            - 补充档案信息
            """,
        operationId = "updateArchive",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ARCHIVE", description = "更新档案元数据")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> update(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String id,
            @Valid @RequestBody Archive archive,
            HttpServletRequest request) {
        archiveService.updateArchive(id, archive);
        return Result.success();
    }

    /**
     * 删除档案
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除档案",
        description = """
            逻辑删除档案（非物理删除）。

            **路径参数:**
            - id: 档案ID

            **业务规则:**
            - 逻辑删除，数据仍保留
            - 已归档档案不可删除
            - 删除操作会被审计记录

            **使用场景:**
            - 移除错误创建的档案
            - 清理测试数据
            """,
        operationId = "deleteArchive",
        tags = {"档案管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "409", description = "档案状态不允许删除")
    })
    @ArchivalAudit(operationType = "DELETE", resourceType = "ARCHIVE", description = "删除档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(
            @Parameter(description = "档案ID", required = true, example = "arc-001")
            @PathVariable String id,
            HttpServletRequest request) {
        archiveService.deleteArchive(id);
        return Result.success();
    }
}
