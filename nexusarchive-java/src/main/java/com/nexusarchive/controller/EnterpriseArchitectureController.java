// Input: Spring Web、EnterpriseArchitectureService、Result
// Output: EnterpriseArchitectureController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.EnterpriseArchitectureTree;
import com.nexusarchive.service.EnterpriseArchitectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 集团架构控制器
 * 
 * 功能: 提供集团架构树视图数据（法人 -> 全宗 -> 档案）
 */
@Slf4j
@RestController
@RequestMapping("/enterprise-architecture")
@RequiredArgsConstructor
@Tag(name = "集团架构", description = "集团架构树视图接口")
public class EnterpriseArchitectureController {
    
    private final EnterpriseArchitectureService architectureService;
    
    @GetMapping("/tree")
    @Operation(summary = "获取完整的集团架构树")
    @PreAuthorize("hasAnyAuthority('entity:view', 'fonds:view') or hasRole('super_admin')")
    public Result<EnterpriseArchitectureTree> getArchitectureTree() {
        try {
            EnterpriseArchitectureTree tree = architectureService.getArchitectureTree();
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取集团架构树失败", e);
            return Result.error("获取集团架构树失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/tree/entity/{entityId}")
    @Operation(summary = "获取指定法人下的架构树")
    @PreAuthorize("hasAnyAuthority('entity:view', 'fonds:view') or hasRole('super_admin')")
    public Result<EnterpriseArchitectureTree> getArchitectureTreeByEntity(@PathVariable String entityId) {
        try {
            EnterpriseArchitectureTree tree = architectureService.getArchitectureTreeByEntity(entityId);
            return Result.success(tree);
        } catch (Exception e) {
            log.error("获取法人架构树失败: entityId={}", entityId, e);
            return Result.error("获取法人架构树失败: " + e.getMessage());
        }
    }
}

