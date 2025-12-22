// Input: Lombok、Spring Security、Spring Framework、Java 标准库、等
// Output: SystemConfigController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.SystemSetting;
import com.nexusarchive.service.SystemSettingService;
import com.nexusarchive.annotation.ArchivalAudit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/settings")
@PreAuthorize("hasAuthority('manage_settings') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public Result<List<SystemSetting>> getSettings() {
        systemSettingService.initDefaultsIfEmpty();
        return Result.success(systemSettingService.listAll());
    }

    @PutMapping
    @ArchivalAudit(operationType = "UPDATE", resourceType = "SETTING", description = "更新系统配置")
    public Result<Void> updateSettings(@RequestBody Map<String, List<SystemSetting>> payload) {
        List<SystemSetting> items = payload.get("settings");
        systemSettingService.saveAll(items);
        return Result.success();
    }
}
