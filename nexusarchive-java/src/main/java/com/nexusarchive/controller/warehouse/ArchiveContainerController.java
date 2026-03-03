// Input: ArchiveContainerService 业务逻辑层
// Output: ArchiveContainerController REST API
// Pos: src/main/java/com/nexusarchive/controller/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveContainer;
import com.nexusarchive.service.warehouse.ArchiveContainerService;
import com.nexusarchive.dto.warehouse.ContainerDTO;
import com.nexusarchive.dto.warehouse.ContainerDetailVO;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import com.nexusarchive.common.result.Result;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 档案袋 Controller 层
 *
 * 提供档案袋管理的 REST API 接口
 *
 * @author Claude Code
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/warehouse/containers")
@RequiredArgsConstructor
@Validated
public class ArchiveContainerController {

    private final ArchiveContainerService containerService;

    /**
     * 分页查询档案袋列表
     *
     * @param cabinetId 档案柜ID（可选）
     * @param status 状态（可选）
     * @param keyword 关键字搜索（袋号）
     * @param fondsId 全宗ID
     * @return 档案袋列表
     */
    @GetMapping
    public Result<List<ArchiveContainer>> page(
        @RequestParam(required = false) Long cabinetId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long fondsId
    ) {
        ContainerDTO dto = new ContainerDTO();
        dto.setCabinetId(cabinetId);
        dto.setStatus(status);
        dto.setKeyword(keyword);
        dto.setFondsId(fondsId);

        return Result.success(containerService.page(dto));
    }

    /**
     * 创建档案袋
     *
     * @param dto 档案袋DTO
     * @return 创建结果
     */
    @PostMapping
    public Result<ContainerDetailVO> create(@Valid @RequestBody ContainerDTO dto) {
        ArchiveContainer created = containerService.create(dto);
        return Result.success(ContainerDetailVO.fromEntity(created));
    }

    /**
     * 更新档案袋
     *
     * @param id 档案袋ID
     * @param dto 档案袋DTO
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<ContainerDetailVO> update(
        @PathVariable Long id,
        @Valid @RequestBody ContainerDTO dto
    ) {
        ArchiveContainer updated = containerService.update(id, dto);
        return Result.success("档案袋更新成功", ContainerDetailVO.fromEntity(updated));
    }

    /**
     * 获取档案袋详情
     *
     * @param id 档案袋ID
     * @return 档案袋详情（含关联案卷列表和柜信息）
     */
    @GetMapping("/{id}")
    public Result<ContainerDetailVO> getDetail(@PathVariable Long id) {
        ContainerDetailVO detail = containerService.getDetail(id);
        return Result.success(detail);
    }

    /**
     * 删除档案袋
     *
     * @param id 档案袋ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        containerService.delete(id);
        return Result.success("档案袋删除成功");
    }

    /**
     * 关联案卷到档案袋
     *
     * @param id 档案袋ID
     * @param dto 包含案卷ID列表和是否主卷标识
     * @return 操作结果
     */
    @PostMapping("/{id}/volumes")
    public Result<Void> linkVolumes(
        @PathVariable Long id,
        @RequestBody ContainerDTO dto
    ) {
        containerService.linkVolumes(id, dto.getVolumeIds(),
            dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        return Result.success("案卷关联成功");
    }

    /**
     * 解除案卷关联
     *
     * @param id 档案袋ID
     * @param dto 要删除的案卷ID列表
     * @return 操作结果
     */
    @DeleteMapping("/{id}/volumes")
    public Result<Void> unlinkVolumes(
        @PathVariable Long id,
        @RequestBody ContainerDTO dto
    ) {
        containerService.unlinkVolumes(id, dto.getVolumeIds());
        return Result.success("案卷解关联成功");
    }

    /**
     * 获取档案袋的案卷列表
     *
     * @param id 档案袋ID
     * @return 案卷列表
     */
    @GetMapping("/{id}/volumes")
    public Result<List<?>> getVolumes(@PathVariable Long id) {
        return Result.success(containerService.getVolumesByContainerId(id));
    }

    /**
     * 更新档案袋状态
     *
     * @param id 档案袋ID
     * @param status 新状态
     * @return 操作结果
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
        @PathVariable Long id,
        @RequestParam String status
    ) {
        containerService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }
}
