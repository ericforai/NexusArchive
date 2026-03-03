// Input: ArchiveCabinetService 业务逻辑层
// Output: ArchiveCabinetController REST API
// Pos: src/main/java/com/nexusarchive/controller/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveCabinet;
import com.nexusarchive.service.warehouse.ArchiveCabinetService;
import com.nexusarchive.dto.warehouse.CabinetDTO;
import com.nexusarchive.dto.warehouse.CabinetDetailVO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.nexusarchive.common.result.Result;

import java.util.List;

/**
 * 档案柜 Controller 层
 *
 * 提供档案柜管理的 REST API 接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/warehouse/cabinets")
@RequiredArgsConstructor
public class ArchiveCabinetController {

    private final ArchiveCabinetService cabinetService;

    /**
     * 分页查询档案柜列表
     *
     * @param fondsId 全宗ID（必填）
     * @param status 状态（可选）
     * @param keyword 关键字搜索（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 档案柜分页列表
     */
    @GetMapping
    public Result<List<ArchiveCabinet>> page(
        @RequestParam(required = false) String fondsId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size
    ) {
        List<ArchiveCabinet> list = cabinetService.listByCondition(fondsId, status, keyword);
        // 简单分页（实际应使用 PageHelper）
        int start = (page - 1) * size;
        int end = Math.min(start + size, list.size());
        return Result.success(list.subList(start, end));
    }

    /**
     * 创建档案柜
     *
     * @param dto 档案柜DTO
     * @return 创建结果
     */
    @PostMapping
    public Result<CabinetDetailVO> create(@Valid @RequestBody CabinetDTO dto) {
        // 转换 DTO 为实体
        ArchiveCabinet entity = ArchiveCabinet.builder()
            .name(dto.getName())
            .location(dto.getLocation())
            .rows(dto.getRows())
            .columns(dto.getColumns())
            .rowCapacity(dto.getRowCapacity())
            .fondsId(dto.getFondsId())
            .remark(dto.getRemark())
            .build();

        ArchiveCabinet created = cabinetService.createWithValidation(entity);
        return Result.success(CabinetDetailVO.fromEntity(created));
    }

    /**
     * 更新档案柜
     *
     * @param id 档案柜ID
     * @param dto 档案柜DTO
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<CabinetDetailVO> update(
        @PathVariable Long id,
        @Valid @RequestBody CabinetDTO dto
    ) {
        // 获取现有实体
        ArchiveCabinet existing = cabinetService.getById(id);

        // 更新字段
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getLocation() != null) {
            existing.setLocation(dto.getLocation());
        }
        if (dto.getRows() != null) {
            existing.setRows(dto.getRows());
        }
        if (dto.getColumns() != null) {
            existing.setColumns(dto.getColumns());
        }
        if (dto.getRowCapacity() != null) {
            existing.setRowCapacity(dto.getRowCapacity());
        }
        if (dto.getRemark() != null) {
            existing.setRemark(dto.getRemark());
        }

        ArchiveCabinet updated = cabinetService.updateWithValidation(existing);
        return Result.success(CabinetDetailVO.fromEntity(updated));
    }

    /**
     * 获取档案柜详情
     *
     * @param id 档案柜ID
     * @return 档案柜详情（含档案袋统计和容量占用）
     */
    @GetMapping("/{id}")
    public Result<CabinetDetailVO> getDetail(@PathVariable Long id) {
        ArchiveCabinet entity = cabinetService.getById(id);
        return Result.success(ArchiveCabinet.toVO(entity));
    }

    /**
     * 删除档案柜
     *
     * @param id 档案柜ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        cabinetService.removeById(id);
        return Result.success();
    }
}
