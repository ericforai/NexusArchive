// Input: HTTP Requests
// Output: JSON Results
// Pos: controller/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller.borrow;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
import com.nexusarchive.domain.borrow.BorrowRequestVO;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.service.borrow.BorrowRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅申请控制器
 *
 * <p>提供档案借阅申请的完整流程管理</p>
 *
 * <p>遵循深奥的简洁：只做映射，逻辑下沉到 Service。</p>
 */
@Tag(name = "档案借阅", description = """
    档案借阅申请管理接口。

    **功能说明:**
    - 提交借阅申请
    - 查询借阅申请列表
    - 审批借阅申请
    - 确认档案出库
    - 档案归还登记

    **借阅流程:**
    1. **申请**: 用户提交借阅申请
    2. **审批**: 管理员批准/拒绝申请
    3. **出库**: 确认档案出库交付
    4. **归还**: 档案使用完毕归还

    **借阅状态:**
    - PENDING: 待审批
    - APPROVED: 已批准
    - REJECTED: 已拒绝
    - BORROWED: 借阅中
    - RETURNED: 已归还
    - OVERDUE: 逾期未还

    **借阅类型:**
    - READING: 阅览室阅览
    - COPY: 复制件借阅
    - ORIGINAL: 原件借出（需特殊授权）

    **业务规则:**
    - 借阅期限默认 30 天
    - 原件借出需要额外审批
    - 机密档案限制借阅
    - 逾期自动记录日志

    **使用场景:**
    - 档案借阅管理
    - 借阅审批流程
    - 档案出库归还

    **权限要求:**
    - 提交申请: 所有认证用户
    - 审批申请: borrow:approve 权限
    - 出库归还: borrow:manage 权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/borrow/requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;

    /**
     * 分页查询借阅申请列表
     */
    @GetMapping
    @Operation(
        summary = "分页查询借阅申请",
        description = """
            分页查询借阅申请列表，支持状态和关键词过滤。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - limit: 每页条数（默认 10）
            - status: 状态过滤（可选）
            - keyword: 搜索关键词（可选，模糊匹配申请人/档案名称）

            **返回数据包括:**
            - records: 借阅申请记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **申请记录包括:**
            - id: 申请ID
            - applicantId: 申请人ID
            - applicantName: 申请人姓名
            - archiveIds: 档案ID列表
            - borrowType: 借阅类型
            - reason: 借阅事由
            - status: 申请状态
            - submitTime: 提交时间
            - approveTime: 审批时间
            - expectedReturnDate: 预计归还日期
            - actualReturnDate: 实际归还日期

            **使用场景:**
            - 借阅申请列表展示
            - 我的借阅查询
            - 待办审批查询
            """,
        operationId = "listBorrowRequests",
        tags = {"档案借阅"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<IPage<BorrowRequestVO>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "状态过滤", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词", example = "发票")
            @RequestParam(required = false) String keyword) {

        // 分页参数适配
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BorrowRequest> pageParam =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit);

        return Result.success(borrowRequestService.list(pageParam, status, keyword));
    }

    /**
     * 提交借阅申请
     */
    @PostMapping
    @Operation(
        summary = "提交借阅申请",
        description = """
            用户提交档案借阅申请。

            **请求参数:**
            - archiveIds: 档案ID列表（必填）
            - borrowType: 借阅类型（READING/COPY/ORIGINAL，默认 READING）
            - reason: 借阅事由（必填）
            - expectedDays: 预计借阅天数（可选，默认 30 天）

            **返回数据包括:**
            - id: 申请ID
            - status: 申请状态（自动设为 PENDING）
            - submitTime: 提交时间
            - expectedReturnDate: 预计归还日期

            **业务规则:**
            - 自动创建待审批申请
            - 原件借阅需要特殊权限
            - 机密档案需要额外审批
            - 借阅期限不超过 90 天

            **使用场景:**
            - 用户提交借阅申请
            - 档案借阅发起
            """,
        operationId = "submitBorrowRequest",
        tags = {"档案借阅"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "申请提交成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或无权借阅"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowRequest> submit(
            @Parameter(description = "借阅申请信息", required = true)
            @RequestBody SubmitBorrowRequestCommand command) {
        return Result.success(borrowRequestService.submit(command));
    }

    /**
     * 审批借阅申请
     */
    @PostMapping("/{id}/approve")
    @Operation(
        summary = "审批借阅申请",
        description = """
            管理员审批借阅申请（批准或拒绝）。

            **路径参数:**
            - id: 借阅申请ID

            **请求参数:**
            - approverId: 审批人ID
            - approverName: 审批人姓名
            - approved: 是否批准（true=批准，false=拒绝）
            - comment: 审批意见（可选）

            **业务规则:**
            - 批准后状态变为 APPROVED
            - 拒绝后状态变为 REJECTED
            - 拒绝必须填写审批意见
            - 审批操作记录审计日志

            **使用场景:**
            - 借阅审批处理
            - 批量审批操作
            """,
        operationId = "approveBorrowRequest",
        tags = {"档案借阅"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "审批完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无审批权限"),
        @ApiResponse(responseCode = "404", description = "申请不存在")
    })
    @PreAuthorize("hasAuthority('borrow:approve')")
    public Result<Void> approve(
            @Parameter(description = "借阅申请ID", required = true, example = "borrow-001")
            @PathVariable String id,
            @Parameter(description = "审批信息", required = true)
            @RequestBody ApproveBorrowRequestCommand command) {
        // 确保 ID 一致
        ApproveBorrowRequestCommand finalCommand = new ApproveBorrowRequestCommand(
            id, command.approverId(), command.approverName(), command.approved(), command.comment()
        );
        borrowRequestService.approve(finalCommand);
        return Result.success();
    }

    /**
     * 确认档案出库
     */
    @PostMapping("/{id}/confirm-out")
    @Operation(
        summary = "确认档案出库",
        description = """
            确认批准的申请档案出库交付给借阅人。

            **路径参数:**
            - id: 借阅申请ID

            **业务规则:**
            - 仅批准后的申请可出库
            - 出库后状态变为 BORROWED
            - 记录出库时间和操作人
            - 更新预计归还日期

            **使用场景:**
            - 档案出库登记
            - 借阅交付确认
            """,
        operationId = "confirmBorrowOut",
        tags = {"档案借阅"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "出库确认成功"),
        @ApiResponse(responseCode = "400", description = "申请状态不允许出库"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无出库权限"),
        @ApiResponse(responseCode = "404", description = "申请不存在")
    })
    @PreAuthorize("hasAuthority('borrow:manage')")
    public Result<Void> confirmOut(
            @Parameter(description = "借阅申请ID", required = true, example = "borrow-001")
            @PathVariable String id) {
        borrowRequestService.confirmOut(id);
        return Result.success();
    }

    /**
     * 档案归还登记
     */
    @PostMapping("/{id}/return")
    @Operation(
        summary = "档案归还登记",
        description = """
            登记借阅档案的归还。

            **路径参数:**
            - id: 借阅申请ID

            **请求参数:**
            - operatorId: 归还操作人ID

            **业务规则:**
            - 归还后状态变为 RETURNED
            - 记录实际归还时间
            - 逾期归还自动记录
            - 检查档案完整性

            **使用场景:**
            - 档案归还登记
            - 逾期处理
            """,
        operationId = "returnArchives",
        tags = {"档案借阅"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "归还登记成功"),
        @ApiResponse(responseCode = "400", description = "申请状态不允许归还"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无归还权限"),
        @ApiResponse(responseCode = "404", description = "申请不存在")
    })
    @PreAuthorize("hasAuthority('borrow:manage')")
    public Result<Void> returnArchives(
            @Parameter(description = "借阅申请ID", required = true, example = "borrow-001")
            @PathVariable String id,
            @Parameter(description = "归还操作人ID", required = true, example = "user-001")
            @RequestParam String operatorId) {
        borrowRequestService.returnArchives(id, operatorId);
        return Result.success();
    }
}
