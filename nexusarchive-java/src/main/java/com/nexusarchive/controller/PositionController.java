package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.CreatePositionRequest;
import com.nexusarchive.dto.request.UpdatePositionRequest;
import com.nexusarchive.entity.Position;
import com.nexusarchive.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Position> create(@Validated @RequestBody CreatePositionRequest request) {
        return Result.success(positionService.create(request));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Position> update(@Validated @RequestBody UpdatePositionRequest request) {
        return Result.success(positionService.update(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Void> delete(@PathVariable String id) {
        positionService.delete(id);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('manage_positions') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    public Result<Page<Position>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return Result.success(positionService.list(page, limit, search, status));
    }
}
