// Input: Lombok、Spring Security、Spring Framework、Java 标准库、等
// Output: LicenseController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @PostMapping("/load")
    // 注意：此接口需对匿名用户开放，因为系统激活时尚未登录
    @ArchivalAudit(operationType = "LOAD_LICENSE", resourceType = "LICENSE", description = "加载/切换 License")
    public Result<LicenseService.LicenseInfo> load(@RequestBody String licenseText) {
        return Result.success("License 加载成功", licenseService.validate(licenseText));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<LicenseService.LicenseInfo> current() {
        return Result.success(licenseService.current());
    }
}
