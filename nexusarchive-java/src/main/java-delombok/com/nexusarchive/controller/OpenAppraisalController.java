// Input: MyBatis-Plus、Lombok、Spring Framework、Swagger/OpenAPI、Jakarta EE、Java 标准库
// Output: OpenAppraisalController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.OpenAppraisal;
import com.nexusarchive.service.OpenAppraisalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 开放鉴定控制器
 *
 * PRD 来源: 档案开放模块
 * 提供档案开放鉴定的管理功能
 */
@Tag(name = "开放鉴定", description = """
    档案开放鉴定管理接口。

    **功能说明:**
    - 获取鉴定任务列表
    - 获取鉴定详情
    - 创建鉴定任务
    - 提交鉴定结果

    **鉴定类型:**
    - AUTOMATIC: 自动鉴定（基于规则）
    - MANUAL: 人工鉴定

    **开放等级:**
    - PUBLIC: 向社会开放
    - INTERNAL: 向本单位开放
    - RESTRICTED: 限制开放
    - CONFIDENTIAL: 不开放

    **鉴定状态:**
    - PENDING: 待鉴定
    - IN_PROGRESS: 鉴定中
    - COMPLETED: 已完成
    - APPROVED: 已批准
    - REJECTED: 已拒绝

    **鉴定流程:**
    1. 创建鉴定任务
    2. 指定鉴定人
    3. 执行鉴定
    4. 提交鉴定结果
    5. 审核批准

    **使用场景:**
    - 档案开放审核
    - 开放等级确定
    - 鉴定结果管理

    **权限要求:**
    - 需要认证
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/open-appraisal")
@RequiredArgsConstructor
public class OpenAppraisalController {

    private final OpenAppraisalService appraisalService;

    /**
     * 获取鉴定任务列表
     */
    @GetMapping("/list")
    @Operation(
        summary = "获取鉴定任务列表",
        description = """
            分页查询鉴定任务列表。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页数量（默认 10）
            - status: 状态筛选（可选）

            **返回数据包括:**
            - records: 鉴定任务列表
            - total: 总记录数
            - current: 当前页码
            - size: 每页数量

            **使用场景:**
            - 鉴定任务列表
            - 待办事项查询
            """,
        operationId = "getAppraisalList",
        tags = {"开放鉴定"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<Page<OpenAppraisal>> getAppraisalList(
            @Parameter(description = "页码", example = "1",
                    schema = @Schema(minimum = "1", defaultValue = "1"))
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量", example = "10",
                    schema = @Schema(minimum = "1", maximum = "100", defaultValue = "10"))
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "状态筛选", example = "PENDING")
            @RequestParam(required = false) String status) {
        Page<OpenAppraisal> result = appraisalService.getAppraisalList(page, limit, status);
        return Result.success(result);
    }

    /**
     * 获取鉴定详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取鉴定详情",
        description = """
            根据鉴定 ID 查询详细信息。

            **路径参数:**
            - id: 鉴定记录 ID

            **返回数据包括:**
            - id: 鉴定 ID
            - archiveId: 档案 ID
            - appraisalType: 鉴定类型
            - status: 鉴定状态
            - appraiserId: 鉴定人 ID
            - appraiserName: 鉴定人姓名
            - appraisalResult: 鉴定结果
            - openLevel: 开放等级
            - reason: 鉴定理由
            - createTime: 创建时间

            **使用场景:**
            - 鉴定详情查看
            - 鉴定结果确认
            """,
        operationId = "getAppraisalDetail",
        tags = {"开放鉴定"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "鉴定记录不存在")
    })
    public Result<OpenAppraisal> getAppraisalById(
            @Parameter(description = "鉴定记录 ID", required = true)
            @PathVariable String id) {
        OpenAppraisal appraisal = appraisalService.getAppraisalById(id);
        if (appraisal == null) {
            return Result.error(404, "Appraisal record not found");
        }
        return Result.success(appraisal);
    }

    /**
     * 创建鉴定任务
     */
    @PostMapping("/create")
    @Operation(
        summary = "创建鉴定任务",
        description = """
            创建新的档案开放鉴定任务。

            **请求参数:**
            - archiveId: 档案 ID（必填）
            - appraisalType: 鉴定类型（必填）
            - appraiserId: 鉴定人 ID（必填）
            - description: 鉴定描述（可选）

            **业务规则:**
            - 自动生成鉴定任务编号
            - 初始状态为 PENDING
            - 记录创建时间和创建人

            **使用场景:**
            - 发起鉴定流程
            - 批量创建鉴定
            """,
        operationId = "createAppraisal",
        tags = {"开放鉴定"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<OpenAppraisal> createAppraisal(
            @Parameter(description = "鉴定任务对象", required = true)
            @Valid @RequestBody OpenAppraisal appraisal) {
        try {
            OpenAppraisal created = appraisalService.createAppraisal(appraisal);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 提交鉴定结果
     */
    @PostMapping("/submit")
    @Operation(
        summary = "提交鉴定结果",
        description = """
            提交档案开放鉴定结果。

            **请求参数:**
            - id: 鉴定记录 ID（必填）
            - appraiserId: 鉴定人 ID（必填）
            - appraiserName: 鉴定人姓名（必填）
            - appraisalResult: 鉴定结果（必填）
              - APPROVED: 同意开放
              - REJECTED: 不同意开放
            - openLevel: 开放等级（必填）
              - PUBLIC: 向社会开放
              - INTERNAL: 向本单位开放
              - RESTRICTED: 限制开放
              - CONFIDENTIAL: 不开放
            - reason: 鉴定理由（必填）

            **业务规则:**
            - 只能提交状态为 PENDING 或 IN_PROGRESS 的鉴定
            - 提交后状态变更为 COMPLETED
            - 需要进一步审核批准

            **使用场景:**
            - 完成鉴定
            - 提交审核
            """,
        operationId = "submitAppraisal",
        tags = {"开放鉴定"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许提交"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "鉴定记录不存在")
    })
    public Result<Void> submitAppraisal(
            @Parameter(description = "鉴定提交请求", required = true)
            @Valid @RequestBody AppraisalSubmitRequest request) {
        try {
            appraisalService.submitAppraisal(
                request.getId(),
                request.getAppraiserId(),
                request.getAppraiserName(),
                request.getAppraisalResult(),
                request.getOpenLevel(),
                request.getReason()
            );
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 鉴定提交请求DTO
     */
    @lombok.Data
    @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定提交请求")
    public static class AppraisalSubmitRequest {
        @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定记录 ID", example = "appr-001")
        private String id;

        @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定人 ID", example = "user-123")
        private String appraiserId;

        @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定人姓名", example = "张三")
        private String appraiserName;

        @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定结果", example = "APPROVED",
                allowableValues = {"APPROVED", "REJECTED"})
        private String appraisalResult;

        @io.swagger.v3.oas.annotations.media.Schema(description = "开放等级", example = "PUBLIC",
                allowableValues = {"PUBLIC", "INTERNAL", "RESTRICTED", "CONFIDENTIAL"})
        private String openLevel;

        @io.swagger.v3.oas.annotations.media.Schema(description = "鉴定理由", example = "符合开放条件")
        private String reason;
    }
}
