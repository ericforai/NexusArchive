// Input: Spring Framework、MyBatis-Plus、Java 标准库
// Output: OnboardingController API
// Pos: 匹配引擎/Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.engine.matching.OnboardingService;
import com.nexusarchive.engine.matching.dto.AutoMappingResult;
import com.nexusarchive.engine.matching.dto.MappingConfirmation;
import com.nexusarchive.engine.matching.dto.OnboardingSummary;
import com.nexusarchive.engine.matching.dto.UnmatchedItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

/**
 * 初始化向导 API
 * 
 * 帮助客户快速完成科目角色和单据类型映射配置
 */
@RestController
@RequestMapping("/matching/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    
    private final OnboardingService onboardingService;
    
    /**
     * Step 1: 扫描客户现有数据
     */
    @PostMapping("/scan/{companyId}")
    public Result<OnboardingSummary> scanExistingData(@PathVariable Long companyId) {
        OnboardingSummary summary = onboardingService.scanExistingData(companyId);
        return Result.success(summary);
    }
    
    /**
     * Step 2: 应用预置规则并返回自动猜测结果
     */
    @PostMapping("/apply-preset/{companyId}")
    public Result<AutoMappingResult> applyPreset(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "KIT_GENERAL") String kitId) {
        AutoMappingResult result = onboardingService.applyPreset(companyId, kitId);
        return Result.success(result);
    }
    
    /**
     * Step 3: 获取需要人工确认的项
     */
    @GetMapping("/pending/{companyId}")
    public Result<List<UnmatchedItem>> getPendingItems(@PathVariable Long companyId) {
        List<UnmatchedItem> items = onboardingService.getPendingItems(companyId);
        return Result.success(items);
    }
    
    /**
     * Step 4: 确认映射
     */
    @PostMapping("/confirm/{companyId}")
    public Result<Void> confirmMappings(
            @PathVariable Long companyId,
            @Valid @RequestBody List<MappingConfirmation> mappings) {
        onboardingService.confirmMappings(companyId, mappings);
        return Result.success();
    }
}
