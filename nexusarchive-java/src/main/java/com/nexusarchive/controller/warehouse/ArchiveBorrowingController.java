// Input: ArchiveBorrowingService 业务逻辑层
// Output: ArchiveBorrowingController REST API
// Pos: src/main/java/com/nexusarchive/controller/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveBorrowing;
import com.nexusarchive.service.warehouse.ArchiveBorrowingService;
import com.nexusarchive.dto.warehouse.*;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.nexusarchive.common.result.Result;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 实物借阅 Controller 层
 *
 * 提供实物借阅管理的 REST API 接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/warehouse/borrowing")
@RequiredArgsConstructor
public class ArchiveBorrowingController {

    private final ArchiveBorrowingService borrowingService;

    /**
     * 查询借阅记录列表
     *
     * @param containerId 档案袋ID（可选）
     * @param status 状态（可选）
     * @param borrower 借阅人（可选）
     * @param fondsId 全宗ID
     * @return 借阅记录列表
     */
    @GetMapping
    public Result<List<ArchiveBorrowing>> list(
        @RequestParam(required = false) Long containerId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String borrower,
        @RequestParam(required = false) Long fondsId
    ) {
        return Result.success(borrowingService.list(containerId, status, borrower, fondsId));
    }

    /**
     * 创建借阅申请
     *
     * @param dto 借阅DTO
     * @return 创建结果
     */
    @PostMapping
    public Result<ArchiveBorrowing> create(@Valid @RequestBody BorrowingDTO dto) {
        ArchiveBorrowing entity = new ArchiveBorrowing();
        entity.setContainerId(dto.getContainerId());
        entity.setBorrower(dto.getBorrower());
        entity.setBorrowerDept(dto.getBorrowerDept());
        entity.setExpectedReturnDate(dto.getExpectedReturnDate());
        entity.setFondsId(dto.getFondsId());
        entity.setRemark(dto.getRemark());

        ArchiveBorrowing created = borrowingService.create(entity);
        return Result.success("借阅申请创建成功", created);
    }

    /**
     * 获取借阅记录详情
     *
     * @param id 借阅ID
     * @return 借阅详情
     */
    @GetMapping("/{id}")
    public Result<ArchiveBorrowing> getDetail(@PathVariable Long id) {
        ArchiveBorrowing borrowing = borrowingService.getById(id);
        if (borrowing == null) {
            return Result.error("借阅记录不存在");
        }
        return Result.success(borrowing);
    }

    /**
     * 审批借阅申请
     *
     * @param id 借阅ID
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        borrowingService.approve(id, null); // TODO: 从当前用户获取
        return Result.success("借阅申请已审批");
    }

    /**
     * 确认归还
     *
     * @param id 借阅ID
     * @return 操作结果
     */
    @PostMapping("/{id}/return")
    public Result<Void> confirmReturn(@PathVariable Long id) {
        borrowingService.confirmReturn(id);
        return Result.success("档案已归还");
    }

    /**
     * 删除借阅记录
     *
     * @param id 借阅ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        borrowingService.removeById(id);
        return Result.success("借阅记录已删除");
    }

    /**
     * 查询逾期借阅列表
     *
     * @param fondsId 全宗ID
     * @return 逾期借阅列表
     */
    @GetMapping("/overdue")
    public Result<List<ArchiveBorrowing>> listOverdue(@RequestParam Long fondsId) {
        return Result.success(borrowingService.listOverdue(fondsId));
    }

    /**
     * 统计逾期数量
     *
     * @param fondsId 全宗ID
     * @return 逾期数量
     */
    @GetMapping("/overdue/count")
    public Result<Integer> countOverdue(@RequestParam Long fondsId) {
        return Result.success(borrowingService.countOverdue(fondsId));
    }

    /**
     * 获取下一个借阅单号
     *
     * @param fondsId 全宗ID
     * @return 下一个借阅单号
     */
    @GetMapping("/next-borrow-no")
    public Result<String> getNextBorrowNo(@RequestParam Long fondsId) {
        String nextNo = borrowingService.generateBorrowNo(fondsId);
        return Result.success(nextNo);
    }
}
