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
    @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
    @ArchivalAudit(operationType = "LOAD_LICENSE", resourceType = "LICENSE", description = "加载/切换 License")
    public Result<LicenseService.LicenseInfo> load(@RequestBody String licenseText) {
        return Result.success("License 加载成功", licenseService.validate(licenseText));
    }

    @GetMapping
    public Result<LicenseService.LicenseInfo> current() {
        return Result.success(licenseService.current());
    }
}
