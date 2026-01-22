// Input: ArchiveReadService, DtoMapper, Result
// Output: ArchiveOperationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.ArchiveResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 档案操作控制器
 * 提供档案生命周期中的特殊操作接口（如到期查询、鉴定等）
 */
@RestController
@RequestMapping("/archive")
@RequiredArgsConstructor
@Tag(name = "Archive Operations", description = "档案特殊操作接口")
public class ArchiveOperationController {

    private final ArchiveReadService archiveService;
    private final DtoMapper dtoMapper;

    @GetMapping("/expired")
    @Operation(summary = "查询到期档案", description = "查询已过保管期限的档案列表")
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<IPage<ArchiveResponse>> listExpired(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fondsNo) {
        
        IPage<Archive> expiredPage = archiveService.getExpiredArchives(page, size, fondsNo);
        
        // Convert to DTO page
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ArchiveResponse> resultPage = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(expiredPage.getCurrent(), expiredPage.getSize(), expiredPage.getTotal());
        
        resultPage.setRecords(dtoMapper.toArchiveResponseList(expiredPage.getRecords()));
        
        return Result.success(resultPage);
    }
}
