// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: BasFondsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.service.BasFondsService;
import com.nexusarchive.service.DataScopeService;
import com.nexusarchive.service.FondsScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bas/fonds")
@RequiredArgsConstructor
public class BasFondsController {

    private final BasFondsService basFondsService;
    private final FondsScopeService fondsScopeService;

    @GetMapping("/page")
    public Result<Page<BasFonds>> getPage(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int limit) {
        Page<BasFonds> pageParam = new Page<>(page, limit);
        return Result.success(basFondsService.page(pageParam));
    }

    @GetMapping("/list")
    public Result<List<BasFonds>> list() {
        // 从当前认证用户获取允许访问的全宗号列表
        List<String> allowedFonds = resolveAllowedFonds();

        // null 表示未认证或无法解析用户，返回空列表
        if (allowedFonds == null) {
            return Result.success(Collections.emptyList());
        }

        // 空列表表示用户没有任何全宗权限，返回空列表
        if (allowedFonds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 有权限：只返回用户有权限的全宗
        LambdaQueryWrapper<BasFonds> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BasFonds::getFondsCode, allowedFonds);
        return Result.success(basFondsService.list(wrapper));
    }

    /**
     * 解析当前用户允许访问的全宗号列表
     */
    private List<String> resolveAllowedFonds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            String userId = null;
            if (principal instanceof String) {
                userId = (String) principal;
            } else if (principal instanceof com.nexusarchive.security.CustomUserDetails userDetails) {
                userId = userDetails.getId();
            }
            if (userId != null) {
                return fondsScopeService.getAllowedFonds(userId);
            }
        }
        return null;
    }

    /**
     * 检查全宗号是否可以修改
     * @param id 全宗ID
     * @return canModify = true 表示可修改
     */
    @GetMapping("/{id}/can-modify")
    public Result<Boolean> canModify(@PathVariable String id) {
        BasFonds fonds = basFondsService.getById(id);
        if (fonds == null) {
            return Result.error("全宗不存在");
        }
        return Result.success(basFondsService.canModifyFondsCode(fonds.getFondsCode()));
    }

    @PostMapping
    public Result<Boolean> save(@Valid @RequestBody BasFonds fonds) {
        if (fonds.getFondsCode() == null || fonds.getFondsName() == null) {
            return Result.error("Fonds Code and Name are required");
        }
        return Result.success(basFondsService.save(fonds));
    }

    @PutMapping
    public Result<Boolean> update(@Valid @RequestBody BasFonds fonds) {
        // 使用带约束的更新方法
        return Result.success(basFondsService.updateFonds(fonds));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable String id) {
        return Result.success(basFondsService.removeById(id));
    }
}
