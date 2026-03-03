// Input: ArchiveInventoryService 业务逻辑层
// Output: ArchiveInventoryController REST API
// Pos: src/main/java/com/nexusarchive/controller/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveInventory;
import com.nexusarchive.service.warehouse.ArchiveInventoryService;
import com.nexusarchive.dto.warehouse.InventoryDTO;
import com.nexusarchive.dto.warehouse.InventoryCompleteDTO;
import com.nexusarchive.dto.warehouse.InventoryDetailDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import com.nexusarchive.common.result.Result;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 盘点任务 Controller 层
 *
 * 提供盘点任务管理的 REST API 接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/warehouse/inventory")
@RequiredArgsConstructor
public class ArchiveInventoryController {

    private final ArchiveInventoryService inventoryService;

    /**
     * 查询盘点任务列表
     *
     * @param cabinetId 档案柜ID（可选）
     * @param status 状态（可选）
     * @param fondsId 全宗ID
     * @return 盘点任务列表
     */
    @GetMapping
    public Result<List<ArchiveInventory>> list(
        @RequestParam(required = false) Long cabinetId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long fondsId
    ) {
        return Result.success(inventoryService.list(cabinetId, status, fondsId));
    }

    /**
     * 创建盘点任务
     *
     * @param dto 盘点任务DTO
     * @return 创建结果
     */
    @PostMapping
    public Result<ArchiveInventory> create(@Valid @RequestBody InventoryDTO dto) {
        ArchiveInventory entity = new ArchiveInventory();
        entity.setTaskName(dto.getTaskName());
        entity.setCabinetId(dto.getCabinetId());
        entity.setStartCabinetCode(dto.getStartCabinetCode());
        entity.setEndCabinetCode(dto.getEndCabinetCode());
        entity.setFondsId(dto.getFondsId());

        ArchiveInventory created = inventoryService.create(entity);
        return Result.success("盘点任务创建成功", created);
    }

    /**
     * 获取盘点任务详情
     *
     * @param id 盘点任务ID
     * @return 盘点任务详情
     */
    @GetMapping("/{id}")
    public Result<ArchiveInventory> getDetail(@PathVariable Long id) {
        ArchiveInventory inventory = inventoryService.getById(id);
        if (inventory == null) {
            return Result.error("盘点任务不存在");
        }
        return Result.success(inventory);
    }

    /**
     * 开始盘点任务
     *
     * @param id 盘点任务ID
     * @return 操作结果
     */
    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        inventoryService.start(id);
        return Result.success("盘点任务已开始");
    }

    /**
     * 完成盘点任务
     *
     * @param id 盘点任务ID
     * @param dto 包含总数、已盘点数、异常数
     * @return 操作结果
     */
    @PostMapping("/{id}/complete")
    public Result<Void> complete(
        @PathVariable Long id,
        @RequestBody InventoryCompleteDTO dto
    ) {
        inventoryService.complete(id, dto.getTotalContainers(),
            dto.getCheckedContainers(), dto.getAbnormalContainers());
        return Result.success("盘点任务已完成");
    }

    /**
     * 取消盘点任务
     *
     * @param id 盘点任务ID
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        inventoryService.cancel(id);
        return Result.success("盘点任务已取消");
    }

    /**
     * 删除盘点任务
     *
     * @param id 盘点任务ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        inventoryService.removeById(id);
        return Result.success("盘点任务已删除");
    }

    /**
     * 获取下一个任务号
     *
     * @param fondsId 全宗ID
     * @return 下一个任务号
     */
    @GetMapping("/next-task-no")
    public Result<String> getNextTaskNo(@RequestParam Long fondsId) {
        String nextNo = inventoryService.getNextTaskNo(fondsId);
        return Result.success(nextNo);
    }
}
